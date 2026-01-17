package com.finuts.domain.usecase

import co.touchlab.kermit.Logger
import com.finuts.data.import.FuzzyDuplicateDetector
import com.finuts.data.import.ImportValidator
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.entity.import.CategorizationTier
import com.finuts.domain.entity.import.DuplicateStatus
import com.finuts.domain.entity.import.ImportPreviewResult
import com.finuts.domain.entity.import.ImportProgress
import com.finuts.domain.entity.import.ImportResult
import com.finuts.domain.entity.import.ImportedTransaction
import com.finuts.domain.entity.import.ReviewableTransaction
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Orchestrator for the import process.
 * Coordinates validation, duplicate detection, categorization, and saving transactions.
 */
class ImportTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val duplicateDetector: FuzzyDuplicateDetector,
    private val validator: ImportValidator,
    private val categorizationUseCase: CategorizePendingTransactionsUseCase,
    private val categoryResolver: CategoryResolver
) {
    private val log = Logger.withTag("ImportTransactionsUseCase")

    private val _progress = MutableStateFlow<ImportProgress>(ImportProgress.Idle)
    val progress: StateFlow<ImportProgress> = _progress.asStateFlow()

    private var currentPreview: ImportPreviewResult? = null

    /**
     * Start the import process with parse results.
     *
     * @param parseResult The result from parsing the import file
     * @param targetAccountId The account to import transactions into
     * @return Result containing the preview for user confirmation
     */
    suspend fun startImport(
        parseResult: ImportResult,
        targetAccountId: String
    ): Result<ImportPreviewResult> {
        log.i { "startImport: parseResult=${parseResult::class.simpleName}, targetAccountId=$targetAccountId" }
        return when (parseResult) {
            is ImportResult.Success -> {
                log.d { "startImport: Success with ${parseResult.transactions.size} transactions" }
                processSuccessResult(parseResult, targetAccountId)
            }
            is ImportResult.Error -> {
                log.e { "startImport: Error - ${parseResult.message}" }
                Result.failure(ImportException(parseResult.message))
            }
            is ImportResult.NeedsUserInput -> {
                log.d { "startImport: NeedsUserInput with ${parseResult.transactions.size} transactions" }
                processSuccessResult(
                    ImportResult.Success(
                        transactions = parseResult.transactions,
                        documentType = parseResult.documentType,
                        totalConfidence = 0.5f
                    ),
                    targetAccountId
                )
            }
        }
    }

    private suspend fun processSuccessResult(
        parseResult: ImportResult.Success,
        targetAccountId: String
    ): Result<ImportPreviewResult> {
        log.d { "processSuccessResult: START with ${parseResult.transactions.size} transactions" }
        val transactions = parseResult.transactions

        // Step 1: Validate
        log.d { "processSuccessResult: Step 1 - Validating..." }
        _progress.value = ImportProgress.Validating(
            totalTransactions = transactions.size,
            processedCount = 0
        )
        val validationResult = validator.validate(transactions)
        log.d { "processSuccessResult: Validation complete, warnings=${validationResult.warnings.size}" }
        _progress.value = ImportProgress.Validating(
            totalTransactions = transactions.size,
            processedCount = transactions.size
        )

        // Step 2: Deduplicate
        log.d { "processSuccessResult: Step 2 - Deduplicating..." }
        _progress.value = ImportProgress.Deduplicating(
            totalTransactions = transactions.size,
            processedCount = 0,
            duplicatesFound = 0
        )

        val existingTransactions = transactionRepository
            .getTransactionsByAccount(targetAccountId)
            .first()
        log.d { "processSuccessResult: Found ${existingTransactions.size} existing transactions in account" }

        val duplicateStatuses = duplicateDetector.checkDuplicates(
            imported = transactions,
            existingTransactions = existingTransactions
        )

        val duplicateCount = duplicateStatuses.values.count { it.isDuplicate }
        log.d { "processSuccessResult: Found $duplicateCount duplicates" }
        _progress.value = ImportProgress.Deduplicating(
            totalTransactions = transactions.size,
            processedCount = transactions.size,
            duplicatesFound = duplicateCount
        )

        // Step 3: Categorize
        log.d { "processSuccessResult: Step 3 - Categorizing..." }
        _progress.value = ImportProgress.Categorizing(
            totalTransactions = transactions.size,
            categorizedCount = 0,
            currentTier = CategorizationTier.RULE_BASED
        )

        val categoryResults = categorizeTransactions(transactions)
        log.d { "processSuccessResult: Categorized ${categoryResults.size} transactions" }

        _progress.value = ImportProgress.Categorizing(
            totalTransactions = transactions.size,
            categorizedCount = categoryResults.size,
            currentTier = CategorizationTier.RULE_BASED
        )

        // Build reviewable transactions with AI categories applied
        val reviewableTransactions = transactions.mapIndexed { index, transaction ->
            val duplicateStatus = duplicateStatuses[index] ?: DuplicateStatus.Unique
            val aiCategory = categoryResults[index]

            // Apply AI category only if not already set
            val categorizedTx = if (aiCategory != null && transaction.category == null) {
                transaction.copy(category = aiCategory)
            } else {
                transaction
            }

            ReviewableTransaction(
                index = index,
                transaction = categorizedTx,
                duplicateStatus = duplicateStatus,
                isSelected = !duplicateStatus.isDuplicate,
                categoryOverride = null
            )
        }

        val preview = ImportPreviewResult(
            transactions = reviewableTransactions,
            documentType = parseResult.documentType,
            duplicateCount = duplicateCount,
            validationWarnings = validationResult.warnings
        )

        currentPreview = preview
        log.i { "processSuccessResult: currentPreview SET with ${reviewableTransactions.size} transactions" }
        _progress.value = ImportProgress.AwaitingConfirmation(preview)
        log.d { "processSuccessResult: Progress set to AwaitingConfirmation" }

        return Result.success(preview)
    }

    /**
     * Confirm the import and save selected transactions.
     *
     * @param selectedIndices Indices of transactions to import
     * @param categoryOverrides Map of index to category ID overrides
     * @param targetAccountId The account to save transactions to
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun confirmImport(
        selectedIndices: Set<Int>,
        categoryOverrides: Map<Int, String>,
        targetAccountId: String
    ): Result<ImportConfirmationResult> {
        log.i { "confirmImport: selectedIndices=${selectedIndices.size}, targetAccountId=$targetAccountId" }
        log.d { "confirmImport: currentPreview is ${if (currentPreview != null) "SET" else "NULL"}" }

        val preview = currentPreview
        if (preview == null) {
            log.e { "confirmImport: FAILED - No import in progress (currentPreview is null)" }
            return Result.failure(ImportException("No import in progress"))
        }

        log.d { "confirmImport: preview has ${preview.transactions.size} transactions" }

        val selectedTransactions = preview.transactions
            .filter { it.index in selectedIndices }

        log.d { "confirmImport: ${selectedTransactions.size} transactions selected for import" }

        _progress.value = ImportProgress.Saving(
            totalTransactions = selectedTransactions.size,
            savedCount = 0
        )
        log.d { "confirmImport: Progress set to Saving" }

        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )

        // Ensure all categories exist before saving (prevents FOREIGN KEY errors)
        // IMPORTANT: Must use explicit for loop with suspend calls, not .map {} or buildList {}
        val transactionsToSave = mutableListOf<Transaction>()
        for (reviewable in selectedTransactions) {
            val rawCategoryId = categoryOverrides[reviewable.index]
                ?: reviewable.transaction.category
            val imported = reviewable.transaction

            // Resolve category - creates if missing, falls back to "other" if unknown
            val resolvedCategoryId = if (rawCategoryId != null) {
                log.d { "confirmImport: resolving category '$rawCategoryId' for ${imported.description?.take(30)}" }
                categoryResolver.ensureExists(rawCategoryId)
            } else {
                null
            }

            transactionsToSave.add(
                Transaction(
                    id = Uuid.random().toString(),
                    accountId = targetAccountId,
                    date = imported.date.atStartOfDayIn(TimeZone.currentSystemDefault()),
                    amount = imported.amount,
                    description = imported.description,
                    type = if (imported.amount >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
                    categoryId = resolvedCategoryId,
                    merchant = imported.merchant,
                    note = null,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        log.d { "confirmImport: Saving ${transactionsToSave.size} transactions to repository..." }

        try {
            log.i { "confirmImport: Starting batch save of ${transactionsToSave.size} transactions" }
            transactionsToSave.forEachIndexed { index, transaction ->
                log.d { "confirmImport: Saving ${index + 1}/${transactionsToSave.size}: ${transaction.description?.take(30)}, categoryId=${transaction.categoryId}" }
                try {
                    transactionRepository.createTransaction(transaction)
                    log.d { "confirmImport: Saved ${index + 1} OK" }
                } catch (inner: Exception) {
                    log.e(inner) { "confirmImport: FAILED to save tx ${index + 1}: ${inner::class.simpleName} - ${inner.message}" }
                    throw inner
                }
            }
            log.i { "confirmImport: All ${transactionsToSave.size} transactions saved successfully" }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown error (${e::class.simpleName})"
            log.e(e) { "confirmImport: EXCEPTION during save - class=${e::class.simpleName}, message=$errorMsg" }
            _progress.value = ImportProgress.Failed(
                message = errorMsg,
                recoverable = true,
                partialResult = preview
            )
            return Result.failure(ImportException("Failed to save transactions: $errorMsg"))
        }

        val skippedCount = preview.transactions.size - selectedTransactions.size
        val duplicateCount = preview.duplicateCount

        log.i { "confirmImport: SUCCESS - saved=${transactionsToSave.size}, skipped=$skippedCount, duplicates=$duplicateCount" }

        _progress.value = ImportProgress.Completed(
            savedCount = transactionsToSave.size,
            skippedCount = skippedCount,
            duplicateCount = duplicateCount
        )
        log.d { "confirmImport: Progress set to Completed" }

        return Result.success(
            ImportConfirmationResult(
                savedCount = transactionsToSave.size,
                skippedCount = skippedCount
            )
        )
    }

    /**
     * Cancel the current import.
     */
    fun cancelImport() {
        log.i { "cancelImport: Cancelling import, clearing currentPreview" }
        currentPreview = null
        _progress.value = ImportProgress.Cancelled
    }

    /**
     * Reset to idle state.
     */
    fun reset() {
        log.i { "reset: Resetting to Idle, clearing currentPreview" }
        currentPreview = null
        _progress.value = ImportProgress.Idle
    }

    /**
     * Categorize imported transactions using all tiers (Tier 1 + Tier 2 AI if available).
     * @return Map of transaction index to category ID
     */
    private suspend fun categorizeTransactions(
        transactions: List<ImportedTransaction>
    ): Map<Int, String> {
        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )

        // Convert imported transactions to domain format for categorization
        val tempTransactions = transactions.mapIndexed { idx, tx ->
            Transaction(
                id = "import-$idx",
                accountId = "",
                date = now,
                amount = tx.amount,
                description = tx.description,
                type = if (tx.amount >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
                categoryId = null,
                merchant = tx.merchant,
                note = null,
                createdAt = now,
                updatedAt = now
            )
        }

        // Use categorizeAll() to include Tier 2 AI when available
        val batchResult = categorizationUseCase.categorizeAll(tempTransactions)
        log.d { "categorizeTransactions: Tier1=${batchResult.localCount}, Tier2=${batchResult.tier2Count}, uncategorized=${batchResult.totalUncategorized}" }

        return batchResult.results
            .filter { it.categoryId != null }
            .associate {
                val index = it.transactionId.removePrefix("import-").toIntOrNull() ?: -1
                index to it.categoryId!!
            }
            .filterKeys { it >= 0 }
    }
}

/**
 * Result of confirming an import.
 */
data class ImportConfirmationResult(
    val savedCount: Int,
    val skippedCount: Int
)

/**
 * Exception thrown during import process.
 */
class ImportException(message: String) : Exception(message)

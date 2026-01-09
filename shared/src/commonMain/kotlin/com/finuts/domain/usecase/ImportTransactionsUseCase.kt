package com.finuts.domain.usecase

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
    private val categorizationUseCase: CategorizePendingTransactionsUseCase
) {
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
        return when (parseResult) {
            is ImportResult.Success -> processSuccessResult(parseResult, targetAccountId)
            is ImportResult.Error -> Result.failure(
                ImportException(parseResult.message)
            )
            is ImportResult.NeedsUserInput -> processSuccessResult(
                ImportResult.Success(
                    transactions = parseResult.transactions,
                    documentType = parseResult.documentType,
                    totalConfidence = 0.5f
                ),
                targetAccountId
            )
        }
    }

    private suspend fun processSuccessResult(
        parseResult: ImportResult.Success,
        targetAccountId: String
    ): Result<ImportPreviewResult> {
        val transactions = parseResult.transactions

        // Step 1: Validate
        _progress.value = ImportProgress.Validating(
            totalTransactions = transactions.size,
            processedCount = 0
        )
        val validationResult = validator.validate(transactions)
        _progress.value = ImportProgress.Validating(
            totalTransactions = transactions.size,
            processedCount = transactions.size
        )

        // Step 2: Deduplicate
        _progress.value = ImportProgress.Deduplicating(
            totalTransactions = transactions.size,
            processedCount = 0,
            duplicatesFound = 0
        )

        val existingTransactions = transactionRepository
            .getTransactionsByAccount(targetAccountId)
            .first()

        val duplicateStatuses = duplicateDetector.checkDuplicates(
            imported = transactions,
            existingTransactions = existingTransactions
        )

        val duplicateCount = duplicateStatuses.values.count { it.isDuplicate }
        _progress.value = ImportProgress.Deduplicating(
            totalTransactions = transactions.size,
            processedCount = transactions.size,
            duplicatesFound = duplicateCount
        )

        // Step 3: Categorize
        _progress.value = ImportProgress.Categorizing(
            totalTransactions = transactions.size,
            categorizedCount = 0,
            currentTier = CategorizationTier.RULE_BASED
        )

        val categoryResults = categorizeTransactions(transactions)

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
        _progress.value = ImportProgress.AwaitingConfirmation(preview)

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
        val preview = currentPreview ?: return Result.failure(
            ImportException("No import in progress")
        )

        val selectedTransactions = preview.transactions
            .filter { it.index in selectedIndices }

        _progress.value = ImportProgress.Saving(
            totalTransactions = selectedTransactions.size,
            savedCount = 0
        )

        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )

        val transactionsToSave = selectedTransactions.map { reviewable ->
            val categoryId = categoryOverrides[reviewable.index]
                ?: reviewable.transaction.category
            val imported = reviewable.transaction

            Transaction(
                id = Uuid.random().toString(),
                accountId = targetAccountId,
                date = imported.date.atStartOfDayIn(TimeZone.currentSystemDefault()),
                amount = imported.amount,
                description = imported.description,
                type = if (imported.amount >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
                categoryId = categoryId,
                merchant = imported.merchant,
                note = null,
                createdAt = now,
                updatedAt = now
            )
        }

        transactionsToSave.forEach { transaction ->
            transactionRepository.createTransaction(transaction)
        }

        val skippedCount = preview.transactions.size - selectedTransactions.size
        val duplicateCount = preview.duplicateCount

        _progress.value = ImportProgress.Completed(
            savedCount = transactionsToSave.size,
            skippedCount = skippedCount,
            duplicateCount = duplicateCount
        )

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
        currentPreview = null
        _progress.value = ImportProgress.Cancelled
    }

    /**
     * Reset to idle state.
     */
    fun reset() {
        currentPreview = null
        _progress.value = ImportProgress.Idle
    }

    /**
     * Categorize imported transactions using Tier 1 (rule-based, free).
     * @return Map of transaction index to category ID
     */
    private fun categorizeTransactions(
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

        val result = categorizationUseCase.categorizeTier1(tempTransactions)

        return result
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

package com.finuts.domain.usecase

import app.cash.turbine.test
import com.finuts.data.categorization.MerchantDatabase
import com.finuts.data.categorization.RuleBasedCategorizer
import com.finuts.data.import.FuzzyDuplicateDetector
import com.finuts.data.import.ImportValidator
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategoryType
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.registry.IconRegistry
import com.finuts.domain.repository.CategoryRepository
import com.finuts.domain.entity.import.CategorizationTier
import com.finuts.domain.entity.import.DocumentType
import com.finuts.domain.entity.import.DuplicateStatus
import com.finuts.domain.entity.import.ImportProgress
import com.finuts.domain.entity.import.ImportSource
import com.finuts.domain.entity.import.ImportedTransaction
import com.finuts.domain.entity.import.ImportResult
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ImportTransactionsUseCase.
 * Tests the orchestration of the import pipeline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportTransactionsUseCaseTest {

    private val fakeTransactionRepository = FakeTransactionRepository()
    private val fakeCategoryRepository = FakeCategoryRepository()
    private val duplicateDetector = FuzzyDuplicateDetector()
    private val validator = ImportValidator()

    // Categorization dependencies (Tier 1 only - rule-based, free)
    private val merchantDatabase = MerchantDatabase()
    private val ruleBasedCategorizer = RuleBasedCategorizer(merchantDatabase)
    private val categorizationUseCase = CategorizePendingTransactionsUseCase(
        ruleBasedCategorizer = ruleBasedCategorizer,
        aiCategorizer = null
    )

    // Category management
    private val iconRegistry = IconRegistry()
    private val categoryResolver = CategoryResolver(
        categoryRepository = fakeCategoryRepository,
        iconRegistry = iconRegistry
    )

    private val useCase = ImportTransactionsUseCase(
        transactionRepository = fakeTransactionRepository,
        duplicateDetector = duplicateDetector,
        validator = validator,
        categorizationUseCase = categorizationUseCase,
        categoryResolver = categoryResolver
    )

    private fun createImportedTransaction(
        date: LocalDate = LocalDate(2026, 1, 8),
        amount: Long = -5000L,
        description: String = "Test Transaction"
    ) = ImportedTransaction(
        date = date,
        amount = amount,
        description = description,
        confidence = 0.9f,
        source = ImportSource.RULE_BASED
    )

    // === Progress Flow Tests ===

    @Test
    fun `progress starts with Idle`() = runTest {
        useCase.progress.test {
            assertEquals(ImportProgress.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startImport emits DetectingFormat as first progress`() = runTest {
        val parseResult = ImportResult.Success(
            transactions = listOf(createImportedTransaction()),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        useCase.progress.test {
            assertEquals(ImportProgress.Idle, awaitItem())

            useCase.startImport(
                parseResult = parseResult,
                targetAccountId = "acc-1"
            )

            val detecting = awaitItem()
            assertIs<ImportProgress.Validating>(detecting)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startImport includes Deduplicating in progress`() = runTest {
        val parseResult = ImportResult.Success(
            transactions = listOf(createImportedTransaction()),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        useCase.progress.test {
            skipItems(1) // Idle

            useCase.startImport(
                parseResult = parseResult,
                targetAccountId = "acc-1"
            )

            // Collect all progress updates until AwaitingConfirmation
            val progressStates = mutableListOf<ImportProgress>()
            var item = awaitItem()
            while (item !is ImportProgress.AwaitingConfirmation) {
                progressStates.add(item)
                item = awaitItem()
            }

            // Verify Deduplicating was in the progress
            assertTrue(progressStates.any { it is ImportProgress.Deduplicating })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startImport emits AwaitingConfirmation at end`() = runTest {
        val parseResult = ImportResult.Success(
            transactions = listOf(createImportedTransaction()),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        useCase.progress.test {
            skipItems(1) // Idle

            val result = useCase.startImport(
                parseResult = parseResult,
                targetAccountId = "acc-1"
            )

            // Skip intermediate steps
            var lastItem = awaitItem()
            while (lastItem !is ImportProgress.AwaitingConfirmation) {
                lastItem = awaitItem()
            }

            assertIs<ImportProgress.AwaitingConfirmation>(lastItem)
            assertTrue(result.isSuccess)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === Duplicate Detection Tests ===

    @Test
    fun `startImport detects duplicates from existing transactions`() = runTest {
        // Add existing transaction
        fakeTransactionRepository.addTransaction(
            createExistingTransaction(description = "Test Transaction")
        )

        val parseResult = ImportResult.Success(
            transactions = listOf(createImportedTransaction(description = "Test Transaction")),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        val result = useCase.startImport(
            parseResult = parseResult,
            targetAccountId = "acc-1"
        )

        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        assertTrue(preview.duplicateCount > 0)
    }

    @Test
    fun `startImport marks unique transactions as selected`() = runTest {
        val parseResult = ImportResult.Success(
            transactions = listOf(createImportedTransaction(description = "Unique Transaction")),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        val result = useCase.startImport(
            parseResult = parseResult,
            targetAccountId = "acc-1"
        )

        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        assertTrue(preview.transactions.first().isSelected)
    }

    @Test
    fun `startImport marks exact duplicates as not selected`() = runTest {
        fakeTransactionRepository.addTransaction(
            createExistingTransaction(description = "Duplicate")
        )

        val parseResult = ImportResult.Success(
            transactions = listOf(createImportedTransaction(description = "Duplicate")),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        val result = useCase.startImport(
            parseResult = parseResult,
            targetAccountId = "acc-1"
        )

        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        // Exact duplicates should be deselected
        assertTrue(preview.transactions.any { !it.isSelected })
    }

    // === Validation Tests ===

    @Test
    fun `startImport collects validation warnings`() = runTest {
        val futureDate = LocalDate(2028, 1, 1)
        val parseResult = ImportResult.Success(
            transactions = listOf(createImportedTransaction(date = futureDate)),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        val result = useCase.startImport(
            parseResult = parseResult,
            targetAccountId = "acc-1"
        )

        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        assertTrue(preview.hasWarnings)
    }

    // === Confirm Import Tests ===

    @Test
    fun `confirmImport saves selected transactions`() = runTest {
        val parseResult = ImportResult.Success(
            transactions = listOf(
                createImportedTransaction(description = "Transaction 1"),
                createImportedTransaction(description = "Transaction 2")
            ),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        val previewResult = useCase.startImport(
            parseResult = parseResult,
            targetAccountId = "acc-1"
        )
        assertTrue(previewResult.isSuccess)

        val confirmResult = useCase.confirmImport(
            selectedIndices = setOf(0, 1),
            categoryOverrides = emptyMap(),
            targetAccountId = "acc-1"
        )

        assertTrue(confirmResult.isSuccess)
        assertEquals(2, fakeTransactionRepository.savedTransactions.size)
    }

    @Test
    fun `confirmImport respects selectedIndices`() = runTest {
        val parseResult = ImportResult.Success(
            transactions = listOf(
                createImportedTransaction(description = "Keep This"),
                createImportedTransaction(description = "Skip This")
            ),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        useCase.startImport(parseResult = parseResult, targetAccountId = "acc-1")

        val confirmResult = useCase.confirmImport(
            selectedIndices = setOf(0), // Only first one
            categoryOverrides = emptyMap(),
            targetAccountId = "acc-1"
        )

        assertTrue(confirmResult.isSuccess)
        assertEquals(1, fakeTransactionRepository.savedTransactions.size)
        assertEquals("Keep This", fakeTransactionRepository.savedTransactions.first().description)
    }

    @Test
    fun `confirmImport applies category overrides`() = runTest {
        val parseResult = ImportResult.Success(
            transactions = listOf(createImportedTransaction(description = "Test")),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        useCase.startImport(parseResult = parseResult, targetAccountId = "acc-1")

        val confirmResult = useCase.confirmImport(
            selectedIndices = setOf(0),
            categoryOverrides = mapOf(0 to "entertainment"),
            targetAccountId = "acc-1"
        )

        assertTrue(confirmResult.isSuccess)
        assertEquals("entertainment", fakeTransactionRepository.savedTransactions.first().categoryId)
    }

    @Test
    fun `confirmImport emits Completed progress`() = runTest {
        val parseResult = ImportResult.Success(
            transactions = listOf(createImportedTransaction()),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        useCase.progress.test {
            skipItems(1) // Idle

            useCase.startImport(parseResult = parseResult, targetAccountId = "acc-1")

            // Wait for AwaitingConfirmation
            var item = awaitItem()
            while (item !is ImportProgress.AwaitingConfirmation) {
                item = awaitItem()
            }

            useCase.confirmImport(
                selectedIndices = setOf(0),
                categoryOverrides = emptyMap(),
                targetAccountId = "acc-1"
            )

            // Find Completed
            item = awaitItem()
            while (item !is ImportProgress.Completed) {
                item = awaitItem()
            }

            assertIs<ImportProgress.Completed>(item)
            assertEquals(1, item.savedCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === Cancel/Reset Tests ===

    @Test
    fun `cancelImport emits Cancelled progress`() = runTest {
        useCase.progress.test {
            skipItems(1) // Idle

            useCase.cancelImport()

            val cancelled = awaitItem()
            assertIs<ImportProgress.Cancelled>(cancelled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reset returns to Idle state`() = runTest {
        useCase.progress.test {
            assertEquals(ImportProgress.Idle, awaitItem())

            useCase.cancelImport()
            assertIs<ImportProgress.Cancelled>(awaitItem())

            useCase.reset()
            assertEquals(ImportProgress.Idle, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // === Error Handling Tests ===

    @Test
    fun `handles parse error result`() = runTest {
        val parseResult = ImportResult.Error(
            message = "Parse failed",
            documentType = null,
            partialTransactions = emptyList()
        )

        val result = useCase.startImport(
            parseResult = parseResult,
            targetAccountId = "acc-1"
        )

        assertTrue(result.isFailure)
    }

    // === Categorization Tests ===

    @Test
    fun `startImport emits Categorizing progress`() = runTest {
        val parseResult = ImportResult.Success(
            transactions = listOf(createImportedTransaction(description = "MAGNUM SUPERMARKET")),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        useCase.progress.test {
            skipItems(1) // Idle

            useCase.startImport(
                parseResult = parseResult,
                targetAccountId = "acc-1"
            )

            // Collect all progress updates until AwaitingConfirmation
            val progressStates = mutableListOf<ImportProgress>()
            var item = awaitItem()
            while (item !is ImportProgress.AwaitingConfirmation) {
                progressStates.add(item)
                item = awaitItem()
            }

            // Verify Categorizing was in the progress after Deduplicating
            assertTrue(
                progressStates.any { it is ImportProgress.Categorizing },
                "Expected Categorizing progress to be emitted"
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startImport applies AI category to known merchants`() = runTest {
        // MAGNUM is a known grocery merchant in MerchantDatabase
        val parseResult = ImportResult.Success(
            transactions = listOf(
                createImportedTransaction(description = "MAGNUM SUPERMARKET ALMATY")
            ),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        val result = useCase.startImport(
            parseResult = parseResult,
            targetAccountId = "acc-1"
        )

        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        val transaction = preview.transactions.first()

        // MAGNUM should be categorized as groceries by MerchantDatabase
        assertNotNull(
            transaction.transaction.category,
            "Known merchant should have category assigned"
        )
        assertEquals("groceries", transaction.transaction.category)
    }

    @Test
    fun `startImport preserves existing category if set`() = runTest {
        val parseResult = ImportResult.Success(
            transactions = listOf(
                ImportedTransaction(
                    date = LocalDate(2026, 1, 8),
                    amount = -5000L,
                    description = "MAGNUM SUPERMARKET",
                    category = "entertainment", // Pre-set category (different from expected)
                    confidence = 0.9f,
                    source = ImportSource.RULE_BASED
                )
            ),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        val result = useCase.startImport(
            parseResult = parseResult,
            targetAccountId = "acc-1"
        )

        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        val transaction = preview.transactions.first()

        // Should preserve the existing category, not overwrite with AI suggestion
        assertEquals(
            "entertainment",
            transaction.transaction.category,
            "Existing category should not be overwritten by AI"
        )
    }

    @Test
    fun `startImport leaves category null for unknown merchants`() = runTest {
        // Use a completely unknown merchant name
        val parseResult = ImportResult.Success(
            transactions = listOf(
                createImportedTransaction(description = "RANDOM_UNKNOWN_MERCHANT_XYZ123")
            ),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        val result = useCase.startImport(
            parseResult = parseResult,
            targetAccountId = "acc-1"
        )

        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        val transaction = preview.transactions.first()

        // Unknown merchant should remain uncategorized
        assertNull(
            transaction.transaction.category,
            "Unknown merchant should have null category"
        )
    }

    // === Helper Functions ===

    private fun createExistingTransaction(
        id: String = "tx-existing",
        description: String = "Existing",
        date: LocalDate = LocalDate(2026, 1, 8)
    ): Transaction {
        val now = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
        // Convert LocalDate to Instant at noon UTC for consistency with imported transactions
        val transactionInstant = Instant.fromEpochMilliseconds(
            date.toEpochDays() * 24L * 60 * 60 * 1000 + 12 * 60 * 60 * 1000
        )
        return Transaction(
            id = id,
            accountId = "acc-1",
            date = transactionInstant,
            amount = -5000L,
            description = description,
            type = TransactionType.EXPENSE,
            categoryId = "food",
            merchant = null,
            note = null,
            createdAt = now,
            updatedAt = now
        )
    }

    // === Fake Repository ===

    private class FakeTransactionRepository : TransactionRepository {
        private val existingTransactions = mutableListOf<Transaction>()
        val savedTransactions = mutableListOf<Transaction>()

        fun addTransaction(transaction: Transaction) {
            existingTransactions.add(transaction)
        }

        override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> {
            return flowOf(existingTransactions.filter { it.accountId == accountId })
        }

        override fun getTransactionById(id: String): Flow<Transaction?> {
            return flowOf(existingTransactions.find { it.id == id })
        }

        override suspend fun createTransaction(transaction: Transaction) {
            savedTransactions.add(transaction)
        }

        override suspend fun updateTransaction(transaction: Transaction) {}

        override suspend fun deleteTransaction(id: String) {}

        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(existingTransactions)

        override fun getTransactionsByDateRange(
            start: Instant,
            end: Instant
        ): Flow<List<Transaction>> = flowOf(emptyList())

        override fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> = flowOf(emptyList())

        override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> = flowOf(emptyList())

        override suspend fun insertTransfer(outgoing: Transaction, incoming: Transaction) {
            savedTransactions.add(outgoing)
            savedTransactions.add(incoming)
        }
    }

    // === Fake Category Repository ===

    private class FakeCategoryRepository : CategoryRepository {
        private val categories = mutableMapOf<String, Category>()

        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories.values.toList())

        override fun getCategoryById(id: String): Flow<Category?> = flowOf(categories[id])

        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> =
            flowOf(categories.values.filter { it.type == type })

        override fun getDefaultCategories(): Flow<List<Category>> =
            flowOf(categories.values.filter { it.isDefault })

        override suspend fun createCategory(category: Category) {
            categories[category.id] = category
        }

        override suspend fun updateCategory(category: Category) {
            categories[category.id] = category
        }

        override suspend fun deleteCategory(id: String) {
            categories.remove(id)
        }

        override suspend fun seedDefaultCategories() {
            // No-op in tests - categories are created via CategoryResolver
        }
    }
}

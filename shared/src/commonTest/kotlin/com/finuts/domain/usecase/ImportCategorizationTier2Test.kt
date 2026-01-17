package com.finuts.domain.usecase

import com.finuts.data.categorization.MerchantDatabase
import com.finuts.data.categorization.RuleBasedCategorizer
import com.finuts.data.categorization.TransactionForCategorization
import com.finuts.data.import.FuzzyDuplicateDetector
import com.finuts.data.import.ImportValidator
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategoryType
import com.finuts.domain.entity.CategorizationResult
import com.finuts.domain.entity.CategorizationSource
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.entity.import.DocumentType
import com.finuts.domain.entity.import.ImportResult
import com.finuts.domain.entity.import.ImportSource
import com.finuts.domain.entity.import.ImportedTransaction
import com.finuts.domain.registry.IconRegistry
import com.finuts.domain.repository.CategoryRepository
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for Tier 2 AI categorization integration during import.
 *
 * These tests verify that:
 * 1. categorizeAll() is called (not just categorizeTier1())
 * 2. AICategorizer is properly used for unknown merchants
 * 3. Tier 2 results are applied to transactions
 *
 * TDD: RED phase - these tests should FAIL until implementation is fixed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportCategorizationTier2Test {

    private val fakeTransactionRepository = FakeTransactionRepository()
    private val fakeCategoryRepository = FakeCategoryRepository()
    private val duplicateDetector = FuzzyDuplicateDetector()
    private val validator = ImportValidator()
    private val iconRegistry = IconRegistry()

    // Categorization dependencies
    private val merchantDatabase = MerchantDatabase()
    private val ruleBasedCategorizer = RuleBasedCategorizer(merchantDatabase)

    /**
     * Test that CategorizePendingTransactionsUseCase.categorizeAll() uses AI for unknown merchants.
     *
     * This is a UNIT test for the categorization use case itself.
     */
    @Test
    fun `categorizeAll calls AI categorizer for transactions not matched by Tier 1`() = runTest {
        // Given: Fake AI categorizer that tracks calls
        val fakeAICategorizer = TrackingAICategorizer()

        val categorizationUseCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = fakeAICategorizer
        )

        // Create test transactions - one known, one unknown
        val transactions = listOf(
            createTestTransaction(id = "tx-1", description = "MAGNUM SUPERMARKET"), // Known
            createTestTransaction(id = "tx-2", description = "UNKNOWN_MERCHANT_XYZ") // Unknown
        )

        // When: Call categorizeAll
        val result = categorizationUseCase.categorizeAll(transactions)

        // Then: AI categorizer should be called for unknown merchant
        assertTrue(
            fakeAICategorizer.categorizeTier2WasCalled,
            "categorizeTier2 should be called for unknown merchants"
        )
        assertTrue(
            fakeAICategorizer.receivedTransactionIds.contains("tx-2"),
            "Unknown transaction should be passed to AI categorizer"
        )
    }

    /**
     * Test that categorizeAll returns both Tier 1 and Tier 2 results.
     */
    @Test
    fun `categorizeAll returns combined Tier 1 and Tier 2 results`() = runTest {
        // Given: Fake AI categorizer that returns shopping for unknown
        val fakeAICategorizer = TrackingAICategorizer().apply {
            setResponse("tx-2", "shopping", 0.85f)
        }

        val categorizationUseCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = fakeAICategorizer
        )

        val transactions = listOf(
            createTestTransaction(id = "tx-1", description = "MAGNUM SUPERMARKET"),
            createTestTransaction(id = "tx-2", description = "UNKNOWN_MERCHANT_XYZ")
        )

        // When
        val result = categorizationUseCase.categorizeAll(transactions)

        // Then: Should have results for both transactions
        assertEquals(2, result.results.size, "Should have 2 categorized transactions")
        assertEquals(0, result.uncategorizedTransactionIds.size, "Should have no uncategorized")

        // Tier 1 result
        val tier1Result = result.results.find { it.transactionId == "tx-1" }
        assertNotNull(tier1Result, "Known merchant should be categorized")
        assertEquals("groceries", tier1Result.categoryId)
        assertTrue(tier1Result.isLocalSource, "Known merchant should use local source")

        // Tier 2 result
        val tier2Result = result.results.find { it.transactionId == "tx-2" }
        assertNotNull(tier2Result, "Unknown merchant should be categorized by AI")
        assertEquals("shopping", tier2Result.categoryId)
        assertEquals(CategorizationSource.LLM_TIER2, tier2Result.source)
    }

    /**
     * Test that categorizeAll gracefully handles null AI categorizer.
     *
     * This test verifies backward compatibility - the system should still
     * work with only Tier 1 if AI is not available.
     */
    @Test
    fun `categorizeAll works without AI categorizer`() = runTest {
        val categorizationUseCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = null
        )

        val transactions = listOf(
            createTestTransaction(id = "tx-1", description = "MAGNUM SUPERMARKET"),
            createTestTransaction(id = "tx-2", description = "UNKNOWN_MERCHANT_XYZ")
        )

        // When
        val result = categorizationUseCase.categorizeAll(transactions)

        // Then: Only Tier 1 matches, unknown remains uncategorized
        assertEquals(1, result.results.size, "Only known merchant should be categorized")
        assertEquals("tx-1", result.results.first().transactionId)
        assertEquals(listOf("tx-2"), result.uncategorizedTransactionIds)
    }

    /**
     * Test that Tier 2 results below confidence threshold are filtered out.
     */
    @Test
    fun `categorizeAll filters out low confidence Tier 2 results`() = runTest {
        // Given: AI returns low confidence result (below 0.70 threshold)
        val fakeAICategorizer = TrackingAICategorizer().apply {
            setResponse("tx-2", "shopping", 0.50f) // Below threshold
        }

        val categorizationUseCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = fakeAICategorizer
        )

        val transactions = listOf(
            createTestTransaction(id = "tx-2", description = "UNKNOWN_MERCHANT_XYZ")
        )

        // When
        val result = categorizationUseCase.categorizeAll(transactions)

        // Then: Low confidence result should be filtered out
        assertEquals(0, result.results.size, "Low confidence result should be filtered")
        assertEquals(listOf("tx-2"), result.uncategorizedTransactionIds)
    }

    /**
     * INTEGRATION TEST: Verify that ImportTransactionsUseCase calls categorizeAll.
     *
     * This test should FAIL with current implementation because:
     * - ImportTransactionsUseCase.kt:342 calls categorizeTier1() instead of categorizeAll()
     *
     * After fix, this test should PASS.
     */
    @Test
    fun `import uses categorizeAll for full tier coverage`() = runTest {
        // Given: Tracking categorization use case
        val fakeAICategorizer = TrackingAICategorizer().apply {
            setResponse("import-0", "coffee_shops", 0.88f)
        }

        val categorizationUseCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = fakeAICategorizer
        )

        val categoryResolver = CategoryResolver(
            categoryRepository = fakeCategoryRepository,
            iconRegistry = iconRegistry
        )

        val useCase = ImportTransactionsUseCase(
            transactionRepository = fakeTransactionRepository,
            duplicateDetector = duplicateDetector,
            validator = validator,
            categorizationUseCase = categorizationUseCase,
            categoryResolver = categoryResolver
        )

        // When: Import with unknown merchant
        val parseResult = ImportResult.Success(
            transactions = listOf(
                createImportedTransaction(description = "RANDOM_COFFEE_SHOP_ABC")
            ),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )

        useCase.startImport(
            parseResult = parseResult,
            targetAccountId = "acc-1"
        )

        // Then: AI categorizer should be called (meaning categorizeAll was used)
        assertTrue(
            fakeAICategorizer.categorizeTier2WasCalled,
            "AICategorizer should be called during import. " +
                "This test FAILS because ImportTransactionsUseCase.categorizeTransactions() " +
                "calls categorizeTier1() instead of categorizeAll(). " +
                "FIX: Change line 342 to call categorizeAll() and make the function suspend."
        )
    }

    // === Helper Functions ===

    private fun createTestTransaction(
        id: String,
        description: String,
        amount: Long = -5000L
    ): Transaction {
        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        return Transaction(
            id = id,
            accountId = "acc-1",
            date = now,
            amount = amount,
            description = description,
            type = if (amount >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
            categoryId = null, // Uncategorized
            merchant = null,
            note = null,
            createdAt = now,
            updatedAt = now
        )
    }

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

    // === Tracking AI Categorizer (for testing) ===

    /**
     * Tracking implementation that records calls without inheriting from AICategorizer.
     *
     * This is injected into CategorizePendingTransactionsUseCase which accepts AICategorizer?.
     * We use interface segregation - the use case only calls categorizeTier2(batch) method.
     */
    private class TrackingAICategorizer : com.finuts.data.categorization.AICategorizer(
        provider = null, // No actual provider needed for tracking
        categoryRepository = object : CategoryRepository {
            override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
            override fun getCategoryById(id: String): Flow<Category?> = flowOf(null)
            override fun getCategoriesByType(type: CategoryType) = flowOf(emptyList<Category>())
            override fun getDefaultCategories() = flowOf(emptyList<Category>())
            override suspend fun createCategory(category: Category) {}
            override suspend fun updateCategory(category: Category) {}
            override suspend fun deleteCategory(id: String) {}
            override suspend fun seedDefaultCategories() {}
        },
        anonymizer = com.finuts.ai.privacy.RegexPIIAnonymizer(),
        costTracker = com.finuts.ai.cost.AICostTracker(dailyBudget = 100f, monthlyBudget = 1000f),
        iconRegistry = IconRegistry()
    ) {
        var categorizeTier2WasCalled = false
            private set

        val receivedTransactionIds = mutableListOf<String>()
        private val responses = mutableMapOf<String, Pair<String, Float>>()

        fun setResponse(transactionId: String, categoryId: String, confidence: Float) {
            responses[transactionId] = categoryId to confidence
        }

        override suspend fun categorizeTier2(
            transactions: List<TransactionForCategorization>,
            categories: List<String>
        ): List<CategorizationResult> {
            categorizeTier2WasCalled = true
            receivedTransactionIds.addAll(transactions.map { it.id })

            return transactions.mapNotNull { tx ->
                responses[tx.id]?.let { (categoryId, confidence) ->
                    CategorizationResult(
                        transactionId = tx.id,
                        categoryId = categoryId,
                        confidence = confidence,
                        source = CategorizationSource.LLM_TIER2
                    )
                }
            }
        }
    }

    // === Fake Repositories ===

    private class FakeTransactionRepository : TransactionRepository {
        private val existingTransactions = mutableListOf<Transaction>()
        val savedTransactions = mutableListOf<Transaction>()

        override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> =
            flowOf(existingTransactions.filter { it.accountId == accountId })

        override fun getTransactionById(id: String): Flow<Transaction?> =
            flowOf(existingTransactions.find { it.id == id })

        override suspend fun createTransaction(transaction: Transaction) {
            savedTransactions.add(transaction)
        }

        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(id: String) {}
        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(existingTransactions)
        override fun getTransactionsByDateRange(start: Instant, end: Instant) = flowOf(emptyList<Transaction>())
        override fun getTransactionsByCategory(categoryId: String) = flowOf(emptyList<Transaction>())
        override fun getTransactionsByType(type: TransactionType) = flowOf(emptyList<Transaction>())
        override suspend fun insertTransfer(outgoing: Transaction, incoming: Transaction) {
            savedTransactions.add(outgoing)
            savedTransactions.add(incoming)
        }
    }

    private class FakeCategoryRepository : CategoryRepository {
        private val categories = mutableMapOf<String, Category>()

        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories.values.toList())
        override fun getCategoryById(id: String): Flow<Category?> = flowOf(categories[id])
        override fun getCategoriesByType(type: CategoryType) = flowOf(categories.values.filter { it.type == type })
        override fun getDefaultCategories() = flowOf(categories.values.filter { it.isDefault })
        override suspend fun createCategory(category: Category) { categories[category.id] = category }
        override suspend fun updateCategory(category: Category) { categories[category.id] = category }
        override suspend fun deleteCategory(id: String) { categories.remove(id) }
        override suspend fun seedDefaultCategories() {}
    }
}

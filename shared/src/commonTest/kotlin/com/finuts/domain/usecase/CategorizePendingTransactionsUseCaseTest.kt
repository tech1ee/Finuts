package com.finuts.domain.usecase

import com.finuts.data.categorization.MerchantDatabase
import com.finuts.data.categorization.RuleBasedCategorizer
import com.finuts.domain.entity.CategorizationResult
import com.finuts.domain.entity.CategorizationSource
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.test.TestData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for CategorizePendingTransactionsUseCase.
 * Tests the 3-tier categorization flow.
 */
class CategorizePendingTransactionsUseCaseTest {

    private val merchantDatabase = MerchantDatabase()
    private val ruleBasedCategorizer = RuleBasedCategorizer(merchantDatabase)

    // --- Tier 1 Only Mode ---

    @Test
    fun `categorizes known merchant with Tier 1`() {
        val useCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = null  // No AI, Tier 1 only
        )

        val transaction = createTransaction(
            id = "tx-1",
            description = "MAGNUM SUPER ALMATY"
        )

        val results = useCase.categorizeTier1(listOf(transaction))

        assertEquals(1, results.size)
        assertEquals("tx-1", results[0].transactionId)
        assertEquals("groceries", results[0].categoryId)
        assertEquals(CategorizationSource.MERCHANT_DATABASE, results[0].source)
    }

    @Test
    fun `returns empty for unknown merchants in Tier 1`() {
        val useCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = null
        )

        val transaction = createTransaction(
            id = "tx-1",
            description = "UNKNOWN RANDOM SHOP 12345"
        )

        val results = useCase.categorizeTier1(listOf(transaction))

        assertTrue(results.isEmpty())
    }

    @Test
    fun `categorizes multiple transactions with Tier 1`() {
        val useCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = null
        )

        val transactions = listOf(
            createTransaction("tx-1", "MAGNUM ALMATY"),
            createTransaction("tx-2", "WOLT DELIVERY"),
            createTransaction("tx-3", "UNKNOWN SHOP"),
            createTransaction("tx-4", "YANDEX TAXI")
        )

        val results = useCase.categorizeTier1(transactions)

        assertEquals(3, results.size)  // 3 known, 1 unknown
        assertEquals("groceries", results.find { it.transactionId == "tx-1" }?.categoryId)
        assertEquals("food_delivery", results.find { it.transactionId == "tx-2" }?.categoryId)
        assertEquals("transport", results.find { it.transactionId == "tx-4" }?.categoryId)
    }

    // --- Result Filtering ---

    @Test
    fun `filters out transactions already categorized`() {
        val useCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = null
        )

        val transactions = listOf(
            createTransaction("tx-1", "MAGNUM", categoryId = null),  // Needs categorization
            createTransaction("tx-2", "WOLT", categoryId = "food_delivery")  // Already done
        )

        val pending = useCase.filterPendingCategorization(transactions)

        assertEquals(1, pending.size)
        assertEquals("tx-1", pending[0].id)
    }

    @Test
    fun `filters out transfer transactions`() {
        val useCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = null
        )

        val transactions = listOf(
            createTransaction("tx-1", "MAGNUM"),  // Expense
            createTransaction("tx-2", "TRANSFER", type = TransactionType.TRANSFER)  // Transfer
        )

        val pending = useCase.filterPendingCategorization(transactions)

        assertEquals(1, pending.size)
        assertEquals("tx-1", pending[0].id)
    }

    // --- High Confidence Detection ---

    @Test
    fun `identifies high confidence results`() {
        val results = listOf(
            CategorizationResult("tx-1", "groceries", 0.95f, CategorizationSource.MERCHANT_DATABASE),
            CategorizationResult("tx-2", "other", 0.60f, CategorizationSource.LLM_TIER2),
            CategorizationResult("tx-3", "transport", 0.85f, CategorizationSource.RULE_BASED)
        )

        val highConfidence = results.filter { it.isHighConfidence }

        assertEquals(2, highConfidence.size)
        assertTrue(highConfidence.any { it.transactionId == "tx-1" })
        assertTrue(highConfidence.any { it.transactionId == "tx-3" })
    }

    @Test
    fun `identifies results needing confirmation`() {
        val results = listOf(
            CategorizationResult("tx-1", "groceries", 0.95f, CategorizationSource.MERCHANT_DATABASE),
            CategorizationResult("tx-2", "other", 0.60f, CategorizationSource.LLM_TIER2),
            CategorizationResult("tx-3", "transport", 0.50f, CategorizationSource.LLM_TIER3)
        )

        val needConfirmation = results.filter { it.requiresUserConfirmation }

        assertEquals(2, needConfirmation.size)
        assertTrue(needConfirmation.any { it.transactionId == "tx-2" })
        assertTrue(needConfirmation.any { it.transactionId == "tx-3" })
    }

    // --- Statistics ---

    @Test
    fun `calculates tier distribution`() {
        val results = listOf(
            CategorizationResult("tx-1", "groceries", 0.95f, CategorizationSource.MERCHANT_DATABASE),
            CategorizationResult("tx-2", "food", 0.90f, CategorizationSource.RULE_BASED),
            CategorizationResult("tx-3", "transport", 0.80f, CategorizationSource.LLM_TIER2),
            CategorizationResult("tx-4", "other", 0.70f, CategorizationSource.LLM_TIER2),
            CategorizationResult("tx-5", "shopping", 0.60f, CategorizationSource.LLM_TIER3)
        )

        val localCount = results.count { it.isLocalSource }
        val tier2Count = results.count { it.source == CategorizationSource.LLM_TIER2 }
        val tier3Count = results.count { it.source == CategorizationSource.LLM_TIER3 }

        assertEquals(2, localCount)
        assertEquals(2, tier2Count)
        assertEquals(1, tier3Count)
    }

    // --- Helper Functions ---

    private fun createTransaction(
        id: String,
        description: String,
        categoryId: String? = null,
        type: TransactionType = TransactionType.EXPENSE
    ): Transaction {
        return TestData.transaction(
            id = id,
            description = description,
            categoryId = categoryId,
            type = type
        )
    }
}

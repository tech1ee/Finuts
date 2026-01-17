package com.finuts.domain.usecase

import com.finuts.ai.providers.ChatMessage
import com.finuts.ai.providers.CompletionRequest
import com.finuts.ai.providers.CompletionResponse
import com.finuts.ai.providers.FinishReason
import com.finuts.ai.providers.LLMProvider
import com.finuts.ai.providers.ModelConfig
import com.finuts.data.categorization.MerchantDatabase
import com.finuts.data.categorization.RuleBasedCategorizer
import com.finuts.domain.entity.CategorizationResult
import com.finuts.domain.entity.CategorizationSource
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.test.TestData
import kotlinx.coroutines.test.runTest
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

    // --- On-Device LLM Parsing Tests (Tier 1.5) ---

    @Test
    fun `categorizeAll parses numbered format response from LLM`() = runTest {
        // LLM returns numbered format "1. groceries" for single transaction batch
        val fakeLLM = FakeLLMProvider(response = "1. groceries")
        val useCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = null,
            onDeviceLLMProvider = fakeLLM
        )

        val transaction = createTransaction(
            id = "tx-1",
            description = "UNKNOWN RANDOM MERCHANT XYZ"  // Not in merchant DB
        )

        val result = useCase.categorizeAll(listOf(transaction))

        assertEquals(1, result.results.size)
        assertEquals("groceries", result.results[0].categoryId)
        assertEquals(CategorizationSource.ON_DEVICE_ML, result.results[0].source)
    }

    @Test
    fun `categorizeAll parses multi-line numbered format from LLM`() = runTest {
        // LLM returns "1. groceries\n2. transport\n3. shopping" format
        // Note: use descriptions that DON'T match Tier 1 (MerchantDatabase/rules)
        val fakeLLM = FakeLLMProvider(response = "1. groceries\n2. transport\n3. shopping")
        val useCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = null,
            onDeviceLLMProvider = fakeLLM
        )

        val transactions = listOf(
            createTransaction("tx-1", "SOMEPLACE ALPHA"),     // No Tier 1 match
            createTransaction("tx-2", "SOMEPLACE BETA"),      // No Tier 1 match
            createTransaction("tx-3", "SOMEPLACE GAMMA")      // No Tier 1 match
        )

        val result = useCase.categorizeAll(transactions)

        assertEquals(3, result.results.size)
        assertEquals("groceries", result.results.find { it.transactionId == "tx-1" }?.categoryId)
        assertEquals("transport", result.results.find { it.transactionId == "tx-2" }?.categoryId)
        assertEquals("shopping", result.results.find { it.transactionId == "tx-3" }?.categoryId)
    }

    @Test
    fun `categorizeAll parses JSON array response from LLM`() = runTest {
        // LLM returns proper JSON array
        val jsonResponse = """[
            {"id":1,"categoryId":"groceries","confidence":0.95},
            {"id":2,"categoryId":"transport","confidence":0.88}
        ]"""
        val fakeLLM = FakeLLMProvider(response = jsonResponse)
        val useCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = null,
            onDeviceLLMProvider = fakeLLM
        )

        // Use descriptions that don't match Tier 1
        val transactions = listOf(
            createTransaction("tx-1", "SOMEPLACE AAA"),
            createTransaction("tx-2", "SOMEPLACE BBB")
        )

        val result = useCase.categorizeAll(transactions)

        assertEquals(2, result.results.size)
        assertEquals("groceries", result.results.find { it.transactionId == "tx-1" }?.categoryId)
        assertEquals("transport", result.results.find { it.transactionId == "tx-2" }?.categoryId)
    }

    @Test
    fun `categorizeAll handles LLM unavailable gracefully`() = runTest {
        val unavailableLLM = FakeLLMProvider(response = "", available = false)
        val useCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = null,
            onDeviceLLMProvider = unavailableLLM
        )

        // Use description that doesn't match Tier 1
        val transaction = createTransaction("tx-1", "SOMEPLACE NONAME")

        val result = useCase.categorizeAll(listOf(transaction))

        // Should not crash, just return uncategorized
        assertEquals(0, result.results.size)
        assertEquals(1, result.uncategorizedTransactionIds.size)
    }

    @Test
    fun `categorizeAll skips Tier 1_5 for transactions categorized by Tier 1`() = runTest {
        // LLM returns numbered format for the remaining unknown transaction
        val fakeLLM = FakeLLMProvider(response = "1. shopping")
        val useCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = null,
            onDeviceLLMProvider = fakeLLM
        )

        val transactions = listOf(
            createTransaction("tx-1", "MAGNUM SUPER"),  // Known: Tier 1
            createTransaction("tx-2", "UNKNOWN SHOP")   // Unknown: needs Tier 1.5
        )

        val result = useCase.categorizeAll(transactions)

        assertEquals(2, result.results.size)
        // MAGNUM categorized by Tier 1 (MERCHANT_DATABASE)
        val magnum = result.results.find { it.transactionId == "tx-1" }
        assertEquals(CategorizationSource.MERCHANT_DATABASE, magnum?.source)
        // Unknown categorized by Tier 1.5 (ON_DEVICE_ML)
        val unknown = result.results.find { it.transactionId == "tx-2" }
        assertEquals(CategorizationSource.ON_DEVICE_ML, unknown?.source)
    }

    // --- CategorizationBatchResult Computed Properties Tests ---

    @Test
    fun `CategorizationBatchResult calculates totalCategorized correctly`() {
        val batchResult = CategorizationBatchResult(
            results = listOf(
                CategorizationResult("tx-1", "groceries", 0.95f, CategorizationSource.MERCHANT_DATABASE),
                CategorizationResult("tx-2", "transport", 0.85f, CategorizationSource.RULE_BASED)
            ),
            uncategorizedTransactionIds = listOf("tx-3")
        )

        assertEquals(2, batchResult.totalCategorized)
    }

    @Test
    fun `CategorizationBatchResult calculates totalUncategorized correctly`() {
        val batchResult = CategorizationBatchResult(
            results = listOf(
                CategorizationResult("tx-1", "groceries", 0.95f, CategorizationSource.MERCHANT_DATABASE)
            ),
            uncategorizedTransactionIds = listOf("tx-2", "tx-3", "tx-4")
        )

        assertEquals(3, batchResult.totalUncategorized)
    }

    @Test
    fun `CategorizationBatchResult calculates localCount correctly`() {
        val batchResult = CategorizationBatchResult(
            results = listOf(
                CategorizationResult("tx-1", "groceries", 0.95f, CategorizationSource.MERCHANT_DATABASE),
                CategorizationResult("tx-2", "transport", 0.85f, CategorizationSource.RULE_BASED),
                CategorizationResult("tx-3", "other", 0.70f, CategorizationSource.LLM_TIER2)
            ),
            uncategorizedTransactionIds = emptyList()
        )

        assertEquals(2, batchResult.localCount)
    }

    @Test
    fun `CategorizationBatchResult calculates tier2Count correctly`() {
        val batchResult = CategorizationBatchResult(
            results = listOf(
                CategorizationResult("tx-1", "groceries", 0.95f, CategorizationSource.MERCHANT_DATABASE),
                CategorizationResult("tx-2", "transport", 0.85f, CategorizationSource.LLM_TIER2),
                CategorizationResult("tx-3", "other", 0.70f, CategorizationSource.LLM_TIER2)
            ),
            uncategorizedTransactionIds = emptyList()
        )

        assertEquals(2, batchResult.tier2Count)
    }

    @Test
    fun `CategorizationBatchResult calculates highConfidenceCount correctly`() {
        val batchResult = CategorizationBatchResult(
            results = listOf(
                CategorizationResult("tx-1", "groceries", 0.95f, CategorizationSource.MERCHANT_DATABASE),
                CategorizationResult("tx-2", "transport", 0.50f, CategorizationSource.LLM_TIER2),
                CategorizationResult("tx-3", "shopping", 0.85f, CategorizationSource.RULE_BASED)
            ),
            uncategorizedTransactionIds = emptyList()
        )

        assertEquals(2, batchResult.highConfidenceCount)
    }

    @Test
    fun `CategorizationBatchResult calculates needsConfirmationCount correctly`() {
        val batchResult = CategorizationBatchResult(
            results = listOf(
                CategorizationResult("tx-1", "groceries", 0.95f, CategorizationSource.MERCHANT_DATABASE),
                CategorizationResult("tx-2", "transport", 0.50f, CategorizationSource.LLM_TIER2),
                CategorizationResult("tx-3", "shopping", 0.65f, CategorizationSource.LLM_TIER3)
            ),
            uncategorizedTransactionIds = emptyList()
        )

        assertEquals(2, batchResult.needsConfirmationCount)
    }

    @Test
    fun `CategorizationBatchResult handles empty results`() {
        val batchResult = CategorizationBatchResult(
            results = emptyList(),
            uncategorizedTransactionIds = listOf("tx-1", "tx-2")
        )

        assertEquals(0, batchResult.totalCategorized)
        assertEquals(2, batchResult.totalUncategorized)
        assertEquals(0, batchResult.localCount)
        assertEquals(0, batchResult.tier2Count)
        assertEquals(0, batchResult.highConfidenceCount)
        assertEquals(0, batchResult.needsConfirmationCount)
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

    /** Fake LLM provider for testing on-device categorization. */
    private class FakeLLMProvider(
        private val response: String,
        private val available: Boolean = true
    ) : LLMProvider {
        override val name: String = "fake-on-device"
        override val availableModels: List<ModelConfig> = emptyList()

        override suspend fun isAvailable(): Boolean = available

        override suspend fun complete(request: CompletionRequest): CompletionResponse {
            return CompletionResponse(
                content = response,
                inputTokens = 10,
                outputTokens = response.length / 4,
                model = name,
                finishReason = FinishReason.STOP
            )
        }

        override suspend fun chat(messages: List<ChatMessage>): CompletionResponse {
            return complete(CompletionRequest(prompt = messages.lastOrNull()?.content ?: ""))
        }

        override suspend fun structuredOutput(prompt: String, schema: String): CompletionResponse {
            return complete(CompletionRequest(prompt = prompt))
        }
    }
}

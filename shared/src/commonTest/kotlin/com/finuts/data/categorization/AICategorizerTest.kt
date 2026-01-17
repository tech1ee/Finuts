package com.finuts.data.categorization

import com.finuts.ai.cost.AICostTracker
import com.finuts.ai.privacy.AnonymizationResult
import com.finuts.ai.privacy.DetectedPII
import com.finuts.ai.privacy.PIIAnonymizer
import com.finuts.ai.providers.ChatMessage
import com.finuts.ai.providers.CompletionRequest
import com.finuts.ai.providers.CompletionResponse
import com.finuts.ai.providers.LLMProvider
import com.finuts.ai.providers.ModelConfig
import com.finuts.domain.entity.CategorizationSource
import com.finuts.domain.registry.IconRegistry
import com.finuts.test.fakes.FakeCategoryRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for AICategorizer - Tier 2 cloud LLM categorization.
 */
class AICategorizerTest {

    // === Fake Dependencies ===

    private class FakeLLMProvider(
        private val responseContent: String,
        override val name: String = "test-provider"
    ) : LLMProvider {
        override val availableModels: List<ModelConfig> = emptyList()
        var lastPrompt: String? = null

        override suspend fun isAvailable(): Boolean = true

        override suspend fun complete(request: CompletionRequest): CompletionResponse {
            lastPrompt = request.prompt
            return CompletionResponse(
                content = responseContent,
                inputTokens = 100,
                outputTokens = 50,
                model = name
            )
        }

        override suspend fun chat(messages: List<ChatMessage>): CompletionResponse =
            complete(CompletionRequest(""))

        override suspend fun structuredOutput(prompt: String, schema: String): CompletionResponse =
            complete(CompletionRequest(prompt))
    }

    private class FakePIIAnonymizer : PIIAnonymizer {
        override fun anonymize(text: String): AnonymizationResult = AnonymizationResult(
            anonymizedText = text,
            mapping = emptyMap(),
            detectedPII = emptyList(),
            wasModified = false
        )

        override fun deanonymize(text: String, mapping: Map<String, String>): String = text
        override fun detectPII(text: String): List<DetectedPII> = emptyList()
    }

    // === Test Setup ===

    private fun createCategorizer(
        provider: LLMProvider? = null,
        costTracker: AICostTracker = AICostTracker()
    ): AICategorizer {
        return AICategorizer(
            provider = provider,
            categoryRepository = FakeCategoryRepository(),
            anonymizer = FakePIIAnonymizer(),
            costTracker = costTracker,
            iconRegistry = IconRegistry()
        )
    }

    // === categorizeTier2 Tests ===

    @Test
    fun `categorizeTier2 returns null when no provider`() = runTest {
        val categorizer = createCategorizer(provider = null)

        val result = categorizer.categorizeTier2(
            transactionId = "tx-1",
            description = "MAGNUM SUPER ALMATY"
        )

        assertNull(result)
    }

    @Test
    fun `categorizeTier2 returns result with valid LLM response`() = runTest {
        val provider = FakeLLMProvider(
            responseContent = """{"categoryId":"groceries","confidence":0.95,"isNew":false}"""
        )
        val categorizer = createCategorizer(provider = provider)

        val result = categorizer.categorizeTier2(
            transactionId = "tx-1",
            description = "MAGNUM SUPER ALMATY"
        )

        assertNotNull(result)
        assertEquals("tx-1", result.transactionId)
        assertEquals("groceries", result.categoryId)
        assertEquals(0.95f, result.confidence)
        assertEquals(CategorizationSource.LLM_TIER2, result.source)
    }

    @Test
    fun `categorizeTier2 passes description to LLM`() = runTest {
        val provider = FakeLLMProvider(
            responseContent = """{"categoryId":"groceries","confidence":0.90}"""
        )
        val categorizer = createCategorizer(provider = provider)

        categorizer.categorizeTier2(
            transactionId = "tx-1",
            description = "COFFEE HOUSE ALMATY"
        )

        assertNotNull(provider.lastPrompt)
        assertTrue(provider.lastPrompt!!.contains("COFFEE HOUSE ALMATY"))
    }

    @Test
    fun `categorizeTier2 returns null on malformed JSON`() = runTest {
        val provider = FakeLLMProvider(responseContent = "invalid json response")
        val categorizer = createCategorizer(provider = provider)

        val result = categorizer.categorizeTier2(
            transactionId = "tx-1",
            description = "SOME MERCHANT"
        )

        assertNull(result)
    }

    // === Batch categorizeTier2 Tests ===

    @Test
    fun `batch categorizeTier2 returns empty when no provider`() = runTest {
        val categorizer = createCategorizer(provider = null)
        val transactions = listOf(
            TransactionForCategorization("tx-1", "MERCHANT A", 10000L)
        )

        val results = categorizer.categorizeTier2(
            transactions = transactions,
            categories = listOf("groceries", "shopping")
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun `batch categorizeTier2 returns empty for empty transactions`() = runTest {
        val provider = FakeLLMProvider(responseContent = "[]")
        val categorizer = createCategorizer(provider = provider)

        val results = categorizer.categorizeTier2(
            transactions = emptyList(),
            categories = listOf("groceries")
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun `batch categorizeTier2 parses array response`() = runTest {
        val provider = FakeLLMProvider(
            responseContent = """[
                {"index":0,"categoryId":"groceries","confidence":0.95},
                {"index":1,"categoryId":"transport","confidence":0.88}
            ]"""
        )
        val categorizer = createCategorizer(provider = provider)
        val transactions = listOf(
            TransactionForCategorization("tx-1", "MAGNUM", 5000L),
            TransactionForCategorization("tx-2", "YANDEX TAXI", 1500L)
        )

        val results = categorizer.categorizeTier2(
            transactions = transactions,
            categories = listOf("groceries", "transport")
        )

        assertEquals(2, results.size)
        assertEquals("groceries", results[0].categoryId)
        assertEquals("transport", results[1].categoryId)
        assertTrue(results.all { it.source == CategorizationSource.LLM_TIER2 })
    }

    @Test
    fun `batch categorizeTier2 handles malformed response gracefully`() = runTest {
        val provider = FakeLLMProvider(responseContent = "not valid json")
        val categorizer = createCategorizer(provider = provider)
        val transactions = listOf(
            TransactionForCategorization("tx-1", "MERCHANT", 1000L)
        )

        val results = categorizer.categorizeTier2(
            transactions = transactions,
            categories = listOf("other")
        )

        assertTrue(results.isEmpty())
    }

    // === isAvailable Tests ===

    @Test
    fun `isAvailable returns false when no provider`() = runTest {
        val categorizer = createCategorizer(provider = null)

        val available = categorizer.isAvailable()

        assertEquals(false, available)
    }

    @Test
    fun `isAvailable returns true when provider available`() = runTest {
        val provider = FakeLLMProvider(responseContent = "")
        val categorizer = createCategorizer(provider = provider)

        val available = categorizer.isAvailable()

        assertTrue(available)
    }

    // === TransactionForCategorization Tests ===

    @Test
    fun `TransactionForCategorization stores all fields`() {
        val tx = TransactionForCategorization(
            id = "tx-123",
            description = "MERCHANT NAME",
            amount = 50000L
        )

        assertEquals("tx-123", tx.id)
        assertEquals("MERCHANT NAME", tx.description)
        assertEquals(50000L, tx.amount)
    }

    @Test
    fun `TransactionForCategorization formats positive amount correctly`() {
        val tx = TransactionForCategorization(
            id = "tx-1",
            description = "TEST",
            amount = 123456L // 1234.56
        )

        assertEquals("1234.56", tx.formattedAmount)
    }

    @Test
    fun `TransactionForCategorization formats negative amount correctly`() {
        val tx = TransactionForCategorization(
            id = "tx-1",
            description = "TEST",
            amount = -99999L // -999.99
        )

        assertEquals("-999.99", tx.formattedAmount)
    }

    @Test
    fun `TransactionForCategorization formats zero correctly`() {
        val tx = TransactionForCategorization(
            id = "tx-1",
            description = "TEST",
            amount = 0L
        )

        assertEquals("0.00", tx.formattedAmount)
    }

    @Test
    fun `TransactionForCategorization formats small amount with padding`() {
        val tx = TransactionForCategorization(
            id = "tx-1",
            description = "TEST",
            amount = 5L // 0.05
        )

        assertEquals("0.05", tx.formattedAmount)
    }
}

package com.finuts.data.categorization

import com.finuts.ai.cost.AICostTrackerInterface
import com.finuts.ai.cost.UsageRecord
import com.finuts.ai.cost.UsageStats
import com.finuts.ai.providers.CompletionRequest
import com.finuts.ai.providers.CompletionResponse
import com.finuts.ai.providers.ChatMessage
import com.finuts.ai.providers.FinishReason
import com.finuts.ai.providers.LLMProvider
import com.finuts.ai.providers.ModelConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for LLMMerchantEnricher - Tier 1.5 merchant enrichment.
 *
 * TDD: RED phase - these tests define expected behavior.
 */
class LLMMerchantEnricherTest {

    // === Basic Enrichment Tests ===

    @Test
    fun `enriches SBUX to Starbucks`() = runTest {
        val provider = MockLLMProvider(
            response = """{"cleanMerchantName": "Starbucks", "merchantType": "COFFEE_SHOP", "confidence": 0.95}"""
        )
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "SBUX #1234 COFFEE",
            normalizedName = "SBUX"
        )

        assertNotNull(result)
        assertEquals("Starbucks", result.cleanMerchantName)
        assertEquals("COFFEE_SHOP", result.merchantType)
        assertEquals(0.95f, result.confidence)
    }

    @Test
    fun `enriches MAGNUM to Magnum Cash and Carry`() = runTest {
        val provider = MockLLMProvider(
            response = """{"cleanMerchantName": "Magnum Cash & Carry", "brandName": "Magnum", "merchantType": "GROCERY", "confidence": 0.92}"""
        )
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "МАГНУМ КЭШ АСТАНА",
            normalizedName = "МАГНУМ КЭШ"
        )

        assertNotNull(result)
        assertEquals("Magnum Cash & Carry", result.cleanMerchantName)
        assertEquals("Magnum", result.brandName)
        assertEquals("GROCERY", result.merchantType)
    }

    @Test
    fun `enriches WOLT to Wolt with delivery type`() = runTest {
        val provider = MockLLMProvider(
            response = """{"cleanMerchantName": "Wolt", "merchantType": "FOOD_DELIVERY", "confidence": 0.93}"""
        )
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "WOLT*RESTO DELIVERY #456",
            normalizedName = "WOLT RESTO DELIVERY"
        )

        assertNotNull(result)
        assertEquals("Wolt", result.cleanMerchantName)
        assertEquals("FOOD_DELIVERY", result.merchantType)
    }

    // === Confidence Threshold Tests ===

    @Test
    fun `returns null for low confidence`() = runTest {
        val provider = MockLLMProvider(
            response = """{"cleanMerchantName": "Unknown Shop", "merchantType": "RETAIL", "confidence": 0.45}"""
        )
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "SOME UNKNOWN SHOP",
            normalizedName = "SOME UNKNOWN SHOP"
        )

        assertNull(result, "Should return null when confidence < 0.7")
    }

    @Test
    fun `returns result when confidence exactly at threshold`() = runTest {
        val provider = MockLLMProvider(
            response = """{"cleanMerchantName": "Border Case", "merchantType": "RETAIL", "confidence": 0.70}"""
        )
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "BORDER CASE SHOP",
            normalizedName = "BORDER CASE"
        )

        assertNotNull(result, "Should return result when confidence >= 0.70")
        assertEquals(0.70f, result.confidence)
    }

    // === Cost Budget Tests ===

    @Test
    fun `respects cost budget - returns null when budget exceeded`() = runTest {
        val provider = MockLLMProvider(
            response = """{"cleanMerchantName": "Starbucks", "merchantType": "COFFEE_SHOP", "confidence": 0.95}"""
        )
        val costTracker = MockCostTracker(budgetAvailable = false)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "SBUX #1234",
            normalizedName = "SBUX"
        )

        assertNull(result, "Should return null when cost budget exceeded")
    }

    @Test
    fun `records cost after successful enrichment`() = runTest {
        val provider = MockLLMProvider(
            response = """{"cleanMerchantName": "Starbucks", "merchantType": "COFFEE_SHOP", "confidence": 0.95}""",
            inputTokens = 150,
            outputTokens = 50
        )
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        enricher.enrich(
            rawDescription = "SBUX #1234",
            normalizedName = "SBUX"
        )

        assertTrue(costTracker.recordedCalls > 0, "Should record cost after successful call")
        assertEquals(150, costTracker.lastInputTokens)
        assertEquals(50, costTracker.lastOutputTokens)
    }

    // === MCC Code Tests ===

    @Test
    fun `extracts MCC code when available`() = runTest {
        val provider = MockLLMProvider(
            response = """{"cleanMerchantName": "Starbucks", "merchantType": "COFFEE_SHOP", "mccCode": "5814", "confidence": 0.95}"""
        )
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "STARBUCKS COFFEE #123",
            normalizedName = "STARBUCKS COFFEE"
        )

        assertNotNull(result)
        assertEquals("5814", result.mccCode)
    }

    @Test
    fun `handles response without MCC code`() = runTest {
        val provider = MockLLMProvider(
            response = """{"cleanMerchantName": "Local Shop", "merchantType": "RETAIL", "confidence": 0.85}"""
        )
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "LOCAL SHOP ABC",
            normalizedName = "LOCAL SHOP"
        )

        assertNotNull(result)
        assertNull(result.mccCode, "MCC code should be null when not provided")
    }

    // === Provider Availability Tests ===

    @Test
    fun `returns null when provider unavailable`() = runTest {
        val provider = MockLLMProvider(available = false)
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "SBUX #1234",
            normalizedName = "SBUX"
        )

        assertNull(result, "Should return null when provider unavailable")
    }

    @Test
    fun `returns null when provider is null`() = runTest {
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(null, costTracker)

        val result = enricher.enrich(
            rawDescription = "SBUX #1234",
            normalizedName = "SBUX"
        )

        assertNull(result, "Should return null when provider is null")
    }

    // === Error Handling Tests ===

    @Test
    fun `handles invalid JSON response gracefully`() = runTest {
        val provider = MockLLMProvider(
            response = "invalid json {"
        )
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "SBUX #1234",
            normalizedName = "SBUX"
        )

        assertNull(result, "Should return null on invalid JSON")
    }

    @Test
    fun `handles empty response gracefully`() = runTest {
        val provider = MockLLMProvider(response = "")
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "SBUX #1234",
            normalizedName = "SBUX"
        )

        assertNull(result, "Should return null on empty response")
    }

    @Test
    fun `handles provider exception gracefully`() = runTest {
        val provider = MockLLMProvider(shouldThrow = true)
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "SBUX #1234",
            normalizedName = "SBUX"
        )

        assertNull(result, "Should return null on provider exception")
    }

    // === Multi-language Tests ===

    @Test
    fun `handles Russian merchant names`() = runTest {
        val provider = MockLLMProvider(
            response = """{"cleanMerchantName": "Яндекс Такси", "brandName": "Яндекс", "merchantType": "TRANSPORT", "confidence": 0.94}"""
        )
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "ЯНДЕКС ТАКСИ ПОЕЗДКА",
            normalizedName = "ЯНДЕКС ТАКСИ"
        )

        assertNotNull(result)
        assertEquals("Яндекс Такси", result.cleanMerchantName)
        assertEquals("Яндекс", result.brandName)
        assertEquals("TRANSPORT", result.merchantType)
    }

    @Test
    fun `handles Kazakh merchant names`() = runTest {
        val provider = MockLLMProvider(
            response = """{"cleanMerchantName": "Kaspi Bank", "brandName": "Kaspi", "merchantType": "BANK", "confidence": 0.96}"""
        )
        val costTracker = MockCostTracker(budgetAvailable = true)
        val enricher = LLMMerchantEnricher(provider, costTracker)

        val result = enricher.enrich(
            rawDescription = "KASPI GOLD ПЕРЕВОД",
            normalizedName = "KASPI GOLD"
        )

        assertNotNull(result)
        assertEquals("Kaspi Bank", result.cleanMerchantName)
        assertEquals("Kaspi", result.brandName)
    }

    // === Mock Implementations ===

    private class MockLLMProvider(
        private val response: String = "",
        private val available: Boolean = true,
        private val shouldThrow: Boolean = false,
        private val inputTokens: Int = 100,
        private val outputTokens: Int = 50
    ) : LLMProvider {
        override val name = "mock-provider"
        override val availableModels = emptyList<ModelConfig>()

        override suspend fun isAvailable() = available

        override suspend fun complete(request: CompletionRequest): CompletionResponse {
            if (shouldThrow) throw RuntimeException("Provider error")
            return CompletionResponse(
                content = response,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                model = "mock-model",
                finishReason = FinishReason.STOP
            )
        }

        override suspend fun chat(messages: List<ChatMessage>) =
            complete(CompletionRequest(messages.lastOrNull()?.content ?: ""))

        override suspend fun structuredOutput(prompt: String, schema: String) =
            complete(CompletionRequest(prompt))
    }

    private class MockCostTracker(
        private val budgetAvailable: Boolean
    ) : AICostTrackerInterface {
        var recordedCalls = 0
        var lastInputTokens = 0
        var lastOutputTokens = 0

        override fun canExecute(estimatedCost: Float) = budgetAvailable

        override fun record(inputTokens: Int, outputTokens: Int, model: String) {
            recordedCalls++
            lastInputTokens = inputTokens
            lastOutputTokens = outputTokens
        }

        override fun getUsageStats() = UsageStats(
            todayCost = 0f,
            monthCost = 0f,
            dailyBudget = 0.10f,
            monthlyBudget = 2.0f,
            todayRemaining = 0.10f,
            monthRemaining = 2.0f,
            recentRecords = emptyList()
        )
    }
}

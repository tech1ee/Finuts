package com.finuts.data.import.ocr

import com.finuts.ai.cost.AICostTrackerInterface
import com.finuts.ai.cost.UsageStats
import com.finuts.ai.privacy.PIIAnonymizer
import com.finuts.ai.privacy.RegexPIIAnonymizer
import com.finuts.ai.providers.CompletionRequest
import com.finuts.ai.providers.CompletionResponse
import com.finuts.ai.providers.LLMProvider
import com.finuts.ai.providers.ModelConfig
import com.finuts.domain.entity.import.ImportedTransaction
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertFalse

/**
 * Tests for LLMDocumentParser - LLM-powered fallback for unknown bank formats.
 *
 * TDD: RED phase - these tests define expected behavior for LLM document parsing.
 *
 * Key behaviors:
 * - Falls back to LLM when regex parsing fails
 * - Anonymizes PII before sending to LLM
 * - Respects cost budget
 * - Handles multi-language documents (RU, EN, KK)
 * - Handles multi-currency documents (KZT, RUB, USD, EUR)
 */
class LLMDocumentParserTest {

    // === Core Parsing Tests ===

    @Test
    fun `parses unknown bank format with LLM`() = runTest {
        // Given: OCR text from unknown bank format (not matching any regex)
        val unknownBankText = """
            Statement Period: 01-15 January 2026
            Account: ****1234

            Transaction History:
            Jan 5    Coffee House Premium    -15.50 USD
            Jan 7    Online Shopping Inc.    -99.99 USD
            Jan 10   Salary Payment         +2500.00 USD
        """.trimIndent()

        val mockProvider = MockLLMProvider(
            responseJson = """
            [
                {"date": "2026-01-05", "amount": -1550, "description": "Coffee House Premium", "currency": "USD"},
                {"date": "2026-01-07", "amount": -9999, "description": "Online Shopping Inc.", "currency": "USD"},
                {"date": "2026-01-10", "amount": 250000, "description": "Salary Payment", "currency": "USD"}
            ]
            """.trimIndent()
        )

        val parser = LLMDocumentParser(
            provider = mockProvider,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = true)
        )

        // When: Parse with LLM
        val result = parser.parse(unknownBankText)

        // Then: Transactions are extracted correctly
        assertEquals(3, result.size, "Should extract 3 transactions")
        assertEquals(-1550L, result[0].amount, "First transaction amount")
        assertEquals("Coffee House Premium", result[0].description)
        assertEquals(250000L, result[2].amount, "Third transaction (income)")
    }

    @Test
    fun `anonymizes PII before sending to LLM`() = runTest {
        // Given: Text with PII
        val textWithPii = """
            Account: KZ123456789012345678
            Owner: Иванов Алексей Петрович
            Phone: +7 777 123 45 67

            Transactions:
            2026-01-15 Transfer to Петров А.С. -50000
        """.trimIndent()

        val capturedPrompt = StringBuilder()
        val mockProvider = MockLLMProvider(
            responseJson = "[]",
            capturePrompt = { capturedPrompt.append(it) }
        )

        val parser = LLMDocumentParser(
            provider = mockProvider,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = true)
        )

        // When: Parse
        parser.parse(textWithPii)

        // Then: PII is anonymized in prompt
        val prompt = capturedPrompt.toString()
        assertFalse(
            prompt.contains("KZ123456789012345678"),
            "IBAN should be anonymized"
        )
        assertFalse(
            prompt.contains("Иванов"),
            "Name should be anonymized"
        )
        assertFalse(
            prompt.contains("+7 777"),
            "Phone should be anonymized"
        )
    }

    @Test
    fun `respects cost budget`() = runTest {
        // Given: Cost budget exceeded
        val text = "Some transaction text"
        val mockProvider = MockLLMProvider(responseJson = "[]")
        val parser = LLMDocumentParser(
            provider = mockProvider,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = false) // Budget exceeded
        )

        // When: Attempt to parse
        val result = parser.parse(text)

        // Then: Returns empty (doesn't call LLM)
        assertTrue(result.isEmpty(), "Should not parse when budget exceeded")
        assertFalse(mockProvider.wasCalled, "LLM should not be called")
    }

    // === Multi-language Tests ===

    @Test
    fun `handles Russian language document`() = runTest {
        val russianText = """
            Выписка по счету ****5678
            Период: 01.01.2026 - 15.01.2026

            15.01.2026 Покупка в МАГНУМ ТОО -15000 ₸
            14.01.2026 Перевод от Иванова А. +50000 ₸
        """.trimIndent()

        val mockProvider = MockLLMProvider(
            responseJson = """
            [
                {"date": "2026-01-15", "amount": -1500000, "description": "Покупка в МАГНУМ ТОО", "currency": "KZT"},
                {"date": "2026-01-14", "amount": 5000000, "description": "Перевод от [PERSON]", "currency": "KZT"}
            ]
            """.trimIndent()
        )

        val parser = LLMDocumentParser(
            provider = mockProvider,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = true)
        )

        val result = parser.parse(russianText)

        assertEquals(2, result.size)
        assertTrue(result[0].description?.contains("МАГНУМ") == true)
    }

    @Test
    fun `handles Kazakh language document`() = runTest {
        val kazakhText = """
            Шот бойынша үзінді ****9012
            Кезең: 01.01.2026 - 15.01.2026

            15.01.2026 Сатып алу ТЕХНОДОМ -89000 ₸
            13.01.2026 Жалақы түсімі +350000 ₸
        """.trimIndent()

        val mockProvider = MockLLMProvider(
            responseJson = """
            [
                {"date": "2026-01-15", "amount": -8900000, "description": "Сатып алу ТЕХНОДОМ", "currency": "KZT"},
                {"date": "2026-01-13", "amount": 35000000, "description": "Жалақы түсімі", "currency": "KZT"}
            ]
            """.trimIndent()
        )

        val parser = LLMDocumentParser(
            provider = mockProvider,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = true)
        )

        val result = parser.parse(kazakhText)

        assertEquals(2, result.size)
        assertTrue(result[0].description?.contains("ТЕХНОДОМ") == true)
    }

    // === Multi-currency Tests ===

    @Test
    fun `handles KZT currency`() = runTest {
        val kztText = """
            15.01.2026 МАГНУМ -15 000 ₸
            15.01.2026 GLOVO -3 500 ₸
        """.trimIndent()

        val mockProvider = MockLLMProvider(
            responseJson = """
            [
                {"date": "2026-01-15", "amount": -1500000, "description": "МАГНУМ", "currency": "KZT"},
                {"date": "2026-01-15", "amount": -350000, "description": "GLOVO", "currency": "KZT"}
            ]
            """.trimIndent()
        )

        val parser = LLMDocumentParser(
            provider = mockProvider,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = true)
        )

        val result = parser.parse(kztText)

        assertEquals(2, result.size)
        assertEquals(-1500000L, result[0].amount) // In minor units (tiyn)
    }

    @Test
    fun `handles EUR currency`() = runTest {
        val eurText = """
            15/01/2026 Amazon EU -49,99 EUR
            14/01/2026 Netflix -12,99 EUR
        """.trimIndent()

        val mockProvider = MockLLMProvider(
            responseJson = """
            [
                {"date": "2026-01-15", "amount": -4999, "description": "Amazon EU", "currency": "EUR"},
                {"date": "2026-01-14", "amount": -1299, "description": "Netflix", "currency": "EUR"}
            ]
            """.trimIndent()
        )

        val parser = LLMDocumentParser(
            provider = mockProvider,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = true)
        )

        val result = parser.parse(eurText)

        assertEquals(2, result.size)
        assertEquals(-4999L, result[0].amount) // In minor units (cents)
    }

    @Test
    fun `detects currency automatically from context`() = runTest {
        // Given: Text with USD currency indicators but no explicit symbol
        val ambiguousText = """
            Bank Statement - USD Account

            01/15/2026 Starbucks Coffee 5.50
            01/14/2026 Uber Trip 12.00
        """.trimIndent()

        val mockProvider = MockLLMProvider(
            responseJson = """
            [
                {"date": "2026-01-15", "amount": -550, "description": "Starbucks Coffee", "currency": "USD"},
                {"date": "2026-01-14", "amount": -1200, "description": "Uber Trip", "currency": "USD"}
            ]
            """.trimIndent()
        )

        val parser = LLMDocumentParser(
            provider = mockProvider,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = true)
        )

        val result = parser.parse(ambiguousText)

        assertEquals(2, result.size)
        // LLM should infer USD from "USD Account" context
    }

    // === Edge Cases ===

    @Test
    fun `handles empty LLM response gracefully`() = runTest {
        val text = "Some unclear document text"
        val mockProvider = MockLLMProvider(responseJson = "[]")

        val parser = LLMDocumentParser(
            provider = mockProvider,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = true)
        )

        val result = parser.parse(text)

        assertTrue(result.isEmpty(), "Should return empty list for unclear document")
    }

    @Test
    fun `handles malformed LLM JSON response`() = runTest {
        val text = "Transaction: Coffee -5.00"
        val mockProvider = MockLLMProvider(responseJson = "not valid json {{{")

        val parser = LLMDocumentParser(
            provider = mockProvider,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = true)
        )

        val result = parser.parse(text)

        assertTrue(result.isEmpty(), "Should handle malformed JSON gracefully")
    }

    @Test
    fun `handles LLM provider unavailable`() = runTest {
        val text = "Transaction text"
        val mockProvider = MockLLMProvider(
            responseJson = "[]",
            isAvailable = false
        )

        val parser = LLMDocumentParser(
            provider = mockProvider,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = true)
        )

        val result = parser.parse(text)

        assertTrue(result.isEmpty(), "Should return empty when provider unavailable")
    }

    @Test
    fun `returns null provider triggers empty result`() = runTest {
        val parser = LLMDocumentParser(
            provider = null,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = true)
        )

        val result = parser.parse("Some text")

        assertTrue(result.isEmpty(), "Should return empty when no provider")
    }

    // === Prompt Quality Tests ===

    @Test
    fun `prompt includes clear JSON output format instructions`() = runTest {
        val capturedPrompt = StringBuilder()
        val mockProvider = MockLLMProvider(
            responseJson = "[]",
            capturePrompt = { capturedPrompt.append(it) }
        )

        val parser = LLMDocumentParser(
            provider = mockProvider,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = true)
        )

        parser.parse("Some bank statement text")

        val prompt = capturedPrompt.toString()
        assertTrue(prompt.contains("JSON"), "Prompt should mention JSON format")
        assertTrue(
            prompt.contains("date") && prompt.contains("amount"),
            "Prompt should specify required fields"
        )
    }

    @Test
    fun `prompt includes multi-language understanding instructions`() = runTest {
        val capturedPrompt = StringBuilder()
        val mockProvider = MockLLMProvider(
            responseJson = "[]",
            capturePrompt = { capturedPrompt.append(it) }
        )

        val parser = LLMDocumentParser(
            provider = mockProvider,
            anonymizer = RegexPIIAnonymizer(),
            costTracker = MockCostTracker(budgetAvailable = true)
        )

        parser.parse("Some text")

        val prompt = capturedPrompt.toString()
        assertTrue(
            prompt.contains("language") || prompt.contains("Russian") ||
            prompt.contains("English") || prompt.contains("ANY"),
            "Prompt should mention multi-language support"
        )
    }
}

// === Test Doubles ===

/**
 * Mock LLM provider for testing.
 */
class MockLLMProvider(
    private val responseJson: String,
    private val isAvailable: Boolean = true,
    private val capturePrompt: ((String) -> Unit)? = null
) : LLMProvider {
    override val name = "mock"
    override val availableModels = listOf(
        ModelConfig("mock-model", "Mock", 4096, 0.001f, 0.002f, true)
    )

    var wasCalled = false
        private set

    override suspend fun isAvailable() = isAvailable

    override suspend fun complete(request: CompletionRequest): CompletionResponse {
        wasCalled = true
        capturePrompt?.invoke(request.prompt)
        return CompletionResponse(
            content = responseJson,
            inputTokens = 100,
            outputTokens = 50,
            model = "mock-model"
        )
    }

    override suspend fun chat(messages: List<com.finuts.ai.providers.ChatMessage>): CompletionResponse {
        wasCalled = true
        return CompletionResponse(
            content = responseJson,
            inputTokens = 100,
            outputTokens = 50,
            model = "mock-model"
        )
    }

    override suspend fun structuredOutput(prompt: String, schema: String): CompletionResponse {
        wasCalled = true
        capturePrompt?.invoke(prompt)
        return CompletionResponse(
            content = responseJson,
            inputTokens = 100,
            outputTokens = 50,
            model = "mock-model"
        )
    }
}

/**
 * Mock cost tracker for testing.
 */
class MockCostTracker(
    private val budgetAvailable: Boolean
) : AICostTrackerInterface {
    override fun canExecute(estimatedCost: Float) = budgetAvailable
    override fun record(inputTokens: Int, outputTokens: Int, model: String) {}
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

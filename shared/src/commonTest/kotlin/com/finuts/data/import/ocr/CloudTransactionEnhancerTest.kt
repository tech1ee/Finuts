package com.finuts.data.import.ocr

import com.finuts.ai.providers.ChatMessage
import com.finuts.ai.providers.ChatRole
import com.finuts.ai.providers.CompletionRequest
import com.finuts.ai.providers.CompletionResponse
import com.finuts.ai.providers.FinishReason
import com.finuts.ai.providers.LLMProvider
import com.finuts.ai.providers.ModelConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloudTransactionEnhancerTest {

    // === Basic Extraction Tests ===

    @Test
    fun `extracts merchant from transaction description`() = runTest {
        val provider = FakeLLMProvider(
            response = """
            [
                {
                    "index": 0,
                    "merchant": "Glovo",
                    "categoryHint": "food_delivery",
                    "transactionType": "DEBIT"
                }
            ]
            """.trimIndent()
        )
        val enhancer = CloudTransactionEnhancer(provider)

        val transactions = listOf(
            PartialTransaction(
                rawDate = "15.01.2026",
                amountMinorUnits = -150000,
                currency = "KZT",
                rawDescription = "Glovo оплата заказа",
                isCredit = false,
                isDebit = true
            )
        )

        val result = enhancer.enhance(transactions)

        assertEquals(1, result.size)
        assertEquals("Glovo", result[0].merchant)
        assertEquals("food_delivery", result[0].categoryHint)
    }

    @Test
    fun `extracts counterparty name from transfer`() = runTest {
        val provider = FakeLLMProvider(
            response = """
            [
                {
                    "index": 0,
                    "merchant": null,
                    "counterpartyName": "[PERSON_NAME_1]",
                    "categoryHint": "transfers",
                    "transactionType": "TRANSFER"
                }
            ]
            """.trimIndent()
        )
        val enhancer = CloudTransactionEnhancer(provider)

        val transactions = listOf(
            PartialTransaction(
                rawDate = "15.01.2026",
                amountMinorUnits = -370000,
                currency = "KZT",
                rawDescription = "Перевод [PERSON_NAME_1]",
                isCredit = false,
                isDebit = true
            )
        )

        val result = enhancer.enhance(transactions)

        assertEquals(1, result.size)
        assertEquals("[PERSON_NAME_1]", result[0].counterpartyName)
        assertEquals("TRANSFER", result[0].transactionType)
    }

    // === Multiple Transactions ===

    @Test
    fun `enhances multiple transactions`() = runTest {
        val provider = FakeLLMProvider(
            response = """
            [
                {"index": 0, "merchant": "Magnum", "categoryHint": "groceries", "transactionType": "DEBIT"},
                {"index": 1, "merchant": null, "counterpartyName": "[NAME_1]", "categoryHint": "income", "transactionType": "CREDIT"},
                {"index": 2, "merchant": "Kaspi Shop", "categoryHint": "shopping", "transactionType": "DEBIT"}
            ]
            """.trimIndent()
        )
        val enhancer = CloudTransactionEnhancer(provider)

        val transactions = listOf(
            PartialTransaction("15.01.2026", -500000, "KZT", "Magnum", false, true),
            PartialTransaction("16.01.2026", 10000000, "KZT", "Перевод от [NAME_1]", true, false),
            PartialTransaction("17.01.2026", -250000, "KZT", "Kaspi Shop", false, true)
        )

        val result = enhancer.enhance(transactions)

        assertEquals(3, result.size)
        assertEquals("Magnum", result[0].merchant)
        assertEquals("[NAME_1]", result[1].counterpartyName)
        assertEquals("Kaspi Shop", result[2].merchant)
    }

    // === Transaction Types ===

    @Test
    fun `detects debit transaction type`() = runTest {
        val provider = FakeLLMProvider(
            response = """[{"index": 0, "merchant": "Store", "transactionType": "DEBIT"}]"""
        )
        val enhancer = CloudTransactionEnhancer(provider)

        val result = enhancer.enhance(listOf(
            PartialTransaction("15.01.2026", -100000, "KZT", "Store", false, true)
        ))

        assertEquals("DEBIT", result[0].transactionType)
    }

    @Test
    fun `detects credit transaction type`() = runTest {
        val provider = FakeLLMProvider(
            response = """[{"index": 0, "merchant": null, "transactionType": "CREDIT"}]"""
        )
        val enhancer = CloudTransactionEnhancer(provider)

        val result = enhancer.enhance(listOf(
            PartialTransaction("15.01.2026", 100000, "KZT", "Income", true, false)
        ))

        assertEquals("CREDIT", result[0].transactionType)
    }

    @Test
    fun `detects fee transaction type`() = runTest {
        val provider = FakeLLMProvider(
            response = """[{"index": 0, "merchant": "Bank", "transactionType": "FEE", "categoryHint": "bank_fees"}]"""
        )
        val enhancer = CloudTransactionEnhancer(provider)

        val result = enhancer.enhance(listOf(
            PartialTransaction("15.01.2026", -50000, "KZT", "Комиссия банка", false, true)
        ))

        assertEquals("FEE", result[0].transactionType)
    }

    // === Edge Cases ===

    @Test
    fun `handles empty transaction list`() = runTest {
        val provider = FakeLLMProvider(response = "[]")
        val enhancer = CloudTransactionEnhancer(provider)

        val result = enhancer.enhance(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `handles LLM returning partial results`() = runTest {
        val provider = FakeLLMProvider(
            response = """[{"index": 0, "merchant": "Store"}]""" // Missing optional fields
        )
        val enhancer = CloudTransactionEnhancer(provider)

        val result = enhancer.enhance(listOf(
            PartialTransaction("15.01.2026", -100000, "KZT", "Store", false, true)
        ))

        assertEquals(1, result.size)
        assertEquals("Store", result[0].merchant)
        assertNull(result[0].categoryHint)
        assertNull(result[0].transactionType)
    }

    @Test
    fun `preserves original transaction data`() = runTest {
        val provider = FakeLLMProvider(
            response = """[{"index": 0, "merchant": "Glovo"}]"""
        )
        val enhancer = CloudTransactionEnhancer(provider)

        val original = PartialTransaction(
            rawDate = "15.01.2026",
            amountMinorUnits = -150000,
            currency = "KZT",
            rawDescription = "Glovo order",
            isCredit = false,
            isDebit = true
        )

        val result = enhancer.enhance(listOf(original))

        assertEquals(original.rawDate, result[0].rawDate)
        assertEquals(original.amountMinorUnits, result[0].amountMinorUnits)
        assertEquals(original.currency, result[0].currency)
        assertEquals(original.rawDescription, result[0].rawDescription)
    }

    // === Error Handling ===

    @Test
    fun `returns empty enhancements on LLM error`() = runTest {
        val provider = FakeLLMProvider(shouldFail = true)
        val enhancer = CloudTransactionEnhancer(provider)

        val result = enhancer.enhance(listOf(
            PartialTransaction("15.01.2026", -100000, "KZT", "Store", false, true)
        ))

        // Should return original with empty enhancements
        assertEquals(1, result.size)
        assertNull(result[0].merchant)
    }

    @Test
    fun `handles malformed LLM response`() = runTest {
        val provider = FakeLLMProvider(response = "invalid json {{{")
        val enhancer = CloudTransactionEnhancer(provider)

        val result = enhancer.enhance(listOf(
            PartialTransaction("15.01.2026", -100000, "KZT", "Store", false, true)
        ))

        // Should return original with empty enhancements
        assertEquals(1, result.size)
        assertNull(result[0].merchant)
    }
}

/**
 * Fake LLM provider for testing.
 */
class FakeLLMProvider(
    private val response: String = "[]",
    private val shouldFail: Boolean = false
) : LLMProvider {

    override val name: String = "fake-provider"

    override val availableModels: List<ModelConfig> = listOf(
        ModelConfig(
            id = "fake-model",
            name = "Fake Model",
            maxTokens = 4096,
            costPer1kInputTokens = 0f,
            costPer1kOutputTokens = 0f,
            supportsStructuredOutput = true
        )
    )

    override suspend fun isAvailable(): Boolean = !shouldFail

    override suspend fun complete(request: CompletionRequest): CompletionResponse {
        if (shouldFail) throw RuntimeException("Provider failed")
        return CompletionResponse(
            content = response,
            inputTokens = 100,
            outputTokens = 50,
            model = "fake-model",
            finishReason = FinishReason.STOP
        )
    }

    override suspend fun chat(messages: List<ChatMessage>): CompletionResponse {
        if (shouldFail) throw RuntimeException("Provider failed")
        return CompletionResponse(
            content = response,
            inputTokens = 100,
            outputTokens = 50,
            model = "fake-model",
            finishReason = FinishReason.STOP
        )
    }

    override suspend fun structuredOutput(prompt: String, schema: String): CompletionResponse {
        if (shouldFail) throw RuntimeException("Provider failed")
        return CompletionResponse(
            content = response,
            inputTokens = 100,
            outputTokens = 50,
            model = "fake-model",
            finishReason = FinishReason.STOP
        )
    }
}

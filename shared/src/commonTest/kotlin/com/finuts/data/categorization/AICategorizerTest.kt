package com.finuts.data.categorization

import com.finuts.domain.entity.CategorizationSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for AICategorizer - Tier 2/3 LLM categorization.
 * Uses fake responses for unit tests.
 */
class AICategorizerTest {

    // --- Prompt Building ---

    @Test
    fun `buildPrompt creates valid prompt for single transaction`() {
        val prompt = AICategorizer.buildPrompt(
            transactions = listOf(
                TransactionForCategorization(
                    id = "tx-1",
                    description = "COFFEE HOUSE ALMATY",
                    amount = 150000L
                )
            ),
            categories = listOf("restaurants", "groceries", "shopping")
        )

        assertTrue(prompt.contains("COFFEE HOUSE ALMATY"))
        assertTrue(prompt.contains("restaurants"))
        assertTrue(prompt.contains("groceries"))
        assertTrue(prompt.contains("shopping"))
    }

    @Test
    fun `buildPrompt handles batch of transactions`() {
        val transactions = (1..5).map { i ->
            TransactionForCategorization(
                id = "tx-$i",
                description = "MERCHANT $i",
                amount = 10000L * i
            )
        }

        val prompt = AICategorizer.buildPrompt(
            transactions = transactions,
            categories = listOf("other")
        )

        transactions.forEach { tx ->
            assertTrue(prompt.contains(tx.description))
        }
    }

    @Test
    fun `buildPrompt formats amount correctly`() {
        val prompt = AICategorizer.buildPrompt(
            transactions = listOf(
                TransactionForCategorization(
                    id = "tx-1",
                    description = "TEST",
                    amount = 150050L // 1500.50 KZT in kopecks
                )
            ),
            categories = listOf("other")
        )

        assertTrue(prompt.contains("1500.50") || prompt.contains("1500,50"))
    }

    // --- Response Parsing ---

    @Test
    fun `parseResponse extracts single categorization`() {
        val response = """
            [
              {"transactionId": "tx-1", "categoryId": "restaurants", "confidence": 0.85}
            ]
        """.trimIndent()

        val results = AICategorizer.parseResponse(
            response = response,
            tier = CategorizationSource.LLM_TIER2
        )

        assertEquals(1, results.size)
        assertEquals("tx-1", results[0].transactionId)
        assertEquals("restaurants", results[0].categoryId)
        assertEquals(0.85f, results[0].confidence)
        assertEquals(CategorizationSource.LLM_TIER2, results[0].source)
    }

    @Test
    fun `parseResponse handles multiple results`() {
        val response = """
            [
              {"transactionId": "tx-1", "categoryId": "restaurants", "confidence": 0.90},
              {"transactionId": "tx-2", "categoryId": "groceries", "confidence": 0.85},
              {"transactionId": "tx-3", "categoryId": "transport", "confidence": 0.75}
            ]
        """.trimIndent()

        val results = AICategorizer.parseResponse(
            response = response,
            tier = CategorizationSource.LLM_TIER3
        )

        assertEquals(3, results.size)
        assertEquals("restaurants", results[0].categoryId)
        assertEquals("groceries", results[1].categoryId)
        assertEquals("transport", results[2].categoryId)
    }

    @Test
    fun `parseResponse handles empty array`() {
        val response = "[]"

        val results = AICategorizer.parseResponse(
            response = response,
            tier = CategorizationSource.LLM_TIER2
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun `parseResponse handles malformed JSON gracefully`() {
        val response = "invalid json"

        val results = AICategorizer.parseResponse(
            response = response,
            tier = CategorizationSource.LLM_TIER2
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun `parseResponse sets correct source for Tier2`() {
        val response = """
            [{"transactionId": "tx-1", "categoryId": "other", "confidence": 0.80}]
        """.trimIndent()

        val results = AICategorizer.parseResponse(
            response = response,
            tier = CategorizationSource.LLM_TIER2
        )

        assertEquals(CategorizationSource.LLM_TIER2, results[0].source)
    }

    @Test
    fun `parseResponse sets correct source for Tier3`() {
        val response = """
            [{"transactionId": "tx-1", "categoryId": "other", "confidence": 0.80}]
        """.trimIndent()

        val results = AICategorizer.parseResponse(
            response = response,
            tier = CategorizationSource.LLM_TIER3
        )

        assertEquals(CategorizationSource.LLM_TIER3, results[0].source)
    }

    // --- TransactionForCategorization ---

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
    fun `TransactionForCategorization formats amount as currency`() {
        val tx = TransactionForCategorization(
            id = "tx-1",
            description = "TEST",
            amount = 123456L // 1234.56
        )

        val formatted = tx.formattedAmount
        assertNotNull(formatted)
        assertTrue(formatted.contains("1234"))
    }
}

/**
 * Data class for transaction to be categorized by AI.
 */
data class TransactionForCategorization(
    val id: String,
    val description: String,
    val amount: Long
) {
    val formattedAmount: String
        get() = "%.2f".format(amount / 100.0)
}

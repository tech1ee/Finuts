package com.finuts.data.categorization

import com.finuts.domain.entity.CategorizationSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for RuleBasedCategorizer - Tier 1 categorization.
 * Combines merchant database, user history, and regex patterns.
 */
class RuleBasedCategorizerTest {

    private val merchantDatabase = MerchantDatabase()
    private val categorizer = RuleBasedCategorizer(merchantDatabase)

    // --- Merchant Database Integration ---

    @Test
    fun `uses merchant database for known merchants`() {
        val result = categorizer.categorize("tx-1", "MAGNUM SUPER ALMATY")
        assertNotNull(result)
        assertEquals("tx-1", result.transactionId)
        assertEquals("groceries", result.categoryId)
        assertEquals(CategorizationSource.MERCHANT_DATABASE, result.source)
    }

    @Test
    fun `returns high confidence for merchant database matches`() {
        val result = categorizer.categorize("tx-1", "WOLT DELIVERY")
        assertNotNull(result)
        assertTrue(result.confidence >= 0.90f)
    }

    // --- User History ---

    @Test
    fun `uses user history when provided`() {
        val userHistory = mapOf(
            "COFFEE SHOP" to "restaurants"
        )
        val categorizerWithHistory = RuleBasedCategorizer(
            merchantDatabase = merchantDatabase,
            userHistory = userHistory
        )

        val result = categorizerWithHistory.categorize("tx-1", "COFFEE SHOP ORDER 123")
        assertNotNull(result)
        assertEquals("restaurants", result.categoryId)
        assertEquals(CategorizationSource.USER_HISTORY, result.source)
    }

    @Test
    fun `user history has high confidence`() {
        val userHistory = mapOf(
            "MY FAVORITE STORE" to "shopping"
        )
        val categorizerWithHistory = RuleBasedCategorizer(
            merchantDatabase = merchantDatabase,
            userHistory = userHistory
        )

        val result = categorizerWithHistory.categorize("tx-1", "MY FAVORITE STORE")
        assertNotNull(result)
        assertTrue(result.confidence >= 0.90f)
    }

    @Test
    fun `merchant database takes priority over user history`() {
        val userHistory = mapOf(
            "MAGNUM" to "other"  // Wrong category in user history
        )
        val categorizerWithHistory = RuleBasedCategorizer(
            merchantDatabase = merchantDatabase,
            userHistory = userHistory
        )

        val result = categorizerWithHistory.categorize("tx-1", "MAGNUM SUPER")
        assertNotNull(result)
        // Merchant database should win
        assertEquals("groceries", result.categoryId)
        assertEquals(CategorizationSource.MERCHANT_DATABASE, result.source)
    }

    // --- Regex Patterns ---

    @Test
    fun `uses regex patterns for common transaction types`() {
        // ATM withdrawal pattern
        val result = categorizer.categorize("tx-1", "ATM WITHDRAWAL HALYK")
        assertNotNull(result)
        assertEquals("transfer", result.categoryId)
        assertEquals(CategorizationSource.RULE_BASED, result.source)
    }

    @Test
    fun `matches salary deposit pattern`() {
        val result = categorizer.categorize("tx-1", "ЗАРПЛАТА ЗА ДЕКАБРЬ")
        assertNotNull(result)
        assertEquals("salary", result.categoryId)
    }

    @Test
    fun `matches income patterns`() {
        val result = categorizer.categorize("tx-1", "SALARY PAYMENT COMPANY")
        assertNotNull(result)
        assertEquals("salary", result.categoryId)
    }

    // --- Edge Cases ---

    @Test
    fun `returns null for unknown transactions`() {
        val result = categorizer.categorize("tx-1", "RANDOM UNKNOWN TX 12345")
        assertNull(result)
    }

    @Test
    fun `handles empty description`() {
        val result = categorizer.categorize("tx-1", "")
        assertNull(result)
    }

    @Test
    fun `handles whitespace-only description`() {
        val result = categorizer.categorize("tx-1", "   ")
        assertNull(result)
    }

    @Test
    fun `is case insensitive`() {
        val upper = categorizer.categorize("tx-1", "YANDEX TAXI")
        val lower = categorizer.categorize("tx-1", "yandex taxi")
        val mixed = categorizer.categorize("tx-1", "Yandex Taxi")

        assertNotNull(upper)
        assertNotNull(lower)
        assertNotNull(mixed)
        assertEquals(upper.categoryId, lower.categoryId)
        assertEquals(upper.categoryId, mixed.categoryId)
    }

    // --- Transaction ID Propagation ---

    @Test
    fun `sets correct transaction ID in result`() {
        val txId = "unique-transaction-id-123"
        val result = categorizer.categorize(txId, "MAGNUM ALMATY")
        assertNotNull(result)
        assertEquals(txId, result.transactionId)
    }

    // --- Batch Processing ---

    @Test
    fun `categorizes multiple transactions efficiently`() {
        val transactions = listOf(
            "tx-1" to "MAGNUM ALMATY",
            "tx-2" to "WOLT DELIVERY",
            "tx-3" to "YANDEX TAXI",
            "tx-4" to "UNKNOWN MERCHANT",
            "tx-5" to "BEELINE PAYMENT"
        )

        val results = transactions.mapNotNull { (id, desc) ->
            categorizer.categorize(id, desc)
        }

        assertEquals(4, results.size)
        assertEquals("tx-1", results[0].transactionId)
        assertEquals("groceries", results[0].categoryId)
    }

    // --- Additional Rule Pattern Tests ---

    @Test
    fun `matches pension deposit pattern`() {
        val result = categorizer.categorize("tx-1", "ПЕНСИЯ ЗА ЯНВАРЬ")
        assertNotNull(result)
        assertEquals("salary", result.categoryId)
    }

    @Test
    fun `matches scholarship pattern`() {
        val result = categorizer.categorize("tx-1", "СТИПЕНДИЯ УНИВЕРСИТЕТ")
        assertNotNull(result)
        assertEquals("salary", result.categoryId)
    }

    @Test
    fun `matches dividend pattern`() {
        val result = categorizer.categorize("tx-1", "ДИВИДЕНДЫ ЗА 2025")
        assertNotNull(result)
        assertEquals("salary", result.categoryId)
    }

    @Test
    fun `matches refund pattern`() {
        val result = categorizer.categorize("tx-1", "ВОЗВРАТ СРЕДСТВ")
        assertNotNull(result)
        assertEquals("other", result.categoryId)
    }

    @Test
    fun `matches cashback pattern`() {
        val result = categorizer.categorize("tx-1", "КЭШБЭК ЗА ПОКУПКИ")
        assertNotNull(result)
        assertEquals("other", result.categoryId)
    }

    @Test
    fun `matches cash withdrawal pattern`() {
        val result = categorizer.categorize("tx-1", "СНЯТИЕ НАЛИЧНЫХ БАНКОМАТ")
        assertNotNull(result)
        assertEquals("transfer", result.categoryId)
    }

    @Test
    fun `matches bankомат Cyrillic pattern`() {
        val result = categorizer.categorize("tx-1", "БАНКОМАТ HALYK BANK")
        assertNotNull(result)
        assertEquals("transfer", result.categoryId)
    }

    @Test
    fun `matches interest pattern`() {
        val result = categorizer.categorize("tx-1", "ПРОЦЕНТ ПО ДЕПОЗИТУ")
        assertNotNull(result)
        assertEquals("other", result.categoryId)
    }

    // --- User History Edge Cases ---

    @Test
    fun `user history is case insensitive`() {
        val userHistory = mapOf(
            "CUSTOM STORE" to "shopping"
        )
        val categorizerWithHistory = RuleBasedCategorizer(
            merchantDatabase = merchantDatabase,
            userHistory = userHistory
        )

        val result = categorizerWithHistory.categorize("tx-1", "custom store almaty")
        assertNotNull(result)
        assertEquals("shopping", result.categoryId)
    }

    @Test
    fun `user history partial match works`() {
        val userHistory = mapOf(
            "SPECIAL" to "entertainment"
        )
        val categorizerWithHistory = RuleBasedCategorizer(
            merchantDatabase = merchantDatabase,
            userHistory = userHistory
        )

        val result = categorizerWithHistory.categorize("tx-1", "MY SPECIAL EVENT TICKET")
        assertNotNull(result)
        assertEquals("entertainment", result.categoryId)
    }
}

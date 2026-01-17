package com.finuts.data.categorization

import com.finuts.domain.entity.CategorizationSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for MerchantDatabase - Tier 1 merchant pattern matching.
 */
class MerchantDatabaseTest {

    private val database = MerchantDatabase()

    // --- Groceries ---

    @Test
    fun `matches Magnum supermarket`() {
        val result = database.findMatch("MAGNUM SUPER ALMATY")
        assertNotNull(result)
        assertEquals("groceries", result.categoryId)
        assertTrue(result.confidence >= 0.90f)
        assertEquals(CategorizationSource.MERCHANT_DATABASE, result.source)
    }

    @Test
    fun `matches Small supermarket`() {
        val result = database.findMatch("SMALL 123 ASTANA")
        assertNotNull(result)
        assertEquals("groceries", result.categoryId)
    }

    @Test
    fun `matches Metro Cash and Carry`() {
        val result = database.findMatch("METRO CASH AND CARRY")
        assertNotNull(result)
        assertEquals("groceries", result.categoryId)
    }

    @Test
    fun `matches Arbuz grocery delivery`() {
        val result = database.findMatch("ARBUZ.KZ ORDER")
        assertNotNull(result)
        assertEquals("groceries", result.categoryId)
    }

    // --- Food Delivery ---

    @Test
    fun `matches Wolt delivery`() {
        val result = database.findMatch("WOLT DELIVERY KZ")
        assertNotNull(result)
        assertEquals("food_delivery", result.categoryId)
        assertTrue(result.confidence >= 0.95f)
    }

    @Test
    fun `matches Glovo delivery`() {
        val result = database.findMatch("GLOVO ALMATY")
        assertNotNull(result)
        assertEquals("food_delivery", result.categoryId)
    }

    @Test
    fun `matches Chocofood delivery`() {
        val result = database.findMatch("CHOCOFOOD ORDER 12345")
        assertNotNull(result)
        assertEquals("food_delivery", result.categoryId)
    }

    // --- Transport ---

    @Test
    fun `matches Yandex Taxi`() {
        val result = database.findMatch("YANDEX TAXI TRIP")
        assertNotNull(result)
        assertEquals("transport", result.categoryId)
        assertTrue(result.confidence >= 0.95f)
    }

    @Test
    fun `matches InDriver`() {
        val result = database.findMatch("INDRIVER ALMATY")
        assertNotNull(result)
        assertEquals("transport", result.categoryId)
    }

    @Test
    fun `matches Onay transit card`() {
        val result = database.findMatch("ONAY BUS PAYMENT")
        assertNotNull(result)
        assertEquals("transport", result.categoryId)
    }

    @Test
    fun `matches DiDi ride`() {
        val result = database.findMatch("DIDI RIDE SERVICE")
        assertNotNull(result)
        assertEquals("transport", result.categoryId)
    }

    // --- Utilities ---

    @Test
    fun `matches AlmatyEnergo`() {
        val result = database.findMatch("АЛМАТЫЭНЕРГОСБЫТ")
        assertNotNull(result)
        assertEquals("utilities", result.categoryId)
        assertTrue(result.confidence >= 0.95f)
    }

    @Test
    fun `matches Kazakhtelecom`() {
        val result = database.findMatch("КАЗАХТЕЛЕКОМ ОПЛАТА")
        assertNotNull(result)
        assertEquals("utilities", result.categoryId)
    }

    @Test
    fun `matches Beeline mobile`() {
        val result = database.findMatch("BEELINE KZ MOBILE")
        assertNotNull(result)
        assertEquals("utilities", result.categoryId)
    }

    @Test
    fun `matches Tele2 mobile`() {
        val result = database.findMatch("TELE2 KAZAKHSTAN")
        assertNotNull(result)
        assertEquals("utilities", result.categoryId)
    }

    // --- Entertainment ---

    @Test
    fun `matches Kinopark cinema`() {
        val result = database.findMatch("KINOPARK ALMATY MEGA")
        assertNotNull(result)
        assertEquals("entertainment", result.categoryId)
    }

    @Test
    fun `matches Chaplin cinema`() {
        val result = database.findMatch("CHAPLIN CINEMAS")
        assertNotNull(result)
        assertEquals("entertainment", result.categoryId)
    }

    @Test
    fun `matches Netflix subscription`() {
        val result = database.findMatch("NETFLIX.COM SUBSCRIPTION")
        assertNotNull(result)
        assertEquals("entertainment", result.categoryId)
    }

    @Test
    fun `matches Spotify subscription`() {
        val result = database.findMatch("SPOTIFY PREMIUM")
        assertNotNull(result)
        assertEquals("entertainment", result.categoryId)
    }

    // --- Shopping ---

    @Test
    fun `matches Kaspi Magazin`() {
        val result = database.findMatch("KASPI MAGAZIN ALMATY")
        assertNotNull(result)
        assertEquals("shopping", result.categoryId)
    }

    @Test
    fun `matches Sulpak electronics`() {
        val result = database.findMatch("SULPAK STORE")
        assertNotNull(result)
        assertEquals("shopping", result.categoryId)
    }

    @Test
    fun `matches Technodom`() {
        val result = database.findMatch("TECHNODOM ELECTRONICS")
        assertNotNull(result)
        assertEquals("shopping", result.categoryId)
    }

    @Test
    fun `matches Wildberries`() {
        val result = database.findMatch("WILDBERRIES ORDER")
        assertNotNull(result)
        assertEquals("shopping", result.categoryId)
    }

    // --- Healthcare ---

    @Test
    fun `matches pharmacy chain`() {
        val result = database.findMatch("EUROPHARMA ALMATY")
        assertNotNull(result)
        assertEquals("healthcare", result.categoryId)
    }

    // --- Transfer Detection ---

    @Test
    fun `matches Kaspi transfer`() {
        val result = database.findMatch("KASPI PEREVOD NA KARTU")
        assertNotNull(result)
        assertEquals("transfer", result.categoryId)
    }

    // --- Edge Cases ---

    @Test
    fun `returns null for unknown merchant`() {
        val result = database.findMatch("UNKNOWN COMPANY XYZ")
        assertNull(result)
    }

    @Test
    fun `matches case insensitively`() {
        val upper = database.findMatch("MAGNUM ALMATY")
        val lower = database.findMatch("magnum almaty")
        val mixed = database.findMatch("Magnum Almaty")

        assertNotNull(upper)
        assertNotNull(lower)
        assertNotNull(mixed)
        assertEquals(upper.categoryId, lower.categoryId)
        assertEquals(upper.categoryId, mixed.categoryId)
    }

    @Test
    fun `handles empty string`() {
        val result = database.findMatch("")
        assertNull(result)
    }

    @Test
    fun `handles whitespace only`() {
        val result = database.findMatch("   ")
        assertNull(result)
    }

    @Test
    fun `all patterns have valid category ids`() {
        val validCategories = setOf(
            "groceries", "food_delivery", "restaurants", "transport",
            "utilities", "entertainment", "shopping", "healthcare",
            "education", "travel", "transfer", "salary", "other"
        )
        val allPatterns = database.getAllPatterns()

        allPatterns.forEach { pattern ->
            assertTrue(
                pattern.categoryId in validCategories,
                "Invalid category: ${pattern.categoryId}"
            )
        }
    }

    @Test
    fun `all patterns have confidence between 0 and 1`() {
        val allPatterns = database.getAllPatterns()

        allPatterns.forEach { pattern ->
            assertTrue(pattern.confidence in 0f..1f)
        }
    }

    @Test
    fun `database contains at least 100 patterns`() {
        val count = database.getAllPatterns().size
        assertTrue(count >= 100, "Expected at least 100 patterns, got $count")
    }

    // --- findMatchForTransaction Tests ---

    @Test
    fun `findMatchForTransaction returns result with transaction ID`() {
        val result = database.findMatchForTransaction("tx-123", "MAGNUM ALMATY")
        assertNotNull(result)
        assertEquals("tx-123", result.transactionId)
        assertEquals("groceries", result.categoryId)
    }

    @Test
    fun `findMatchForTransaction returns null for unknown merchant`() {
        val result = database.findMatchForTransaction("tx-456", "UNKNOWN COMPANY")
        assertNull(result)
    }

    @Test
    fun `findMatchForTransaction handles empty description`() {
        val result = database.findMatchForTransaction("tx-789", "")
        assertNull(result)
    }

    // --- getPatternCountByCategory Tests ---

    @Test
    fun `getPatternCountByCategory returns map of category counts`() {
        val counts = database.getPatternCountByCategory()

        assertTrue(counts.isNotEmpty())
        assertTrue(counts.containsKey("groceries"))
        assertTrue(counts.containsKey("transport"))
        assertTrue(counts.containsKey("food_delivery"))
        assertTrue(counts["groceries"]!! > 0)
    }

    @Test
    fun `getPatternCountByCategory sums correctly`() {
        val counts = database.getPatternCountByCategory()
        val totalFromCounts = counts.values.sum()
        val totalPatterns = database.getAllPatterns().size

        assertEquals(totalPatterns, totalFromCounts)
    }
}

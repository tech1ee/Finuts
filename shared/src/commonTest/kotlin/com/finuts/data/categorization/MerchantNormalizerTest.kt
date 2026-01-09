package com.finuts.data.categorization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for MerchantNormalizer.
 * Ensures consistent merchant name normalization for pattern matching.
 */
class MerchantNormalizerTest {

    // --- Basic Normalization ---

    @Test
    fun `normalizes to uppercase`() {
        val result = MerchantNormalizer.normalize("magnum super")
        assertEquals("MAGNUM SUPER", result)
    }

    @Test
    fun `trims whitespace`() {
        val result = MerchantNormalizer.normalize("  MAGNUM  ")
        assertEquals("MAGNUM", result)
    }

    @Test
    fun `collapses multiple spaces`() {
        // Note: ALMATY is removed because it's a location prefix
        val result = MerchantNormalizer.normalize("MAGNUM    SUPER    MARKET")
        assertEquals("MAGNUM SUPER MARKET", result)
    }

    @Test
    fun `handles empty string`() {
        val result = MerchantNormalizer.normalize("")
        assertEquals("", result)
    }

    @Test
    fun `handles blank string`() {
        val result = MerchantNormalizer.normalize("   ")
        assertEquals("", result)
    }

    // --- Edge Cases: Fallback handling ---

    @Test
    fun `handles location-only merchant name with fallback`() {
        // ALMATY alone would be filtered out, but fallback preserves it
        val result = MerchantNormalizer.normalize("ALMATY")
        assertEquals("ALMATY", result)
    }

    @Test
    fun `handles all-filtered content with numeric fallback`() {
        // Only numbers and symbols - should extract something useful or take first chars
        val result = MerchantNormalizer.normalize("*1234 15/01/2025")
        // Since there are no alphabetic chars >= 2 chars, falls back to take(20)
        assertTrue(result.isNotBlank())
    }

    // --- Business Suffix Removal ---

    @Test
    fun `removes TOO suffix`() {
        val result = MerchantNormalizer.normalize("МАГНУМ ТОО")
        assertEquals("МАГНУМ", result)
    }

    @Test
    fun `removes LLC suffix`() {
        val result = MerchantNormalizer.normalize("AMAZON LLC")
        assertEquals("AMAZON", result)
    }

    @Test
    fun `removes multiple suffixes`() {
        val result = MerchantNormalizer.normalize("COMPANY LTD INC")
        assertEquals("COMPANY", result)
    }

    @Test
    fun `removes АО suffix`() {
        val result = MerchantNormalizer.normalize("KASPI АО")
        assertEquals("KASPI", result)
    }

    // --- Location Removal ---

    @Test
    fun `removes city names`() {
        val result = MerchantNormalizer.normalize("ALMATY MAGNUM SUPER")
        assertEquals("MAGNUM SUPER", result)
    }

    @Test
    fun `removes branch indicators`() {
        val result = MerchantNormalizer.normalize("ФИЛИАЛ МАГНУМ АСТАНА")
        assertEquals("МАГНУМ", result)
    }

    // --- Pattern Removal ---

    @Test
    fun `removes card masks`() {
        val result = MerchantNormalizer.normalize("KASPI *1234")
        assertEquals("KASPI", result)
    }

    @Test
    fun `removes terminal IDs`() {
        val result = MerchantNormalizer.normalize("MAGNUM TERMINAL 12345678")
        assertEquals("MAGNUM", result)
    }

    @Test
    fun `removes dates`() {
        val result = MerchantNormalizer.normalize("PAYMENT 15/01/2025")
        assertEquals("PAYMENT", result)
    }

    @Test
    fun `removes times`() {
        val result = MerchantNormalizer.normalize("MAGNUM 14:30")
        assertEquals("MAGNUM", result)
    }

    @Test
    fun `removes order numbers`() {
        val result = MerchantNormalizer.normalize("WOLT ORDER #123456")
        assertEquals("WOLT ORDER", result)
    }

    // --- Cyrillic Support ---

    @Test
    fun `handles Cyrillic text`() {
        val result = MerchantNormalizer.normalize("МАГНУМ СУПЕР")
        assertEquals("МАГНУМ СУПЕР", result)
    }

    @Test
    fun `handles mixed Latin and Cyrillic`() {
        val result = MerchantNormalizer.normalize("KASPI БАНК")
        assertEquals("KASPI БАНК", result)
    }

    // --- Keyword Extraction ---

    @Test
    fun `extracts keywords from merchant name`() {
        val keywords = MerchantNormalizer.extractKeywords("MAGNUM SUPER ALMATY")
        assertTrue(keywords.contains("MAGNUM"))
        assertTrue(keywords.contains("SUPER"))
    }

    @Test
    fun `filters out common words`() {
        val keywords = MerchantNormalizer.extractKeywords("THE COFFEE AND TEA SHOP")
        assertFalse(keywords.contains("THE"))
        assertFalse(keywords.contains("AND"))
    }

    @Test
    fun `limits keywords to 5`() {
        val keywords = MerchantNormalizer.extractKeywords(
            "VERY LONG MERCHANT NAME WITH MANY WORDS HERE TODAY"
        )
        assertTrue(keywords.size <= 5)
    }

    @Test
    fun `returns empty list for empty input`() {
        val keywords = MerchantNormalizer.extractKeywords("")
        assertTrue(keywords.isEmpty())
    }

    // --- Similarity Detection ---

    @Test
    fun `detects similar merchants - exact match`() {
        val similar = MerchantNormalizer.isSimilar("MAGNUM", "MAGNUM")
        assertTrue(similar)
    }

    @Test
    fun `detects similar merchants - after normalization`() {
        val similar = MerchantNormalizer.isSimilar(
            "MAGNUM SUPER ALMATY",
            "magnum super astana"
        )
        assertTrue(similar)
    }

    @Test
    fun `detects similar merchants - one contains other`() {
        val similar = MerchantNormalizer.isSimilar("MAGNUM", "MAGNUM SUPER")
        assertTrue(similar)
    }

    @Test
    fun `detects different merchants`() {
        val similar = MerchantNormalizer.isSimilar("MAGNUM", "SMALL")
        assertFalse(similar)
    }

    @Test
    fun `handles empty in similarity check`() {
        val similar = MerchantNormalizer.isSimilar("MAGNUM", "")
        assertFalse(similar)
    }

    // --- Pattern Generation ---

    @Test
    fun `generates pattern from normalized name`() {
        val pattern = MerchantNormalizer.toPattern("MAGNUM SUPER")
        assertTrue(pattern.isNotEmpty())
    }

    @Test
    fun `pattern contains primary keywords`() {
        // toPattern returns first 2 keywords for SQL LIKE matching
        val pattern = MerchantNormalizer.toPattern("MAGNUM SUPER STORE")
        assertEquals("MAGNUM SUPER", pattern) // First 2 keywords
    }

    @Test
    fun `pattern for single keyword merchant`() {
        val pattern = MerchantNormalizer.toPattern("GLOVO")
        assertEquals("GLOVO", pattern)
    }

    @Test
    fun `returns empty pattern for empty input`() {
        val pattern = MerchantNormalizer.toPattern("")
        assertEquals("", pattern)
    }

    // --- Real World Examples ---

    @Test
    fun `normalizes Kaspi transaction`() {
        val result = MerchantNormalizer.normalize("KASPI GOLD *1234 15.01.2025 14:30")
        assertEquals("KASPI GOLD", result)
    }

    @Test
    fun `normalizes Glovo transaction`() {
        val result = MerchantNormalizer.normalize("GLOVO ORDER #789456 ALMATY")
        assertEquals("GLOVO ORDER", result)
    }

    @Test
    fun `normalizes Wolt transaction`() {
        val result = MerchantNormalizer.normalize("WOLT*RESTAURANT DELIVERY")
        assertEquals("WOLT RESTAURANT DELIVERY", result)
    }

    @Test
    fun `normalizes Kazakhstan business`() {
        val result = MerchantNormalizer.normalize("ТОО МАГНУМ КЭШ ЭНД КЕРРИ")
        assertEquals("МАГНУМ КЭШ ЭНД КЕРРИ", result)
    }
}

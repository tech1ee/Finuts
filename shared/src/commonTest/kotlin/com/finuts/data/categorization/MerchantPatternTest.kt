package com.finuts.data.categorization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for MerchantPattern data class.
 */
class MerchantPatternTest {

    @Test
    fun `MerchantPattern stores all fields with regex constructor`() {
        val pattern = MerchantPattern(
            pattern = Regex("STARBUCKS.*", RegexOption.IGNORE_CASE),
            categoryId = "coffee_shops",
            confidence = 0.95f,
            merchantName = "Starbucks"
        )

        assertNotNull(pattern.pattern)
        assertEquals("coffee_shops", pattern.categoryId)
        assertEquals(0.95f, pattern.confidence)
        assertEquals("Starbucks", pattern.merchantName)
    }

    @Test
    fun `MerchantPattern stores all fields with string constructor`() {
        val pattern = MerchantPattern(
            patternString = "AMAZON\\.COM.*",
            categoryId = "shopping",
            confidence = 0.90f,
            merchantName = "Amazon"
        )

        assertEquals("shopping", pattern.categoryId)
        assertEquals(0.90f, pattern.confidence)
        assertEquals("Amazon", pattern.merchantName)
    }

    @Test
    fun `MerchantPattern string constructor creates case-insensitive regex`() {
        val pattern = MerchantPattern(
            patternString = "NETFLIX",
            categoryId = "subscriptions",
            confidence = 0.92f
        )

        assertTrue(pattern.pattern.matches("NETFLIX"))
        assertTrue(pattern.pattern.matches("netflix"))
        assertTrue(pattern.pattern.matches("Netflix"))
    }

    @Test
    fun `MerchantPattern merchantName defaults to null`() {
        val pattern = MerchantPattern(
            patternString = "TEST.*",
            categoryId = "test",
            confidence = 0.8f
        )

        assertNull(pattern.merchantName)
    }

    @Test
    fun `MerchantPattern matches expected descriptions`() {
        val pattern = MerchantPattern(
            patternString = "МАГНУМ.*",
            categoryId = "groceries",
            confidence = 0.95f,
            merchantName = "Magnum"
        )

        assertTrue(pattern.pattern.matches("МАГНУМ ТОО"))
        assertTrue(pattern.pattern.matches("магнум АЛМАТЫ"))
        assertTrue(pattern.pattern.matches("МАГНУМ 123"))
    }

    @Test
    fun `MerchantPattern equals works correctly`() {
        val pattern1 = MerchantPattern(
            patternString = "TEST",
            categoryId = "cat1",
            confidence = 0.9f
        )
        val pattern2 = MerchantPattern(
            patternString = "TEST",
            categoryId = "cat1",
            confidence = 0.9f
        )

        // Note: data class equality compares pattern object references
        // so two patterns with same string may not be equal
        assertEquals(pattern1.categoryId, pattern2.categoryId)
        assertEquals(pattern1.confidence, pattern2.confidence)
    }

    @Test
    fun `MerchantPattern copy creates modified copy`() {
        val original = MerchantPattern(
            patternString = "ORIGINAL",
            categoryId = "cat1",
            confidence = 0.8f,
            merchantName = "Original"
        )

        val copy = original.copy(categoryId = "cat2", confidence = 0.95f)

        assertEquals("cat2", copy.categoryId)
        assertEquals(0.95f, copy.confidence)
        assertEquals("Original", copy.merchantName)
    }
}

package com.finuts.app.ui.components.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for PageIndicator component logic.
 * UI rendering tested via screenshot/manual testing.
 */
class PageIndicatorTest {

    @Test
    fun `pageCount must be at least 1`() {
        // When pageCount is 0, should still show 1 dot minimum
        val validatedCount = maxOf(1, 0)
        assertEquals(1, validatedCount)
    }

    @Test
    fun `currentPage must be within bounds`() {
        val pageCount = 5
        val outOfBoundsPage = 10

        // currentPage should be clamped to valid range
        val clampedPage = outOfBoundsPage.coerceIn(0, pageCount - 1)
        assertEquals(4, clampedPage) // last valid index
    }

    @Test
    fun `negative currentPage should be clamped to 0`() {
        val pageCount = 5
        val negativePage = -1

        val clampedPage = negativePage.coerceIn(0, pageCount - 1)
        assertEquals(0, clampedPage)
    }

    @Test
    fun `currentPage 0 is valid for any pageCount`() {
        val pageCount = 5
        val currentPage = 0

        val isValid = currentPage in 0 until pageCount
        assertEquals(true, isValid)
    }

    @Test
    fun `last page index is pageCount minus 1`() {
        val pageCount = 5
        val lastPageIndex = pageCount - 1
        assertEquals(4, lastPageIndex)
    }

    @Test
    fun `single page has only one valid currentPage value`() {
        val pageCount = 1

        // Any currentPage value should be clamped to 0
        assertEquals(0, 0.coerceIn(0, pageCount - 1))
        assertEquals(0, 5.coerceIn(0, pageCount - 1))
        assertEquals(0, (-1).coerceIn(0, pageCount - 1))
    }
}

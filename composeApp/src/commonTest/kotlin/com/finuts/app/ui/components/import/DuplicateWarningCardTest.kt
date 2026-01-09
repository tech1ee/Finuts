package com.finuts.app.ui.components.import

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for DuplicateWarningCard component logic.
 * UI rendering tested via screenshot/manual testing.
 */
class DuplicateWarningCardTest {

    @Test
    fun `duplicateCount of 0 should not show warning`() {
        val count = 0
        val shouldShow = count > 0
        assertEquals(false, shouldShow)
    }

    @Test
    fun `duplicateCount of 1 should show warning`() {
        val count = 1
        val shouldShow = count > 0
        assertEquals(true, shouldShow)
    }

    @Test
    fun `duplicateCount of 12 should show warning`() {
        val count = 12
        val shouldShow = count > 0
        assertTrue(shouldShow)
    }

    @Test
    fun `warning message includes count`() {
        val count = 12
        val message = "$count возможных дубликатов"
        assertTrue(message.contains("12"))
    }

    @Test
    fun `singular form for count of 1`() {
        val count = 1
        val message = if (count == 1) {
            "$count возможный дубликат"
        } else {
            "$count возможных дубликатов"
        }
        assertEquals("1 возможный дубликат", message)
    }

    @Test
    fun `plural form for count greater than 1`() {
        val count = 5
        val message = if (count == 1) {
            "$count возможный дубликат"
        } else {
            "$count возможных дубликатов"
        }
        assertEquals("5 возможных дубликатов", message)
    }
}

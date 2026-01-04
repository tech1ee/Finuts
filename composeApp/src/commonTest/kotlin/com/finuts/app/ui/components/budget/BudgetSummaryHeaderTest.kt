package com.finuts.app.ui.components.budget

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for BudgetSummaryHeader helper functions.
 *
 * TDD: These tests are written BEFORE implementation.
 */
class BudgetSummaryHeaderTest {

    // === Summary Calculation Tests ===

    @Test
    fun `calculates overall percentage correctly`() {
        val percent = calculateOverallPercentage(
            totalSpent = 72_000_00L,
            totalBudgeted = 100_000_00L
        )
        assertEquals(72f, percent, 0.1f)
    }

    @Test
    fun `overall percentage is zero when no budget`() {
        val percent = calculateOverallPercentage(
            totalSpent = 0L,
            totalBudgeted = 0L
        )
        assertEquals(0f, percent, 0.1f)
    }

    @Test
    fun `overall percentage can exceed 100`() {
        val percent = calculateOverallPercentage(
            totalSpent = 150_000_00L,
            totalBudgeted = 100_000_00L
        )
        assertEquals(150f, percent, 0.1f)
    }

    // === Summary Text Tests ===

    @Test
    fun `formats summary amounts correctly`() {
        val text = formatSummaryText(
            totalSpent = 145_000_00L,
            totalBudgeted = 200_000_00L,
            currencySymbol = "₸"
        )
        assertEquals("₸145,000 of ₸200,000", text)
    }

    @Test
    fun `formats percentage text correctly`() {
        val text = formatPercentageText(percentUsed = 72f)
        assertEquals("72% used", text)
    }
}

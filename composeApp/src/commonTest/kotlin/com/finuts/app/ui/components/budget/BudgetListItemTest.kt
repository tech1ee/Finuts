package com.finuts.app.ui.components.budget

import com.finuts.domain.entity.BudgetPeriod
import com.finuts.domain.entity.BudgetStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for BudgetListItem helper functions.
 *
 * TDD: These tests are written BEFORE implementation.
 */
class BudgetListItemTest {

    // === Period Label Tests ===

    @Test
    fun `period label for DAILY`() {
        val label = getBudgetPeriodLabel(BudgetPeriod.DAILY)
        assertEquals("Daily", label)
    }

    @Test
    fun `period label for WEEKLY`() {
        val label = getBudgetPeriodLabel(BudgetPeriod.WEEKLY)
        assertEquals("Weekly", label)
    }

    @Test
    fun `period label for MONTHLY`() {
        val label = getBudgetPeriodLabel(BudgetPeriod.MONTHLY)
        assertEquals("Monthly", label)
    }

    @Test
    fun `period label for QUARTERLY`() {
        val label = getBudgetPeriodLabel(BudgetPeriod.QUARTERLY)
        assertEquals("Quarterly", label)
    }

    @Test
    fun `period label for YEARLY`() {
        val label = getBudgetPeriodLabel(BudgetPeriod.YEARLY)
        assertEquals("Yearly", label)
    }

    // === Remaining/Overspent Text Tests ===

    @Test
    fun `remaining text when under budget`() {
        val text = getRemainingText(
            spent = 50_000_00L,
            budget = 100_000_00L,
            currencySymbol = "₸"
        )
        assertEquals("₸50,000 left", text)
    }

    @Test
    fun `overspent text when over budget`() {
        val text = getRemainingText(
            spent = 120_000_00L,
            budget = 100_000_00L,
            currencySymbol = "₸"
        )
        assertEquals("₸20,000 over", text)
    }

    @Test
    fun `zero remaining when exactly at budget`() {
        val text = getRemainingText(
            spent = 100_000_00L,
            budget = 100_000_00L,
            currencySymbol = "₸"
        )
        assertEquals("₸0 left", text)
    }

    // === Status Tests ===

    @Test
    fun `status ON_TRACK when under 80 percent`() {
        val status = calculateBudgetStatus(percentUsed = 50f)
        assertEquals(BudgetStatus.ON_TRACK, status)
    }

    @Test
    fun `status WARNING when 80 to 99 percent`() {
        val status = calculateBudgetStatus(percentUsed = 85f)
        assertEquals(BudgetStatus.WARNING, status)
    }

    @Test
    fun `status OVER_BUDGET when 100 plus percent`() {
        val status = calculateBudgetStatus(percentUsed = 110f)
        assertEquals(BudgetStatus.OVER_BUDGET, status)
    }
}

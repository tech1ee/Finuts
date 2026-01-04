package com.finuts.app.feature.budgets

import com.finuts.app.feature.budgets.components.formatRemainingAmount
import com.finuts.app.feature.budgets.components.getBudgetStatusFromPercent
import com.finuts.domain.entity.BudgetStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for BudgetDetailScreen helper functions.
 *
 * TDD: These tests are written BEFORE implementation.
 */
class BudgetDetailScreenTest {

    // === Format Remaining/Overspent Tests ===

    @Test
    fun `formatRemainingAmount shows remaining when under budget`() {
        val result = formatRemainingAmount(
            spent = 50_000_00L,
            budget = 100_000_00L,
            currencySymbol = "₸"
        )
        assertEquals("₸50,000 remaining", result)
    }

    @Test
    fun `formatRemainingAmount shows overspent when over budget`() {
        val result = formatRemainingAmount(
            spent = 120_000_00L,
            budget = 100_000_00L,
            currencySymbol = "₸"
        )
        assertEquals("₸20,000 overspent", result)
    }

    @Test
    fun `formatRemainingAmount shows zero remaining at exact budget`() {
        val result = formatRemainingAmount(
            spent = 100_000_00L,
            budget = 100_000_00L,
            currencySymbol = "₸"
        )
        assertEquals("₸0 remaining", result)
    }

    // === Budget Status Calculation Tests ===

    @Test
    fun `getBudgetStatusFromPercent returns ON_TRACK under 80`() {
        val status = getBudgetStatusFromPercent(50f)
        assertEquals(BudgetStatus.ON_TRACK, status)
    }

    @Test
    fun `getBudgetStatusFromPercent returns WARNING at 80 to 99`() {
        val status = getBudgetStatusFromPercent(85f)
        assertEquals(BudgetStatus.WARNING, status)
    }

    @Test
    fun `getBudgetStatusFromPercent returns OVER_BUDGET at 100 plus`() {
        val status = getBudgetStatusFromPercent(110f)
        assertEquals(BudgetStatus.OVER_BUDGET, status)
    }
}

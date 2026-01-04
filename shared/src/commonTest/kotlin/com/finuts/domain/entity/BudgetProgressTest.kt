package com.finuts.domain.entity

import com.finuts.test.TestData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for BudgetProgress calculation logic.
 * Covers progress percentage, status determination, and remaining amount.
 */
class BudgetProgressTest {

    // === BudgetProgress Creation Tests ===

    @Test
    fun `BudgetProgress can be created with budget and spent amount`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 50_000_00L)

        assertEquals(budget, progress.budget)
        assertEquals(50_000_00L, progress.spent)
    }

    @Test
    fun `BudgetProgress calculates remaining correctly`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 30_000_00L)

        assertEquals(70_000_00L, progress.remaining)
    }

    @Test
    fun `remaining is zero when spent equals budget`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 100_000_00L)

        assertEquals(0L, progress.remaining)
    }

    @Test
    fun `remaining is negative when overspent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 120_000_00L)

        assertEquals(-20_000_00L, progress.remaining)
    }

    @Test
    fun `remaining equals budget when nothing spent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 0L)

        assertEquals(100_000_00L, progress.remaining)
    }

    // === Percentage Calculation Tests ===

    @Test
    fun `percentUsed is 0 when nothing spent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 0L)

        assertEquals(0f, progress.percentUsed)
    }

    @Test
    fun `percentUsed is 50 when half spent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 50_000_00L)

        assertEquals(50f, progress.percentUsed)
    }

    @Test
    fun `percentUsed is 100 when fully spent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 100_000_00L)

        assertEquals(100f, progress.percentUsed)
    }

    @Test
    fun `percentUsed exceeds 100 when overspent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 150_000_00L)

        assertEquals(150f, progress.percentUsed)
    }

    @Test
    fun `percentUsed handles small amounts correctly`() {
        val budget = TestData.budget(amount = 1000L)
        val progress = BudgetProgress(budget = budget, spent = 333L)

        assertEquals(33.3f, progress.percentUsed, 0.1f)
    }

    @Test
    fun `percentUsed is 0 when budget is zero`() {
        val budget = TestData.budget(amount = 0L)
        val progress = BudgetProgress(budget = budget, spent = 0L)

        assertEquals(0f, progress.percentUsed)
    }

    // === Status Determination Tests (Thresholds: 0-79%, 80-99%, 100%+) ===

    @Test
    fun `status is ON_TRACK at 0 percent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 0L)

        assertEquals(BudgetStatus.ON_TRACK, progress.status)
    }

    @Test
    fun `status is ON_TRACK at 50 percent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 50_000_00L)

        assertEquals(BudgetStatus.ON_TRACK, progress.status)
    }

    @Test
    fun `status is ON_TRACK at 79 percent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 79_000_00L)

        assertEquals(BudgetStatus.ON_TRACK, progress.status)
    }

    @Test
    fun `status is WARNING at 80 percent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 80_000_00L)

        assertEquals(BudgetStatus.WARNING, progress.status)
    }

    @Test
    fun `status is WARNING at 90 percent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 90_000_00L)

        assertEquals(BudgetStatus.WARNING, progress.status)
    }

    @Test
    fun `status is WARNING at 99 percent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 99_000_00L)

        assertEquals(BudgetStatus.WARNING, progress.status)
    }

    @Test
    fun `status is OVER_BUDGET at 100 percent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 100_000_00L)

        assertEquals(BudgetStatus.OVER_BUDGET, progress.status)
    }

    @Test
    fun `status is OVER_BUDGET at 150 percent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 150_000_00L)

        assertEquals(BudgetStatus.OVER_BUDGET, progress.status)
    }

    // === Edge Cases ===

    @Test
    fun `handles very large budget amounts`() {
        val largeAmount = 999_999_999_99L
        val budget = TestData.budget(amount = largeAmount)
        val progress = BudgetProgress(budget = budget, spent = largeAmount / 2)

        assertEquals(50f, progress.percentUsed, 0.1f)
        assertEquals(BudgetStatus.ON_TRACK, progress.status)
    }

    @Test
    fun `handles minimum spent amount`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 1L)

        assertTrue(progress.percentUsed < 1f)
        assertEquals(BudgetStatus.ON_TRACK, progress.status)
    }

    // === Convenience Properties ===

    @Test
    fun `isOnTrack returns true for ON_TRACK status`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 50_000_00L)

        assertTrue(progress.isOnTrack)
        assertFalse(progress.isWarning)
        assertFalse(progress.isOverBudget)
    }

    @Test
    fun `isWarning returns true for WARNING status`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 85_000_00L)

        assertFalse(progress.isOnTrack)
        assertTrue(progress.isWarning)
        assertFalse(progress.isOverBudget)
    }

    @Test
    fun `isOverBudget returns true for OVER_BUDGET status`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 110_000_00L)

        assertFalse(progress.isOnTrack)
        assertFalse(progress.isWarning)
        assertTrue(progress.isOverBudget)
    }

    // === Progress Bar Value Tests (clamped 0-1) ===

    @Test
    fun `progressFraction is 0 when nothing spent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 0L)

        assertEquals(0f, progress.progressFraction)
    }

    @Test
    fun `progressFraction is 0_5 when half spent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 50_000_00L)

        assertEquals(0.5f, progress.progressFraction)
    }

    @Test
    fun `progressFraction is clamped at 1 when overspent`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 150_000_00L)

        assertEquals(1f, progress.progressFraction)
    }

    // === Formatted Display Values ===

    @Test
    fun `formattedRemaining shows positive remaining`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 30_000_00L)

        assertEquals("70000.00", progress.formattedRemaining)
    }

    @Test
    fun `formattedOverspent shows amount over budget`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 120_000_00L)

        assertEquals("20000.00", progress.formattedOverspent)
    }

    @Test
    fun `formattedOverspent is zero when under budget`() {
        val budget = TestData.budget(amount = 100_000_00L)
        val progress = BudgetProgress(budget = budget, spent = 80_000_00L)

        assertEquals("0.00", progress.formattedOverspent)
    }
}

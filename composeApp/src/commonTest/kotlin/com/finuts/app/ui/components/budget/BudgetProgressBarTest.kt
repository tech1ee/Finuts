package com.finuts.app.ui.components.budget

import com.finuts.app.theme.FinutsColors
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for BudgetProgressBar color and progress logic.
 *
 * Since Compose Multiplatform doesn't support UI testing (composeTestRule),
 * we test the pure logic functions that the component uses.
 *
 * TDD: These tests are written BEFORE implementation.
 */
class BudgetProgressBarTest {

    // === Progress Clamping Tests ===

    @Test
    fun `progress fraction clamps negative to zero`() {
        val result = clampProgress(-0.5f)
        assertEquals(0f, result)
    }

    @Test
    fun `progress fraction clamps over 1 to 1`() {
        val result = clampProgress(1.5f)
        assertEquals(1f, result)
    }

    @Test
    fun `progress fraction preserves valid values`() {
        val result = clampProgress(0.75f)
        assertEquals(0.75f, result)
    }

    // === Color Selection Tests ===

    @Test
    fun `on track color when percentage under 80`() {
        val color = getBudgetProgressColor(percentUsed = 50f)
        assertEquals(FinutsColors.ProgressOnTrack, color)
    }

    @Test
    fun `on track color when percentage exactly 79`() {
        val color = getBudgetProgressColor(percentUsed = 79f)
        assertEquals(FinutsColors.ProgressOnTrack, color)
    }

    @Test
    fun `warning color when percentage is 80`() {
        val color = getBudgetProgressColor(percentUsed = 80f)
        assertEquals(FinutsColors.ProgressBehind, color)
    }

    @Test
    fun `warning color when percentage is 99`() {
        val color = getBudgetProgressColor(percentUsed = 99f)
        assertEquals(FinutsColors.ProgressBehind, color)
    }

    @Test
    fun `over budget color when percentage is 100`() {
        val color = getBudgetProgressColor(percentUsed = 100f)
        assertEquals(FinutsColors.ProgressOverdue, color)
    }

    @Test
    fun `over budget color when percentage exceeds 100`() {
        val color = getBudgetProgressColor(percentUsed = 150f)
        assertEquals(FinutsColors.ProgressOverdue, color)
    }

    @Test
    fun `on track color when percentage is zero`() {
        val color = getBudgetProgressColor(percentUsed = 0f)
        assertEquals(FinutsColors.ProgressOnTrack, color)
    }
}

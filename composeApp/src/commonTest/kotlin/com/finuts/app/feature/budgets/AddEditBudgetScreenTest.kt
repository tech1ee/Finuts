package com.finuts.app.feature.budgets

import com.finuts.app.feature.budgets.components.formatPeriodDisplay
import com.finuts.domain.entity.BudgetPeriod
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for AddEditBudgetScreen helper functions.
 *
 * TDD: These tests are written BEFORE implementation.
 */
class AddEditBudgetScreenTest {

    // === Period Display Tests ===

    @Test
    fun `formatPeriodDisplay formats DAILY`() {
        val display = formatPeriodDisplay(BudgetPeriod.DAILY)
        assertEquals("Daily", display)
    }

    @Test
    fun `formatPeriodDisplay formats WEEKLY`() {
        val display = formatPeriodDisplay(BudgetPeriod.WEEKLY)
        assertEquals("Weekly", display)
    }

    @Test
    fun `formatPeriodDisplay formats MONTHLY`() {
        val display = formatPeriodDisplay(BudgetPeriod.MONTHLY)
        assertEquals("Monthly", display)
    }

    @Test
    fun `formatPeriodDisplay formats QUARTERLY`() {
        val display = formatPeriodDisplay(BudgetPeriod.QUARTERLY)
        assertEquals("Quarterly", display)
    }

    @Test
    fun `formatPeriodDisplay formats YEARLY`() {
        val display = formatPeriodDisplay(BudgetPeriod.YEARLY)
        assertEquals("Yearly", display)
    }

    // === Title Tests ===

    @Test
    fun `getScreenTitle returns Add Budget when not edit mode`() {
        val title = getScreenTitle(isEditMode = false)
        assertEquals("Add Budget", title)
    }

    @Test
    fun `getScreenTitle returns Edit Budget when edit mode`() {
        val title = getScreenTitle(isEditMode = true)
        assertEquals("Edit Budget", title)
    }

    // === Button Label Tests ===

    @Test
    fun `getSaveButtonLabel returns Saving when saving`() {
        val label = getSaveButtonLabel(isSaving = true)
        assertEquals("Saving...", label)
    }

    @Test
    fun `getSaveButtonLabel returns Save when not saving`() {
        val label = getSaveButtonLabel(isSaving = false)
        assertEquals("Save", label)
    }
}

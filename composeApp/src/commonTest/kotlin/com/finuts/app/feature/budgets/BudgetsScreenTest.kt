package com.finuts.app.feature.budgets

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for BudgetsScreen helper functions.
 *
 * TDD: These tests are written BEFORE implementation.
 * Tests cover format functions and state calculations.
 */
class BudgetsScreenTest {

    // === Default Currency Tests ===

    @Test
    fun `getDefaultCurrencySymbol returns KZT symbol for empty list`() {
        val symbol = getDefaultCurrencySymbol(emptyList())
        assertEquals("₸", symbol)
    }

    @Test
    fun `getDefaultCurrencySymbol returns first budget currency`() {
        // Uses USD budget
        val symbol = getDefaultCurrencySymbol(
            listOf(createTestBudgetProgress(currencySymbol = "$"))
        )
        assertEquals("$", symbol)
    }
}

// === Test Helpers ===

/**
 * Creates test BudgetProgress for testing.
 * Uses minimal data needed for tests.
 */
private fun createTestBudgetProgress(
    currencySymbol: String = "₸"
): com.finuts.domain.entity.BudgetProgress {
    val currency = com.finuts.domain.entity.Currency(
        code = if (currencySymbol == "$") "USD" else "KZT",
        symbol = currencySymbol,
        name = "Test Currency"
    )
    val budget = com.finuts.domain.entity.Budget(
        id = "test_budget",
        categoryId = null,
        name = "Test Budget",
        amount = 100_000_00L,
        currency = currency,
        period = com.finuts.domain.entity.BudgetPeriod.MONTHLY,
        startDate = kotlinx.datetime.Instant.DISTANT_PAST,
        endDate = null,
        isActive = true,
        createdAt = kotlinx.datetime.Instant.DISTANT_PAST,
        updatedAt = kotlinx.datetime.Instant.DISTANT_PAST
    )
    return com.finuts.domain.entity.BudgetProgress(
        budget = budget,
        spent = 50_000_00L
    )
}

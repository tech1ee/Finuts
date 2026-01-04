package com.finuts.domain.entity

import com.finuts.test.TestData
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Budget entity and related types.
 */
class BudgetTest {

    @Test
    fun `Budget can be created with all parameters`() {
        val startDate = Instant.parse("2024-01-01T00:00:00Z")
        val endDate = Instant.parse("2024-12-31T23:59:59Z")
        val currency = Currency("KZT", "₸", "Kazakhstani Tenge")

        val budget = Budget(
            id = "budget-1",
            categoryId = "cat-1",
            name = "Monthly Food Budget",
            amount = 100000_00L,
            currency = currency,
            period = BudgetPeriod.MONTHLY,
            startDate = startDate,
            endDate = endDate,
            isActive = true,
            createdAt = startDate,
            updatedAt = startDate
        )

        assertEquals("budget-1", budget.id)
        assertEquals("cat-1", budget.categoryId)
        assertEquals("Monthly Food Budget", budget.name)
        assertEquals(100000_00L, budget.amount)
        assertEquals(currency, budget.currency)
        assertEquals(BudgetPeriod.MONTHLY, budget.period)
        assertEquals(startDate, budget.startDate)
        assertEquals(endDate, budget.endDate)
        assertTrue(budget.isActive)
    }

    @Test
    fun `Budget defaults isActive to true`() {
        val budget = TestData.budget()
        assertTrue(budget.isActive)
    }

    @Test
    fun `Budget allows null categoryId for overall budget`() {
        val budget = TestData.budget(categoryId = null)
        assertNull(budget.categoryId)
    }

    @Test
    fun `Budget allows null endDate for ongoing budget`() {
        val budget = TestData.budget(endDate = null)
        assertNull(budget.endDate)
    }

    @Test
    fun `Budget can be inactive`() {
        val budget = TestData.budget(isActive = false)
        assertFalse(budget.isActive)
    }

    @Test
    fun `Budget copy works correctly`() {
        val original = TestData.budget(name = "Original", amount = 10000L)
        val modified = original.copy(name = "Modified", amount = 20000L)

        assertEquals("Original", original.name)
        assertEquals(10000L, original.amount)
        assertEquals("Modified", modified.name)
        assertEquals(20000L, modified.amount)
        assertEquals(original.id, modified.id)
    }

    @Test
    fun `BudgetPeriod has all expected values`() {
        val periods = BudgetPeriod.entries.map { it.name }
        assertTrue("DAILY" in periods)
        assertTrue("WEEKLY" in periods)
        assertTrue("MONTHLY" in periods)
        assertTrue("QUARTERLY" in periods)
        assertTrue("YEARLY" in periods)
        assertEquals(5, BudgetPeriod.entries.size)
    }

    @Test
    fun `all BudgetPeriods can be used in Budget`() {
        BudgetPeriod.entries.forEach { period ->
            val budget = TestData.budget(period = period)
            assertEquals(period, budget.period)
        }
    }

    @Test
    fun `Budget with daily period`() {
        val budget = TestData.budget(period = BudgetPeriod.DAILY)
        assertEquals(BudgetPeriod.DAILY, budget.period)
    }

    @Test
    fun `Budget with weekly period`() {
        val budget = TestData.budget(period = BudgetPeriod.WEEKLY)
        assertEquals(BudgetPeriod.WEEKLY, budget.period)
    }

    @Test
    fun `Budget with monthly period`() {
        val budget = TestData.budget(period = BudgetPeriod.MONTHLY)
        assertEquals(BudgetPeriod.MONTHLY, budget.period)
    }

    @Test
    fun `Budget with quarterly period`() {
        val budget = TestData.budget(period = BudgetPeriod.QUARTERLY)
        assertEquals(BudgetPeriod.QUARTERLY, budget.period)
    }

    @Test
    fun `Budget with yearly period`() {
        val budget = TestData.budget(period = BudgetPeriod.YEARLY)
        assertEquals(BudgetPeriod.YEARLY, budget.period)
    }

    @Test
    fun `Budget allows zero amount`() {
        val budget = TestData.budget(amount = 0L)
        assertEquals(0L, budget.amount)
    }

    @Test
    fun `Budget allows large amount`() {
        val largeAmount = 999_999_999_99L // Almost 10 billion in smallest units
        val budget = TestData.budget(amount = largeAmount)
        assertEquals(largeAmount, budget.amount)
    }

    @Test
    fun `Budget with different currencies`() {
        val currencies = listOf(
            Currency("KZT", "₸", "Kazakhstani Tenge"),
            Currency("USD", "$", "US Dollar"),
            Currency("EUR", "€", "Euro"),
            Currency("RUB", "₽", "Russian Ruble")
        )

        currencies.forEach { currency ->
            val budget = TestData.budget(currency = currency)
            assertEquals(currency, budget.currency)
        }
    }

    @Test
    fun `Budget with date range`() {
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val end = Instant.parse("2024-03-31T23:59:59Z")

        val budget = TestData.budget(startDate = start, endDate = end)

        assertEquals(start, budget.startDate)
        assertEquals(end, budget.endDate)
    }

    // BudgetStatus Tests

    @Test
    fun `BudgetStatus has all expected values`() {
        val statuses = BudgetStatus.entries.map { it.name }
        assertTrue("ON_TRACK" in statuses)
        assertTrue("WARNING" in statuses)
        assertTrue("OVER_BUDGET" in statuses)
        assertEquals(3, BudgetStatus.entries.size)
    }

    @Test
    fun `BudgetStatus ON_TRACK is default for low usage`() {
        assertEquals(BudgetStatus.ON_TRACK, BudgetStatus.entries.first())
    }

    @Test
    fun `BudgetStatus values are distinct`() {
        val statuses = BudgetStatus.entries
        assertNotEquals(statuses[0], statuses[1])
        assertNotEquals(statuses[1], statuses[2])
        assertNotEquals(statuses[0], statuses[2])
    }
}

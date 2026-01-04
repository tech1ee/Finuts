package com.finuts.domain.entity

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for BudgetPeriod calculations.
 * Covers period duration, date range calculations, and period boundaries.
 */
class BudgetPeriodTest {

    private val timeZone = TimeZone.UTC

    // === Period Duration Tests ===

    @Test
    fun `DAILY period has 1 day duration`() {
        assertEquals(1, BudgetPeriod.DAILY.durationDays)
    }

    @Test
    fun `WEEKLY period has 7 days duration`() {
        assertEquals(7, BudgetPeriod.WEEKLY.durationDays)
    }

    @Test
    fun `MONTHLY period has 30 days approximate duration`() {
        assertEquals(30, BudgetPeriod.MONTHLY.durationDays)
    }

    @Test
    fun `QUARTERLY period has 90 days approximate duration`() {
        assertEquals(90, BudgetPeriod.QUARTERLY.durationDays)
    }

    @Test
    fun `YEARLY period has 365 days approximate duration`() {
        assertEquals(365, BudgetPeriod.YEARLY.durationDays)
    }

    // === Period Label Tests ===

    @Test
    fun `DAILY period has correct display label`() {
        assertEquals("Daily", BudgetPeriod.DAILY.displayLabel)
    }

    @Test
    fun `WEEKLY period has correct display label`() {
        assertEquals("Weekly", BudgetPeriod.WEEKLY.displayLabel)
    }

    @Test
    fun `MONTHLY period has correct display label`() {
        assertEquals("Monthly", BudgetPeriod.MONTHLY.displayLabel)
    }

    @Test
    fun `QUARTERLY period has correct display label`() {
        assertEquals("Quarterly", BudgetPeriod.QUARTERLY.displayLabel)
    }

    @Test
    fun `YEARLY period has correct display label`() {
        assertEquals("Yearly", BudgetPeriod.YEARLY.displayLabel)
    }

    // === Period End Date Calculation Tests ===

    @Test
    fun `calculateEndDate for DAILY adds 1 day`() {
        val startDate = LocalDate(2024, 1, 15)
        val endDate = BudgetPeriod.DAILY.calculateEndDate(startDate)

        assertEquals(LocalDate(2024, 1, 15), endDate)
    }

    @Test
    fun `calculateEndDate for WEEKLY adds 7 days`() {
        val startDate = LocalDate(2024, 1, 1)
        val endDate = BudgetPeriod.WEEKLY.calculateEndDate(startDate)

        assertEquals(LocalDate(2024, 1, 7), endDate)
    }

    @Test
    fun `calculateEndDate for MONTHLY goes to end of month`() {
        val startDate = LocalDate(2024, 1, 1)
        val endDate = BudgetPeriod.MONTHLY.calculateEndDate(startDate)

        assertEquals(LocalDate(2024, 1, 31), endDate)
    }

    @Test
    fun `calculateEndDate for MONTHLY handles February`() {
        val startDate = LocalDate(2024, 2, 1)
        val endDate = BudgetPeriod.MONTHLY.calculateEndDate(startDate)

        assertEquals(LocalDate(2024, 2, 29), endDate) // 2024 is leap year
    }

    @Test
    fun `calculateEndDate for QUARTERLY adds 3 months`() {
        val startDate = LocalDate(2024, 1, 1)
        val endDate = BudgetPeriod.QUARTERLY.calculateEndDate(startDate)

        assertEquals(LocalDate(2024, 3, 31), endDate)
    }

    @Test
    fun `calculateEndDate for YEARLY adds 1 year`() {
        val startDate = LocalDate(2024, 1, 1)
        val endDate = BudgetPeriod.YEARLY.calculateEndDate(startDate)

        assertEquals(LocalDate(2024, 12, 31), endDate)
    }

    // === Current Period Detection Tests ===

    @Test
    fun `isDateInPeriod returns true for date within period`() {
        val startDate = LocalDate(2024, 1, 1)
        val checkDate = LocalDate(2024, 1, 15)

        assertTrue(BudgetPeriod.MONTHLY.isDateInPeriod(startDate, checkDate))
    }

    @Test
    fun `isDateInPeriod returns true for start date`() {
        val startDate = LocalDate(2024, 1, 1)

        assertTrue(BudgetPeriod.MONTHLY.isDateInPeriod(startDate, startDate))
    }

    @Test
    fun `isDateInPeriod returns true for end date`() {
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)

        assertTrue(BudgetPeriod.MONTHLY.isDateInPeriod(startDate, endDate))
    }

    // === Period Iteration Tests ===

    @Test
    fun `all periods are iterable`() {
        val periods = BudgetPeriod.entries
        assertEquals(5, periods.size)
    }

    @Test
    fun `periods are ordered by duration`() {
        val periods = BudgetPeriod.entries.sortedBy { it.durationDays }

        assertEquals(BudgetPeriod.DAILY, periods[0])
        assertEquals(BudgetPeriod.WEEKLY, periods[1])
        assertEquals(BudgetPeriod.MONTHLY, periods[2])
        assertEquals(BudgetPeriod.QUARTERLY, periods[3])
        assertEquals(BudgetPeriod.YEARLY, periods[4])
    }

    // === Days Remaining Calculation ===

    @Test
    fun `daysRemaining calculates correctly for MONTHLY`() {
        val startDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 1, 15)

        val remaining = BudgetPeriod.MONTHLY.daysRemaining(startDate, currentDate)

        assertEquals(16, remaining) // Jan 16-31 = 16 days
    }

    @Test
    fun `daysRemaining is 0 on last day of period`() {
        val startDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 1, 31)

        val remaining = BudgetPeriod.MONTHLY.daysRemaining(startDate, currentDate)

        assertEquals(0, remaining)
    }

    @Test
    fun `daysRemaining is full duration on first day`() {
        val startDate = LocalDate(2024, 1, 1)

        val remaining = BudgetPeriod.MONTHLY.daysRemaining(startDate, startDate)

        assertEquals(30, remaining)
    }
}

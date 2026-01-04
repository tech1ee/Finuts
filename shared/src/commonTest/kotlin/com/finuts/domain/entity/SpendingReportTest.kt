package com.finuts.domain.entity

import com.finuts.test.TestData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for SpendingReport and related entities.
 * Covers report period calculations, category spending, and aggregations.
 */
class SpendingReportTest {

    // === ReportPeriod Tests ===

    @Test
    fun `ReportPeriod has all expected values`() {
        val periods = ReportPeriod.entries
        assertEquals(5, periods.size)
        assertTrue(periods.contains(ReportPeriod.THIS_WEEK))
        assertTrue(periods.contains(ReportPeriod.THIS_MONTH))
        assertTrue(periods.contains(ReportPeriod.LAST_MONTH))
        assertTrue(periods.contains(ReportPeriod.THIS_YEAR))
        assertTrue(periods.contains(ReportPeriod.CUSTOM))
    }

    @Test
    fun `ReportPeriod displayLabel returns user-friendly text`() {
        assertEquals("This Week", ReportPeriod.THIS_WEEK.displayLabel)
        assertEquals("This Month", ReportPeriod.THIS_MONTH.displayLabel)
        assertEquals("Last Month", ReportPeriod.LAST_MONTH.displayLabel)
        assertEquals("This Year", ReportPeriod.THIS_YEAR.displayLabel)
        assertEquals("Custom", ReportPeriod.CUSTOM.displayLabel)
    }

    // === CategorySpending Tests ===

    @Test
    fun `CategorySpending can be created with category and amount`() {
        val category = TestData.category(id = "food", name = "Food")
        val spending = CategorySpending(
            category = category,
            amount = 50_000_00L,
            percentage = 25.5f,
            transactionCount = 10
        )

        assertEquals(category, spending.category)
        assertEquals(50_000_00L, spending.amount)
        assertEquals(25.5f, spending.percentage)
        assertEquals(10, spending.transactionCount)
    }

    @Test
    fun `CategorySpending with zero amount has zero percentage`() {
        val category = TestData.category()
        val spending = CategorySpending(
            category = category,
            amount = 0L,
            percentage = 0f,
            transactionCount = 0
        )

        assertEquals(0L, spending.amount)
        assertEquals(0f, spending.percentage)
    }

    @Test
    fun `CategorySpending percentage can exceed 50 for dominant category`() {
        val category = TestData.category()
        val spending = CategorySpending(
            category = category,
            amount = 150_000_00L,
            percentage = 75f,
            transactionCount = 5
        )

        assertEquals(75f, spending.percentage)
    }

    // === DailyAmount Tests ===

    @Test
    fun `DailyAmount can be created with date and amounts`() {
        val daily = DailyAmount(
            date = TestData.DEFAULT_INSTANT,
            income = 100_000_00L,
            expense = 50_000_00L
        )

        assertEquals(TestData.DEFAULT_INSTANT, daily.date)
        assertEquals(100_000_00L, daily.income)
        assertEquals(50_000_00L, daily.expense)
    }

    @Test
    fun `DailyAmount netChange is income minus expense`() {
        val daily = DailyAmount(
            date = TestData.DEFAULT_INSTANT,
            income = 100_000_00L,
            expense = 30_000_00L
        )

        assertEquals(70_000_00L, daily.netChange)
    }

    @Test
    fun `DailyAmount netChange is negative when expense exceeds income`() {
        val daily = DailyAmount(
            date = TestData.DEFAULT_INSTANT,
            income = 20_000_00L,
            expense = 50_000_00L
        )

        assertEquals(-30_000_00L, daily.netChange)
    }

    @Test
    fun `DailyAmount with zero income and expense has zero netChange`() {
        val daily = DailyAmount(
            date = TestData.DEFAULT_INSTANT,
            income = 0L,
            expense = 0L
        )

        assertEquals(0L, daily.netChange)
    }

    // === SpendingReport Tests ===

    @Test
    fun `SpendingReport can be created with all fields`() {
        val category = TestData.category()
        val report = SpendingReport(
            period = ReportPeriod.THIS_MONTH,
            totalIncome = 500_000_00L,
            totalExpense = 200_000_00L,
            categoryBreakdown = listOf(
                CategorySpending(category, 200_000_00L, 100f, 15)
            ),
            dailyTrend = emptyList()
        )

        assertEquals(ReportPeriod.THIS_MONTH, report.period)
        assertEquals(500_000_00L, report.totalIncome)
        assertEquals(200_000_00L, report.totalExpense)
        assertEquals(1, report.categoryBreakdown.size)
    }

    @Test
    fun `SpendingReport netChange is income minus expense`() {
        val report = SpendingReport(
            period = ReportPeriod.THIS_MONTH,
            totalIncome = 500_000_00L,
            totalExpense = 200_000_00L,
            categoryBreakdown = emptyList(),
            dailyTrend = emptyList()
        )

        assertEquals(300_000_00L, report.netChange)
    }

    @Test
    fun `SpendingReport netChange is negative when expense exceeds income`() {
        val report = SpendingReport(
            period = ReportPeriod.THIS_MONTH,
            totalIncome = 100_000_00L,
            totalExpense = 250_000_00L,
            categoryBreakdown = emptyList(),
            dailyTrend = emptyList()
        )

        assertEquals(-150_000_00L, report.netChange)
    }

    @Test
    fun `SpendingReport with zero totals has zero netChange`() {
        val report = SpendingReport(
            period = ReportPeriod.THIS_MONTH,
            totalIncome = 0L,
            totalExpense = 0L,
            categoryBreakdown = emptyList(),
            dailyTrend = emptyList()
        )

        assertEquals(0L, report.netChange)
    }

    @Test
    fun `SpendingReport savingsRate calculates correctly`() {
        val report = SpendingReport(
            period = ReportPeriod.THIS_MONTH,
            totalIncome = 500_000_00L,
            totalExpense = 200_000_00L,
            categoryBreakdown = emptyList(),
            dailyTrend = emptyList()
        )

        // Savings = 300,000 / 500,000 = 60%
        assertTrue(kotlin.math.abs(report.savingsRate - 60f) < 0.01f)
    }

    @Test
    fun `SpendingReport savingsRate is zero when income is zero`() {
        val report = SpendingReport(
            period = ReportPeriod.THIS_MONTH,
            totalIncome = 0L,
            totalExpense = 200_000_00L,
            categoryBreakdown = emptyList(),
            dailyTrend = emptyList()
        )

        assertEquals(0f, report.savingsRate)
    }

    @Test
    fun `SpendingReport savingsRate is negative when overspending`() {
        val report = SpendingReport(
            period = ReportPeriod.THIS_MONTH,
            totalIncome = 100_000_00L,
            totalExpense = 150_000_00L,
            categoryBreakdown = emptyList(),
            dailyTrend = emptyList()
        )

        // Savings = -50,000 / 100,000 = -50%
        assertTrue(kotlin.math.abs(report.savingsRate - (-50f)) < 0.01f)
    }

    @Test
    fun `SpendingReport topCategories returns top 5 by amount`() {
        val categories = (1..8).map { i ->
            CategorySpending(
                category = TestData.category(id = "cat-$i", name = "Category $i"),
                amount = (i * 10_000_00L),
                percentage = (i * 10f),
                transactionCount = i
            )
        }

        val report = SpendingReport(
            period = ReportPeriod.THIS_MONTH,
            totalIncome = 500_000_00L,
            totalExpense = 360_000_00L,
            categoryBreakdown = categories,
            dailyTrend = emptyList()
        )

        val topCategories = report.topCategories
        assertEquals(5, topCategories.size)
        // Should be sorted by amount descending
        assertEquals("cat-8", topCategories[0].category.id)
        assertEquals("cat-7", topCategories[1].category.id)
        assertEquals("cat-4", topCategories[4].category.id)
    }

    @Test
    fun `SpendingReport topCategories returns all if less than 5`() {
        val categories = listOf(
            CategorySpending(
                category = TestData.category(id = "food", name = "Food"),
                amount = 50_000_00L,
                percentage = 50f,
                transactionCount = 10
            ),
            CategorySpending(
                category = TestData.category(id = "transport", name = "Transport"),
                amount = 30_000_00L,
                percentage = 30f,
                transactionCount = 5
            )
        )

        val report = SpendingReport(
            period = ReportPeriod.THIS_MONTH,
            totalIncome = 200_000_00L,
            totalExpense = 80_000_00L,
            categoryBreakdown = categories,
            dailyTrend = emptyList()
        )

        assertEquals(2, report.topCategories.size)
    }

    @Test
    fun `SpendingReport hasData is true when there are transactions`() {
        val category = TestData.category()
        val report = SpendingReport(
            period = ReportPeriod.THIS_MONTH,
            totalIncome = 100_000_00L,
            totalExpense = 50_000_00L,
            categoryBreakdown = listOf(
                CategorySpending(category, 50_000_00L, 100f, 5)
            ),
            dailyTrend = emptyList()
        )

        assertTrue(report.hasData)
    }

    @Test
    fun `SpendingReport hasData is false when no income and no expense`() {
        val report = SpendingReport(
            period = ReportPeriod.THIS_MONTH,
            totalIncome = 0L,
            totalExpense = 0L,
            categoryBreakdown = emptyList(),
            dailyTrend = emptyList()
        )

        assertTrue(!report.hasData)
    }
}

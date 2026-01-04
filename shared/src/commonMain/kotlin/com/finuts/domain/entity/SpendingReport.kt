package com.finuts.domain.entity

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Report period for spending analysis.
 * Used to filter and aggregate transactions for reports.
 */
@Serializable
enum class ReportPeriod {
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    CUSTOM;

    /** User-friendly display label */
    val displayLabel: String get() = when (this) {
        THIS_WEEK -> "This Week"
        THIS_MONTH -> "This Month"
        LAST_MONTH -> "Last Month"
        THIS_YEAR -> "This Year"
        CUSTOM -> "Custom"
    }
}

/**
 * Spending breakdown by category.
 * Used for pie/donut chart visualization.
 */
data class CategorySpending(
    val category: Category,
    val amount: Long,
    val percentage: Float,
    val transactionCount: Int
)

/**
 * Daily income and expense amounts.
 * Used for trend line visualization.
 */
data class DailyAmount(
    val date: Instant,
    val income: Long,
    val expense: Long
) {
    /** Net change for the day (income - expense) */
    val netChange: Long get() = income - expense
}

/**
 * Complete spending report for a period.
 * Contains aggregated financial data for reports screen.
 */
data class SpendingReport(
    val period: ReportPeriod,
    val totalIncome: Long,
    val totalExpense: Long,
    val categoryBreakdown: List<CategorySpending>,
    val dailyTrend: List<DailyAmount>
) {
    /** Net change for the period (income - expense) */
    val netChange: Long get() = totalIncome - totalExpense

    /** Savings rate as percentage (can be negative if overspending) */
    val savingsRate: Float get() = if (totalIncome == 0L) 0f
        else (netChange.toFloat() / totalIncome.toFloat()) * 100f

    /** Top 5 categories by spending amount (sorted descending) */
    val topCategories: List<CategorySpending> get() =
        categoryBreakdown.sortedByDescending { it.amount }.take(5)

    /** Whether there is any financial data in this report */
    val hasData: Boolean get() = totalIncome > 0 || totalExpense > 0
}

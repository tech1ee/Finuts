package com.finuts.domain.usecase

import com.finuts.domain.entity.CategorySpending
import com.finuts.domain.entity.DailyAmount
import com.finuts.domain.entity.ReportPeriod
import com.finuts.domain.entity.SpendingReport
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.CategoryRepository
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Use case for generating spending reports.
 * Aggregates transaction data by period and category.
 *
 * @param transactionRepository Source of transaction data
 * @param categoryRepository Source of category data
 * @param clock Provider for current time (allows testing with fixed time)
 */
class GetSpendingReportUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val clock: () -> Instant = {
        Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
    }
) {

    /**
     * Execute the use case to generate a spending report.
     *
     * @param period The report period to generate
     * @return Flow of SpendingReport for the given period
     */
    fun execute(period: ReportPeriod): Flow<SpendingReport> {
        val (startDate, endDate) = calculateDateRange(period)

        return combine(
            transactionRepository.getTransactionsByDateRange(startDate, endDate),
            categoryRepository.getAllCategories()
        ) { transactions, categories ->
            val categoryMap = categories.associateBy { it.id }

            // Filter out transfers
            val relevantTransactions = transactions.filter {
                it.type != TransactionType.TRANSFER
            }

            // Calculate totals
            val totalIncome = relevantTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }

            val totalExpense = relevantTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            // Group expenses by category
            val expensesByCategory = relevantTransactions
                .filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
                .groupBy { it.categoryId!! }

            // Calculate category breakdown
            val categoryBreakdown = expensesByCategory.mapNotNull { (categoryId, txns) ->
                val category = categoryMap[categoryId] ?: return@mapNotNull null
                val amount = txns.sumOf { it.amount }
                val percentage = if (totalExpense > 0) {
                    (amount.toFloat() / totalExpense.toFloat()) * 100f
                } else {
                    0f
                }

                CategorySpending(
                    category = category,
                    amount = amount,
                    percentage = percentage,
                    transactionCount = txns.size
                )
            }.sortedByDescending { it.amount }

            // Calculate daily trend
            val dailyTrend = calculateDailyTrend(relevantTransactions, startDate, endDate)

            SpendingReport(
                period = period,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                categoryBreakdown = categoryBreakdown,
                dailyTrend = dailyTrend
            )
        }
    }

    /**
     * Calculate the date range for a given report period.
     */
    private fun calculateDateRange(period: ReportPeriod): Pair<Instant, Instant> {
        val now = clock()
        val timeZone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(timeZone).date

        return when (period) {
            ReportPeriod.THIS_WEEK -> {
                val startOfWeek = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
                val endOfWeek = startOfWeek.plus(6, DateTimeUnit.DAY)
                startOfWeek.atStartOfDayIn(timeZone) to endOfWeek.plusDays(1).atStartOfDayIn(timeZone)
            }
            ReportPeriod.THIS_MONTH -> {
                val startOfMonth = LocalDate(today.year, today.month, 1)
                val endOfMonth = startOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
                startOfMonth.atStartOfDayIn(timeZone) to endOfMonth.plusDays(1).atStartOfDayIn(timeZone)
            }
            ReportPeriod.LAST_MONTH -> {
                val startOfLastMonth = LocalDate(today.year, today.month, 1)
                    .minus(1, DateTimeUnit.MONTH)
                val endOfLastMonth = LocalDate(today.year, today.month, 1)
                    .minus(1, DateTimeUnit.DAY)
                startOfLastMonth.atStartOfDayIn(timeZone) to endOfLastMonth.plusDays(1).atStartOfDayIn(timeZone)
            }
            ReportPeriod.THIS_YEAR -> {
                val startOfYear = LocalDate(today.year, 1, 1)
                val endOfYear = LocalDate(today.year, 12, 31)
                startOfYear.atStartOfDayIn(timeZone) to endOfYear.plusDays(1).atStartOfDayIn(timeZone)
            }
            ReportPeriod.CUSTOM -> {
                // For custom, return last 30 days as default
                val start = today.minus(30, DateTimeUnit.DAY)
                start.atStartOfDayIn(timeZone) to today.plusDays(1).atStartOfDayIn(timeZone)
            }
        }
    }

    /**
     * Calculate daily income/expense trend.
     */
    private fun calculateDailyTrend(
        transactions: List<com.finuts.domain.entity.Transaction>,
        startDate: Instant,
        endDate: Instant
    ): List<DailyAmount> {
        val timeZone = TimeZone.currentSystemDefault()

        return transactions
            .groupBy { it.date.toLocalDateTime(timeZone).date }
            .map { (_, dayTransactions) ->
                val income = dayTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }
                val expense = dayTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }

                DailyAmount(
                    date = dayTransactions.first().date,
                    income = income,
                    expense = expense
                )
            }
            .sortedBy { it.date }
    }

    /**
     * Extension to add days to LocalDate.
     */
    private fun LocalDate.plusDays(days: Int): LocalDate =
        this.plus(days, DateTimeUnit.DAY)
}

package com.finuts.domain.entity

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val id: String,
    val categoryId: String?,
    val name: String,
    val amount: Long,
    val currency: Currency,
    val period: BudgetPeriod,
    val startDate: Instant,
    val endDate: Instant?,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class BudgetPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY;

    /** Approximate duration in days */
    val durationDays: Int get() = when (this) {
        DAILY -> 1
        WEEKLY -> 7
        MONTHLY -> 30
        QUARTERLY -> 90
        YEARLY -> 365
    }

    /** User-friendly display label */
    val displayLabel: String get() = when (this) {
        DAILY -> "Daily"
        WEEKLY -> "Weekly"
        MONTHLY -> "Monthly"
        QUARTERLY -> "Quarterly"
        YEARLY -> "Yearly"
    }

    /** Calculate end date for this period starting from given date */
    fun calculateEndDate(startDate: LocalDate): LocalDate = when (this) {
        DAILY -> startDate
        WEEKLY -> startDate.plus(6, DateTimeUnit.DAY)
        MONTHLY -> {
            val nextMonth = startDate.plus(1, DateTimeUnit.MONTH)
            LocalDate(nextMonth.year, nextMonth.month, 1).plus(-1, DateTimeUnit.DAY)
        }
        QUARTERLY -> {
            val threeMonthsLater = startDate.plus(3, DateTimeUnit.MONTH)
            LocalDate(threeMonthsLater.year, threeMonthsLater.month, 1).plus(-1, DateTimeUnit.DAY)
        }
        YEARLY -> LocalDate(startDate.year, 12, 31)
    }

    /** Check if a date falls within this period starting from startDate */
    fun isDateInPeriod(startDate: LocalDate, checkDate: LocalDate): Boolean {
        val endDate = calculateEndDate(startDate)
        return checkDate >= startDate && checkDate <= endDate
    }

    /** Calculate days remaining in this period from current date */
    fun daysRemaining(startDate: LocalDate, currentDate: LocalDate): Int {
        val endDate = calculateEndDate(startDate)
        return (endDate.toEpochDays() - currentDate.toEpochDays()).toInt().coerceAtLeast(0)
    }
}

/**
 * Status of a budget based on spending percentage.
 * Thresholds: ON_TRACK (0-79%), WARNING (80-99%), OVER_BUDGET (100%+)
 */
@Serializable
enum class BudgetStatus {
    ON_TRACK,   // 0-79% spent
    WARNING,    // 80-99% spent
    OVER_BUDGET // 100%+ spent
}

/**
 * Budget with calculated progress information.
 * Used for UI display of budget status and remaining amounts.
 */
data class BudgetProgress(
    val budget: Budget,
    val spent: Long
) {
    /** Amount remaining in budget (negative if overspent) */
    val remaining: Long get() = budget.amount - spent

    /** Percentage of budget used (0-100+, can exceed 100 if overspent) */
    val percentUsed: Float get() = if (budget.amount == 0L) 0f
        else (spent.toFloat() / budget.amount.toFloat()) * 100f

    /** Current status based on spending thresholds */
    val status: BudgetStatus get() = when {
        percentUsed >= 100f -> BudgetStatus.OVER_BUDGET
        percentUsed >= 80f -> BudgetStatus.WARNING
        else -> BudgetStatus.ON_TRACK
    }

    /** Progress fraction for UI (clamped 0-1) */
    val progressFraction: Float get() = (percentUsed / 100f).coerceIn(0f, 1f)

    // Convenience status checks
    val isOnTrack: Boolean get() = status == BudgetStatus.ON_TRACK
    val isWarning: Boolean get() = status == BudgetStatus.WARNING
    val isOverBudget: Boolean get() = status == BudgetStatus.OVER_BUDGET

    /** Formatted remaining amount (always positive, for display) */
    val formattedRemaining: String get() {
        val value = remaining.coerceAtLeast(0L) / 100.0
        val intPart = value.toLong()
        val decPart = ((value - intPart) * 100).toLong().toString().padStart(2, '0')
        return "$intPart.$decPart"
    }

    /** Formatted overspent amount (0 if under budget) */
    val formattedOverspent: String get() {
        val value = (-remaining).coerceAtLeast(0L) / 100.0
        val intPart = value.toLong()
        val decPart = ((value - intPart) * 100).toLong().toString().padStart(2, '0')
        return "$intPart.$decPart"
    }
}

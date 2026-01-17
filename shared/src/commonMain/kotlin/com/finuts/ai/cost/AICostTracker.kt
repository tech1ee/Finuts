package com.finuts.ai.cost

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Tracks AI API costs and enforces budget limits.
 *
 * Features:
 * - Per-model cost calculation
 * - Daily/monthly budget limits
 * - Usage history for analytics
 * - Real-time cost state
 */
class AICostTracker(
    private val dailyBudget: Float = 0.10f, // $0.10/day default
    private val monthlyBudget: Float = 2.0f // $2.00/month default
) : AICostTrackerInterface {
    private val log = Logger.withTag("AICostTracker")

    private val _todayCost = MutableStateFlow(0f)
    val todayCost: StateFlow<Float> = _todayCost.asStateFlow()

    private val _monthCost = MutableStateFlow(0f)
    val monthCost: StateFlow<Float> = _monthCost.asStateFlow()

    private val usageHistory = mutableListOf<UsageRecord>()
    private var lastResetDate: LocalDate? = null

    /**
     * Check if an operation with estimated cost can be executed.
     */
    override fun canExecute(estimatedCost: Float): Boolean {
        checkAndResetIfNewDay()
        val dailyOk = (_todayCost.value + estimatedCost) <= dailyBudget
        val monthlyOk = (_monthCost.value + estimatedCost) <= monthlyBudget
        val canExecute = dailyOk && monthlyOk

        if (!canExecute) {
            log.w {
                "canExecute: DENIED - estimated=${formatCost(estimatedCost)}, " +
                    "today=${formatCost(_todayCost.value)}/$dailyBudget, " +
                    "month=${formatCost(_monthCost.value)}/$monthlyBudget"
            }
        } else {
            log.d {
                "canExecute: OK - estimated=${formatCost(estimatedCost)}, " +
                    "today=${formatCost(_todayCost.value)}/$dailyBudget"
            }
        }
        return canExecute
    }

    /**
     * Record usage after an API call.
     */
    override fun record(inputTokens: Int, outputTokens: Int, model: String) {
        checkAndResetIfNewDay()

        val cost = calculateCost(inputTokens, outputTokens, model)
        _todayCost.value += cost
        _monthCost.value += cost

        log.i {
            "record: model=$model, tokens=${inputTokens}+${outputTokens}, " +
                "cost=${formatCost(cost)}, todayTotal=${formatCost(_todayCost.value)}"
        }

        usageHistory.add(
            UsageRecord(
                timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                model = model,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                cost = cost
            )
        )
    }

    private fun formatCost(cost: Float): String {
        // Simple formatting without String.format (not available in KMP)
        return ((cost * 100000).toInt() / 100000.0).toString()
    }

    /**
     * Get current usage statistics.
     */
    override fun getUsageStats(): UsageStats {
        checkAndResetIfNewDay()
        return UsageStats(
            todayCost = _todayCost.value,
            monthCost = _monthCost.value,
            dailyBudget = dailyBudget,
            monthlyBudget = monthlyBudget,
            todayRemaining = (dailyBudget - _todayCost.value).coerceAtLeast(0f),
            monthRemaining = (monthlyBudget - _monthCost.value).coerceAtLeast(0f),
            recentRecords = usageHistory.takeLast(10)
        )
    }

    /**
     * Get usage history for a period.
     */
    fun getHistory(days: Int = 30): List<UsageRecord> {
        val cutoff = kotlin.time.Clock.System.now().toEpochMilliseconds() - (days * 24 * 60 * 60 * 1000L)
        return usageHistory.filter { it.timestamp >= cutoff }
    }

    /**
     * Reset daily counter if new day.
     */
    private fun checkAndResetIfNewDay() {
        val today = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        ).toLocalDateTime(TimeZone.currentSystemDefault()).date

        if (lastResetDate != today) {
            val previousDayCost = _todayCost.value
            // New day - reset daily counter
            _todayCost.value = 0f
            lastResetDate = today

            log.i { "checkAndResetIfNewDay: DAILY_RESET previousDayCost=${formatCost(previousDayCost)}" }

            // Check if new month - reset monthly counter
            val yesterday = today.minus(1, DateTimeUnit.DAY)
            if (yesterday.month != today.month) {
                val previousMonthCost = _monthCost.value
                _monthCost.value = 0f
                log.i { "checkAndResetIfNewDay: MONTHLY_RESET previousMonthCost=${formatCost(previousMonthCost)}" }
            }
        }
    }

    companion object {
        // Cost per 1K tokens (as of January 2026)
        private val modelCosts = mapOf(
            // OpenAI
            "gpt-4o-mini" to TokenCost(0.00015f, 0.0006f),
            "gpt-4o" to TokenCost(0.005f, 0.015f),
            "gpt-4-turbo" to TokenCost(0.01f, 0.03f),

            // Anthropic
            "claude-3-5-haiku-latest" to TokenCost(0.0008f, 0.004f),
            "claude-sonnet-4-20250514" to TokenCost(0.003f, 0.015f),
            "claude-opus-4-20250514" to TokenCost(0.015f, 0.075f),

            // Defaults for unknown models
            "default" to TokenCost(0.001f, 0.005f)
        )

        fun calculateCost(inputTokens: Int, outputTokens: Int, model: String): Float {
            val costs = modelCosts[model] ?: modelCosts["default"]!!
            return (inputTokens / 1000f * costs.input) + (outputTokens / 1000f * costs.output)
        }
    }
}

/**
 * Cost per 1K tokens.
 */
data class TokenCost(
    val input: Float,
    val output: Float
)

/**
 * Record of a single API usage.
 */
data class UsageRecord(
    val timestamp: Long,
    val model: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val cost: Float
)

/**
 * Current usage statistics.
 */
data class UsageStats(
    val todayCost: Float,
    val monthCost: Float,
    val dailyBudget: Float,
    val monthlyBudget: Float,
    val todayRemaining: Float,
    val monthRemaining: Float,
    val recentRecords: List<UsageRecord>
) {
    val todayPercentUsed: Float get() = (todayCost / dailyBudget * 100).coerceIn(0f, 100f)
    val monthPercentUsed: Float get() = (monthCost / monthlyBudget * 100).coerceIn(0f, 100f)
}

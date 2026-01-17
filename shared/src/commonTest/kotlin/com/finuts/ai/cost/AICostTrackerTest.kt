package com.finuts.ai.cost

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AICostTrackerTest {

    // === Cost Calculation ===

    @Test
    fun `calculates GPT-4o-mini cost correctly`() {
        val cost = AICostTracker.calculateCost(
            inputTokens = 1000,
            outputTokens = 500,
            model = "gpt-4o-mini"
        )

        // 1K input * 0.00015 + 0.5K output * 0.0006 = 0.00015 + 0.0003 = 0.00045
        assertEquals(0.00045f, cost, 0.00001f)
    }

    @Test
    fun `calculates GPT-4o cost correctly`() {
        val cost = AICostTracker.calculateCost(
            inputTokens = 1000,
            outputTokens = 500,
            model = "gpt-4o"
        )

        // 1K input * 0.005 + 0.5K output * 0.015 = 0.005 + 0.0075 = 0.0125
        assertEquals(0.0125f, cost, 0.0001f)
    }

    @Test
    fun `calculates Claude Haiku cost correctly`() {
        val cost = AICostTracker.calculateCost(
            inputTokens = 2000,
            outputTokens = 1000,
            model = "claude-3-5-haiku-latest"
        )

        // 2K input * 0.0008 + 1K output * 0.004 = 0.0016 + 0.004 = 0.0056
        assertEquals(0.0056f, cost, 0.0001f)
    }

    @Test
    fun `uses default cost for unknown model`() {
        val cost = AICostTracker.calculateCost(
            inputTokens = 1000,
            outputTokens = 500,
            model = "unknown-model-xyz"
        )

        // Should use default: 1K * 0.001 + 0.5K * 0.005 = 0.001 + 0.0025 = 0.0035
        assertEquals(0.0035f, cost, 0.0001f)
    }

    // === Budget Enforcement ===

    @Test
    fun `allows execution within daily budget`() {
        val tracker = AICostTracker(dailyBudget = 0.10f, monthlyBudget = 2.0f)

        assertTrue(tracker.canExecute(0.05f))
        assertTrue(tracker.canExecute(0.09f))
    }

    @Test
    fun `blocks execution exceeding daily budget`() {
        val tracker = AICostTracker(dailyBudget = 0.10f, monthlyBudget = 2.0f)

        // Record some usage
        tracker.record(inputTokens = 10000, outputTokens = 5000, model = "gpt-4o")
        // Cost: 10K * 0.005 + 5K * 0.015 = 0.05 + 0.075 = 0.125 > 0.10 budget

        assertFalse(tracker.canExecute(0.001f))
    }

    // === Usage Recording ===

    @Test
    fun `records usage and updates today cost`() {
        val tracker = AICostTracker()

        assertEquals(0f, tracker.todayCost.value)

        tracker.record(inputTokens = 1000, outputTokens = 500, model = "gpt-4o-mini")

        assertTrue(tracker.todayCost.value > 0)
    }

    @Test
    fun `accumulates multiple records`() {
        val tracker = AICostTracker()

        tracker.record(inputTokens = 1000, outputTokens = 500, model = "gpt-4o-mini")
        val afterFirst = tracker.todayCost.value

        tracker.record(inputTokens = 1000, outputTokens = 500, model = "gpt-4o-mini")
        val afterSecond = tracker.todayCost.value

        assertEquals(afterFirst * 2, afterSecond, 0.0001f)
    }

    // === Usage Stats ===

    @Test
    fun `returns correct usage stats`() {
        val tracker = AICostTracker(dailyBudget = 0.10f, monthlyBudget = 2.0f)

        tracker.record(inputTokens = 1000, outputTokens = 500, model = "gpt-4o-mini")

        val stats = tracker.getUsageStats()

        assertTrue(stats.todayCost > 0)
        assertEquals(0.10f, stats.dailyBudget)
        assertEquals(2.0f, stats.monthlyBudget)
        assertTrue(stats.todayRemaining > 0)
        assertTrue(stats.todayPercentUsed > 0)
        assertTrue(stats.recentRecords.isNotEmpty())
    }

    // === History ===

    @Test
    fun `maintains usage history`() {
        val tracker = AICostTracker()

        repeat(5) {
            tracker.record(inputTokens = 100, outputTokens = 50, model = "gpt-4o-mini")
        }

        val history = tracker.getHistory(days = 1)

        assertEquals(5, history.size)
    }

    @Test
    fun `history records contain correct model`() {
        val tracker = AICostTracker()

        tracker.record(inputTokens = 100, outputTokens = 50, model = "claude-sonnet-4-20250514")

        val history = tracker.getHistory()

        assertEquals("claude-sonnet-4-20250514", history.first().model)
    }

    // === Edge Cases ===

    @Test
    fun `handles zero tokens`() {
        val cost = AICostTracker.calculateCost(
            inputTokens = 0,
            outputTokens = 0,
            model = "gpt-4o"
        )

        assertEquals(0f, cost)
    }

    @Test
    fun `returns empty history when no records`() {
        val tracker = AICostTracker()

        val history = tracker.getHistory()

        assertTrue(history.isEmpty())
    }

    @Test
    fun `new tracker starts with zero cost`() {
        val tracker = AICostTracker()

        assertEquals(0f, tracker.todayCost.value)
        assertEquals(0f, tracker.monthCost.value)
    }

    // === Data Class Tests ===

    @Test
    fun `TokenCost stores input and output costs`() {
        val cost = TokenCost(input = 0.002f, output = 0.010f)

        assertEquals(0.002f, cost.input)
        assertEquals(0.010f, cost.output)
    }

    @Test
    fun `UsageRecord stores all fields`() {
        val record = UsageRecord(
            timestamp = 1234567890L,
            model = "gpt-4o-mini",
            inputTokens = 500,
            outputTokens = 100,
            cost = 0.001f
        )

        assertEquals(1234567890L, record.timestamp)
        assertEquals("gpt-4o-mini", record.model)
        assertEquals(500, record.inputTokens)
        assertEquals(100, record.outputTokens)
        assertEquals(0.001f, record.cost)
    }

    @Test
    fun `UsageStats stores all fields`() {
        val stats = UsageStats(
            todayCost = 0.05f,
            monthCost = 1.0f,
            dailyBudget = 0.10f,
            monthlyBudget = 2.0f,
            todayRemaining = 0.05f,
            monthRemaining = 1.0f,
            recentRecords = emptyList()
        )

        assertEquals(0.05f, stats.todayCost)
        assertEquals(1.0f, stats.monthCost)
        assertEquals(0.10f, stats.dailyBudget)
        assertEquals(2.0f, stats.monthlyBudget)
        assertEquals(0.05f, stats.todayRemaining)
        assertEquals(1.0f, stats.monthRemaining)
        assertTrue(stats.recentRecords.isEmpty())
    }

    @Test
    fun `UsageStats todayPercentUsed calculates correctly`() {
        val stats = UsageStats(
            todayCost = 0.05f,
            monthCost = 0.5f,
            dailyBudget = 0.10f,
            monthlyBudget = 2.0f,
            todayRemaining = 0.05f,
            monthRemaining = 1.5f,
            recentRecords = emptyList()
        )

        assertEquals(50f, stats.todayPercentUsed, 0.01f)
    }

    @Test
    fun `UsageStats monthPercentUsed calculates correctly`() {
        val stats = UsageStats(
            todayCost = 0.05f,
            monthCost = 1.0f,
            dailyBudget = 0.10f,
            monthlyBudget = 2.0f,
            todayRemaining = 0.05f,
            monthRemaining = 1.0f,
            recentRecords = emptyList()
        )

        assertEquals(50f, stats.monthPercentUsed, 0.01f)
    }

    @Test
    fun `UsageStats percentUsed is clamped to 100`() {
        val stats = UsageStats(
            todayCost = 0.20f, // Over budget
            monthCost = 3.0f,
            dailyBudget = 0.10f,
            monthlyBudget = 2.0f,
            todayRemaining = 0f,
            monthRemaining = 0f,
            recentRecords = emptyList()
        )

        assertEquals(100f, stats.todayPercentUsed)
        assertEquals(100f, stats.monthPercentUsed)
    }

    @Test
    fun `calculates Claude Sonnet cost correctly`() {
        val cost = AICostTracker.calculateCost(
            inputTokens = 1000,
            outputTokens = 1000,
            model = "claude-sonnet-4-20250514"
        )

        // 1K input * 0.003 + 1K output * 0.015 = 0.003 + 0.015 = 0.018
        assertEquals(0.018f, cost, 0.0001f)
    }

    @Test
    fun `calculates Claude Opus cost correctly`() {
        val cost = AICostTracker.calculateCost(
            inputTokens = 1000,
            outputTokens = 500,
            model = "claude-opus-4-20250514"
        )

        // 1K input * 0.015 + 0.5K output * 0.075 = 0.015 + 0.0375 = 0.0525
        assertEquals(0.0525f, cost, 0.0001f)
    }

    @Test
    fun `calculates GPT-4-turbo cost correctly`() {
        val cost = AICostTracker.calculateCost(
            inputTokens = 1000,
            outputTokens = 500,
            model = "gpt-4-turbo"
        )

        // 1K input * 0.01 + 0.5K output * 0.03 = 0.01 + 0.015 = 0.025
        assertEquals(0.025f, cost, 0.0001f)
    }

    @Test
    fun `blocks execution exceeding monthly budget`() {
        val tracker = AICostTracker(dailyBudget = 10f, monthlyBudget = 0.05f)

        // Record some usage that exceeds monthly budget
        tracker.record(inputTokens = 10000, outputTokens = 5000, model = "gpt-4o")

        assertFalse(tracker.canExecute(0.001f))
    }

    @Test
    fun `limits recent records to 10`() {
        val tracker = AICostTracker()

        repeat(15) {
            tracker.record(inputTokens = 100, outputTokens = 50, model = "gpt-4o-mini")
        }

        val stats = tracker.getUsageStats()

        assertEquals(10, stats.recentRecords.size)
    }
}

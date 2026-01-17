package com.finuts.ai.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for AI model data classes.
 */
class AIModelsTest {

    // === AIResult ===

    @Test
    fun `AIResult Success stores data and tokens`() {
        val result = AIResult.Success("test data", tokensUsed = 100)

        assertEquals("test data", result.data)
        assertEquals(100, result.tokensUsed)
    }

    @Test
    fun `AIResult Success default tokensUsed is zero`() {
        val result = AIResult.Success("data")

        assertEquals(0, result.tokensUsed)
    }

    @Test
    fun `AIResult Error stores message and cause`() {
        val exception = RuntimeException("test")
        val result = AIResult.Error("error message", exception)

        assertEquals("error message", result.message)
        assertEquals(exception, result.cause)
    }

    @Test
    fun `AIResult Error cause is optional`() {
        val result = AIResult.Error("error message")

        assertEquals("error message", result.message)
        assertNull(result.cause)
    }

    @Test
    fun `AIResult sealed class variants are distinct`() {
        val success: AIResult<String> = AIResult.Success("data")
        val error: AIResult<String> = AIResult.Error("error")
        val costLimit: AIResult<String> = AIResult.CostLimitExceeded
        val unavailable: AIResult<String> = AIResult.ProviderUnavailable

        assertIs<AIResult.Success<String>>(success)
        assertIs<AIResult.Error>(error)
        assertIs<AIResult.CostLimitExceeded>(costLimit)
        assertIs<AIResult.ProviderUnavailable>(unavailable)
    }

    // === Period ===

    @Test
    fun `Period stores start and end dates`() {
        val start = LocalDate(2026, 1, 1)
        val end = LocalDate(2026, 1, 31)
        val period = Period(start, end)

        assertEquals(start, period.start)
        assertEquals(end, period.end)
    }

    @Test
    fun `Period lastDays creates correct range`() {
        val period = Period.lastDays(7)

        assertNotNull(period.start)
        assertNotNull(period.end)
        // End should be >= start
        assertTrue(period.end >= period.start)
    }

    @Test
    fun `Period lastDays 30 creates month range`() {
        val period = Period.lastDays(30)

        assertNotNull(period)
        assertTrue(period.end >= period.start)
    }

    // === SpendingInsights ===

    @Test
    fun `SpendingInsights stores all fields`() {
        val period = Period(LocalDate(2026, 1, 1), LocalDate(2026, 1, 31))
        val insights = SpendingInsights(
            period = period,
            totalSpent = 100000L,
            topCategories = listOf(
                CategorySpending("groceries", "Groceries", 50000L, 50f, Trend.UP)
            ),
            comparisonToPrevious = 10.5f,
            insights = listOf(
                InsightItem(InsightType.SPENDING_SPIKE, "High spending", "desc")
            ),
            recommendations = listOf("Reduce spending")
        )

        assertEquals(100000L, insights.totalSpent)
        assertEquals(1, insights.topCategories.size)
        assertEquals(10.5f, insights.comparisonToPrevious)
        assertEquals(1, insights.insights.size)
        assertEquals(1, insights.recommendations.size)
    }

    // === CategorySpending ===

    @Test
    fun `CategorySpending stores all fields`() {
        val spending = CategorySpending(
            categoryId = "groceries",
            categoryName = "Groceries",
            amount = 50000L,
            percentage = 25.5f,
            trend = Trend.DOWN
        )

        assertEquals("groceries", spending.categoryId)
        assertEquals("Groceries", spending.categoryName)
        assertEquals(50000L, spending.amount)
        assertEquals(25.5f, spending.percentage)
        assertEquals(Trend.DOWN, spending.trend)
    }

    // === Trend enum ===

    @Test
    fun `Trend enum has all values`() {
        val trends = Trend.entries

        assertEquals(3, trends.size)
        assertTrue(Trend.UP in trends)
        assertTrue(Trend.DOWN in trends)
        assertTrue(Trend.STABLE in trends)
    }

    // === InsightItem ===

    @Test
    fun `InsightItem stores all fields`() {
        val item = InsightItem(
            type = InsightType.BUDGET_WARNING,
            title = "Budget Alert",
            description = "You exceeded your budget",
            severity = Severity.ALERT
        )

        assertEquals(InsightType.BUDGET_WARNING, item.type)
        assertEquals("Budget Alert", item.title)
        assertEquals("You exceeded your budget", item.description)
        assertEquals(Severity.ALERT, item.severity)
    }

    @Test
    fun `InsightItem default severity is INFO`() {
        val item = InsightItem(
            type = InsightType.SAVINGS_OPPORTUNITY,
            title = "Save",
            description = "desc"
        )

        assertEquals(Severity.INFO, item.severity)
    }

    // === InsightType enum ===

    @Test
    fun `InsightType enum has all values`() {
        val types = InsightType.entries

        assertEquals(6, types.size)
        assertTrue(InsightType.SPENDING_SPIKE in types)
        assertTrue(InsightType.UNUSUAL_TRANSACTION in types)
        assertTrue(InsightType.RECURRING_DETECTED in types)
        assertTrue(InsightType.BUDGET_WARNING in types)
        assertTrue(InsightType.SAVINGS_OPPORTUNITY in types)
        assertTrue(InsightType.GOAL_PROGRESS in types)
    }

    // === Severity enum ===

    @Test
    fun `Severity enum has all values`() {
        val severities = Severity.entries

        assertEquals(3, severities.size)
        assertTrue(Severity.INFO in severities)
        assertTrue(Severity.WARNING in severities)
        assertTrue(Severity.ALERT in severities)
    }

    // === Anomaly ===

    @Test
    fun `Anomaly stores all fields`() {
        val anomaly = Anomaly(
            transactionId = "tx-123",
            type = AnomalyType.UNUSUAL_AMOUNT,
            description = "Very large transaction",
            severity = Severity.WARNING
        )

        assertEquals("tx-123", anomaly.transactionId)
        assertEquals(AnomalyType.UNUSUAL_AMOUNT, anomaly.type)
        assertEquals("Very large transaction", anomaly.description)
        assertEquals(Severity.WARNING, anomaly.severity)
    }

    // === AnomalyType enum ===

    @Test
    fun `AnomalyType enum has all values`() {
        val types = AnomalyType.entries

        assertEquals(5, types.size)
        assertTrue(AnomalyType.UNUSUAL_AMOUNT in types)
        assertTrue(AnomalyType.UNUSUAL_CATEGORY in types)
        assertTrue(AnomalyType.UNUSUAL_TIME in types)
        assertTrue(AnomalyType.DUPLICATE_SUSPECTED in types)
        assertTrue(AnomalyType.FRAUD_SUSPECTED in types)
    }

    // === ChatResponse ===

    @Test
    fun `ChatResponse stores all fields`() {
        val response = ChatResponse(
            message = "Here is your analysis",
            suggestions = listOf("Reduce dining out"),
            actions = listOf(
                QuickAction(ActionType.VIEW_CATEGORY, "View Dining")
            ),
            sources = listOf("transaction history")
        )

        assertEquals("Here is your analysis", response.message)
        assertEquals(1, response.suggestions.size)
        assertEquals(1, response.actions.size)
        assertEquals(1, response.sources.size)
    }

    @Test
    fun `ChatResponse defaults are empty lists`() {
        val response = ChatResponse(message = "Simple message")

        assertTrue(response.suggestions.isEmpty())
        assertTrue(response.actions.isEmpty())
        assertTrue(response.sources.isEmpty())
    }

    // === QuickAction ===

    @Test
    fun `QuickAction stores all fields`() {
        val action = QuickAction(
            type = ActionType.CREATE_BUDGET,
            label = "Create Budget",
            payload = mapOf("categoryId" to "groceries")
        )

        assertEquals(ActionType.CREATE_BUDGET, action.type)
        assertEquals("Create Budget", action.label)
        assertEquals("groceries", action.payload["categoryId"])
    }

    @Test
    fun `QuickAction default payload is empty`() {
        val action = QuickAction(
            type = ActionType.EXPORT_REPORT,
            label = "Export"
        )

        assertTrue(action.payload.isEmpty())
    }

    // === ActionType enum ===

    @Test
    fun `ActionType enum has all values`() {
        val types = ActionType.entries

        assertEquals(5, types.size)
        assertTrue(ActionType.CREATE_BUDGET in types)
        assertTrue(ActionType.VIEW_CATEGORY in types)
        assertTrue(ActionType.SET_GOAL in types)
        assertTrue(ActionType.EXPORT_REPORT in types)
        assertTrue(ActionType.SHOW_TRANSACTIONS in types)
    }

    // === Predictions ===

    @Test
    fun `Predictions stores all fields`() {
        val predictions = Predictions(
            type = PredictionType.MONTHLY_SPENDING,
            predictions = listOf(
                PredictionItem("January", 500000L, 0.85f)
            )
        )

        assertEquals(PredictionType.MONTHLY_SPENDING, predictions.type)
        assertEquals(1, predictions.predictions.size)
    }

    // === PredictionType enum ===

    @Test
    fun `PredictionType enum has all values`() {
        val types = PredictionType.entries

        assertEquals(3, types.size)
        assertTrue(PredictionType.MONTHLY_SPENDING in types)
        assertTrue(PredictionType.CATEGORY_SPENDING in types)
        assertTrue(PredictionType.RECURRING_PAYMENTS in types)
    }

    // === PredictionItem ===

    @Test
    fun `PredictionItem stores all fields`() {
        val item = PredictionItem(
            label = "February 2026",
            predictedAmount = 450000L,
            confidence = 0.92f
        )

        assertEquals("February 2026", item.label)
        assertEquals(450000L, item.predictedAmount)
        assertEquals(0.92f, item.confidence)
    }

    // === BudgetSuggestion ===

    @Test
    fun `BudgetSuggestion stores all fields`() {
        val suggestion = BudgetSuggestion(
            categoryId = "dining",
            suggestedAmount = 50000L,
            reasoning = "Based on your average spending",
            confidence = 0.88f
        )

        assertEquals("dining", suggestion.categoryId)
        assertEquals(50000L, suggestion.suggestedAmount)
        assertEquals("Based on your average spending", suggestion.reasoning)
        assertEquals(0.88f, suggestion.confidence)
    }

    // === RecurringPattern ===

    @Test
    fun `RecurringPattern stores all fields`() {
        val pattern = RecurringPattern(
            merchantName = "Netflix",
            averageAmount = 1500L,
            frequency = "monthly",
            nextExpectedDate = LocalDate(2026, 2, 15),
            confidence = 0.95f
        )

        assertEquals("Netflix", pattern.merchantName)
        assertEquals(1500L, pattern.averageAmount)
        assertEquals("monthly", pattern.frequency)
        assertEquals(LocalDate(2026, 2, 15), pattern.nextExpectedDate)
        assertEquals(0.95f, pattern.confidence)
    }

    @Test
    fun `RecurringPattern nextExpectedDate can be null`() {
        val pattern = RecurringPattern(
            merchantName = "Unknown",
            averageAmount = 1000L,
            frequency = "irregular",
            nextExpectedDate = null,
            confidence = 0.5f
        )

        assertNull(pattern.nextExpectedDate)
    }
}

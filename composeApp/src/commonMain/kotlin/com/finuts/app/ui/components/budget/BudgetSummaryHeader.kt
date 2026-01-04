package com.finuts.app.ui.components.budget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMoneyTypography
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Budget Summary Header - Shows total budgeted/spent with progress.
 *
 * Layout:
 * ┌─────────────────────────────────────────┐
 * │ This Month (label)                      │
 * │ ₸145,000 of ₸200,000 (displayMedium)   │
 * │ 32dp                                    │
 * │ ████████████████░░░  72% used           │
 * └─────────────────────────────────────────┘
 */
@Composable
fun BudgetSummaryHeader(
    totalSpent: Long,
    totalBudgeted: Long,
    currencySymbol: String,
    periodLabel: String = "This Month",
    modifier: Modifier = Modifier
) {
    val percentUsed = calculateOverallPercentage(totalSpent, totalBudgeted)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FinutsSpacing.screenPadding)
    ) {
        // Period label
        Text(
            text = periodLabel,
            style = FinutsTypography.labelMedium,
            color = FinutsColors.TextTertiary
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.xxs))

        // Total amounts
        Text(
            text = formatSummaryText(totalSpent, totalBudgeted, currencySymbol),
            style = FinutsMoneyTypography.displayMedium,
            color = FinutsColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.lg))

        // Progress bar with percentage
        BudgetProgressBar(
            percentUsed = percentUsed,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.sm))

        Text(
            text = formatPercentageText(percentUsed),
            style = FinutsTypography.bodyMedium,
            color = when {
                percentUsed >= 100f -> FinutsColors.Expense
                percentUsed >= 80f -> FinutsColors.Warning
                else -> FinutsColors.TextTertiary
            }
        )
    }
}

/**
 * Calculates overall budget usage percentage.
 */
fun calculateOverallPercentage(totalSpent: Long, totalBudgeted: Long): Float =
    if (totalBudgeted > 0) (totalSpent.toFloat() / totalBudgeted * 100) else 0f

/**
 * Formats summary text showing spent of budgeted.
 */
fun formatSummaryText(totalSpent: Long, totalBudgeted: Long, currencySymbol: String): String {
    val spentFormatted = formatSummaryMoney(totalSpent, currencySymbol)
    val budgetFormatted = formatSummaryMoney(totalBudgeted, currencySymbol)
    return "$spentFormatted of $budgetFormatted"
}

/**
 * Formats percentage text.
 */
fun formatPercentageText(percentUsed: Float): String =
    "${percentUsed.toInt()}% used"

private fun formatSummaryMoney(amount: Long, currencySymbol: String): String {
    val whole = amount / 100
    val formatted = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$currencySymbol$formatted"
}

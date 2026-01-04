package com.finuts.app.feature.budgets.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMoneyTypography
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.budget.BudgetProgressBar
import com.finuts.domain.entity.BudgetStatus

/**
 * Budget detail header with linear progress bar.
 * Displays percentage, progress bar, and remaining amount.
 */
@Composable
fun BudgetDetailHeader(
    percentUsed: Float,
    spent: Long,
    budget: Long,
    currencySymbol: String,
    period: String,
    modifier: Modifier = Modifier
) {
    val status = getBudgetStatusFromPercent(percentUsed)
    val statusColor = when (status) {
        BudgetStatus.OVER_BUDGET -> FinutsColors.Expense
        BudgetStatus.WARNING -> FinutsColors.Warning
        else -> FinutsColors.TextPrimary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FinutsSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Percentage display
        Text(
            text = "${percentUsed.coerceAtMost(999f).toInt()}%",
            style = FinutsMoneyTypography.displayMedium,
            color = statusColor
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Linear progress bar (replaces circular ring per UX research)
        BudgetProgressBar(
            percentUsed = percentUsed,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Remaining amount
        Text(
            text = formatRemainingAmount(spent, budget, currencySymbol),
            style = FinutsTypography.titleMedium,
            color = statusColor
        )

        Spacer(Modifier.height(FinutsSpacing.xxs))

        // Period label
        Text(
            text = period,
            style = FinutsTypography.bodySmall,
            color = FinutsColors.TextTertiary
        )
    }
}

/** Formats remaining/overspent amount for display. */
fun formatRemainingAmount(spent: Long, budget: Long, currencySymbol: String): String {
    val diff = budget - spent
    val absDiff = kotlin.math.abs(diff)
    val formatted = (absDiff / 100).toString().reversed().chunked(3).joinToString(",").reversed()
    return if (diff >= 0) "$currencySymbol$formatted remaining" else "$currencySymbol$formatted overspent"
}

/** Returns budget status based on percentage used. */
fun getBudgetStatusFromPercent(percentUsed: Float): BudgetStatus = when {
    percentUsed < 80f -> BudgetStatus.ON_TRACK
    percentUsed < 100f -> BudgetStatus.WARNING
    else -> BudgetStatus.OVER_BUDGET
}

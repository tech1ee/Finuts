package com.finuts.app.ui.components.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMoneyTypography
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.domain.entity.BudgetPeriod
import com.finuts.domain.entity.BudgetProgress
import com.finuts.domain.entity.BudgetStatus

/**
 * Budget List Item - 64dp height with progress bar.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 16dp │ [40dp] │ 12dp │ Name           │    │ ₸45,000/₸60,000│
 * │      │  Icon  │      │ Monthly        │    │                 │
 * │      │        │      │ ████████░░░    │    │ ₸15,000 left    │
 * └─────────────────────────────────────────────────────────────┘
 */
@Composable
fun BudgetListItem(
    budgetProgress: BudgetProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budget = budgetProgress.budget
    val currencySymbol = budget.currency.symbol

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FinutsSpacing.listItemHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = FinutsSpacing.screenPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category icon placeholder (40dp)
        Box(
            modifier = Modifier
                .size(FinutsSpacing.transactionIconSize)
                .clip(RoundedCornerShape(10.dp))
                .background(FinutsColors.AccentMuted),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = budget.name.take(1).uppercase(),
                style = FinutsTypography.titleMedium,
                color = FinutsColors.Accent
            )
        }

        Spacer(modifier = Modifier.width(FinutsSpacing.iconToTextGap))

        // Name + Period + Progress
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = budget.name,
                style = FinutsTypography.titleMedium,
                color = FinutsColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = getBudgetPeriodLabel(budget.period),
                style = FinutsTypography.bodySmall,
                color = FinutsColors.TextTertiary
            )
            Spacer(modifier = Modifier.height(4.dp))
            BudgetProgressBar(
                percentUsed = budgetProgress.percentUsed,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }

        Spacer(modifier = Modifier.width(FinutsSpacing.sm))

        // Amounts column (right side)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${formatBudgetMoney(budgetProgress.spent, currencySymbol)}/${formatBudgetMoney(budget.amount, currencySymbol)}",
                style = FinutsMoneyTypography.body,
                color = FinutsColors.TextPrimary
            )
            Text(
                text = getRemainingText(budgetProgress.spent, budget.amount, currencySymbol),
                style = FinutsTypography.bodySmall,
                color = when (budgetProgress.status) {
                    BudgetStatus.OVER_BUDGET -> FinutsColors.Expense
                    BudgetStatus.WARNING -> FinutsColors.Warning
                    else -> FinutsColors.TextTertiary
                }
            )
        }
    }
}

/**
 * Returns human-readable period label.
 */
fun getBudgetPeriodLabel(period: BudgetPeriod): String = when (period) {
    BudgetPeriod.DAILY -> "Daily"
    BudgetPeriod.WEEKLY -> "Weekly"
    BudgetPeriod.MONTHLY -> "Monthly"
    BudgetPeriod.QUARTERLY -> "Quarterly"
    BudgetPeriod.YEARLY -> "Yearly"
}

/**
 * Returns remaining or overspent text.
 */
fun getRemainingText(spent: Long, budget: Long, currencySymbol: String): String {
    val diff = budget - spent
    return if (diff >= 0) {
        "${formatBudgetMoney(diff, currencySymbol)} left"
    } else {
        "${formatBudgetMoney(-diff, currencySymbol)} over"
    }
}

/**
 * Calculates budget status from percentage.
 */
fun calculateBudgetStatus(percentUsed: Float): BudgetStatus = when {
    percentUsed < 80f -> BudgetStatus.ON_TRACK
    percentUsed < 100f -> BudgetStatus.WARNING
    else -> BudgetStatus.OVER_BUDGET
}

/**
 * Formats money amount for display.
 */
private fun formatBudgetMoney(amount: Long, currencySymbol: String): String {
    val whole = amount / 100
    val formatted = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$currencySymbol$formatted"
}

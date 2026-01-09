package com.finuts.app.ui.components.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.feedback.EmptyStatePrompt
import com.finuts.app.ui.icons.FinutsIcons
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.create_budget
import finuts.composeapp.generated.resources.no_budget_yet
import finuts.composeapp.generated.resources.no_budget_yet_desc
import org.jetbrains.compose.resources.stringResource

/**
 * Monthly Overview Card - Combines Spending Progress + Financial Health
 *
 * Vertical layout as per user preference:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ This Month                              December 2025       │
 * │ ₸125,000 / ₸200,000                    62% of budget       │
 * │ ████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │
 * ├─────────────────────────────────────────────────────────────┤
 * │ ✓  On track this month                                     │
 * │    ↓ 15% less spending vs last month                       │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Specs:
 * - Corner radius: 12dp
 * - Internal padding: 16dp
 * - Divider: 1dp, BorderSubtle color
 */

private val CardShape = RoundedCornerShape(12.dp)
private val ProgressShape = RoundedCornerShape(4.dp)

@Composable
fun MonthlyOverviewCard(
    spent: Long,
    budget: Long?,
    periodLabel: String = "December 2025",
    currencySymbol: String = "₸",
    comparisonToLastMonth: Float = 0f,
    onCreateBudget: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Show empty state when no budget is set
    if (budget == null || budget == 0L) {
        MonthlyOverviewEmptyState(
            onCreateBudget = onCreateBudget ?: {},
            modifier = modifier
        )
        return
    }

    val percentage = (spent.toFloat() / budget * 100)
    val animatedProgress by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "progressAnimation"
    )

    val progressColor = when {
        percentage < 80f -> FinutsColors.Accent
        percentage < 100f -> FinutsColors.Warning
        else -> FinutsColors.Expense
    }

    val healthStatus = when {
        percentage < 80f -> HealthStatus.ON_TRACK
        percentage < 100f -> HealthStatus.WARNING
        else -> HealthStatus.OVER_BUDGET
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(FinutsColors.Surface)
            .border(
                width = 1.dp,
                color = FinutsColors.BorderSubtle,
                shape = CardShape
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Section: Spending Progress
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FinutsSpacing.md)
            ) {
                // Header: Title + Period
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "This Month",
                        style = FinutsTypography.labelMedium,
                        color = FinutsColors.TextSecondary
                    )
                    Text(
                        text = periodLabel,
                        style = FinutsTypography.bodySmall,
                        color = FinutsColors.TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(FinutsSpacing.sm))

                // Amount + Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatMoney(spent, currencySymbol)} / ${formatMoney(budget, currencySymbol)}",
                        style = FinutsTypography.titleMedium,
                        color = FinutsColors.TextPrimary
                    )
                    Text(
                        text = "${percentage.toInt()}% of budget",
                        style = FinutsTypography.bodyMedium,
                        color = progressColor
                    )
                }

                Spacer(modifier = Modifier.height(FinutsSpacing.sm))

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(ProgressShape)
                        .background(FinutsColors.SurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(8.dp)
                            .clip(ProgressShape)
                            .background(progressColor)
                    )
                }
            }

            // Divider
            HorizontalDivider(
                color = FinutsColors.BorderSubtle,
                thickness = 1.dp
            )

            // Bottom Section: Financial Health
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FinutsSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Icon
                val (icon, iconColor, statusMessage) = getHealthInfo(healthStatus)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconColor
                )

                Spacer(modifier = Modifier.width(FinutsSpacing.sm))

                Column {
                    Text(
                        text = statusMessage,
                        style = FinutsTypography.titleSmall,
                        color = FinutsColors.TextPrimary
                    )

                    val comparisonText = getComparisonText(comparisonToLastMonth)
                    Text(
                        text = comparisonText,
                        style = FinutsTypography.bodySmall,
                        color = FinutsColors.TextSecondary
                    )
                }
            }
        }
    }
}

private fun getHealthInfo(status: HealthStatus) = when (status) {
    HealthStatus.ON_TRACK -> Triple(
        FinutsIcons.Success,
        FinutsColors.Accent,
        "On track this month"
    )
    HealthStatus.WARNING -> Triple(
        FinutsIcons.Warning,
        FinutsColors.Warning,
        "Approaching budget limit"
    )
    HealthStatus.OVER_BUDGET -> Triple(
        FinutsIcons.Error,
        FinutsColors.Expense,
        "Over budget this month"
    )
}

private fun getComparisonText(comparisonToLastMonth: Float): String {
    return when {
        comparisonToLastMonth < 0 -> "↓ ${kotlin.math.abs(comparisonToLastMonth).toInt()}% less vs last month"
        comparisonToLastMonth > 0 -> "↑ ${comparisonToLastMonth.toInt()}% more vs last month"
        else -> "Same spending as last month"
    }
}

private fun formatMoney(amount: Long, currencySymbol: String): String {
    val whole = amount / 100
    val formattedWhole = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$currencySymbol$formattedWhole"
}

/**
 * Empty state for MonthlyOverviewCard when no budget is set.
 */
@Composable
private fun MonthlyOverviewEmptyState(
    onCreateBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(FinutsColors.Surface)
            .border(
                width = 1.dp,
                color = FinutsColors.BorderSubtle,
                shape = CardShape
            )
    ) {
        EmptyStatePrompt(
            title = stringResource(Res.string.no_budget_yet),
            description = stringResource(Res.string.no_budget_yet_desc),
            actionLabel = stringResource(Res.string.create_budget),
            onAction = onCreateBudget
        )
    }
}

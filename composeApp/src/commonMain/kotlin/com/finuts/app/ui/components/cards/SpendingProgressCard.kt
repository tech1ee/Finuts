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
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Spending Progress Card - Monthly Spending vs Budget
 *
 * Architecture v3.0 component for Overview screen (STATE).
 * Shows how much has been spent this month relative to budget.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ SPENDING THIS MONTH                      December 2025     │
 * │ ████████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │
 * │ ₸125,000 / ₸200,000              62.5% of monthly budget   │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Specs:
 * - Height: 80dp (calculated to fit content)
 * - Corner radius: 12dp
 * - Progress bar height: 8dp, radius 4dp
 * - Colors based on percentage:
 *   - < 80%: Accent (green)
 *   - 80-100%: Warning (amber)
 *   - > 100%: Expense (red)
 */

private val CardShape = RoundedCornerShape(12.dp)
private val ProgressShape = RoundedCornerShape(4.dp)

@Composable
fun SpendingProgressCard(
    spent: Long,
    budget: Long,
    periodLabel: String = "December 2025",
    currencySymbol: String = "₸",
    modifier: Modifier = Modifier
) {
    val percentage = if (budget > 0) (spent.toFloat() / budget * 100) else 0f
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
            .padding(FinutsSpacing.md) // 16dp internal padding
    ) {
        Column {
            // Header: Title + Period
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPENDING THIS MONTH",
                    style = FinutsTypography.labelMedium,
                    color = FinutsColors.TextSecondary
                )
                Text(
                    text = periodLabel,
                    style = FinutsTypography.bodySmall,
                    color = FinutsColors.TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(FinutsSpacing.sm)) // 8dp

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

            Spacer(modifier = Modifier.height(FinutsSpacing.sm)) // 8dp

            // Footer: Amounts + Percentage
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
                    text = "${percentage.toInt()}% of monthly budget",
                    style = FinutsTypography.bodyMedium,
                    color = FinutsColors.TextTertiary
                )
            }
        }
    }
}

private fun formatMoney(amount: Long, currencySymbol: String): String {
    val whole = amount / 100
    val formattedWhole = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$currencySymbol$formattedWhole"
}

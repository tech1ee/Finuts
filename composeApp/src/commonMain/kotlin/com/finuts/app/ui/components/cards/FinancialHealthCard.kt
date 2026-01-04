package com.finuts.app.ui.components.cards

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Financial Health Card - Health Status Indicator
 *
 * Architecture v3.0 component for Overview screen (STATE).
 * Shows overall financial health based on spending vs budget.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 💚 │ On track this month                                   │
 * │    │ ↓ 15% less spending vs last month                     │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Specs:
 * - Height: 72dp
 * - Corner radius: 12dp
 * - Background: Subtle tint based on status
 * - Icon: 24dp emoji
 * - Status colors:
 *   - ON_TRACK: green
 *   - WARNING: amber
 *   - OVER_BUDGET: red
 */

enum class HealthStatus {
    ON_TRACK,
    WARNING,
    OVER_BUDGET
}

data class FinancialHealth(
    val status: HealthStatus,
    val message: String,
    val comparisonText: String
)

private val CardShape = RoundedCornerShape(12.dp)

@Composable
fun FinancialHealthCard(
    health: FinancialHealth,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, statusIcon, iconColor) = when (health.status) {
        HealthStatus.ON_TRACK -> Triple(
            FinutsColors.Accent.copy(alpha = 0.1f),
            "💚",
            FinutsColors.Accent
        )
        HealthStatus.WARNING -> Triple(
            FinutsColors.Warning.copy(alpha = 0.1f),
            "🟡",
            FinutsColors.Warning
        )
        HealthStatus.OVER_BUDGET -> Triple(
            FinutsColors.Expense.copy(alpha = 0.1f),
            "🔴",
            FinutsColors.Expense
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(CardShape)
            .background(backgroundColor)
            .padding(FinutsSpacing.md), // 16dp
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Icon
        Text(
            text = statusIcon,
            style = FinutsTypography.headlineSmall, // 24sp
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(FinutsSpacing.iconToTextGap)) // 12dp

        // Content
        Column {
            Text(
                text = health.message,
                style = FinutsTypography.titleMedium,
                color = FinutsColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = health.comparisonText,
                style = FinutsTypography.bodyMedium,
                color = FinutsColors.TextSecondary
            )
        }
    }
}

/**
 * Create a FinancialHealth based on spending percentage
 */
fun calculateFinancialHealth(
    spendingPercentage: Float,
    comparisonToLastMonth: Float
): FinancialHealth {
    val status = when {
        spendingPercentage < 80f -> HealthStatus.ON_TRACK
        spendingPercentage < 100f -> HealthStatus.WARNING
        else -> HealthStatus.OVER_BUDGET
    }

    val message = when (status) {
        HealthStatus.ON_TRACK -> "On track this month"
        HealthStatus.WARNING -> "Approaching budget limit"
        HealthStatus.OVER_BUDGET -> "Over budget this month"
    }

    val comparisonText = if (comparisonToLastMonth < 0) {
        "↓ ${kotlin.math.abs(comparisonToLastMonth).toInt()}% less spending vs last month"
    } else if (comparisonToLastMonth > 0) {
        "↑ ${comparisonToLastMonth.toInt()}% more spending vs last month"
    } else {
        "Same spending as last month"
    }

    return FinancialHealth(
        status = status,
        message = message,
        comparisonText = comparisonText
    )
}

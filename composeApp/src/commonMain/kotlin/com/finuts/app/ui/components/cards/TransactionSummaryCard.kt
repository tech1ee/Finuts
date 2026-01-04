package com.finuts.app.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Transaction Summary Card - Monthly Income/Expense/Net
 *
 * Architecture v3.0 component for History screen (HISTORY).
 * Shows income, expense, and net change for the period.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ THIS MONTH                              December 2025      │
 * │ ↑ Income                                    +₸200,000      │
 * │ ↓ Expense                                   -₸125,000      │
 * │ ─────────────────────────────────────────────────────────  │
 * │ = Net                                       +₸75,000 💚    │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Specs:
 * - Height: ~140dp (auto)
 * - Corner radius: 12dp
 * - Border: 1dp BorderSubtle
 * - Arrow icons as emoji (16dp text)
 */

data class TransactionSummary(
    val income: Long,
    val expense: Long,
    val periodLabel: String = "December 2025"
) {
    val net: Long get() = income - expense
    val isPositive: Boolean get() = net >= 0
}

private val CardShape = RoundedCornerShape(12.dp)

@Composable
fun TransactionSummaryCard(
    summary: TransactionSummary,
    currencySymbol: String = "₸",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(FinutsColors.Surface)
            .border(
                width = 1.dp,
                color = FinutsColors.BorderSubtle,
                shape = CardShape
            )
            .padding(FinutsSpacing.md) // 16dp
    ) {
        // Header: THIS MONTH + Period
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "THIS MONTH",
                style = FinutsTypography.labelMedium,
                color = FinutsColors.TextTertiary
            )
            Text(
                text = summary.periodLabel,
                style = FinutsTypography.bodySmall,
                color = FinutsColors.TextTertiary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Income row
        SummaryRow(
            icon = "↑",
            label = "Income",
            amount = summary.income,
            currencySymbol = currencySymbol,
            amountColor = FinutsColors.Income,
            isPositive = true
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.sm)) // 8dp

        // Expense row
        SummaryRow(
            icon = "↓",
            label = "Expense",
            amount = summary.expense,
            currencySymbol = currencySymbol,
            amountColor = FinutsColors.Expense,
            isPositive = false
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.sm)) // 8dp

        // Divider
        HorizontalDivider(
            color = FinutsColors.BorderSubtle,
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.sm)) // 8dp

        // Net row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "=",
                style = FinutsTypography.titleMedium,
                color = FinutsColors.TextTertiary,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(FinutsSpacing.sm)) // 8dp

            Text(
                text = "Net",
                style = FinutsTypography.titleMedium,
                color = FinutsColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = formatMoneyWithSign(summary.net, currencySymbol, summary.isPositive),
                style = FinutsTypography.titleLarge,
                color = if (summary.isPositive) FinutsColors.Income else FinutsColors.Expense
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = if (summary.isPositive) "💚" else "🔴",
                style = FinutsTypography.bodyMedium
            )
        }
    }
}

@Composable
private fun SummaryRow(
    icon: String,
    label: String,
    amount: Long,
    currencySymbol: String,
    amountColor: androidx.compose.ui.graphics.Color,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = FinutsTypography.titleMedium,
            color = FinutsColors.TextTertiary,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(FinutsSpacing.sm)) // 8dp

        Text(
            text = label,
            style = FinutsTypography.bodyMedium,
            color = FinutsColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = formatMoneyWithSign(amount, currencySymbol, isPositive),
            style = FinutsTypography.titleMedium,
            color = amountColor
        )
    }
}

private fun formatMoneyWithSign(amount: Long, currencySymbol: String, isPositive: Boolean): String {
    val whole = kotlin.math.abs(amount) / 100
    val formattedWhole = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    val sign = if (isPositive) "+" else "-"
    return "$sign$currencySymbol$formattedWhole"
}

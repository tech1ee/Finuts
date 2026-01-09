package com.finuts.app.ui.components.import

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMoneyTypography
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Import Summary Card - Shows summary of import before confirmation.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Всего транзакций                                       35  │
 * │ Расходы                                          -87,500 ₸ │
 * │ Доходы                                           +12,000 ₸ │
 * │ Период                                   10 янв - 15 янв   │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Specs:
 * - Padding: 16dp
 * - Background: FinutsColors.Surface
 * - Row height: 32dp
 * - Label: FinutsTypography.bodyMedium, left aligned
 * - Value: FinutsMoneyTypography.body, right aligned
 */
@Composable
fun ImportSummaryCard(
    totalTransactions: Int,
    totalExpenses: Long,
    totalIncome: Long,
    dateRange: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FinutsColors.Surface)
            .padding(FinutsSpacing.md)
    ) {
        SummaryRow(
            label = "Всего транзакций",
            value = totalTransactions.toString()
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.sm))

        SummaryRow(
            label = "Расходы",
            value = formatMoney(totalExpenses),
            valueColor = FinutsColors.Expense
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.sm))

        SummaryRow(
            label = "Доходы",
            value = formatMoney(totalIncome),
            valueColor = FinutsColors.Income
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.sm))

        SummaryRow(
            label = "Период",
            value = dateRange
        )
    }
}

/**
 * Balance Change Card - Shows before/after balance.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Было:                                          125,000 ₸   │
 * │ Станет:                            49,500 ₸  (-75,500 ₸)   │
 * └─────────────────────────────────────────────────────────────┘
 */
@Composable
fun BalanceChangeCard(
    currentBalance: Long,
    newBalance: Long,
    modifier: Modifier = Modifier
) {
    val change = newBalance - currentBalance

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FinutsColors.Surface)
            .padding(FinutsSpacing.md)
    ) {
        SummaryRow(
            label = "Было:",
            value = formatMoney(currentBalance)
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Станет:",
                style = FinutsTypography.bodyMedium,
                color = FinutsColors.TextSecondary
            )

            Row {
                Text(
                    text = formatMoney(newBalance),
                    style = FinutsMoneyTypography.body,
                    color = FinutsColors.TextPrimary
                )

                Text(
                    text = "  (${formatMoneyWithSign(change)})",
                    style = FinutsMoneyTypography.label,
                    color = if (change >= 0) FinutsColors.Income else FinutsColors.Expense
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = FinutsColors.TextPrimary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = FinutsTypography.bodyMedium,
            color = FinutsColors.TextSecondary
        )

        Text(
            text = value,
            style = FinutsMoneyTypography.body,
            color = valueColor
        )
    }
}

/**
 * Format money value with currency symbol.
 */
private fun formatMoney(amount: Long): String {
    val formatted = formatNumber(kotlin.math.abs(amount))
    return if (amount < 0) "-$formatted ₸" else "$formatted ₸"
}

/**
 * Format money value with explicit sign.
 */
private fun formatMoneyWithSign(amount: Long): String {
    val formatted = formatNumber(kotlin.math.abs(amount))
    val sign = if (amount >= 0) "+" else "-"
    return "$sign$formatted ₸"
}

/**
 * Format number with thousand separators.
 */
private fun formatNumber(value: Long): String {
    return value.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
}

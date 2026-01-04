package com.finuts.app.feature.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.domain.entity.TransactionType

/**
 * Segmented button selector for transaction type.
 * Shows Expense, Income, Transfer options.
 */
@Composable
fun TransactionTypeSelector(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FinutsSpacing.sm))
            .background(FinutsColors.SurfaceVariant),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TransactionType.entries.forEach { type ->
            TypeChip(
                type = type,
                isSelected = type == selected,
                onClick = { onSelect(type) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TypeChip(
    type: TransactionType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) getTypeColor(type).copy(alpha = 0.12f) else Color.Transparent
    val textColor = if (isSelected) getTypeColor(type) else FinutsColors.TextSecondary

    Text(
        text = getTypeLabel(type),
        style = FinutsTypography.labelLarge,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(FinutsSpacing.sm))
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(vertical = FinutsSpacing.sm)
            .padding(horizontal = FinutsSpacing.md),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

private fun getTypeColor(type: TransactionType) = when (type) {
    TransactionType.INCOME -> FinutsColors.Income
    TransactionType.EXPENSE -> FinutsColors.Expense
    TransactionType.TRANSFER -> FinutsColors.Transfer
}

private fun getTypeLabel(type: TransactionType) = when (type) {
    TransactionType.INCOME -> "Income"
    TransactionType.EXPENSE -> "Expense"
    TransactionType.TRANSFER -> "Transfer"
}

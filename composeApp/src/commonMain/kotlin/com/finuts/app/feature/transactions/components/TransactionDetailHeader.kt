package com.finuts.app.feature.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMoneyTypography
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.utils.formatWithSign
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType

/**
 * Header section for transaction detail.
 * Shows: Amount (large), Type chip, Merchant/Description.
 */
@Composable
fun TransactionDetailHeader(
    transaction: Transaction,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val typeColor = getTypeColor(transaction.type)
    val typeBgColor = getTypeBgColor(transaction.type)
    val displayAmount = transaction.amount.formatWithSign(currencySymbol)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FinutsSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = displayAmount,
            style = FinutsMoneyTypography.displayMedium,
            color = typeColor
        )

        Row(
            modifier = Modifier.padding(top = FinutsSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)
        ) {
            Text(
                text = getTypeLabel(transaction.type),
                style = FinutsTypography.labelMedium,
                color = typeColor,
                modifier = Modifier
                    .background(typeBgColor, RoundedCornerShape(FinutsSpacing.xs))
                    .padding(horizontal = FinutsSpacing.sm, vertical = FinutsSpacing.xxs)
            )
        }

        transaction.merchant?.let { merchant ->
            Text(
                text = merchant,
                style = FinutsTypography.titleMedium,
                color = FinutsColors.TextPrimary,
                modifier = Modifier.padding(top = FinutsSpacing.md)
            )
        }

        transaction.description?.let { description ->
            Text(
                text = description,
                style = FinutsTypography.bodyMedium,
                color = FinutsColors.TextSecondary,
                modifier = Modifier.padding(top = FinutsSpacing.xs)
            )
        }
    }
}

private fun getTypeColor(type: TransactionType) = when (type) {
    TransactionType.INCOME -> FinutsColors.Income
    TransactionType.EXPENSE -> FinutsColors.Expense
    TransactionType.TRANSFER -> FinutsColors.Transfer
}

private fun getTypeBgColor(type: TransactionType) = when (type) {
    TransactionType.INCOME -> FinutsColors.CategoryIncomeMuted
    TransactionType.EXPENSE -> FinutsColors.CategoryFoodMuted
    TransactionType.TRANSFER -> FinutsColors.CategoryEducationMuted
}

private fun getTypeLabel(type: TransactionType) = when (type) {
    TransactionType.INCOME -> "Income"
    TransactionType.EXPENSE -> "Expense"
    TransactionType.TRANSFER -> "Transfer"
}

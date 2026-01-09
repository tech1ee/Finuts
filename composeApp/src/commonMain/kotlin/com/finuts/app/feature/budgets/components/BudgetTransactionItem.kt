package com.finuts.app.feature.budgets.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.ui.components.list.TransactionListItem
import com.finuts.app.ui.components.list.TransactionType as ListTransactionType
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType

@Composable
fun BudgetTransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    TransactionListItem(
        merchantName = transaction.merchant ?: transaction.description ?: "Transaction",
        category = transaction.categoryId ?: "Uncategorized",
        time = formatTime(transaction.date),
        amount = formatAmount(transaction),
        transactionType = transaction.type.toListType(),
        onClick = onClick,
        showDivider = showDivider,
        modifier = modifier
    )
}

private fun TransactionType.toListType(): ListTransactionType = when (this) {
    TransactionType.EXPENSE -> ListTransactionType.EXPENSE
    TransactionType.INCOME -> ListTransactionType.INCOME
    TransactionType.TRANSFER -> ListTransactionType.TRANSFER
}

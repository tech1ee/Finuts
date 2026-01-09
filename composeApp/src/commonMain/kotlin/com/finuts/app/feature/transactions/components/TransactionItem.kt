package com.finuts.app.feature.transactions.components

import androidx.compose.runtime.Composable
import com.finuts.app.ui.components.list.TransactionListItem
import com.finuts.app.ui.components.list.TransactionType as ListTransactionType
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Transaction list item for transactions screen.
 */
@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    val listTransactionType = when (transaction.type) {
        TransactionType.EXPENSE -> ListTransactionType.EXPENSE
        TransactionType.INCOME -> ListTransactionType.INCOME
        TransactionType.TRANSFER -> ListTransactionType.TRANSFER
    }

    TransactionListItem(
        merchantName = transaction.merchant ?: transaction.description ?: "Transaction",
        category = transaction.categoryId ?: "Uncategorized",
        time = formatTime(transaction.date),
        amount = formatAmount(transaction),
        transactionType = listTransactionType,
        isAISuggested = transaction.isAICategorized,
        onClick = onClick,
        showDivider = showDivider
    )
}

private fun formatAmount(transaction: Transaction): String {
    val amount = kotlin.math.abs(transaction.amount)
    val whole = amount / 100
    val fraction = amount % 100
    return "₸$whole.${fraction.toString().padStart(2, '0')}"
}

private fun formatTime(instant: kotlinx.datetime.Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}

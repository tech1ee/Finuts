package com.finuts.app.feature.budgets.components

import com.finuts.domain.entity.Transaction
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

/**
 * Formats an Instant to a time string (HH:mm).
 */
fun formatTime(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour.toString().padStart(2, '0')}:" +
           "${localDateTime.minute.toString().padStart(2, '0')}"
}

/**
 * Formats a transaction amount with currency symbol.
 * Note: Transaction doesn't have currency, so we use default "₸" symbol.
 */
fun formatAmount(transaction: Transaction, symbol: String = "₸"): String {
    val amount = abs(transaction.amount)
    val whole = amount / 100
    val fraction = amount % 100
    return "$symbol$whole.${fraction.toString().padStart(2, '0')}"
}

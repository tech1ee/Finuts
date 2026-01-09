package com.finuts.data.categorization

/**
 * Data class representing a transaction to be categorized by AI.
 */
data class TransactionForCategorization(
    val id: String,
    val description: String,
    val amount: Long
) {
    /** Amount formatted as currency (e.g., "1234.56") */
    val formattedAmount: String
        get() {
            val absAmount = kotlin.math.abs(amount)
            val wholePart = absAmount / 100
            val decimalPart = absAmount % 100
            val sign = if (amount < 0) "-" else ""
            return "$sign$wholePart.${decimalPart.toString().padStart(2, '0')}"
        }
}

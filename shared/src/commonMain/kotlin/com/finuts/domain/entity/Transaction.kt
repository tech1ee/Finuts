package com.finuts.domain.entity

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: String,
    val accountId: String,
    val amount: Long,
    val type: TransactionType,
    val categoryId: String?,
    val description: String?,
    val merchant: String?,
    val note: String?,
    val date: Instant,
    val isRecurring: Boolean = false,
    val recurringRuleId: String? = null,
    val attachments: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    // Transfer fields: used when type == TRANSFER
    val linkedTransactionId: String? = null,
    val transferAccountId: String? = null
)

@Serializable
enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

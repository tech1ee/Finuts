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
    val transferAccountId: String? = null,
    // AI categorization metadata (Tier 0-3 tracking)
    val categorizationSource: CategorizationSource? = null,
    val categorizationConfidence: Float? = null
) {
    /** True if category was assigned by AI (Tier 0-3, not manual) */
    val isAICategorized: Boolean
        get() = categorizationSource != null && categorizationSource != CategorizationSource.USER

    /** True if categorization confidence is below the high threshold (needs review) */
    val needsCategoryReview: Boolean
        get() = categorizationConfidence != null &&
                categorizationConfidence < CategorizationResult.HIGH_CONFIDENCE_THRESHOLD
}

@Serializable
enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

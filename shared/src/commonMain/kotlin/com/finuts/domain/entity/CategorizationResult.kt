package com.finuts.domain.entity

import kotlinx.serialization.Serializable

/**
 * Source of the categorization decision.
 * Used to track which tier/method produced the result.
 */
@Serializable
enum class CategorizationSource {
    /** Tier 0: Learned from user corrections (highest priority) */
    USER_LEARNED,
    /** Rule-based regex pattern matching */
    RULE_BASED,
    /** Merchant database lookup */
    MERCHANT_DATABASE,
    /** User's previous categorization for same merchant */
    USER_HISTORY,
    /** Tier 1.5: On-device ML model */
    ON_DEVICE_ML,
    /** LLM Tier 2 (GPT-4o-mini / Claude Haiku) */
    LLM_TIER2,
    /** LLM Tier 3 (GPT-4o / Claude Sonnet) */
    LLM_TIER3,
    /** Manual user categorization */
    USER
}

/**
 * Result of transaction categorization.
 * Contains the suggested category and confidence score.
 */
@Serializable
data class CategorizationResult(
    val transactionId: String,
    val categoryId: String,
    val confidence: Float,
    val source: CategorizationSource
) {
    companion object {
        const val HIGH_CONFIDENCE_THRESHOLD = 0.85f
        const val MEDIUM_CONFIDENCE_THRESHOLD = 0.70f
    }

    /** True if confidence >= 0.85 (auto-apply without confirmation) */
    val isHighConfidence: Boolean
        get() = confidence >= HIGH_CONFIDENCE_THRESHOLD

    /** True if confidence is between 0.70 and 0.85 */
    val isMediumConfidence: Boolean
        get() = confidence >= MEDIUM_CONFIDENCE_THRESHOLD && confidence < HIGH_CONFIDENCE_THRESHOLD

    /** True if confidence < 0.70 */
    val isLowConfidence: Boolean
        get() = confidence < MEDIUM_CONFIDENCE_THRESHOLD

    /** True if user should confirm the categorization */
    val requiresUserConfirmation: Boolean
        get() = isLowConfidence

    /** True if categorization came from Tier 0-1 (free, local) */
    val isLocalSource: Boolean
        get() = source in listOf(
            CategorizationSource.USER_LEARNED,
            CategorizationSource.RULE_BASED,
            CategorizationSource.MERCHANT_DATABASE,
            CategorizationSource.USER_HISTORY,
            CategorizationSource.ON_DEVICE_ML
        )
}

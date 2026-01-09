package com.finuts.domain.entity.import

import kotlinx.serialization.Serializable

/**
 * Result of duplicate detection for an imported transaction.
 * Indicates whether a transaction is unique or matches an existing one.
 */
@Serializable
sealed interface DuplicateStatus {

    /**
     * Whether this status indicates a duplicate (exact or probable).
     */
    val isDuplicate: Boolean

    /**
     * Transaction is unique - no matching transaction found in database.
     */
    @Serializable
    data object Unique : DuplicateStatus {
        override val isDuplicate: Boolean = false
    }

    /**
     * Transaction is an exact duplicate of an existing transaction.
     * Same date, amount, and description match perfectly.
     *
     * @property matchingTransactionId ID of the matching transaction in database
     */
    @Serializable
    data class ExactDuplicate(
        val matchingTransactionId: String
    ) : DuplicateStatus {
        override val isDuplicate: Boolean = true
        val similarity: Float = 1.0f
    }

    /**
     * Transaction is a probable duplicate with high similarity.
     * Date and amount match, description is similar.
     *
     * @property matchingTransactionId ID of the potentially matching transaction
     * @property similarity Similarity score from 0.0 to 1.0
     * @property reason Human-readable explanation of why it's considered duplicate
     */
    @Serializable
    data class ProbableDuplicate(
        val matchingTransactionId: String,
        val similarity: Float,
        val reason: String
    ) : DuplicateStatus {
        override val isDuplicate: Boolean = true
    }
}

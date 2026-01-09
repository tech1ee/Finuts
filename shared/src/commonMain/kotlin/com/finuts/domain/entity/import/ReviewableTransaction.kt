package com.finuts.domain.entity.import

import kotlinx.serialization.Serializable

/**
 * Wrapper for ImportedTransaction during the review step.
 * Contains additional state for user selection and modifications.
 *
 * @property index Position in the list (used as key for LazyColumn)
 * @property transaction The underlying imported transaction
 * @property duplicateStatus Result of duplicate detection
 * @property isSelected Whether user wants to import this transaction
 * @property categoryOverride User-selected category (overrides AI suggestion)
 */
@Serializable
data class ReviewableTransaction(
    val index: Int,
    val transaction: ImportedTransaction,
    val duplicateStatus: DuplicateStatus,
    val isSelected: Boolean,
    val categoryOverride: String?
) {
    /**
     * Returns the effective category to use: override if set, otherwise original.
     */
    val effectiveCategory: String?
        get() = categoryOverride ?: transaction.category

    /**
     * Whether this is a probable (but not exact) duplicate.
     */
    val isProbableDuplicate: Boolean
        get() = duplicateStatus is DuplicateStatus.ProbableDuplicate

    /**
     * Whether this is an exact duplicate.
     */
    val isExactDuplicate: Boolean
        get() = duplicateStatus is DuplicateStatus.ExactDuplicate
}

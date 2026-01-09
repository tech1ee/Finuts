package com.finuts.domain.entity.import

import kotlinx.serialization.Serializable

/**
 * Result of import processing shown to user before confirmation.
 * Contains all transactions with their duplicate status and validation warnings.
 *
 * @property transactions List of transactions ready for review
 * @property documentType The detected document type
 * @property duplicateCount Total number of duplicates detected
 * @property validationWarnings List of validation warnings to show user
 */
@Serializable
data class ImportPreviewResult(
    val transactions: List<ReviewableTransaction>,
    val documentType: DocumentType,
    val duplicateCount: Int,
    val validationWarnings: List<String>
) {
    /**
     * Total number of transactions in the import.
     */
    val totalCount: Int
        get() = transactions.size

    /**
     * Number of transactions selected for import.
     */
    val selectedCount: Int
        get() = transactions.count { it.isSelected }

    /**
     * Whether there are any validation warnings.
     */
    val hasWarnings: Boolean
        get() = validationWarnings.isNotEmpty()

    /**
     * Whether there are any duplicates.
     */
    val hasDuplicates: Boolean
        get() = duplicateCount > 0

    /**
     * Total income amount from selected transactions.
     */
    val totalIncome: Long
        get() = transactions
            .filter { it.isSelected && it.transaction.amount > 0 }
            .sumOf { it.transaction.amount }

    /**
     * Total expenses amount from selected transactions.
     */
    val totalExpenses: Long
        get() = transactions
            .filter { it.isSelected && it.transaction.amount < 0 }
            .sumOf { it.transaction.amount }
}

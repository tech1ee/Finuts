package com.finuts.domain.entity.import

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Represents a transaction parsed from an imported document.
 * This is a temporary entity used during the import process before
 * the transaction is confirmed and saved to the database.
 *
 * @property date The transaction date
 * @property amount The transaction amount in minor units (kopecks/cents).
 *                  Negative for expenses, positive for income.
 * @property description The transaction description or memo
 * @property merchant The merchant name if identifiable
 * @property balance The account balance after this transaction (if available)
 * @property category The suggested category (from auto-categorization)
 * @property confidence Parsing confidence score from 0.0 to 1.0
 * @property source Which tier of the import pipeline recognized this transaction
 * @property rawData Original field values from the source for debugging/correction
 */
@Serializable
data class ImportedTransaction(
    val date: LocalDate,
    val amount: Long,
    val description: String,
    val merchant: String? = null,
    val balance: Long? = null,
    val category: String? = null,
    val confidence: Float,
    val source: ImportSource,
    val rawData: Map<String, String> = emptyMap()
)

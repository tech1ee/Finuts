package com.finuts.domain.entity.import

import kotlinx.serialization.Serializable

/**
 * Result of the import process.
 * Represents different outcomes from parsing a document.
 */
@Serializable
sealed interface ImportResult {

    /**
     * Successfully parsed all transactions from the document.
     *
     * @property transactions List of parsed transactions ready for user confirmation
     * @property documentType The detected document type
     * @property totalConfidence Overall confidence score for the import (0.0-1.0)
     */
    @Serializable
    data class Success(
        val transactions: List<ImportedTransaction>,
        val documentType: DocumentType,
        val totalConfidence: Float
    ) : ImportResult

    /**
     * Failed to parse the document or encountered critical errors.
     *
     * @property message Error description for the user
     * @property documentType The detected document type (null if unrecognized)
     * @property partialTransactions Any transactions that were successfully parsed
     */
    @Serializable
    data class Error(
        val message: String,
        val documentType: DocumentType?,
        val partialTransactions: List<ImportedTransaction>
    ) : ImportResult

    /**
     * Parsed transactions but some require user clarification.
     * Used when confidence is too low or there are ambiguities.
     *
     * @property transactions Transactions that need user review
     * @property documentType The detected document type
     * @property issues List of issues that need user attention
     */
    @Serializable
    data class NeedsUserInput(
        val transactions: List<ImportedTransaction>,
        val documentType: DocumentType,
        val issues: List<String>
    ) : ImportResult
}

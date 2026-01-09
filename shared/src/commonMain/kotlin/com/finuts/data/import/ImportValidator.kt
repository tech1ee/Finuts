package com.finuts.data.import

import com.finuts.domain.entity.import.ImportValidationResult
import com.finuts.domain.entity.import.ImportedTransaction
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

/**
 * Validator for imported transactions.
 * Checks for common issues and potential data problems.
 */
class ImportValidator {

    companion object {
        /**
         * Amount threshold for "unusually large" warning (1 million in minor units).
         */
        private const val LARGE_AMOUNT_THRESHOLD = 1_000_000_00L
    }

    /**
     * Validate a list of imported transactions.
     * Returns validation result with all warnings and errors found.
     */
    fun validate(transactions: List<ImportedTransaction>): ImportValidationResult {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val now = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

        transactions.forEachIndexed { index, transaction ->
            // Check for future dates
            if (transaction.date > today) {
                warnings.add("Transaction ${index + 1}: Future date detected (${transaction.date})")
            }

            // Check for unusually large amounts
            if (abs(transaction.amount) > LARGE_AMOUNT_THRESHOLD) {
                warnings.add("Transaction ${index + 1}: Unusually large amount")
            }

            // Check for empty descriptions
            if (transaction.description.isBlank()) {
                warnings.add("Transaction ${index + 1}: Empty description")
            }
        }

        return ImportValidationResult(
            warnings = warnings,
            errors = errors
        )
    }

    /**
     * Validate a single transaction.
     */
    fun validateSingle(
        transaction: ImportedTransaction,
        index: Int = 0
    ): ImportValidationResult {
        return validate(listOf(transaction))
    }
}

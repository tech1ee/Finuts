package com.finuts.domain.entity.import

import kotlinx.serialization.Serializable

/**
 * Result of validating imported transactions.
 * Contains warnings and errors found during validation.
 *
 * @property warnings List of warning messages (non-blocking)
 * @property errors List of error messages (blocking)
 */
@Serializable
data class ImportValidationResult(
    val warnings: List<String>,
    val errors: List<String> = emptyList()
) {
    /**
     * Whether validation passed (no errors).
     */
    val isValid: Boolean
        get() = errors.isEmpty()

    /**
     * Whether there are any warnings.
     */
    val hasWarnings: Boolean
        get() = warnings.isNotEmpty()

    /**
     * Whether there are any errors.
     */
    val hasErrors: Boolean
        get() = errors.isNotEmpty()

    /**
     * Total count of warnings.
     */
    val warningCount: Int
        get() = warnings.size

    /**
     * Total count of errors.
     */
    val errorCount: Int
        get() = errors.size

    companion object {
        /**
         * Create a successful validation result with no warnings or errors.
         */
        fun success() = ImportValidationResult(
            warnings = emptyList(),
            errors = emptyList()
        )
    }
}

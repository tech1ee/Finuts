package com.finuts.domain.entity.import

import kotlinx.serialization.Serializable

/**
 * Progress state of the import process.
 * Used to update UI during the multi-step import flow.
 */
@Serializable
sealed interface ImportProgress {

    /**
     * Whether this is a terminal state (completed, failed, or cancelled).
     */
    val isTerminal: Boolean

    /**
     * Initial state - no import in progress.
     */
    @Serializable
    data object Idle : ImportProgress {
        override val isTerminal: Boolean = false
    }

    /**
     * Step 1: Detecting file format.
     *
     * @property filename Name of the file being processed
     * @property fileSizeBytes Size of the file in bytes
     */
    @Serializable
    data class DetectingFormat(
        val filename: String,
        val fileSizeBytes: Long
    ) : ImportProgress {
        override val isTerminal: Boolean = false
    }

    /**
     * Step 2: Parsing transactions from the file.
     *
     * @property documentType The detected document type
     */
    @Serializable
    data class Parsing(
        val documentType: DocumentType
    ) : ImportProgress {
        override val isTerminal: Boolean = false
    }

    /**
     * Step 3: Validating parsed transactions.
     *
     * @property totalTransactions Total number of transactions to validate
     * @property processedCount Number of transactions validated so far
     */
    @Serializable
    data class Validating(
        val totalTransactions: Int,
        val processedCount: Int
    ) : ImportProgress {
        override val isTerminal: Boolean = false
    }

    /**
     * Step 4: Detecting duplicates against existing transactions.
     *
     * @property totalTransactions Total number of transactions to check
     * @property processedCount Number of transactions checked so far
     * @property duplicatesFound Number of duplicates found so far
     */
    @Serializable
    data class Deduplicating(
        val totalTransactions: Int,
        val processedCount: Int,
        val duplicatesFound: Int
    ) : ImportProgress {
        override val isTerminal: Boolean = false
    }

    /**
     * Step 5: Categorizing transactions using AI pipeline.
     *
     * @property totalTransactions Total number of transactions to categorize
     * @property categorizedCount Number of transactions categorized so far
     * @property currentTier Current tier of the categorization pipeline
     */
    @Serializable
    data class Categorizing(
        val totalTransactions: Int,
        val categorizedCount: Int,
        val currentTier: CategorizationTier
    ) : ImportProgress {
        override val isTerminal: Boolean = false
    }

    /**
     * Processing complete - awaiting user confirmation.
     *
     * @property result The preview result for user review
     */
    @Serializable
    data class AwaitingConfirmation(
        val result: ImportPreviewResult
    ) : ImportProgress {
        override val isTerminal: Boolean = false
    }

    /**
     * Step 6: Saving confirmed transactions to database.
     *
     * @property totalTransactions Total number of transactions to save
     * @property savedCount Number of transactions saved so far
     */
    @Serializable
    data class Saving(
        val totalTransactions: Int,
        val savedCount: Int
    ) : ImportProgress {
        override val isTerminal: Boolean = false
    }

    /**
     * Import completed successfully.
     *
     * @property savedCount Number of transactions saved
     * @property skippedCount Number of transactions skipped by user
     * @property duplicateCount Number of duplicates not imported
     */
    @Serializable
    data class Completed(
        val savedCount: Int,
        val skippedCount: Int,
        val duplicateCount: Int
    ) : ImportProgress {
        override val isTerminal: Boolean = true
    }

    /**
     * Import failed with an error.
     *
     * @property message Error message for the user
     * @property recoverable Whether the user can retry
     * @property partialResult Partial result if some transactions were processed
     */
    @Serializable
    data class Failed(
        val message: String,
        val recoverable: Boolean,
        val partialResult: ImportPreviewResult?
    ) : ImportProgress {
        override val isTerminal: Boolean = true
    }

    /**
     * Import cancelled by the user.
     */
    @Serializable
    data object Cancelled : ImportProgress {
        override val isTerminal: Boolean = true
    }
}

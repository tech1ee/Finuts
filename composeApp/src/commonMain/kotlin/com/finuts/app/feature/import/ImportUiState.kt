package com.finuts.app.feature.`import`

import com.finuts.domain.entity.import.ImportPreviewResult
import com.finuts.domain.entity.import.ImportProgress
import com.finuts.domain.entity.import.ReviewableTransaction

/**
 * UI State for the Import flow.
 */
data class ImportUiState(
    val currentStep: ImportStep = ImportStep.ENTRY,
    val progress: ImportProgress = ImportProgress.Idle,
    val previewResult: ImportPreviewResult? = null,
    val selectedAccountId: String? = null,
    val selectedIndices: Set<Int> = emptySet(),
    val categoryOverrides: Map<Int, String> = emptyMap(),
    val filename: String? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
    val importResult: FinalImportResult? = null
) {
    /**
     * Number of transactions selected for import.
     */
    val selectedCount: Int
        get() = selectedIndices.size

    /**
     * Total number of transactions available.
     */
    val totalCount: Int
        get() = previewResult?.totalCount ?: 0

    /**
     * Number of duplicates detected.
     */
    val duplicateCount: Int
        get() = previewResult?.duplicateCount ?: 0

    /**
     * Reviewable transactions from preview result.
     */
    val reviewableTransactions: List<ReviewableTransaction>
        get() = previewResult?.transactions ?: emptyList()

    /**
     * Current step number (1-based) for display.
     */
    val stepNumber: Int
        get() = when (currentStep) {
            ImportStep.ENTRY -> 1
            ImportStep.PROCESSING -> 2
            ImportStep.REVIEW -> 3
            ImportStep.CONFIRM -> 4
            ImportStep.RESULT -> 5
        }

    /**
     * Progress fraction (0.0 - 1.0) for progress bar.
     */
    val progressFraction: Float
        get() = stepNumber / 5f

    /**
     * Step counter text for top bar.
     */
    val stepCounterText: String
        get() = "$stepNumber из 5"
}

/**
 * Final result of the import operation.
 */
data class FinalImportResult(
    val isSuccess: Boolean,
    val savedCount: Int,
    val skippedCount: Int,
    val duplicateCount: Int,
    val errorMessage: String? = null
)

/**
 * Step in the import flow.
 */
enum class ImportStep {
    ENTRY,         // Select file
    PROCESSING,    // Parsing, validating, detecting
    REVIEW,        // Review transactions
    CONFIRM,       // Select account, review summary
    RESULT         // Success or error
}

/**
 * Events from the Import UI.
 */
sealed interface ImportUiEvent {
    data class FileSelected(val filename: String, val content: ByteArray) : ImportUiEvent
    data class AccountSelected(val accountId: String) : ImportUiEvent
    data class TransactionToggled(val index: Int, val selected: Boolean) : ImportUiEvent
    data class CategoryOverridden(val index: Int, val categoryId: String) : ImportUiEvent
    data object ConfirmImport : ImportUiEvent
    data object CancelImport : ImportUiEvent
    data object RetryImport : ImportUiEvent
    data object Reset : ImportUiEvent
    data object NavigateToTransactions : ImportUiEvent
    data object NavigateBack : ImportUiEvent
}

/**
 * One-time navigation/side-effect events.
 */
sealed interface ImportSideEffect {
    data object NavigateToTransactions : ImportSideEffect
    data object NavigateBack : ImportSideEffect
    data class ShowError(val message: String) : ImportSideEffect
    data object ShowFilePicker : ImportSideEffect
}

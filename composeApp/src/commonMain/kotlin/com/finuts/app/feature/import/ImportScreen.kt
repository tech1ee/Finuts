package com.finuts.app.feature.`import`

import co.touchlab.kermit.Logger
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.finuts.domain.entity.import.ReviewableTransaction
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private val log = Logger.withTag("ImportScreen")

/**
 * Main Import screen that orchestrates the import flow.
 * Delegates to step-specific screens based on current step.
 */
@Composable
fun ImportScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    viewModel: ImportViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val scope = rememberCoroutineScope()

    // File picker launcher with supported formats (text + PDF + images for OCR)
    val filePicker = rememberFilePickerLauncher(
        type = PickerType.File(
            extensions = listOf(
                "csv", "ofx", "qfx", "qif", "txt", // Text formats
                "pdf",                              // PDF statements
                "jpg", "jpeg", "png", "heic", "heif" // Images for OCR
            )
        ),
        mode = PickerMode.Single,
        title = "Выберите файл выписки"
    ) { file ->
        file?.let {
            scope.launch {
                val bytes = it.readBytes()
                viewModel.onSelectFileWithBytes(it.name, bytes)
            }
        }
    }

    when (uiState.currentStep) {
        ImportStep.ENTRY -> {
            ImportEntryScreen(
                onSelectFile = { filePicker.launch() },
                onNavigateBack = onNavigateBack,
                modifier = modifier
            )
        }

        ImportStep.PROCESSING -> {
            ImportProgressScreen(
                progress = uiState.progress,
                filename = uiState.filename ?: "",
                onCancel = { viewModel.onCancel() },
                onNavigateBack = { viewModel.onCancel() },
                modifier = modifier
            )
        }

        ImportStep.REVIEW -> {
            ImportReviewScreen(
                transactions = uiState.reviewableTransactions,
                selectedIndices = uiState.selectedIndices,
                duplicateCount = uiState.duplicateCount,
                stepCounterText = uiState.stepCounterText,
                progressFraction = uiState.progressFraction,
                onTransactionToggle = viewModel::onTransactionToggle,
                onSelectAll = viewModel::onSelectAll,
                onDeselectDuplicates = viewModel::onDeselectDuplicates,
                onDeselectAll = viewModel::onDeselectAll,
                onContinue = viewModel::onContinueToConfirm,
                onNavigateBack = { viewModel.onCancel() },
                modifier = modifier
            )
        }

        ImportStep.CONFIRM -> {
            log.i { "CONFIRM STEP: uiState.selectedIndices=${uiState.selectedIndices.size}" }
            log.d { "CONFIRM STEP: uiState.selectedAccountId=${uiState.selectedAccountId}" }
            log.d { "CONFIRM STEP: uiState.previewResult=${uiState.previewResult != null}" }

            val transactions = uiState.reviewableTransactions
            log.d { "CONFIRM STEP: reviewableTransactions=${transactions.size}" }

            val totalExpenses = transactions
                .filter { it.index in uiState.selectedIndices && it.transaction.amount < 0 }
                .sumOf { it.transaction.amount }

            val totalIncome = transactions
                .filter { it.index in uiState.selectedIndices && it.transaction.amount > 0 }
                .sumOf { it.transaction.amount }

            log.d { "CONFIRM STEP CALC: totalExpenses=$totalExpenses, totalIncome=$totalIncome" }

            val dateRange = buildDateRange(transactions)
            log.d { "CONFIRM STEP: dateRange=$dateRange" }

            ImportConfirmScreen(
                accounts = accounts,
                selectedAccountId = uiState.selectedAccountId,
                transactionCount = uiState.selectedIndices.size,
                totalExpenses = totalExpenses,
                totalIncome = totalIncome,
                dateRange = dateRange,
                stepCounterText = uiState.stepCounterText,
                progressFraction = uiState.progressFraction,
                onAccountSelect = viewModel::onAccountSelect,
                onConfirm = {
                    log.i { "onConfirm CALLBACK: Calling viewModel.onConfirmImport()" }
                    viewModel.onConfirmImport()
                },
                onNavigateBack = { viewModel.onNavigateBack() },
                modifier = modifier
            )
        }

        ImportStep.RESULT -> {
            val result = uiState.importResult
            ImportResultScreen(
                isSuccess = result?.isSuccess == true,
                savedCount = result?.savedCount ?: 0,
                skippedCount = result?.skippedCount ?: 0,
                duplicateCount = result?.duplicateCount ?: 0,
                errorMessage = result?.errorMessage ?: uiState.error,
                onViewTransactions = onNavigateToTransactions,
                onGoHome = {
                    viewModel.onReset()
                    onNavigateBack()
                },
                onRetry = viewModel::onReset,
                modifier = modifier
            )
        }
    }
}

private fun buildDateRange(transactions: List<ReviewableTransaction>): String {
    if (transactions.isEmpty()) return ""
    val dates = transactions.map { it.transaction.date }
    val minDate = dates.minOrNull() ?: return ""
    val maxDate = dates.maxOrNull() ?: minDate
    return "${formatDate(minDate)} - ${formatDate(maxDate)}"
}

private fun formatDate(date: kotlinx.datetime.LocalDate): String {
    val months = listOf(
        "янв", "фев", "мар", "апр", "май", "июн",
        "июл", "авг", "сен", "окт", "ноя", "дек"
    )
    return "${date.dayOfMonth} ${months[date.monthNumber - 1]}"
}

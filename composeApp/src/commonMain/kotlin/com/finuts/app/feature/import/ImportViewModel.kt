package com.finuts.app.feature.`import`

import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.data.import.ImportFileProcessor
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.import.DuplicateStatus
import com.finuts.domain.entity.import.ImportProgress
import com.finuts.domain.entity.import.ImportResult
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.usecase.ImportTransactionsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Import flow.
 * Orchestrates the multi-step import process.
 */
class ImportViewModel(
    private val importTransactionsUseCase: ImportTransactionsUseCase,
    private val accountRepository: AccountRepository,
    private val fileProcessor: ImportFileProcessor
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _sideEffects = MutableSharedFlow<ImportSideEffect>()
    val sideEffects: SharedFlow<ImportSideEffect> = _sideEffects.asSharedFlow()

    init {
        loadAccounts()
        observeProgress()
    }

    private fun loadAccounts() {
        safeScope.launch {
            accountRepository.getAllAccounts().collect { accountList ->
                _accounts.value = accountList
                // Auto-select first non-archived account if none selected
                if (_uiState.value.selectedAccountId == null) {
                    accountList.firstOrNull { !it.isArchived }?.let { account ->
                        _uiState.update { it.copy(selectedAccountId = account.id) }
                    }
                }
            }
        }
    }

    private fun observeProgress() {
        safeScope.launch {
            importTransactionsUseCase.progress.collect { progress ->
                _uiState.update { it.copy(progress = progress) }

                when (progress) {
                    is ImportProgress.AwaitingConfirmation -> {
                        // Auto-select non-duplicate transactions
                        val selectedIndices = progress.result.transactions
                            .filter { it.isSelected }
                            .map { it.index }
                            .toSet()

                        _uiState.update {
                            it.copy(
                                currentStep = ImportStep.REVIEW,
                                previewResult = progress.result,
                                selectedIndices = selectedIndices,
                                isLoading = false
                            )
                        }
                    }

                    is ImportProgress.Completed -> {
                        _uiState.update {
                            it.copy(
                                currentStep = ImportStep.RESULT,
                                isLoading = false
                            )
                        }
                    }

                    is ImportProgress.Failed -> {
                        _uiState.update {
                            it.copy(
                                currentStep = ImportStep.RESULT,
                                error = progress.message,
                                isLoading = false
                            )
                        }
                    }

                    is ImportProgress.Cancelled -> {
                        _uiState.update {
                            it.copy(
                                currentStep = ImportStep.ENTRY,
                                isLoading = false
                            )
                        }
                    }

                    else -> {
                        // Processing states - just update progress
                    }
                }
            }
        }
    }

    /**
     * Handle file selection with raw bytes from file picker.
     * Parses the file and starts the import process.
     */
    fun onSelectFileWithBytes(filename: String, bytes: ByteArray) {
        _uiState.update {
            it.copy(
                filename = filename,
                currentStep = ImportStep.PROCESSING,
                isLoading = true,
                error = null
            )
        }

        safeScope.launch {
            val parseResult = fileProcessor.process(filename, bytes)
            onFileSelected(filename, parseResult)
        }
    }

    /**
     * Handle file selection from the file picker.
     */
    fun onFileSelected(filename: String, parseResult: ImportResult) {
        _uiState.update {
            it.copy(
                filename = filename,
                currentStep = ImportStep.PROCESSING,
                isLoading = true,
                error = null
            )
        }

        safeScope.launch {
            val accountId = _uiState.value.selectedAccountId
                ?: _accounts.value.firstOrNull()?.id
                ?: "default"

            val result = importTransactionsUseCase.startImport(
                parseResult = parseResult,
                targetAccountId = accountId
            )

            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        currentStep = ImportStep.RESULT,
                        error = error.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Toggle transaction selection.
     */
    fun onTransactionToggle(index: Int, selected: Boolean) {
        _uiState.update { state ->
            val newIndices = if (selected) {
                state.selectedIndices + index
            } else {
                state.selectedIndices - index
            }
            state.copy(selectedIndices = newIndices)
        }
    }

    /**
     * Select all transactions.
     */
    fun onSelectAll() {
        _uiState.update { state ->
            val allIndices = state.previewResult?.transactions
                ?.indices?.toSet() ?: emptySet()
            state.copy(selectedIndices = allIndices)
        }
    }

    /**
     * Deselect all transactions.
     */
    fun onDeselectAll() {
        _uiState.update { it.copy(selectedIndices = emptySet()) }
    }

    /**
     * Deselect duplicate transactions only.
     */
    fun onDeselectDuplicates() {
        _uiState.update { state ->
            val duplicateIndices = state.previewResult?.transactions
                ?.filter { it.duplicateStatus !is DuplicateStatus.Unique }
                ?.map { it.index }
                ?.toSet() ?: emptySet()
            state.copy(selectedIndices = state.selectedIndices - duplicateIndices)
        }
    }

    /**
     * Select account for import.
     */
    fun onAccountSelect(accountId: String) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    /**
     * Override category for a transaction.
     */
    fun onCategoryOverride(index: Int, categoryId: String) {
        _uiState.update { state ->
            val newOverrides = state.categoryOverrides + (index to categoryId)
            state.copy(categoryOverrides = newOverrides)
        }
    }

    /**
     * Continue from REVIEW to CONFIRM step.
     */
    fun onContinueToConfirm() {
        _uiState.update { it.copy(currentStep = ImportStep.CONFIRM) }
    }

    /**
     * Confirm and execute the import.
     */
    fun onConfirmImport() {
        val state = _uiState.value
        val accountId = state.selectedAccountId ?: return

        _uiState.update { it.copy(isLoading = true) }

        safeScope.launch {
            val result = importTransactionsUseCase.confirmImport(
                selectedIndices = state.selectedIndices,
                categoryOverrides = state.categoryOverrides,
                targetAccountId = accountId
            )

            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        error = error.message,
                        isLoading = false
                    )
                }
                _sideEffects.emit(ImportSideEffect.ShowError(error.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Cancel the import.
     */
    fun onCancel() {
        importTransactionsUseCase.cancelImport()
    }

    /**
     * Reset all state and return to entry.
     */
    fun onReset() {
        importTransactionsUseCase.reset()
        _uiState.value = ImportUiState()
    }

    /**
     * Navigate to transactions list.
     */
    fun onNavigateToTransactions() {
        safeScope.launch {
            _sideEffects.emit(ImportSideEffect.NavigateToTransactions)
        }
    }

    /**
     * Navigate back.
     */
    fun onNavigateBack() {
        safeScope.launch {
            when (_uiState.value.currentStep) {
                ImportStep.ENTRY -> _sideEffects.emit(ImportSideEffect.NavigateBack)
                ImportStep.PROCESSING -> onCancel()
                ImportStep.REVIEW -> {
                    onCancel()
                    _uiState.update { it.copy(currentStep = ImportStep.ENTRY) }
                }
                ImportStep.CONFIRM -> _uiState.update { it.copy(currentStep = ImportStep.REVIEW) }
                ImportStep.RESULT -> onReset()
            }
        }
    }

    /**
     * Retry failed import.
     */
    fun onRetry() {
        onReset()
        safeScope.launch {
            _sideEffects.emit(ImportSideEffect.ShowFilePicker)
        }
    }
}

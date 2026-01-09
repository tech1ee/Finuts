package com.finuts.app.feature.transactions

import androidx.lifecycle.viewModelScope
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.app.presentation.base.SnackbarMessageType
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.repository.CategoryRepository
import com.finuts.domain.repository.TransactionRepository
import com.finuts.domain.usecase.LearnFromCorrectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.math.abs

/**
 * ViewModel for Add/Edit Transaction screen.
 * Handles form state, validation, save operations, and AI learning from corrections.
 */
class AddEditTransactionViewModel(
    private val transactionId: String?,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val learnFromCorrectionUseCase: LearnFromCorrectionUseCase? = null
) : BaseViewModel() {

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val isEditMode: Boolean = transactionId != null

    val accounts: StateFlow<List<Account>> = accountRepository.getActiveAccounts()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (transactionId != null) loadExistingTransaction()
    }

    private fun loadExistingTransaction() {
        safeScope.launch {
            val transaction = transactionRepository.getTransactionById(transactionId!!).first()
            if (transaction != null) {
                _formState.update {
                    TransactionFormState(
                        type = transaction.type,
                        amount = formatAmountForDisplay(transaction.amount),
                        accountId = transaction.accountId,
                        categoryId = transaction.categoryId,
                        originalCategoryId = transaction.categoryId, // Track for learning
                        merchant = transaction.merchant ?: "",
                        description = transaction.description ?: "",
                        note = transaction.note ?: "",
                        date = transaction.date
                    )
                }
            }
        }
    }

    private fun formatAmountForDisplay(amountCents: Long): String {
        val absolute = abs(amountCents)
        return (absolute / 100.0).toString()
    }

    fun onAmountChange(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _formState.update { it.copy(amount = amount, amountError = null) }
        }
    }

    fun onTypeChange(type: TransactionType) = _formState.update { it.copy(type = type) }

    fun onAccountChange(accountId: String) = _formState.update {
        it.copy(accountId = accountId, accountError = null)
    }

    fun onCategoryChange(categoryId: String?) = _formState.update { it.copy(categoryId = categoryId) }
    fun onMerchantChange(merchant: String) = _formState.update { it.copy(merchant = merchant) }
    fun onDescriptionChange(description: String) = _formState.update { it.copy(description = description) }
    fun onNoteChange(note: String) = _formState.update { it.copy(note = note) }
    fun onDateChange(date: Instant) = _formState.update { it.copy(date = date) }

    fun save() {
        val state = _formState.value
        var hasError = false

        if (state.amount.isBlank() || state.amount.toDoubleOrNull() == null) {
            _formState.update { it.copy(amountError = "Enter valid amount") }
            hasError = true
        }

        if (state.accountId.isBlank()) {
            _formState.update { it.copy(accountError = "Select account") }
            hasError = true
        }

        if (hasError) return

        _isSaving.value = true
        launchSafe(
            onError = { _isSaving.value = false; sendError(it.message ?: "Save failed") }
        ) {
            val now = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
            val amountCents = ((state.amount.toDoubleOrNull() ?: 0.0) * 100).toLong()
            val signedAmount = if (state.type == TransactionType.EXPENSE) -amountCents else amountCents

            val txId = transactionId ?: generateId()
            val transaction = Transaction(
                id = txId,
                accountId = state.accountId,
                amount = signedAmount,
                type = state.type,
                categoryId = state.categoryId,
                description = state.description.ifBlank { null },
                merchant = state.merchant.ifBlank { null },
                note = state.note.ifBlank { null },
                date = state.date,
                createdAt = now,
                updatedAt = now
            )

            if (isEditMode) {
                transactionRepository.updateTransaction(transaction)
                // Learn from category correction if changed
                learnFromCategoryCorrection(txId, state)
            } else {
                transactionRepository.createTransaction(transaction)
            }

            _isSaving.value = false
            navigateBack()
        }
    }

    /**
     * Learn from user's category correction.
     * Called when user changes category in edit mode.
     * Shows feedback to user about learning progress.
     */
    private suspend fun learnFromCategoryCorrection(
        transactionId: String,
        state: TransactionFormState
    ) {
        // Only learn if: category changed AND we have merchant info
        val categoryChanged = state.originalCategoryId != state.categoryId
        val hasMerchant = state.merchant.isNotBlank() || state.description.isNotBlank()

        if (!categoryChanged || !hasMerchant || state.categoryId == null) return

        val merchantName = state.merchant.ifBlank { state.description }
        val result = learnFromCorrectionUseCase?.execute(
            transactionId = transactionId,
            originalCategoryId = state.originalCategoryId,
            correctedCategoryId = state.categoryId,
            merchantName = merchantName
        )

        // Show feedback based on learning result
        result?.onSuccess { learnResult ->
            when (learnResult) {
                is LearnFromCorrectionUseCase.LearnResult.MappingCreated -> {
                    sendSnackbar(
                        "✓ Запомнили категорию для $merchantName",
                        SnackbarMessageType.SUCCESS
                    )
                }
                is LearnFromCorrectionUseCase.LearnResult.MappingUpdated -> {
                    val confidence = (learnResult.newConfidence * 100).toInt()
                    sendSnackbar(
                        "✓ Улучшили точность для $merchantName ($confidence%)",
                        SnackbarMessageType.SUCCESS
                    )
                }
                is LearnFromCorrectionUseCase.LearnResult.CorrectionSaved -> {
                    sendSnackbar(
                        "Ещё 1 исправление для запоминания",
                        SnackbarMessageType.INFO
                    )
                }
            }
        }
    }

    fun onBackClick() = navigateBack()

    private fun generateId() = "tx_${kotlin.random.Random.nextLong().toString(16)}"
}

data class TransactionFormState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val accountId: String = "",
    val categoryId: String? = null,
    /** Original category when editing - used to detect corrections for AI learning */
    val originalCategoryId: String? = null,
    val merchant: String = "",
    val description: String = "",
    val note: String = "",
    val date: Instant = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds()),
    val amountError: String? = null,
    val accountError: String? = null
)

package com.finuts.app.feature.transactions

import androidx.lifecycle.viewModelScope
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.repository.CategoryRepository
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.math.abs

/**
 * ViewModel for Add/Edit Transaction screen.
 * Handles form state, validation, and save operations.
 */
class AddEditTransactionViewModel(
    private val transactionId: String?,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : BaseViewModel() {

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val isEditMode: Boolean = transactionId != null

    val accounts: StateFlow<List<Account>> = accountRepository.getActiveAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (transactionId != null) loadExistingTransaction()
    }

    private fun loadExistingTransaction() {
        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(transactionId!!).first()
            if (transaction != null) {
                _formState.update {
                    TransactionFormState(
                        type = transaction.type,
                        amount = formatAmountForDisplay(transaction.amount),
                        accountId = transaction.accountId,
                        categoryId = transaction.categoryId,
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

            val transaction = Transaction(
                id = transactionId ?: generateId(),
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
            } else {
                transactionRepository.createTransaction(transaction)
            }

            _isSaving.value = false
            navigateBack()
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
    val merchant: String = "",
    val description: String = "",
    val note: String = "",
    val date: Instant = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds()),
    val amountError: String? = null,
    val accountError: String? = null
)

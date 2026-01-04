package com.finuts.app.feature.transactions

import androidx.lifecycle.viewModelScope
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant

/**
 * ViewModel for Quick Add Transaction sheet.
 * Minimal form for fast transaction entry.
 */
class QuickAddViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : BaseViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getActiveAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _formState = MutableStateFlow(QuickAddFormState())
    val formState: StateFlow<QuickAddFormState> = _formState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    fun onTypeChange(type: TransactionType) = _formState.update { it.copy(type = type) }
    fun onAccountChange(accountId: String) = _formState.update { it.copy(accountId = accountId) }
    fun onCategoryChange(categoryId: String) = _formState.update { it.copy(categoryId = categoryId) }
    fun onNoteChange(note: String) = _formState.update { it.copy(note = note) }

    fun onAmountChange(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _formState.update { it.copy(amount = amount, amountError = null) }
        }
    }

    fun save() {
        val state = _formState.value

        if (state.accountId.isBlank()) {
            _formState.update { it.copy(amountError = "Select an account") }
            return
        }

        val amountValue = state.amount.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            _formState.update { it.copy(amountError = "Enter valid amount") }
            return
        }

        _isSaving.value = true
        launchSafe(
            onError = { _isSaving.value = false; sendError(it.message ?: "Save failed") }
        ) {
            val now = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
            val amountCents = (amountValue * 100).toLong()
            val signedAmount = if (state.type == TransactionType.EXPENSE) -amountCents else amountCents

            val transaction = Transaction(
                id = generateId(),
                accountId = state.accountId,
                amount = signedAmount,
                type = state.type,
                categoryId = state.categoryId.ifBlank { null },
                description = null,
                merchant = null,
                note = state.note.ifBlank { null },
                date = now,
                isRecurring = false,
                recurringRuleId = null,
                attachments = emptyList(),
                tags = emptyList(),
                createdAt = now,
                updatedAt = now
            )

            transactionRepository.createTransaction(transaction)
            _isSaving.value = false
            _saveSuccess.value = true
        }
    }

    fun resetForm() {
        _formState.value = QuickAddFormState()
        _saveSuccess.value = false
    }

    private fun generateId() = "tx_${kotlin.random.Random.nextLong().toString(16)}"

    companion object {
        val QUICK_CATEGORIES = listOf(
            "food" to "Food",
            "transport" to "Transport",
            "shopping" to "Shopping",
            "entertainment" to "Entertainment",
            "utilities" to "Utilities"
        )
    }
}

data class QuickAddFormState(
    val type: TransactionType = TransactionType.EXPENSE,
    val accountId: String = "",
    val amount: String = "",
    val categoryId: String = "",
    val note: String = "",
    val amountError: String? = null
)

package com.finuts.app.feature.transfers

import androidx.lifecycle.viewModelScope
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Account
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.usecase.CreateTransferUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

data class AddTransferUiState(
    val accounts: List<Account> = emptyList(),
    val fromAccount: Account? = null,
    val toAccount: Account? = null,
    val amount: String = "",
    val note: String = "",
    val date: Instant = Instant.fromEpochMilliseconds(
        kotlin.time.Clock.System.now().toEpochMilliseconds()
    ),
    val isLoading: Boolean = false
) {
    val isValid: Boolean
        get() = fromAccount != null &&
                toAccount != null &&
                fromAccount.id != toAccount.id &&
                amount.isNotBlank() &&
                (amount.toDoubleOrNull() ?: 0.0) > 0
}

sealed interface AddTransferEvent {
    data object Success : AddTransferEvent
    data class Error(val message: String) : AddTransferEvent
}

class AddTransferViewModel(
    private val accountRepository: AccountRepository,
    private val createTransferUseCase: CreateTransferUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AddTransferUiState())
    val uiState: StateFlow<AddTransferUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddTransferEvent>()
    val events: SharedFlow<AddTransferEvent> = _events.asSharedFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        safeScope.launch {
            accountRepository.getActiveAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }

    fun onFromAccountSelected(account: Account) {
        _uiState.update { it.copy(fromAccount = account) }
    }

    fun onToAccountSelected(account: Account) {
        _uiState.update { it.copy(toAccount = account) }
    }

    fun onAmountChanged(amount: String) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun onNoteChanged(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onDateChanged(date: Instant) {
        _uiState.update { it.copy(date = date) }
    }

    fun submitTransfer() {
        val state = _uiState.value

        if (!state.isValid) {
            safeScope.launch {
                _events.emit(AddTransferEvent.Error("Please fill all required fields"))
            }
            return
        }

        safeScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = createTransferUseCase.execute(
                fromAccountId = state.fromAccount!!.id,
                toAccountId = state.toAccount!!.id,
                amount = ((state.amount.toDoubleOrNull() ?: 0.0) * 100).toLong(),
                date = state.date,
                note = state.note.takeIf { it.isNotBlank() }
            )

            _uiState.update { it.copy(isLoading = false) }

            result.fold(
                onSuccess = { _events.emit(AddTransferEvent.Success) },
                onFailure = { _events.emit(AddTransferEvent.Error(it.message ?: "Transfer failed")) }
            )
        }
    }
}

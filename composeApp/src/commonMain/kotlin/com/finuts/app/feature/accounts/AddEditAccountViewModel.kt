package com.finuts.app.feature.accounts

import androidx.lifecycle.viewModelScope
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.AccountType
import com.finuts.domain.entity.Currency
import com.finuts.domain.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

/**
 * ViewModel for Add/Edit Account screen.
 * Handles form state, validation, and save operations.
 */
class AddEditAccountViewModel(
    private val accountId: String?,
    private val accountRepository: AccountRepository
) : BaseViewModel() {

    private val _formState = MutableStateFlow(AccountFormState())
    val formState: StateFlow<AccountFormState> = _formState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val isEditMode: Boolean = accountId != null

    init {
        if (accountId != null) loadExistingAccount()
    }

    private fun loadExistingAccount() {
        safeScope.launch {
            val account = accountRepository.getAccountById(accountId!!).first()
            if (account != null) {
                _formState.update {
                    AccountFormState(
                        name = account.name,
                        type = account.type,
                        currencyCode = account.currency.code,
                        balance = (account.balance / 100.0).toString(),
                        icon = account.icon ?: "",
                        color = account.color ?: ""
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) = _formState.update { it.copy(name = name, nameError = null) }
    fun onTypeChange(type: AccountType) = _formState.update { it.copy(type = type) }
    fun onCurrencyChange(code: String) = _formState.update { it.copy(currencyCode = code) }
    fun onBalanceChange(balance: String) {
        if (balance.isEmpty() || balance.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _formState.update { it.copy(balance = balance, balanceError = null) }
        }
    }
    fun onIconChange(icon: String) = _formState.update { it.copy(icon = icon) }

    fun save() {
        val state = _formState.value
        var hasError = false

        if (state.name.isBlank()) {
            _formState.update { it.copy(nameError = "Name is required") }
            hasError = true
        }

        val balanceValue = state.balance.toDoubleOrNull()
        if (balanceValue == null) {
            _formState.update { it.copy(balanceError = "Invalid balance") }
            hasError = true
        }

        if (hasError) return

        _isSaving.value = true
        launchSafe(
            onError = { _isSaving.value = false; sendError(it.message ?: "Save failed") }
        ) {
            val now = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
            val currency = SUPPORTED_CURRENCIES.find { it.code == state.currencyCode } ?: SUPPORTED_CURRENCIES.first()
            val account = Account(
                id = accountId ?: generateId(),
                name = state.name.trim(),
                type = state.type,
                currency = currency,
                balance = ((balanceValue ?: 0.0) * 100).toLong(),
                icon = state.icon.ifBlank { null },
                color = state.color.ifBlank { null },
                isArchived = false,
                createdAt = now,
                updatedAt = now
            )

            if (isEditMode) accountRepository.updateAccount(account)
            else accountRepository.createAccount(account)

            _isSaving.value = false
            navigateBack()
        }
    }

    fun onBackClick() = navigateBack()

    private fun generateId() = "acc_${kotlin.random.Random.nextLong().toString(16)}"

    companion object {
        val SUPPORTED_CURRENCIES = listOf(
            Currency("KZT", "₸", "Kazakhstani Tenge"),
            Currency("USD", "$", "US Dollar"),
            Currency("EUR", "€", "Euro"),
            Currency("RUB", "₽", "Russian Ruble")
        )

        val ACCOUNT_TYPES = AccountType.entries.toList()
    }
}

data class AccountFormState(
    val name: String = "",
    val type: AccountType = AccountType.BANK_ACCOUNT,
    val currencyCode: String = "KZT",
    val balance: String = "0",
    val icon: String = "",
    val color: String = "",
    val nameError: String? = null,
    val balanceError: String? = null
)

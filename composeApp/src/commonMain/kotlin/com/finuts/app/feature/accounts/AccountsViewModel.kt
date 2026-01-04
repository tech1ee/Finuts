package com.finuts.app.feature.accounts

import androidx.lifecycle.viewModelScope
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Account
import com.finuts.domain.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Accounts list screen.
 * Handles account list display, swipe-to-delete/archive, and navigation.
 */
class AccountsViewModel(
    private val accountRepository: AccountRepository
) : BaseViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val uiState: StateFlow<AccountsUiState> = accountRepository.getAllAccounts()
        .map { accounts ->
            val active = accounts.filter { !it.isArchived }
            val archived = accounts.filter { it.isArchived }
            AccountsUiState.Success(
                activeAccounts = active,
                archivedAccounts = archived,
                totalBalance = active.sumOf { it.balance }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AccountsUiState.Loading
        )

    fun onAccountClick(accountId: String) {
        navigateTo(Route.AccountDetail(accountId))
    }

    fun onAddAccountClick() {
        navigateTo(Route.AddAccount())
    }

    fun onEditAccountClick(accountId: String) {
        navigateTo(Route.EditAccount(accountId))
    }

    fun onArchiveAccount(accountId: String) {
        launchSafe {
            accountRepository.archiveAccount(accountId)
        }
    }

    fun onDeleteAccount(accountId: String) {
        launchSafe {
            accountRepository.deleteAccount(accountId)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(300)
            _isRefreshing.value = false
        }
    }
}

sealed interface AccountsUiState {
    data object Loading : AccountsUiState

    data class Success(
        val activeAccounts: List<Account>,
        val archivedAccounts: List<Account>,
        val totalBalance: Long
    ) : AccountsUiState {
        val isEmpty: Boolean get() = activeAccounts.isEmpty()
        val hasArchived: Boolean get() = archivedAccounts.isNotEmpty()
    }

    data class Error(val message: String) : AccountsUiState
}

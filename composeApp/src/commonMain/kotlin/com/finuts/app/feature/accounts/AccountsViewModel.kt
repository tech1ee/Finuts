package com.finuts.app.feature.accounts

import androidx.lifecycle.viewModelScope
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Account
import com.finuts.domain.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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

    // Track pending archive for undo functionality
    private var pendingArchiveId: String? = null
    private val _pendingArchiveIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<AccountsUiState> = kotlinx.coroutines.flow.combine(
        accountRepository.getAllAccounts(),
        _pendingArchiveIds
    ) { accounts, pendingIds ->
        val active = accounts.filter { !it.isArchived && it.id !in pendingIds }
        val archived = accounts.filter { it.isArchived }
        AccountsUiState.Success(
            activeAccounts = active,
            archivedAccounts = archived,
            totalBalance = active.sumOf { it.balance }
        ) as AccountsUiState
    }
        .catch { e -> emit(AccountsUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AccountsUiState.Loading
        )

    fun onAccountClick(accountId: String) {
        navigateTo(Route.AccountDetail(accountId))
    }

    fun onAddAccountClick() {
        navigateTo(Route.AddAccount)
    }

    fun onEditAccountClick(accountId: String) {
        navigateTo(Route.EditAccount(accountId))
    }

    fun onArchiveAccount(accountId: String) {
        launchSafe {
            accountRepository.archiveAccount(accountId)
        }
    }

    /**
     * Soft archive - temporarily hides account from UI for undo.
     * Does not actually persist to database until commitArchive is called.
     */
    fun softArchiveAccount(accountId: String) {
        pendingArchiveId = accountId
        _pendingArchiveIds.value = _pendingArchiveIds.value + accountId
    }

    /**
     * Restore account after soft archive (user pressed Undo).
     */
    fun restoreAccount(accountId: String) {
        if (pendingArchiveId == accountId) {
            pendingArchiveId = null
        }
        _pendingArchiveIds.value = _pendingArchiveIds.value - accountId
    }

    /**
     * Commit the archive to database (snackbar timeout or dismissed).
     */
    fun commitArchive(accountId: String) {
        if (accountId in _pendingArchiveIds.value) {
            launchSafe {
                accountRepository.archiveAccount(accountId)
                _pendingArchiveIds.value = _pendingArchiveIds.value - accountId
                if (pendingArchiveId == accountId) {
                    pendingArchiveId = null
                }
            }
        }
    }

    fun onDeleteAccount(accountId: String) {
        launchSafe {
            accountRepository.deleteAccount(accountId)
        }
    }

    fun refresh() {
        safeScope.launch {
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

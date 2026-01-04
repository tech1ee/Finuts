package com.finuts.app.feature.accounts

import androidx.lifecycle.viewModelScope
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.Transaction
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for Account Detail screen.
 * Shows account info and transaction history for this account.
 */
class AccountDetailViewModel(
    private val accountId: String,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : BaseViewModel() {

    val uiState: StateFlow<AccountDetailUiState> = combine(
        accountRepository.getAccountById(accountId),
        transactionRepository.getTransactionsByAccount(accountId)
    ) { account, transactions ->
        if (account == null) {
            AccountDetailUiState.Error("Account not found")
        } else {
            AccountDetailUiState.Success(
                account = account,
                transactions = transactions.sortedByDescending { it.date }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountDetailUiState.Loading
    )

    fun onEditClick() {
        navigateTo(Route.EditAccount(accountId))
    }

    fun onTransactionClick(transactionId: String) {
        navigateTo(Route.TransactionDetail(transactionId))
    }

    fun onAddTransactionClick() {
        navigateTo(Route.AddTransaction)
    }

    fun onArchiveClick() {
        launchSafe {
            accountRepository.archiveAccount(accountId)
            navigateBack()
        }
    }

    fun onDeleteClick() {
        launchSafe {
            accountRepository.deleteAccount(accountId)
            navigateBack()
        }
    }

    fun onBackClick() {
        navigateBack()
    }
}

sealed interface AccountDetailUiState {
    data object Loading : AccountDetailUiState

    data class Success(
        val account: Account,
        val transactions: List<Transaction>
    ) : AccountDetailUiState {
        val hasTransactions: Boolean get() = transactions.isNotEmpty()
    }

    data class Error(val message: String) : AccountDetailUiState
}

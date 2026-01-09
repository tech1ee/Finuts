package com.finuts.app.feature.transactions

import androidx.lifecycle.viewModelScope
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Transaction
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.repository.CategoryRepository
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for Transaction detail screen.
 * Shows transaction info with account name and currency.
 */
class TransactionDetailViewModel(
    private val transactionId: String,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : BaseViewModel() {

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    val uiState: StateFlow<TransactionDetailUiState> = combine(
        transactionRepository.getTransactionById(transactionId),
        accountRepository.getAllAccounts(),
        categoryRepository.getAllCategories()
    ) { transaction, accounts, categories ->
        if (transaction == null) {
            TransactionDetailUiState.Error("Transaction not found")
        } else {
            val account = accounts.find { it.id == transaction.accountId }
            val category = transaction.categoryId?.let { catId ->
                categories.find { it.id == catId }
            }
            TransactionDetailUiState.Success(
                transaction = transaction,
                accountName = account?.name ?: "Unknown",
                currencySymbol = account?.currency?.symbol ?: "₸",
                categoryName = category?.name
            )
        }
    }.catch { e ->
        emit(TransactionDetailUiState.Error(e.message ?: "Unknown error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionDetailUiState.Loading
    )

    fun onBackClick() {
        navigateBack()
    }

    fun onEditClick() {
        navigateTo(Route.EditTransaction(transactionId))
    }

    fun onDeleteClick() {
        _showDeleteDialog.value = true
    }

    fun onDismissDeleteDialog() {
        _showDeleteDialog.value = false
    }

    fun onConfirmDelete() {
        launchSafe {
            transactionRepository.deleteTransaction(transactionId)
            navigateBack()
        }
    }
}

sealed interface TransactionDetailUiState {
    data object Loading : TransactionDetailUiState

    data class Success(
        val transaction: Transaction,
        val accountName: String,
        val currencySymbol: String,
        val categoryName: String?
    ) : TransactionDetailUiState

    data class Error(val message: String) : TransactionDetailUiState
}

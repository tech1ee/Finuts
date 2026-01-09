package com.finuts.app.feature.budgets

import androidx.lifecycle.viewModelScope
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Budget
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.BudgetRepository
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for Budget detail screen.
 * Shows budget info with related transactions and progress.
 */
class BudgetDetailViewModel(
    private val budgetId: String,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) : BaseViewModel() {

    val uiState: StateFlow<BudgetDetailUiState> = combine(
        budgetRepository.getBudgetById(budgetId),
        transactionRepository.getAllTransactions()
    ) { budget, allTransactions ->
        if (budget == null) {
            BudgetDetailUiState.Error("Budget not found")
        } else {
            val categoryTransactions = budget.categoryId?.let { catId ->
                allTransactions.filter {
                    it.categoryId == catId && it.type == TransactionType.EXPENSE
                }
            } ?: emptyList()

            val spent = categoryTransactions.sumOf { it.amount }

            BudgetDetailUiState.Success(
                budget = budget,
                transactions = categoryTransactions,
                spent = spent
            )
        }
    }.catch { e ->
        emit(BudgetDetailUiState.Error(e.message ?: "Unknown error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetDetailUiState.Loading
    )

    fun onEditClick() {
        navigateTo(Route.EditBudget(budgetId))
    }

    fun onTransactionClick(transactionId: String) {
        navigateTo(Route.TransactionDetail(transactionId))
    }

    fun onBackClick() {
        navigateBack()
    }

    fun onDeleteClick() {
        launchSafe {
            budgetRepository.deleteBudget(budgetId)
            navigateBack()
        }
    }

    fun onDeactivateClick() {
        launchSafe {
            budgetRepository.deactivateBudget(budgetId)
            navigateBack()
        }
    }
}

sealed interface BudgetDetailUiState {
    data object Loading : BudgetDetailUiState

    data class Success(
        val budget: Budget,
        val transactions: List<Transaction>,
        val spent: Long
    ) : BudgetDetailUiState {
        val remaining: Long get() = budget.amount - spent
        val percentUsed: Float get() = if (budget.amount == 0L) 0f
            else (spent.toFloat() / budget.amount.toFloat()) * 100f
        val hasTransactions: Boolean get() = transactions.isNotEmpty()
    }

    data class Error(val message: String) : BudgetDetailUiState
}

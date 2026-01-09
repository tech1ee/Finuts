package com.finuts.app.feature.budgets

import androidx.lifecycle.viewModelScope
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.BudgetProgress
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.BudgetRepository
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Budgets list screen.
 * Displays active budgets with progress, sorted by spending percentage.
 */
class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) : BaseViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val uiState: StateFlow<BudgetsUiState> = combine(
        budgetRepository.getActiveBudgets(),
        transactionRepository.getAllTransactions()
    ) { budgets, transactions ->
        val expensesByCategory = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        val budgetProgresses = budgets.map { budget ->
            val spent = budget.categoryId?.let { expensesByCategory[it] } ?: 0L
            BudgetProgress(budget = budget, spent = spent)
        }.sortedByDescending { it.percentUsed }

        val result: BudgetsUiState = BudgetsUiState.Success(budgets = budgetProgresses)
        result
    }.catch { e ->
        emit(BudgetsUiState.Error(e.message ?: "Unknown error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetsUiState.Loading
    )

    fun onBudgetClick(budgetId: String) {
        navigateTo(Route.BudgetDetail(budgetId))
    }

    fun onAddBudgetClick() {
        navigateTo(Route.AddBudget)
    }

    fun onDeleteBudget(budgetId: String) {
        launchSafe {
            budgetRepository.deleteBudget(budgetId)
        }
    }

    fun onDeactivateBudget(budgetId: String) {
        launchSafe {
            budgetRepository.deactivateBudget(budgetId)
        }
    }

    fun refresh() {
        safeScope.launch {
            _isRefreshing.value = true
            delay(300)
            _isRefreshing.value = false
        }
    }
}

sealed interface BudgetsUiState {
    data object Loading : BudgetsUiState

    data class Success(
        val budgets: List<BudgetProgress>
    ) : BudgetsUiState {
        val isEmpty: Boolean get() = budgets.isEmpty()
        val totalBudgeted: Long get() = budgets.sumOf { it.budget.amount }
        val totalSpent: Long get() = budgets.sumOf { it.spent }
    }

    data class Error(val message: String) : BudgetsUiState
}

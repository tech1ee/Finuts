package com.finuts.app.feature.budgets

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.finuts.app.feature.budgets.components.BudgetDetailHeader
import com.finuts.app.feature.budgets.components.BudgetDetailTopBar
import com.finuts.app.feature.budgets.components.BudgetErrorState
import com.finuts.app.feature.budgets.components.BudgetLoadingState
import com.finuts.app.feature.budgets.components.BudgetTransactionItem
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.ui.components.budget.getBudgetPeriodLabel
import com.finuts.app.ui.components.feedback.EmptyStateCompact
import com.finuts.app.ui.components.list.SectionHeader
import com.finuts.domain.entity.Budget
import com.finuts.domain.entity.Transaction
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BudgetDetailScreen(
    budgetId: String,
    onNavigateBack: () -> Unit,
    onEditBudget: (String) -> Unit,
    onTransactionClick: (String) -> Unit,
    viewModel: BudgetDetailViewModel = koinViewModel { parametersOf(budgetId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is NavigationEvent.NavigateTo -> handleNavigation(event.route, onEditBudget, onTransactionClick)
                is NavigationEvent.PopBackStack -> onNavigateBack()
            }
        }
    }

    when (val state = uiState) {
        is BudgetDetailUiState.Loading -> BudgetLoadingState()
        is BudgetDetailUiState.Error -> BudgetErrorState(state.message)
        is BudgetDetailUiState.Success -> {
            BudgetDetailContent(
                budget = state.budget,
                transactions = state.transactions,
                spent = state.spent,
                percentUsed = state.percentUsed,
                showMenu = showMenu,
                onShowMenu = { showMenu = true },
                onDismissMenu = { showMenu = false },
                onBackClick = { viewModel.onBackClick() },
                onEditClick = { viewModel.onEditClick() },
                onDeleteClick = { showMenu = false; viewModel.onDeleteClick() },
                onTransactionClick = { viewModel.onTransactionClick(it) }
            )
        }
    }
}

private fun handleNavigation(route: Route, onEdit: (String) -> Unit, onTransaction: (String) -> Unit) {
    when (route) {
        is Route.EditBudget -> onEdit(route.budgetId)
        is Route.TransactionDetail -> onTransaction(route.transactionId)
        else -> Unit
    }
}

@Composable
private fun BudgetDetailContent(
    budget: Budget,
    transactions: List<Transaction>,
    spent: Long,
    percentUsed: Float,
    showMenu: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTransactionClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = FinutsSpacing.lg,
            bottom = FinutsSpacing.bottomNavHeight + FinutsSpacing.lg
        )
    ) {
        item {
            BudgetDetailTopBar(
                title = budget.name,
                showMenu = showMenu,
                onShowMenu = onShowMenu,
                onDismissMenu = onDismissMenu,
                onBackClick = onBackClick,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick
            )
        }

        item { Spacer(Modifier.height(FinutsSpacing.lg)) }

        item {
            BudgetDetailHeader(
                percentUsed = percentUsed,
                spent = spent,
                budget = budget.amount,
                currencySymbol = budget.currency.symbol,
                period = getBudgetPeriodLabel(budget.period)
            )
        }

        item { Spacer(Modifier.height(FinutsSpacing.sectionGap)) }

        item {
            SectionHeader(
                title = "Transactions",
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            )
        }

        item { Spacer(Modifier.height(FinutsSpacing.sectionHeaderGap)) }

        if (transactions.isEmpty()) {
            item {
                EmptyStateCompact(
                    message = "No transactions for this budget yet",
                    actionLabel = null,
                    onAction = {}
                )
            }
        } else {
            itemsIndexed(transactions, key = { _, tx -> tx.id }) { index, tx ->
                BudgetTransactionItem(
                    transaction = tx,
                    onClick = { onTransactionClick(tx.id) },
                    showDivider = index < transactions.lastIndex
                )
            }
        }
    }
}

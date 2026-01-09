package com.finuts.app.feature.dashboard

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.finuts.app.feature.dashboard.states.DashboardEmptyState
import com.finuts.app.feature.dashboard.states.DashboardErrorState
import com.finuts.app.feature.dashboard.states.DashboardLoadingState
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.ui.components.state.AnimatedStateContent
import org.koin.compose.viewmodel.koinViewModel

/**
 * Dashboard (Overview) Screen — Financial STATE
 *
 * Shows current financial state:
 * - Total balance (Hero card)
 * - Accounts overview (Carousel)
 * - Monthly spending progress
 * - Top spending categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAccountClick: (String) -> Unit,
    onAddTransactionClick: () -> Unit,
    onAddAccountClick: () -> Unit,
    onSeeAllAccountsClick: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onCreateBudgetClick: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val emptyStateType by viewModel.emptyStateType.collectAsState()
    val showFirstTransactionPrompt by viewModel.showFirstTransactionPrompt.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is NavigationEvent.NavigateTo -> {
                    when (val route = event.route) {
                        is Route.AccountDetail -> onAccountClick(route.accountId)
                        is Route.AddTransaction -> onAddTransactionClick()
                        is Route.Accounts -> onSeeAllAccountsClick()
                        is Route.Transactions -> onNavigateToHistory()
                        else -> Unit
                    }
                }
                else -> Unit
            }
        }
    }

    AnimatedStateContent(
        targetState = uiState,
        contentKey = { it::class }
    ) { state ->
        when (state) {
            is DashboardUiState.Loading -> {
                DashboardLoadingState()
            }
            is DashboardUiState.Success -> {
                emptyStateType?.let { type ->
                    DashboardEmptyState(
                        emptyStateType = type,
                        onAddAccount = onAddAccountClick,
                        onAddTransaction = { viewModel.onAddTransactionClick() }
                    )
                } ?: PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() }
                    ) {
                        DashboardContent(
                            totalBalance = state.totalBalance,
                            accounts = state.accounts,
                            monthlySpending = state.monthlySpending,
                            monthlyIncome = state.monthlyIncome,
                            monthlyBudget = state.monthlyBudget,
                            categorySpending = state.categorySpending,
                            healthStatus = state.healthStatus,
                            periodLabel = state.periodLabel,
                            showFirstTransactionPrompt = showFirstTransactionPrompt,
                            onAccountClick = { viewModel.onAccountClick(it) },
                            onSeeAllAccounts = { viewModel.onSeeAllAccountsClick() },
                            onAddTransaction = { viewModel.onAddTransactionClick() },
                            onCreateBudget = onCreateBudgetClick,
                            onSend = {},
                            onReceive = {},
                            onHistory = { viewModel.onSeeAllTransactionsClick() }
                        )
                    }
            }
            is DashboardUiState.Error -> {
                DashboardErrorState(message = state.message)
            }
        }
    }
}

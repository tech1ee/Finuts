package com.finuts.app.feature.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.feature.transactions.components.TransactionFilterChips
import com.finuts.app.feature.transactions.components.TransactionItem
import com.finuts.app.feature.transactions.states.TransactionsEmptyState
import com.finuts.app.feature.transactions.states.TransactionsNoAccountsState
import com.finuts.app.feature.transactions.states.TransactionsNoResultsState
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.cards.TransactionSummary
import com.finuts.app.ui.components.cards.TransactionSummaryCard
import com.finuts.app.ui.components.list.SectionHeader
import com.finuts.app.ui.components.navigation.FinutsTopBar
import com.finuts.app.ui.components.navigation.TopBarAction
import com.finuts.app.ui.icons.FinutsIcons
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.add_transaction
import finuts.composeapp.generated.resources.filter
import finuts.composeapp.generated.resources.history
import finuts.composeapp.generated.resources.search
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Transactions History Screen showing all transactions grouped by date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onTransactionClick: (String) -> Unit,
    onAddTransactionClick: () -> Unit,
    onGoToAccounts: () -> Unit = {},
    viewModel: TransactionsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val hasAccounts by viewModel.hasAccounts.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is NavigationEvent.NavigateTo -> when (val route = event.route) {
                    is Route.TransactionDetail -> onTransactionClick(route.transactionId)
                    is Route.AddTransaction -> onAddTransactionClick()
                    else -> Unit
                }
                else -> Unit
            }
        }
    }

    when (val state = uiState) {
        is TransactionsUiState.Loading -> LoadingContent()
        is TransactionsUiState.Error -> ErrorContent(state.message)
        is TransactionsUiState.Success -> {
            if (state.isEmpty && filter == TransactionFilter.ALL) {
                if (!hasAccounts) {
                    TransactionsNoAccountsState(
                        onGoToAccounts = onGoToAccounts,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    TransactionsEmptyState(
                        onAddTransaction = { viewModel.onAddTransactionClick() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    TransactionsContent(
                        groups = state.groupedTransactions,
                        filter = filter,
                        monthlyIncome = state.monthlyIncome,
                        monthlyExpense = state.monthlyExpense,
                        periodLabel = state.periodLabel,
                        onFilterChange = viewModel::onFilterChange,
                        onTransactionClick = { viewModel.onTransactionClick(it) },
                        onAddTransaction = { viewModel.onAddTransactionClick() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionsContent(
    groups: List<TransactionGroup>,
    filter: TransactionFilter,
    monthlyIncome: Long,
    monthlyExpense: Long,
    periodLabel: String,
    onFilterChange: (TransactionFilter) -> Unit,
    onTransactionClick: (String) -> Unit,
    onAddTransaction: () -> Unit
) {
    val summary = remember(monthlyIncome, monthlyExpense, periodLabel) {
        TransactionSummary(income = monthlyIncome, expense = monthlyExpense, periodLabel = periodLabel)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FinutsTopBar(
            title = stringResource(Res.string.history),
            actions = {
                TopBarAction(FinutsIcons.Search, stringResource(Res.string.search)) {}
                TopBarAction(FinutsIcons.Filter, stringResource(Res.string.filter)) {}
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = FinutsSpacing.md,
                bottom = FinutsSpacing.bottomNavHeight + FinutsSpacing.lg
            )
        ) {
            item { TransactionFilterChips(filter, onFilterChange) }
            item { Spacer(Modifier.height(FinutsSpacing.md)) }

            item {
                TransactionSummaryCard(
                    summary = summary,
                    currencySymbol = "₸",
                    modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
                )
            }

            item { Spacer(Modifier.height(FinutsSpacing.lg)) }

            if (groups.isEmpty()) {
                item { TransactionsNoResultsState() }
            } else {
                groups.forEach { group ->
                    item {
                        SectionHeader(
                            title = group.label,
                            modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
                        )
                    }
                    item { Spacer(Modifier.height(FinutsSpacing.sectionHeaderGap)) }

                    itemsIndexed(group.transactions, key = { _, tx -> tx.id }) { index, tx ->
                        TransactionItem(
                            transaction = tx,
                            onClick = { onTransactionClick(tx.id) },
                            showDivider = index < group.transactions.lastIndex
                        )
                    }
                    item { Spacer(Modifier.height(FinutsSpacing.sectionGap)) }
                }
            }

            item { Spacer(Modifier.height(FinutsSpacing.md)) }
            item {
                Text(
                    text = stringResource(Res.string.add_transaction),
                    style = FinutsTypography.labelLarge,
                    color = FinutsColors.Accent,
                    modifier = Modifier
                        .padding(horizontal = FinutsSpacing.screenPadding)
                        .clickable(onClick = onAddTransaction)
                        .padding(vertical = FinutsSpacing.sm)
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun ErrorContent(message: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(message, style = FinutsTypography.bodyLarge, color = FinutsColors.Expense)
    }
}

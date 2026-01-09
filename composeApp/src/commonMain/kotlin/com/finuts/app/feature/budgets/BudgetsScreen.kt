package com.finuts.app.feature.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.feature.budgets.components.SwipeableBudgetItem
import com.finuts.app.feature.budgets.states.BudgetsEmptyState
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.ui.components.state.AnimatedStateContent
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.budget.BudgetSummaryHeader
import com.finuts.app.ui.components.navigation.FinutsTopBar
import com.finuts.domain.entity.BudgetProgress
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.budgets
import finuts.composeapp.generated.resources.add_budget
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBudgetClick: (String) -> Unit,
    onAddBudgetClick: () -> Unit,
    viewModel: BudgetsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is NavigationEvent.NavigateTo -> {
                    when (val route = event.route) {
                        is Route.BudgetDetail -> onBudgetClick(route.budgetId)
                        is Route.AddBudget -> onAddBudgetClick()
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
            is BudgetsUiState.Loading -> LoadingContent()
            is BudgetsUiState.Error -> ErrorContent(state.message)
            is BudgetsUiState.Success -> {
                if (state.isEmpty) {
                    BudgetsEmptyState(
                        onAddBudget = { viewModel.onAddBudgetClick() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        BudgetsList(
                            budgets = state.budgets,
                            totalSpent = state.totalSpent,
                            totalBudgeted = state.totalBudgeted,
                            onBudgetClick = { viewModel.onBudgetClick(it) },
                            onDeactivate = { viewModel.onDeactivateBudget(it) },
                            onAddBudget = { viewModel.onAddBudgetClick() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetsList(
    budgets: List<BudgetProgress>,
    totalSpent: Long,
    totalBudgeted: Long,
    onBudgetClick: (String) -> Unit,
    onDeactivate: (String) -> Unit,
    onAddBudget: () -> Unit
) {
    val currencySymbol = getDefaultCurrencySymbol(budgets)

    Column(modifier = Modifier.fillMaxSize()) {
        FinutsTopBar(title = stringResource(Res.string.budgets))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = FinutsSpacing.md,
                bottom = FinutsSpacing.bottomNavHeight + FinutsSpacing.lg
            )
        ) {
            if (budgets.isNotEmpty()) {
                item {
                    BudgetSummaryHeader(
                        totalSpent = totalSpent,
                        totalBudgeted = totalBudgeted,
                        currencySymbol = currencySymbol,
                        periodLabel = "This Month"
                    )
                }
                item { Spacer(Modifier.height(FinutsSpacing.sectionGap)) }
            }

            items(budgets, key = { it.budget.id }) { budgetProgress ->
                SwipeableBudgetItem(
                    budgetProgress = budgetProgress,
                    onClick = { onBudgetClick(budgetProgress.budget.id) },
                    onDeactivate = { onDeactivate(budgetProgress.budget.id) }
                )
            }

            item { Spacer(Modifier.height(FinutsSpacing.lg)) }
            item {
                Text(
                    text = stringResource(Res.string.add_budget),
                    style = FinutsTypography.labelLarge,
                    color = FinutsColors.Accent,
                    modifier = Modifier
                        .padding(horizontal = FinutsSpacing.screenPadding)
                        .clickable(onClick = onAddBudget)
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
        Text(text = message, style = FinutsTypography.bodyLarge, color = FinutsColors.Expense)
    }
}

// === Testable Helper Functions ===

fun getDefaultCurrencySymbol(budgets: List<BudgetProgress>): String =
    budgets.firstOrNull()?.budget?.currency?.symbol ?: "₸"

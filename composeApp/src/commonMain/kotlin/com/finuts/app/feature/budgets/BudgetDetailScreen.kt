package com.finuts.app.feature.budgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.feature.budgets.components.BudgetDetailHeader
import com.finuts.app.feature.budgets.components.BudgetDetailTopBar
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.budget.getBudgetPeriodLabel
import com.finuts.app.ui.components.feedback.EmptyStateCompact
import com.finuts.app.ui.components.list.SectionHeader
import com.finuts.app.ui.components.list.TransactionListItem
import com.finuts.app.ui.components.list.TransactionType as ListTransactionType
import com.finuts.domain.entity.Budget
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
        is BudgetDetailUiState.Loading -> LoadingContent()
        is BudgetDetailUiState.Error -> ErrorContent(state.message)
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
                TransactionItem(
                    transaction = tx,
                    onClick = { onTransactionClick(tx.id) },
                    showDivider = index < transactions.lastIndex
                )
            }
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction, onClick: () -> Unit, showDivider: Boolean) {
    val listType = when (transaction.type) {
        TransactionType.EXPENSE -> ListTransactionType.EXPENSE
        TransactionType.INCOME -> ListTransactionType.INCOME
        TransactionType.TRANSFER -> ListTransactionType.TRANSFER
    }
    TransactionListItem(
        merchantName = transaction.merchant ?: transaction.description ?: "Transaction",
        category = transaction.categoryId ?: "Uncategorized",
        time = formatTime(transaction.date),
        amount = formatAmount(transaction),
        transactionType = listType,
        onClick = onClick,
        showDivider = showDivider
    )
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

private fun formatTime(instant: kotlinx.datetime.Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}

private fun formatAmount(transaction: Transaction): String {
    val amount = kotlin.math.abs(transaction.amount)
    val whole = amount / 100
    val fraction = amount % 100
    return "₸$whole.${fraction.toString().padStart(2, '0')}"
}

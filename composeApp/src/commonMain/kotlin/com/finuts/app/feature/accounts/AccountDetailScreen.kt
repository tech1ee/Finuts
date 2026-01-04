package com.finuts.app.feature.accounts

import androidx.compose.foundation.clickable
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
import com.finuts.app.feature.accounts.components.AccountDetailHeader
import com.finuts.app.feature.accounts.components.AccountDetailTopBar
import com.finuts.app.feature.accounts.components.AccountTransactionItem
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.feedback.EmptyStateCompact
import com.finuts.app.ui.components.list.SectionHeader
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.Transaction
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Account Detail Screen showing account info and transactions.
 */
@Composable
fun AccountDetailScreen(
    accountId: String,
    onNavigateBack: () -> Unit,
    onEditAccount: (String) -> Unit,
    onTransactionClick: (String) -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: AccountDetailViewModel = koinViewModel { parametersOf(accountId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is NavigationEvent.NavigateTo -> handleNavigation(
                    event.route, onEditAccount, onTransactionClick, onAddTransaction
                )
                is NavigationEvent.PopBackStack -> onNavigateBack()
            }
        }
    }

    when (val state = uiState) {
        is AccountDetailUiState.Loading -> LoadingContent()
        is AccountDetailUiState.Error -> ErrorContent(state.message)
        is AccountDetailUiState.Success -> {
            AccountDetailContent(
                account = state.account,
                transactions = state.transactions,
                showMenu = showMenu,
                onShowMenu = { showMenu = true },
                onDismissMenu = { showMenu = false },
                onBackClick = { viewModel.onBackClick() },
                onEditClick = { viewModel.onEditClick() },
                onArchiveClick = { showMenu = false; viewModel.onArchiveClick() },
                onDeleteClick = { showMenu = false; viewModel.onDeleteClick() },
                onTransactionClick = { viewModel.onTransactionClick(it) },
                onAddTransaction = { viewModel.onAddTransactionClick() }
            )
        }
    }
}

private fun handleNavigation(
    route: Route,
    onEdit: (String) -> Unit,
    onTransaction: (String) -> Unit,
    onAdd: () -> Unit
) {
    when (route) {
        is Route.EditAccount -> onEdit(route.accountId)
        is Route.TransactionDetail -> onTransaction(route.transactionId)
        is Route.AddTransaction -> onAdd()
        else -> Unit
    }
}

@Composable
private fun AccountDetailContent(
    account: Account,
    transactions: List<Transaction>,
    showMenu: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onAddTransaction: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = FinutsSpacing.lg,
            bottom = FinutsSpacing.bottomNavHeight + FinutsSpacing.lg
        )
    ) {
        item {
            AccountDetailTopBar(
                title = account.name,
                showMenu = showMenu,
                onShowMenu = onShowMenu,
                onDismissMenu = onDismissMenu,
                onBackClick = onBackClick,
                onEditClick = onEditClick,
                onArchiveClick = onArchiveClick,
                onDeleteClick = onDeleteClick
            )
        }

        item { Spacer(Modifier.height(FinutsSpacing.lg)) }
        item { AccountDetailHeader(account) }
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
                    message = "No transactions for this account yet",
                    actionLabel = "Add Transaction",
                    onAction = onAddTransaction
                )
            }
        } else {
            itemsIndexed(transactions, key = { _, tx -> tx.id }) { index, tx ->
                AccountTransactionItem(
                    transaction = tx,
                    onClick = { onTransactionClick(tx.id) },
                    showDivider = index < transactions.lastIndex
                )
            }
        }

        item { Spacer(Modifier.height(FinutsSpacing.lg)) }
        item {
            Text(
                text = "+ Add Transaction",
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

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(message, style = FinutsTypography.bodyLarge, color = FinutsColors.Expense)
    }
}

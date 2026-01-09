package com.finuts.app.feature.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.ui.components.state.AnimatedStateContent
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMoneyTypography
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.cards.AccountListItem
import com.finuts.app.ui.components.cards.AccountType
import com.finuts.app.ui.components.feedback.EmptyState
import com.finuts.app.ui.components.navigation.FinutsTopBar
import com.finuts.app.ui.components.snackbar.LocalSnackbarController
import com.finuts.app.ui.components.snackbar.SnackbarDurations
import com.finuts.domain.entity.Account
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.accounts
import finuts.composeapp.generated.resources.total_balance
import finuts.composeapp.generated.resources.active
import finuts.composeapp.generated.resources.archived
import finuts.composeapp.generated.resources.add_account
import finuts.composeapp.generated.resources.account_archived
import finuts.composeapp.generated.resources.archive
import finuts.composeapp.generated.resources.no_accounts
import finuts.composeapp.generated.resources.no_accounts_desc
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onAccountClick: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    viewModel: AccountsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val snackbarController = LocalSnackbarController.current
    val archivedMessage = stringResource(Res.string.account_archived)

    // Handle archive with undo snackbar
    val onArchiveWithUndo: (String) -> Unit = { accountId ->
        viewModel.softArchiveAccount(accountId)
        snackbarController.showUndoSnackbar(
            message = archivedMessage,
            durationMs = SnackbarDurations.ARCHIVE_ACCOUNT,
            onUndo = { viewModel.restoreAccount(accountId) },
            onTimeout = { viewModel.commitArchive(accountId) }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is NavigationEvent.NavigateTo -> {
                    when (val route = event.route) {
                        is Route.AccountDetail -> onAccountClick(route.accountId)
                        is Route.AddAccount -> onAddAccountClick()
                        is Route.EditAccount -> onAccountClick(route.accountId)
                        else -> Unit
                    }
                }
                else -> Unit
            }
        }
    }

    // Simple layout without Scaffold (pill nav overlays)
    AnimatedStateContent(
        targetState = uiState,
        contentKey = { it::class }
    ) { state ->
        when (state) {
            is AccountsUiState.Loading -> LoadingContent(Modifier.fillMaxSize())
            is AccountsUiState.Error -> ErrorContent(state.message, Modifier.fillMaxSize())
            is AccountsUiState.Success -> {
                if (state.isEmpty) {
                    EmptyAccountsContent(
                        onAddAccount = { viewModel.onAddAccountClick() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AccountsList(
                            accounts = state.activeAccounts,
                            archivedAccounts = state.archivedAccounts,
                            totalBalance = state.totalBalance,
                            onAccountClick = { viewModel.onAccountClick(it) },
                            onArchive = onArchiveWithUndo,
                            onAddAccount = { viewModel.onAddAccountClick() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountsList(
    accounts: List<Account>,
    archivedAccounts: List<Account>,
    totalBalance: Long,
    onAccountClick: (String) -> Unit,
    onArchive: (String) -> Unit,
    onAddAccount: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // TopBar
        FinutsTopBar(title = stringResource(Res.string.accounts))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = FinutsSpacing.md,
                bottom = FinutsSpacing.bottomNavHeight + FinutsSpacing.lg
            )
        ) {

        // Total Balance
        item { TotalBalanceHeader(totalBalance, accounts.firstOrNull()?.currency?.symbol ?: "₸") }

        item { Spacer(Modifier.height(FinutsSpacing.sectionGap)) }

        // Section label
        item { SectionLabel(stringResource(Res.string.active)) }

        item { Spacer(Modifier.height(FinutsSpacing.iconToTextGap)) }

        // Account list items
        items(accounts, key = { it.id }) { account ->
            SwipeableAccountItem(
                account = account,
                onClick = { onAccountClick(account.id) },
                onArchive = { onArchive(account.id) }
            )
        }

        // Inline "+ Add Account" button
        item { Spacer(Modifier.height(FinutsSpacing.lg)) }
        item {
            Text(
                text = stringResource(Res.string.add_account),
                style = FinutsTypography.labelLarge,
                color = FinutsColors.Accent,
                modifier = Modifier
                    .padding(horizontal = FinutsSpacing.screenPadding)
                    .clickable(onClick = onAddAccount)
                    .padding(vertical = FinutsSpacing.sm)
            )
        }

        // Archived section (if any)
        if (archivedAccounts.isNotEmpty()) {
            item { Spacer(Modifier.height(FinutsSpacing.sectionGap)) }
            item { SectionLabel(stringResource(Res.string.archived)) }
            item { Spacer(Modifier.height(FinutsSpacing.iconToTextGap)) }
            items(archivedAccounts, key = { it.id }) { account ->
                AccountItem(account, onClick = { onAccountClick(account.id) })
            }
        }
        }
    }
}

@Composable
private fun TotalBalanceHeader(balance: Long, currency: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FinutsSpacing.screenPadding)
    ) {
        Text(
            text = stringResource(Res.string.total_balance),
            style = FinutsTypography.bodyMedium,
            color = FinutsColors.TextTertiary
        )
        Spacer(Modifier.height(FinutsSpacing.xxs))
        Text(
            text = formatMoney(balance, currency),
            style = FinutsMoneyTypography.displayMedium,
            color = FinutsColors.TextPrimary
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = FinutsTypography.labelMedium,
        color = FinutsColors.TextTertiary,
        modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableAccountItem(account: Account, onClick: () -> Unit, onArchive: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onArchive()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground() },
        enableDismissFromStartToEnd = false
    ) {
        AccountItem(account = account, onClick = onClick)
    }
}

@Composable
private fun SwipeBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = FinutsSpacing.screenPadding),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Archive,
            contentDescription = stringResource(Res.string.archive),
            tint = FinutsColors.Expense
        )
    }
}

@Composable
private fun AccountItem(account: Account, onClick: () -> Unit) {
    val accountType = when (account.type.name.uppercase()) {
        "CASH" -> AccountType.CASH
        "DEBIT_CARD" -> AccountType.DEBIT_CARD
        "CREDIT_CARD" -> AccountType.CREDIT_CARD
        "SAVINGS" -> AccountType.SAVINGS
        "INVESTMENT" -> AccountType.INVESTMENT
        else -> AccountType.CASH
    }

    AccountListItem(
        name = account.name,
        type = accountType,
        balance = formatMoney(account.balance, account.currency.symbol),
        onClick = onClick
    )
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun ErrorContent(message: String, modifier: Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        Text(
            text = message,
            style = FinutsTypography.bodyLarge,
            color = FinutsColors.Expense
        )
    }
}

@Composable
private fun EmptyAccountsContent(onAddAccount: () -> Unit, modifier: Modifier) {
    EmptyState(
        title = stringResource(Res.string.no_accounts),
        description = stringResource(Res.string.no_accounts_desc),
        actionLabel = stringResource(Res.string.add_account),
        onAction = onAddAccount,
        modifier = modifier
    )
}

private fun formatMoney(amount: Long, symbol: String): String {
    val whole = amount / 100
    val fraction = kotlin.math.abs(amount % 100)
    val formatted = whole.toString().reversed().chunked(3).joinToString(" ").reversed()
    return "$symbol $formatted.${fraction.toString().padStart(2, '0')}"
}

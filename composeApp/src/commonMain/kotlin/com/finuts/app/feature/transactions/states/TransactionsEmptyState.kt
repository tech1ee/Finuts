package com.finuts.app.feature.transactions.states

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.feedback.EmptyState
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.add_account
import finuts.composeapp.generated.resources.add_transaction
import finuts.composeapp.generated.resources.no_accounts_for_transactions
import finuts.composeapp.generated.resources.no_accounts_for_transactions_desc
import finuts.composeapp.generated.resources.no_filter_results
import finuts.composeapp.generated.resources.no_transactions
import finuts.composeapp.generated.resources.no_transactions_desc
import org.jetbrains.compose.resources.stringResource

/**
 * Empty state when no transactions exist.
 */
@Composable
fun TransactionsEmptyState(
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = stringResource(Res.string.no_transactions),
        description = stringResource(Res.string.no_transactions_desc),
        actionLabel = stringResource(Res.string.add_transaction),
        onAction = onAddTransaction,
        modifier = modifier
    )
}

/**
 * Empty state when user has no accounts yet.
 * Must create account before adding transactions.
 */
@Composable
fun TransactionsNoAccountsState(
    onGoToAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = stringResource(Res.string.no_accounts_for_transactions),
        description = stringResource(Res.string.no_accounts_for_transactions_desc),
        actionLabel = stringResource(Res.string.add_account),
        onAction = onGoToAccounts,
        modifier = modifier
    )
}

/**
 * Empty state when filter returns no results.
 */
@Composable
fun TransactionsNoResultsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(FinutsSpacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Res.string.no_filter_results),
            style = FinutsTypography.bodyMedium,
            color = FinutsColors.TextTertiary
        )
    }
}

package com.finuts.app.feature.dashboard.states

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.ui.components.feedback.EmptyState
import com.finuts.app.ui.components.feedback.EmptyStateType
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.account_ready_title
import finuts.composeapp.generated.resources.add_account
import finuts.composeapp.generated.resources.add_first_account_desc
import finuts.composeapp.generated.resources.add_first_transaction_desc
import finuts.composeapp.generated.resources.add_transaction
import finuts.composeapp.generated.resources.welcome_title
import org.jetbrains.compose.resources.stringResource

/**
 * Contextual empty state for Dashboard based on user's data state.
 * Shows different messaging depending on whether accounts or transactions are missing.
 */
@Composable
fun DashboardEmptyState(
    emptyStateType: EmptyStateType,
    onAddAccount: () -> Unit,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (emptyStateType) {
        is EmptyStateType.DashboardNoAccounts -> EmptyState(
            title = stringResource(Res.string.welcome_title),
            description = stringResource(Res.string.add_first_account_desc),
            actionLabel = stringResource(Res.string.add_account),
            onAction = onAddAccount,
            modifier = modifier
        )
        is EmptyStateType.DashboardNoTransactions -> EmptyState(
            title = stringResource(Res.string.account_ready_title),
            description = stringResource(Res.string.add_first_transaction_desc),
            actionLabel = stringResource(Res.string.add_transaction),
            onAction = onAddTransaction,
            modifier = modifier
        )
        else -> Unit
    }
}

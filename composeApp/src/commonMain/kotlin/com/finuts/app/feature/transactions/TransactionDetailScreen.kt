package com.finuts.app.feature.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.finuts.app.feature.transactions.components.DeleteTransactionDialog
import com.finuts.app.feature.transactions.components.TransactionDetailHeader
import com.finuts.app.feature.transactions.components.TransactionDetailInfo
import com.finuts.app.feature.transactions.components.TransactionDetailTopBar
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.domain.entity.Transaction
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Transaction Detail Screen showing transaction info with edit/delete actions.
 */
@Composable
fun TransactionDetailScreen(
    transactionId: String,
    onNavigateBack: () -> Unit,
    onEditTransaction: (String) -> Unit,
    viewModel: TransactionDetailViewModel = koinViewModel { parametersOf(transactionId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is NavigationEvent.NavigateTo -> {
                    if (event.route is Route.EditTransaction) {
                        onEditTransaction((event.route as Route.EditTransaction).transactionId)
                    }
                }
                is NavigationEvent.PopBackStack -> onNavigateBack()
            }
        }
    }

    when (val state = uiState) {
        is TransactionDetailUiState.Loading -> LoadingContent()
        is TransactionDetailUiState.Error -> ErrorContent(state.message)
        is TransactionDetailUiState.Success -> {
            TransactionDetailContent(
                transaction = state.transaction,
                accountName = state.accountName,
                currencySymbol = state.currencySymbol,
                categoryName = state.categoryName,
                showMenu = showMenu,
                onShowMenu = { showMenu = true },
                onDismissMenu = { showMenu = false },
                onBackClick = viewModel::onBackClick,
                onEditClick = viewModel::onEditClick,
                onDeleteClick = {
                    showMenu = false
                    viewModel.onDeleteClick()
                }
            )
        }
    }

    if (showDeleteDialog) {
        DeleteTransactionDialog(
            onConfirm = viewModel::onConfirmDelete,
            onDismiss = viewModel::onDismissDeleteDialog
        )
    }
}

@Composable
private fun TransactionDetailContent(
    transaction: Transaction,
    accountName: String,
    currencySymbol: String,
    categoryName: String?,
    showMenu: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = FinutsSpacing.lg,
            bottom = FinutsSpacing.bottomNavHeight + FinutsSpacing.lg
        )
    ) {
        item {
            TransactionDetailTopBar(
                title = "Transaction",
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
            TransactionDetailHeader(
                transaction = transaction,
                currencySymbol = currencySymbol
            )
        }

        item { Spacer(Modifier.height(FinutsSpacing.sectionGap)) }

        item {
            TransactionDetailInfo(
                transaction = transaction,
                accountName = accountName,
                categoryName = categoryName
            )
        }

        transaction.note?.let { note ->
            item { Spacer(Modifier.height(FinutsSpacing.sectionGap)) }
            item { NoteSection(note) }
        }
    }
}

@Composable
private fun NoteSection(note: String) {
    Text(
        text = "Note",
        style = FinutsTypography.labelMedium,
        color = FinutsColors.TextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FinutsSpacing.screenPadding)
    )
    Spacer(Modifier.height(FinutsSpacing.xs))
    Text(
        text = note,
        style = FinutsTypography.bodyMedium,
        color = FinutsColors.TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FinutsSpacing.screenPadding)
    )
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

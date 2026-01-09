package com.finuts.app.feature.transactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.finuts.app.feature.transactions.components.TransactionForm
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.presentation.base.SnackbarMessageType
import com.finuts.app.theme.FinutsColors
import com.finuts.app.ui.components.snackbar.LocalSnackbarController
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.form.FormTopBar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Screen for adding or editing a transaction.
 */
@Composable
fun AddEditTransactionScreen(
    transactionId: String?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditTransactionViewModel = koinViewModel { parametersOf(transactionId) }
) {
    val formState by viewModel.formState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    // Get currency symbol from selected account
    val currencySymbol = accounts
        .find { it.id == formState.accountId }
        ?.currency?.symbol ?: "₸"

    // Get snackbar controller for showing feedback
    val snackbarController = LocalSnackbarController.current

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            if (event is NavigationEvent.PopBackStack) onNavigateBack()
        }
    }

    // Handle snackbar events (learning feedback)
    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { event ->
            when (event.type) {
                SnackbarMessageType.SUCCESS -> snackbarController.showSuccess(event.message)
                SnackbarMessageType.ERROR -> snackbarController.showError(event.message)
                SnackbarMessageType.INFO -> snackbarController.showSuccess(event.message)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = FinutsSpacing.lg, bottom = FinutsSpacing.bottomNavHeight + FinutsSpacing.lg)
    ) {
        FormTopBar(
            title = getScreenTitle(viewModel.isEditMode),
            onBackClick = viewModel::onBackClick
        )

        Spacer(Modifier.height(FinutsSpacing.lg))

        TransactionForm(
            formState = formState,
            accounts = accounts,
            categories = categories,
            currencySymbol = currencySymbol,
            onTypeChange = viewModel::onTypeChange,
            onAmountChange = viewModel::onAmountChange,
            onAccountChange = viewModel::onAccountChange,
            onCategoryChange = viewModel::onCategoryChange,
            onMerchantChange = viewModel::onMerchantChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onNoteChange = viewModel::onNoteChange,
            modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
        )

        Spacer(Modifier.height(FinutsSpacing.xl))

        Button(
            onClick = viewModel::save,
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = FinutsColors.Accent,
                contentColor = FinutsColors.OnAccent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinutsSpacing.screenPadding)
                .height(FinutsSpacing.buttonHeight)
        ) {
            Text(
                text = getSaveButtonLabel(isSaving),
                style = FinutsTypography.labelLarge
            )
        }
    }
}

// === Testable Helper Functions ===

fun getScreenTitle(isEditMode: Boolean): String =
    if (isEditMode) "Edit Transaction" else "Add Transaction"

fun getSaveButtonLabel(isSaving: Boolean): String =
    if (isSaving) "Saving..." else "Save"

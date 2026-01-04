package com.finuts.app.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.form.FormDropdown
import com.finuts.app.ui.components.form.FormLabel
import com.finuts.app.ui.components.form.FormTopBar
import com.finuts.app.ui.icons.AccountIcon
import com.finuts.app.ui.icons.getAccountColor
import com.finuts.domain.entity.AccountType
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddEditAccountScreen(
    accountId: String?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditAccountViewModel = koinViewModel { parametersOf(accountId) }
) {
    val formState by viewModel.formState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            if (event is NavigationEvent.PopBackStack) onNavigateBack()
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
            onBackClick = { viewModel.onBackClick() }
        )

        Spacer(Modifier.height(FinutsSpacing.lg))

        AccountForm(
            formState = formState,
            onNameChange = viewModel::onNameChange,
            onTypeChange = viewModel::onTypeChange,
            onCurrencyChange = viewModel::onCurrencyChange,
            onBalanceChange = viewModel::onBalanceChange,
            modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
        )

        Spacer(Modifier.height(FinutsSpacing.xl))

        Button(
            onClick = { viewModel.save() },
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
            Text(text = getSaveButtonLabel(isSaving), style = FinutsTypography.labelLarge)
        }
    }
}

@Composable
private fun AccountForm(
    formState: AccountFormState,
    onNameChange: (String) -> Unit,
    onTypeChange: (AccountType) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Account Name
        FormLabel("Account Name")
        OutlinedTextField(
            value = formState.name,
            onValueChange = onNameChange,
            placeholder = { Text("e.g., Cash, Savings", style = FinutsTypography.bodyMedium) },
            isError = formState.nameError != null,
            supportingText = formState.nameError?.let {
                { Text(it, style = FinutsTypography.bodySmall, color = FinutsColors.Expense) }
            },
            singleLine = true,
            textStyle = FinutsTypography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Account Type
        FormLabel("Account Type")
        FormDropdown(
            selected = formState.type,
            options = AddEditAccountViewModel.ACCOUNT_TYPES,
            onSelect = onTypeChange,
            displayText = { formatAccountType(it) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Currency
        FormLabel("Currency")
        FormDropdown(
            selected = AddEditAccountViewModel.SUPPORTED_CURRENCIES.find { it.code == formState.currencyCode }
                ?: AddEditAccountViewModel.SUPPORTED_CURRENCIES.first(),
            options = AddEditAccountViewModel.SUPPORTED_CURRENCIES,
            onSelect = { onCurrencyChange(it.code) },
            displayText = { "${it.symbol} ${it.code} - ${it.name}" },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Initial Balance
        FormLabel("Initial Balance")
        OutlinedTextField(
            value = formState.balance,
            onValueChange = onBalanceChange,
            placeholder = { Text("0.00", style = FinutsTypography.bodyMedium) },
            isError = formState.balanceError != null,
            supportingText = formState.balanceError?.let {
                { Text(it, style = FinutsTypography.bodySmall, color = FinutsColors.Expense) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = FinutsTypography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Icon Preview
        FormLabel("Icon")
        IconPreview(formState.type)
    }
}

@Composable
private fun IconPreview(accountType: AccountType) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        val iconColor = getAccountColor(accountType)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            AccountIcon(accountType = accountType, size = 24.dp, tint = iconColor)
        }
        Spacer(Modifier.width(FinutsSpacing.md))
        Text(
            text = "Auto-selected based on account type",
            style = FinutsTypography.bodySmall,
            color = FinutsColors.TextTertiary
        )
    }
}

// === Testable Helper Functions ===

fun formatAccountType(type: AccountType): String =
    type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

fun getScreenTitle(isEditMode: Boolean): String = if (isEditMode) "Edit Account" else "Add Account"

fun getSaveButtonLabel(isSaving: Boolean): String = if (isSaving) "Saving..." else "Save"

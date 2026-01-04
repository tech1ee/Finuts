package com.finuts.app.feature.budgets.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.finuts.app.feature.budgets.AddEditBudgetViewModel
import com.finuts.app.feature.budgets.BudgetFormState
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.form.FormDropdown
import com.finuts.app.ui.components.form.FormLabel
import com.finuts.app.ui.components.form.FormNullableDropdown
import com.finuts.domain.entity.BudgetPeriod
import com.finuts.domain.entity.Category

/**
 * Budget form component with all input fields.
 * Uses shared FormDropdown and FormLabel components.
 */
@Composable
fun BudgetForm(
    formState: BudgetFormState,
    categories: List<Category>,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onPeriodChange: (BudgetPeriod) -> Unit,
    onCurrencyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Name
        FormLabel("Budget Name")
        OutlinedTextField(
            value = formState.name,
            onValueChange = onNameChange,
            placeholder = { Text("e.g., Groceries, Entertainment", style = FinutsTypography.bodyMedium) },
            isError = formState.nameError != null,
            supportingText = formState.nameError?.let {
                { Text(it, style = FinutsTypography.bodySmall, color = FinutsColors.Expense) }
            },
            singleLine = true,
            textStyle = FinutsTypography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Amount
        FormLabel("Amount")
        OutlinedTextField(
            value = formState.amount,
            onValueChange = onAmountChange,
            placeholder = { Text("0.00", style = FinutsTypography.bodyMedium) },
            isError = formState.amountError != null,
            supportingText = formState.amountError?.let {
                { Text(it, style = FinutsTypography.bodySmall, color = FinutsColors.Expense) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = FinutsTypography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Period
        FormLabel("Period")
        FormDropdown(
            selected = formState.period,
            options = AddEditBudgetViewModel.BUDGET_PERIODS,
            onSelect = onPeriodChange,
            displayText = { formatPeriodDisplay(it) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Category (optional)
        FormLabel("Category (optional)")
        FormNullableDropdown(
            selected = categories.find { it.id == formState.categoryId },
            options = categories,
            onSelect = { onCategoryChange(it?.id) },
            displayText = { it.name },
            noneText = "None",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Currency
        FormLabel("Currency")
        FormDropdown(
            selected = AddEditBudgetViewModel.SUPPORTED_CURRENCIES.find { it.code == formState.currencyCode }
                ?: AddEditBudgetViewModel.SUPPORTED_CURRENCIES.first(),
            options = AddEditBudgetViewModel.SUPPORTED_CURRENCIES,
            onSelect = { onCurrencyChange(it.code) },
            displayText = { "${it.symbol} ${it.code} - ${it.name}" },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Formats period for display. */
fun formatPeriodDisplay(period: BudgetPeriod): String = when (period) {
    BudgetPeriod.DAILY -> "Daily"
    BudgetPeriod.WEEKLY -> "Weekly"
    BudgetPeriod.MONTHLY -> "Monthly"
    BudgetPeriod.QUARTERLY -> "Quarterly"
    BudgetPeriod.YEARLY -> "Yearly"
}

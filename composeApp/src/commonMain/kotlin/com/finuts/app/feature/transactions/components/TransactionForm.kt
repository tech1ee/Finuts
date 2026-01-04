package com.finuts.app.feature.transactions.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.feature.transactions.TransactionFormState
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.form.FormAmountField
import com.finuts.app.ui.components.form.FormDropdown
import com.finuts.app.ui.components.form.FormLabel
import com.finuts.app.ui.components.form.FormNullableDropdown
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.TransactionType

/**
 * Transaction form with all input fields.
 * Includes: type selector, amount, account, category, merchant, description, note.
 */
@Composable
fun TransactionForm(
    formState: TransactionFormState,
    accounts: List<Account>,
    categories: List<Category>,
    currencySymbol: String,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onAccountChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onMerchantChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Type selector
        TransactionTypeSelector(
            selected = formState.type,
            onSelect = onTypeChange
        )

        Spacer(Modifier.height(FinutsSpacing.lg))

        // Amount
        FormAmountField(
            value = formState.amount,
            onValueChange = onAmountChange,
            currencySymbol = currencySymbol,
            error = formState.amountError
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Account (required)
        FormLabel("Account")
        val selectedAccount = accounts.find { it.id == formState.accountId }
        if (accounts.isNotEmpty()) {
            FormDropdown(
                selected = selectedAccount ?: accounts.first(),
                options = accounts,
                onSelect = { onAccountChange(it.id) },
                displayText = { it.name },
                modifier = Modifier.fillMaxWidth()
            )
        }
        formState.accountError?.let {
            Text(
                text = it,
                style = FinutsTypography.bodySmall,
                color = FinutsColors.Expense
            )
        }

        Spacer(Modifier.height(FinutsSpacing.md))

        // Category (optional)
        FormLabel("Category")
        FormNullableDropdown(
            selected = categories.find { it.id == formState.categoryId },
            options = categories,
            onSelect = { onCategoryChange(it?.id) },
            displayText = { it.name },
            noneText = "No category",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Merchant
        FormLabel("Merchant")
        OutlinedTextField(
            value = formState.merchant,
            onValueChange = onMerchantChange,
            placeholder = { Text("e.g., Starbucks", style = FinutsTypography.bodyMedium) },
            singleLine = true,
            textStyle = FinutsTypography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Description
        FormLabel("Description")
        OutlinedTextField(
            value = formState.description,
            onValueChange = onDescriptionChange,
            placeholder = { Text("What's this for?", style = FinutsTypography.bodyMedium) },
            singleLine = true,
            textStyle = FinutsTypography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        // Note
        FormLabel("Note")
        OutlinedTextField(
            value = formState.note,
            onValueChange = onNoteChange,
            placeholder = { Text("Additional notes...", style = FinutsTypography.bodyMedium) },
            maxLines = 3,
            textStyle = FinutsTypography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

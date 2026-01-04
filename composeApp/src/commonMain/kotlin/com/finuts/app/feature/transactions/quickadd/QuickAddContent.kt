package com.finuts.app.feature.transactions.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.feature.transactions.QuickAddFormState
import com.finuts.app.feature.transactions.QuickAddViewModel
import com.finuts.app.feature.transactions.quickadd.components.QuickAddAccountPicker
import com.finuts.app.feature.transactions.quickadd.components.QuickAddAmountField
import com.finuts.app.feature.transactions.quickadd.components.QuickAddCategoryPicker
import com.finuts.app.feature.transactions.quickadd.components.QuickAddNoteField
import com.finuts.app.feature.transactions.quickadd.components.QuickAddTypeSelector
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.domain.entity.TransactionType

/**
 * Main content layout for quick add transaction sheet.
 */
@Composable
fun QuickAddContent(
    formState: QuickAddFormState,
    accounts: List<Pair<String, String>>,
    isSaving: Boolean,
    onTypeChange: (TransactionType) -> Unit,
    onAccountChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FinutsSpacing.screenPadding)
            .padding(bottom = FinutsSpacing.xl)
    ) {
        Text(
            text = "Add Transaction",
            style = FinutsTypography.headlineMedium,
            color = FinutsColors.TextPrimary
        )

        Spacer(Modifier.height(FinutsSpacing.lg))

        QuickAddTypeSelector(
            selectedType = formState.type,
            onTypeChange = onTypeChange
        )

        Spacer(Modifier.height(FinutsSpacing.lg))

        QuickAddAmountField(
            amount = formState.amount,
            currencySymbol = "₸",
            error = formState.amountError,
            onAmountChange = onAmountChange
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        QuickAddAccountPicker(
            accounts = accounts,
            selectedAccountId = formState.accountId,
            onAccountChange = onAccountChange
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        QuickAddCategoryPicker(
            categories = QuickAddViewModel.QUICK_CATEGORIES,
            selectedCategoryId = formState.categoryId,
            onCategoryChange = onCategoryChange
        )

        Spacer(Modifier.height(FinutsSpacing.md))

        QuickAddNoteField(
            note = formState.note,
            onNoteChange = onNoteChange
        )

        Spacer(Modifier.height(FinutsSpacing.xl))

        QuickAddActionButtons(
            isSaving = isSaving,
            onSave = onSave,
            onCancel = onCancel
        )
    }
}

@Composable
private fun QuickAddActionButtons(
    isSaving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        OutlinedButton(onClick = onCancel) {
            Text("Cancel", style = FinutsTypography.labelLarge)
        }
        Spacer(Modifier.width(FinutsSpacing.sm))
        Button(
            onClick = onSave,
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = FinutsColors.Accent,
                contentColor = FinutsColors.OnAccent
            )
        ) {
            Text(
                text = if (isSaving) "Saving..." else "Add",
                style = FinutsTypography.labelLarge
            )
        }
    }
}

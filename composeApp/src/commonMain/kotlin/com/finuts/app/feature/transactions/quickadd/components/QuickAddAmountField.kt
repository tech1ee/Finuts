package com.finuts.app.feature.transactions.quickadd.components

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
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMoneyTypography
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Amount input field with currency prefix and error handling.
 */
@Composable
fun QuickAddAmountField(
    amount: String,
    currencySymbol: String,
    error: String?,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            text = "Amount",
            style = FinutsTypography.labelMedium,
            color = FinutsColors.TextSecondary
        )
        Spacer(Modifier.height(FinutsSpacing.xs))
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            placeholder = { Text("0.00", style = FinutsTypography.bodyMedium) },
            prefix = { Text("$currencySymbol ", style = FinutsMoneyTypography.medium) },
            textStyle = FinutsMoneyTypography.large,
            isError = error != null,
            supportingText = error?.let {
                { Text(it, style = FinutsTypography.bodySmall, color = FinutsColors.Expense) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

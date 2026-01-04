package com.finuts.app.ui.components.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMoneyTypography

/**
 * Amount input field with currency prefix.
 * Features:
 * - Currency symbol prefix (e.g., ₸)
 * - Decimal keyboard
 * - Validation error display
 * - Tabular figures for proper number alignment
 */
@Composable
fun FormAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    error: String? = null,
    placeholder: String = "0.00"
) {
    Column(modifier = modifier) {
        FormLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            prefix = {
                Text(
                    text = currencySymbol,
                    style = FinutsMoneyTypography.body
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    style = FinutsMoneyTypography.body,
                    color = FinutsColors.TextTertiary
                )
            },
            textStyle = FinutsMoneyTypography.body,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = error != null,
            supportingText = error?.let {
                {
                    Text(
                        text = it,
                        color = FinutsColors.Error
                    )
                }
            }
        )
    }
}

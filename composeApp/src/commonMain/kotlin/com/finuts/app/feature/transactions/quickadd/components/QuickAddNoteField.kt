package com.finuts.app.feature.transactions.quickadd.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Optional note input field for transaction.
 */
@Composable
fun QuickAddNoteField(
    note: String,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            text = "Note (optional)",
            style = FinutsTypography.labelMedium,
            color = FinutsColors.TextSecondary
        )
        Spacer(Modifier.height(FinutsSpacing.xs))
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            placeholder = { Text("Add a note...", style = FinutsTypography.bodyMedium) },
            singleLine = true,
            textStyle = FinutsTypography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

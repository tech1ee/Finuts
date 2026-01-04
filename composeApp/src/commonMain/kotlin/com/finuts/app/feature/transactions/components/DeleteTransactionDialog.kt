package com.finuts.app.feature.transactions.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsTypography

/**
 * Confirmation dialog for deleting a transaction.
 * Uses question format and explicit button labels per UX best practices.
 */
@Composable
fun DeleteTransactionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete transaction?",
                style = FinutsTypography.headlineSmall,
                color = FinutsColors.TextPrimary
            )
        },
        text = {
            Text(
                text = "This action cannot be undone.",
                style = FinutsTypography.bodyMedium,
                color = FinutsColors.TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Delete",
                    style = FinutsTypography.labelLarge,
                    color = FinutsColors.Expense
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = FinutsTypography.labelLarge,
                    color = FinutsColors.TextSecondary
                )
            }
        }
    )
}

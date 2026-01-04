package com.finuts.app.feature.transactions.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Top bar for transaction detail screen.
 * Shows back button, title, edit button, and delete menu.
 */
@Composable
fun TransactionDetailTopBar(
    title: String,
    showMenu: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FinutsSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = FinutsColors.TextPrimary)
        }

        Spacer(Modifier.width(FinutsSpacing.xs))

        Text(
            text = title,
            style = FinutsTypography.headlineMedium,
            color = FinutsColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onEditClick) {
            Icon(Icons.Default.Edit, "Edit", tint = FinutsColors.TextSecondary)
        }

        Box {
            IconButton(onClick = onShowMenu) {
                Icon(Icons.Default.MoreVert, "More options", tint = FinutsColors.TextSecondary)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete",
                            style = FinutsTypography.bodyMedium,
                            color = FinutsColors.Expense
                        )
                    },
                    onClick = onDeleteClick,
                    leadingIcon = {
                        Icon(Icons.Default.Delete, null, tint = FinutsColors.Expense)
                    }
                )
            }
        }
    }
}

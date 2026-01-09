package com.finuts.app.ui.components.import

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing

/**
 * Bulk Selection Bar - Quick actions for selecting/deselecting transactions.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────┐
 * │  Выбрать все   │   Снять дубликаты   │   Снять все  │
 * └─────────────────────────────────────────────────┘
 *
 * Specs:
 * - Height: 48dp (min touch target)
 * - Background: transparent
 * - Buttons: TextButton with appropriate colors
 * - Spacing: 8dp between buttons
 */
@Composable
fun BulkSelectionBar(
    onSelectAll: () -> Unit,
    onDeselectDuplicates: () -> Unit,
    onDeselectAll: () -> Unit,
    hasDuplicates: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = FinutsSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onSelectAll) {
            Text(
                text = "Выбрать все",
                color = FinutsColors.Accent
            )
        }

        if (hasDuplicates) {
            TextButton(onClick = onDeselectDuplicates) {
                Text(
                    text = "Снять дубликаты",
                    color = FinutsColors.Warning
                )
            }
        }

        TextButton(onClick = onDeselectAll) {
            Text(
                text = "Снять все",
                color = FinutsColors.TextSecondary
            )
        }
    }
}

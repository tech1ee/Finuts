package com.finuts.app.ui.components.import

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Duplicate Warning Card - Shows warning about possible duplicates.
 *
 * Layout:
 * ┌───────────────────────────────────────────┐
 * │ ⚠️  12 возможных дубликатов              │  ← Icon + title row
 * │     Снимите галочки с тех, которые       │  ← Description
 * │     не нужно импортировать               │
 * └───────────────────────────────────────────┘
 *
 * Specs:
 * - Background: FinutsColors.Warning.copy(alpha = 0.1f)
 * - Border: 1dp, FinutsColors.Warning.copy(alpha = 0.3f)
 * - Radius: 12dp
 * - Padding: 16dp
 * - Icon: 20dp, FinutsColors.Warning
 * - Title: FinutsTypography.titleSmall, FinutsColors.Warning
 * - Description: FinutsTypography.bodySmall, FinutsColors.TextSecondary
 */
@Composable
fun DuplicateWarningCard(
    duplicateCount: Int,
    modifier: Modifier = Modifier
) {
    if (duplicateCount <= 0) return

    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FinutsColors.Warning.copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                color = FinutsColors.Warning.copy(alpha = 0.3f),
                shape = shape
            )
            .padding(FinutsSpacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = FinutsColors.Warning,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(FinutsSpacing.sm))

            Text(
                text = formatDuplicateCount(duplicateCount),
                style = FinutsTypography.titleSmall,
                color = FinutsColors.Warning
            )
        }

        Text(
            text = "Снимите галочки с тех, которые не нужно импортировать",
            style = FinutsTypography.bodySmall,
            color = FinutsColors.TextSecondary,
            modifier = Modifier.padding(top = FinutsSpacing.xs)
        )
    }
}

/**
 * Format duplicate count with proper Russian plural form.
 */
private fun formatDuplicateCount(count: Int): String {
    return if (count == 1) {
        "$count возможный дубликат"
    } else {
        "$count возможных дубликатов"
    }
}

package com.finuts.app.ui.components.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsTypography

/**
 * CategoryChip — One-tap category display with AI indicator.
 *
 * Design (from memory.md):
 * - Shows sparkle icon (AutoAwesome) when AI-suggested
 * - Accent background tint when AI-suggested
 * - Edit icon for correction affordance
 * - Rounded pill shape (16dp radius)
 *
 * Usage:
 * - Transaction list items
 * - Import review items
 * - Transaction detail screen
 *
 * @param category Category name, or null for placeholder
 * @param isAISuggested True if category was assigned by AI
 * @param onCorrect Callback when user taps to correct category
 * @param modifier Optional modifier
 */
@Composable
fun CategoryChip(
    category: String?,
    isAISuggested: Boolean,
    onCorrect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayText = category ?: "Категория"
    val backgroundColor = if (isAISuggested) {
        FinutsColors.Accent.copy(alpha = 0.1f)
    } else {
        FinutsColors.Surface
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onCorrect)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AI sparkle indicator
        if (isAISuggested) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI suggested",
                modifier = Modifier.size(14.dp),
                tint = FinutsColors.Accent
            )
            Spacer(Modifier.width(4.dp))
        }

        // Category name
        Text(
            text = displayText,
            style = FinutsTypography.bodySmall,
            color = if (category != null) {
                FinutsColors.TextPrimary
            } else {
                FinutsColors.TextTertiary
            }
        )

        Spacer(Modifier.width(4.dp))

        // Edit indicator
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit category",
            modifier = Modifier.size(12.dp),
            tint = FinutsColors.TextTertiary
        )
    }
}

/**
 * Compact version without edit icon — for read-only display.
 */
@Composable
fun CategoryChipReadOnly(
    category: String?,
    isAISuggested: Boolean,
    modifier: Modifier = Modifier
) {
    val displayText = category ?: "Категория"
    val backgroundColor = if (isAISuggested) {
        FinutsColors.Accent.copy(alpha = 0.1f)
    } else {
        FinutsColors.Surface
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isAISuggested) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI suggested",
                modifier = Modifier.size(14.dp),
                tint = FinutsColors.Accent
            )
            Spacer(Modifier.width(4.dp))
        }

        Text(
            text = displayText,
            style = FinutsTypography.bodySmall,
            color = if (category != null) {
                FinutsColors.TextPrimary
            } else {
                FinutsColors.TextTertiary
            }
        )
    }
}

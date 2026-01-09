package com.finuts.app.feature.categories.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finuts.app.feature.dashboard.utils.hexToColor
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing

/**
 * Category Color Palette - 12 preset colors
 *
 * Layout (Tailwind-based colors):
 * ┌─────────────────────────────────────────┐
 * │  ● ● ● ● ● ●                           │
 * │  ● ● ● ● ● ●                           │
 * └─────────────────────────────────────────┘
 *
 * Specs:
 * - 12 preset colors (6 per row)
 * - Circular swatches, 40dp
 * - Selected: ring indicator (2dp border)
 * - Gap: 12dp
 */

val CategoryColors = listOf(
    "#EF4444", // Red
    "#F97316", // Orange
    "#F59E0B", // Amber
    "#EAB308", // Yellow
    "#84CC16", // Lime
    "#22C55E", // Green
    "#14B8A6", // Teal
    "#06B6D4", // Cyan
    "#3B82F6", // Blue
    "#6366F1", // Indigo
    "#8B5CF6", // Violet
    "#EC4899"  // Pink
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryColorPalette(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)
    ) {
        CategoryColors.forEach { colorHex ->
            ColorSwatch(
                colorHex = colorHex,
                isSelected = colorHex.equals(selectedColor, ignoreCase = true),
                onClick = { onColorSelected(colorHex) }
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = hexToColor(colorHex)

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = FinutsColors.TextPrimary,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Inner white ring for selected state
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
            )
        }
    }
}

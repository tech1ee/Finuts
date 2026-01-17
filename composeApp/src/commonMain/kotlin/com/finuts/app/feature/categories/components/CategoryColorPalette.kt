package com.finuts.app.feature.categories.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finuts.app.feature.dashboard.utils.hexToColor
import com.finuts.app.theme.FinutsSpacing
import com.finuts.domain.registry.IconRegistry

/**
 * Category Color Palette - 15 preset colors from IconRegistry
 *
 * Layout:
 * ┌─────────────────────────────────────────┐
 * │  ● ● ● ● ●                              │
 * │  ● ● ● ● ●                              │
 * │  ● ● ● ● ●                              │
 * └─────────────────────────────────────────┘
 *
 * Specs:
 * - 15 preset colors (5 per row)
 * - Circular swatches, 40dp
 * - Selected: white check icon and border
 * - Gap: 12dp
 */

/**
 * Legacy colors for backward compatibility.
 * New code should use IconRegistry.colorPalette.
 */
val CategoryColors: List<String>
    get() = IconRegistry().colorPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryColorPalette(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val iconRegistry = remember { IconRegistry() }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)
    ) {
        iconRegistry.colorPalette.forEach { colorHex ->
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
                        color = Color.White,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

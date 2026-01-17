package com.finuts.app.ui.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.finuts.app.theme.FinutsSpacing
import com.finuts.domain.registry.IconRegistry

/**
 * Color Picker Palette - Grid of preset colors for category customization.
 *
 * Layout:
 * ┌──────────────────────────────────────────┐
 * │  ●   ●   ●   ●   ●                       │
 * │  ●   ●   ●   ●   ●                       │
 * │  ●   ●   ●   ●   ●                       │
 * └──────────────────────────────────────────┘
 *
 * Specs:
 * - 15 preset colors from IconRegistry.colorPalette
 * - 5 columns grid
 * - 36dp circle per color
 * - Selected color has check icon and border
 */
@Composable
fun ColorPickerPalette(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val iconRegistry = remember { IconRegistry() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(FinutsSpacing.md)
    ) {
        items(iconRegistry.colorPalette) { colorHex ->
            ColorPickerItem(
                colorHex = colorHex,
                isSelected = colorHex.equals(selectedColor, ignoreCase = true),
                onClick = { onColorSelected(colorHex) }
            )
        }
    }
}

@Composable
private fun ColorPickerItem(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = remember(colorHex) { parseColor(colorHex) }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                } else Modifier
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

/**
 * Parses hex color string to Compose Color.
 * Supports formats: #RRGGBB, #AARRGGBB
 */
private fun parseColor(colorHex: String): Color {
    return try {
        val hex = colorHex.removePrefix("#")
        val colorLong = when (hex.length) {
            6 -> hex.toLong(16) or 0xFF000000
            8 -> hex.toLong(16)
            else -> 0xFF9E9E9E // Grey fallback
        }
        Color(colorLong.toInt())
    } catch (e: Exception) {
        Color(0xFF9E9E9E) // Grey fallback
    }
}

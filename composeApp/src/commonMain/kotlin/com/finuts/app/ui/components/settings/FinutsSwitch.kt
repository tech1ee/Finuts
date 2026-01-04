package com.finuts.app.ui.components.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMotion
import com.finuts.app.theme.FinutsSpacing

/**
 * Custom Toggle Switch - Finuts Design System
 *
 * Design: iOS-inspired toggle with custom styling
 *
 * Layout:
 * ┌──────────────────────────────────────┐
 * │  ●═══════════════════════════       │  OFF state (gray track)
 * │  ← 28dp thumb                       │
 * │  ← 52dp × 32dp track               │
 * └──────────────────────────────────────┘
 *
 * ┌──────────────────────────────────────┐
 * │       ═══════════════════════════●  │  ON state (accent track)
 * │                         thumb →     │
 * └──────────────────────────────────────┘
 *
 * Calculated Specs (from DESIGN_SYSTEM.md):
 * - Track: 52dp × 32dp
 * - Thumb: 28dp diameter
 * - Corner radius: 16dp (half height — pill shape)
 * - Thumb margin: 2dp from edge
 * - OFF track: Border color
 * - ON track: Accent color
 * - Thumb: White with subtle shadow
 * - Animation: 150ms ease-out
 */
@Composable
fun FinutsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Animated thumb position
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) {
            FinutsSpacing.toggleTrackWidth - FinutsSpacing.toggleThumbSize - FinutsSpacing.toggleThumbMargin
        } else {
            FinutsSpacing.toggleThumbMargin
        },
        animationSpec = FinutsMotion.standardTween(),
        label = "thumbOffset"
    )

    // Track color animation
    val trackColor = if (checked) FinutsColors.Accent else FinutsColors.Border

    // Disabled state alpha
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else FinutsMotion.disabledAlpha,
        animationSpec = FinutsMotion.microTween(),
        label = "switchAlpha"
    )

    Box(
        modifier = modifier
            .size(
                width = FinutsSpacing.toggleTrackWidth,
                height = FinutsSpacing.toggleTrackHeight
            )
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(FinutsSpacing.toggleTrackHeight / 2))
            .background(trackColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Thumb
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(FinutsSpacing.toggleThumbSize)
                .shadow(
                    elevation = 2.dp,
                    shape = CircleShape,
                    clip = false
                )
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

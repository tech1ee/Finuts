package com.finuts.app.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Finuts Shape System
 *
 * Philosophy: Linear-style minimalism
 * - Cards: 8dp max (subtle, professional)
 * - Sheets/dialogs: 12dp max
 * - Avoid excessive rounding (>12dp)
 */
object FinutsShapes {
    // === CORNER RADIUS SCALE (Linear: 3-8px) ===
    val none: Shape = RoundedCornerShape(0.dp)
    val xs: Shape = RoundedCornerShape(4.dp)     // Chips, buttons, small elements
    val sm: Shape = RoundedCornerShape(6.dp)     // Inputs, secondary cards
    val md: Shape = RoundedCornerShape(8.dp)     // Cards (MAX for cards!)
    val lg: Shape = RoundedCornerShape(12.dp)    // Sheets, dialogs
    val xl: Shape = RoundedCornerShape(20.dp)    // Hero cards
    val xxl: Shape = RoundedCornerShape(32.dp)   // Navigation pill
    val full: Shape = CircleShape                 // FAB, avatars, icons

    // === COMPONENT SHAPES ===
    val card = md           // 8dp (reduced from 12dp)
    val cardCompact = xs    // 4dp
    val button = xs         // 4dp
    val input = sm          // 6dp
    val chip = xs           // 4dp
    val fab = full          // Circle
    val bottomSheet = lg    // 12dp top corners
    val dialog = lg         // 12dp (reduced from 24dp)
    val snackbar = sm       // 6dp
    val avatar = full       // Circle
    val categoryIcon = sm   // 6dp
}

/**
 * Corner radius values as Dp for raw value access
 */
object FinutsCornerRadius {
    val none: Dp = 0.dp
    val xs: Dp = 4.dp
    val sm: Dp = 6.dp
    val md: Dp = 8.dp
    val lg: Dp = 12.dp
}

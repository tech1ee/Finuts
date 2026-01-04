package com.finuts.app.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Finuts Elevation System
 *
 * Philosophy: Subtle shadows (Linear style)
 * Dark mode: Use borders instead of shadows
 */
object FinutsElevation {
    // === ELEVATION LEVELS ===
    val none: Dp = 0.dp
    val xs: Dp = 1.dp      // Cards at rest
    val sm: Dp = 2.dp      // Elevated cards, hover state
    val md: Dp = 4.dp      // FAB, dialogs
    val lg: Dp = 8.dp      // Bottom sheets
    val xl: Dp = 16.dp     // Modals, drawers

    // === COMPONENT DEFAULTS ===
    val card = xs          // 1dp
    val cardHover = sm     // 2dp
    val button = none      // Flat buttons
    val fab = md           // 4dp
    val bottomSheet = lg   // 8dp
    val dialog = xl        // 16dp
    val menu = md          // 4dp
    val snackbar = md      // 4dp
    val navPill = lg       // 8dp - floating navigation pill
    val heroCard = none    // 0dp - uses color contrast instead

    // === TONAL ELEVATION (M3) ===
    // Used for surface tint in dark mode
    val tonalNone = 0.dp
    val tonalLow = 1.dp
    val tonalMedium = 3.dp
    val tonalHigh = 6.dp
}

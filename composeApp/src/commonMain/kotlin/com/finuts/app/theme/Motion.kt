package com.finuts.app.theme

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * Finuts Motion System — Calculated Animation Specs
 *
 * Design philosophy: Subtle, purposeful, responsive
 * Principle: Natural deceleration (ease-out) for all UI feedback
 *
 * Hierarchy:
 * - Micro (100ms): Instant feedback — press, toggle
 * - Standard (150ms): UI transitions — button states, switches
 * - Emphasis (200ms): Page transitions, complex animations
 * - MAXIMUM: 300ms (never exceed — feels sluggish)
 *
 * Reference: docs/design-system/DESIGN_SYSTEM.md Section 10
 */
object FinutsMotion {
    // ═══════════════════════════════════════════════════════════════
    // DURATION HIERARCHY (milliseconds)
    // ═══════════════════════════════════════════════════════════════
    const val instant = 0           // No animation
    const val micro = 100           // Press feedback, immediate response
    const val standard = 150        // UI transitions, toggle switches
    const val emphasis = 200        // Page transitions, modals
    const val maximum = 300         // NEVER exceed (complex only)

    // Legacy aliases for compatibility
    const val fast = micro          // 100ms
    const val normal = standard     // 150ms
    const val slow = emphasis       // 200ms

    // ═══════════════════════════════════════════════════════════════
    // EASING FUNCTIONS
    // ═══════════════════════════════════════════════════════════════
    val easeOut = FastOutSlowInEasing      // PRIMARY: Natural deceleration
    val easeIn = FastOutLinearInEasing     // Exit: Accelerate out
    val easeInOut = LinearOutSlowInEasing  // Enter: Decelerate in

    // ═══════════════════════════════════════════════════════════════
    // SCALE TRANSFORMS (Press Feedback)
    // ═══════════════════════════════════════════════════════════════
    const val pressScale = 0.98f           // Default press scale
    const val cardPressScale = 0.98f       // Cards, hero buttons
    const val buttonPressScale = 0.96f     // Buttons, CTAs
    const val listItemPressScale = 0.99f   // List items (subtle)
    const val navItemPressScale = 0.95f    // Navigation items

    // ═══════════════════════════════════════════════════════════════
    // TOGGLE SWITCH
    // ═══════════════════════════════════════════════════════════════
    const val toggleDuration = standard    // 150ms for smooth toggle

    // ═══════════════════════════════════════════════════════════════
    // OPACITY STATES
    // ═══════════════════════════════════════════════════════════════
    const val pressedAlpha = 0.7f          // Pressed state opacity
    const val disabledAlpha = 0.5f         // Disabled state opacity
    const val hoverOverlay = 0.04f         // 4% overlay for hover
    const val pressedOverlay = 0.08f       // 8% overlay for pressed

    // ═══════════════════════════════════════════════════════════════
    // ANIMATION SPEC FACTORIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Micro animation for instant feedback (100ms, ease-out)
     * Use for: press effects, toggles, immediate response
     */
    fun <T> microTween() = tween<T>(
        durationMillis = micro,
        easing = easeOut
    )

    /**
     * Standard animation for UI transitions (150ms, ease-out)
     * Use for: button states, switch animations, state changes
     */
    fun <T> standardTween() = tween<T>(
        durationMillis = standard,
        easing = easeOut
    )

    /**
     * Emphasis animation for page transitions (200ms, ease-in-out)
     * Use for: page navigation, modals, complex animations
     */
    fun <T> emphasisTween() = tween<T>(
        durationMillis = emphasis,
        easing = easeInOut
    )

    // Legacy factories for compatibility
    fun <T> fastTween() = microTween<T>()
    fun <T> defaultTween() = standardTween<T>()
    fun <T> slowTween() = emphasisTween<T>()
}

/**
 * Stagger delays for list animations
 */
object FinutsStagger {
    const val itemDelay = 30       // Delay between list items (ms)
    const val maxItems = 8         // Stop staggering after N items

    /**
     * Calculate stagger delay for item at index
     */
    fun delayForIndex(index: Int): Int {
        return if (index < maxItems) index * itemDelay else maxItems * itemDelay
    }
}

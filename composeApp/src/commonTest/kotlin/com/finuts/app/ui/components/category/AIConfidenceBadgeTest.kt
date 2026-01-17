package com.finuts.app.ui.components.category

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for AIConfidenceBadge UI component.
 *
 * Design rules from memory.md:
 * - Show confidence only when < 85% (high confidence = trust silently)
 * - Text labels: "Вероятно" (>=70%) / "Проверьте" (<70%)
 * - Anti-pattern: Don't show percentage (confusing for users)
 */
class AIConfidenceBadgeTest {

    // --- Visibility Tests ---

    @Test
    fun `badge is hidden when confidence is 85 percent or higher`() {
        val state = AIConfidenceBadgeState(confidence = 0.85f)
        assertFalse(state.isVisible)
    }

    @Test
    fun `badge is hidden when confidence is 90 percent`() {
        val state = AIConfidenceBadgeState(confidence = 0.90f)
        assertFalse(state.isVisible)
    }

    @Test
    fun `badge is hidden when confidence is 100 percent`() {
        val state = AIConfidenceBadgeState(confidence = 1.0f)
        assertFalse(state.isVisible)
    }

    @Test
    fun `badge is visible when confidence is below 85 percent`() {
        val state = AIConfidenceBadgeState(confidence = 0.84f)
        assertTrue(state.isVisible)
    }

    @Test
    fun `badge is visible when confidence is 70 percent`() {
        val state = AIConfidenceBadgeState(confidence = 0.70f)
        assertTrue(state.isVisible)
    }

    @Test
    fun `badge is visible when confidence is 50 percent`() {
        val state = AIConfidenceBadgeState(confidence = 0.50f)
        assertTrue(state.isVisible)
    }

    // --- Label Tests ---

    @Test
    fun `badge shows Veroyatno when confidence is 70 percent or higher`() {
        val state = AIConfidenceBadgeState(confidence = 0.75f)
        assertEquals("Вероятно", state.labelText)
    }

    @Test
    fun `badge shows Veroyatno at exactly 70 percent`() {
        val state = AIConfidenceBadgeState(confidence = 0.70f)
        assertEquals("Вероятно", state.labelText)
    }

    @Test
    fun `badge shows Proverte when confidence is below 70 percent`() {
        val state = AIConfidenceBadgeState(confidence = 0.69f)
        assertEquals("Проверьте", state.labelText)
    }

    @Test
    fun `badge shows Proverte when confidence is 50 percent`() {
        val state = AIConfidenceBadgeState(confidence = 0.50f)
        assertEquals("Проверьте", state.labelText)
    }

    @Test
    fun `badge shows Proverte when confidence is very low`() {
        val state = AIConfidenceBadgeState(confidence = 0.10f)
        assertEquals("Проверьте", state.labelText)
    }

    // --- Color Tests ---

    @Test
    fun `badge uses warning color when confidence is medium 70-84 percent`() {
        val state = AIConfidenceBadgeState(confidence = 0.75f)
        assertEquals(BadgeColorType.WARNING, state.colorType)
    }

    @Test
    fun `badge uses tertiary color when confidence is below 70 percent`() {
        val state = AIConfidenceBadgeState(confidence = 0.50f)
        assertEquals(BadgeColorType.TERTIARY, state.colorType)
    }

    // --- Edge Cases ---

    @Test
    fun `badge handles zero confidence`() {
        val state = AIConfidenceBadgeState(confidence = 0.0f)
        assertTrue(state.isVisible)
        assertEquals("Проверьте", state.labelText)
    }

    @Test
    fun `badge handles negative confidence by clamping`() {
        val state = AIConfidenceBadgeState(confidence = -0.1f)
        assertTrue(state.isVisible)
        assertEquals("Проверьте", state.labelText)
    }

    @Test
    fun `badge handles confidence above 1 by clamping`() {
        val state = AIConfidenceBadgeState(confidence = 1.5f)
        assertFalse(state.isVisible) // Clamped to 1.0 = high confidence
    }

    @Test
    fun `label is null when badge is not visible`() {
        val state = AIConfidenceBadgeState(confidence = 0.90f)
        assertNull(state.labelTextOrNull)
    }
}

/**
 * Color type for badge styling.
 */
enum class BadgeColorType {
    WARNING,  // For medium confidence (70-84%)
    TERTIARY  // For low confidence (<70%)
}

/**
 * State holder for AIConfidenceBadge test assertions.
 */
data class AIConfidenceBadgeState(
    val confidence: Float
) {
    private val clampedConfidence = confidence.coerceIn(0f, 1f)

    val isVisible: Boolean
        get() = clampedConfidence < 0.85f

    val labelText: String
        get() = if (clampedConfidence >= 0.70f) "Вероятно" else "Проверьте"

    val labelTextOrNull: String?
        get() = if (isVisible) labelText else null

    val colorType: BadgeColorType
        get() = if (clampedConfidence >= 0.70f) BadgeColorType.WARNING else BadgeColorType.TERTIARY
}

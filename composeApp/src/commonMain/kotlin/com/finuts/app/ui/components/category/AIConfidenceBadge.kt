package com.finuts.app.ui.components.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsTypography

/**
 * AIConfidenceBadge — Text-based confidence indicator for AI categorization.
 *
 * Design rules (from memory.md):
 * - Show only when confidence < 85% (high confidence = trust silently)
 * - Text labels: "Вероятно" (>=70%) / "Проверьте" (<70%)
 * - Anti-pattern: ❌ Don't show percentage (confusing for users)
 * - Anti-pattern: ❌ Don't show for every transaction (noise)
 *
 * Usage:
 * - Transaction list items (next to category)
 * - Import review screen (next to AI-assigned categories)
 *
 * @param confidence Confidence level from 0.0 to 1.0
 * @param modifier Optional modifier
 */
@Composable
fun AIConfidenceBadge(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val clampedConfidence = confidence.coerceIn(0f, 1f)

    // Don't show for high confidence (>=85%)
    if (clampedConfidence >= 0.85f) return

    val (text, color) = getConfidenceTextAndColor(clampedConfidence)

    Text(
        text = text,
        style = FinutsTypography.labelSmall,
        color = color,
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/**
 * Get display text and color based on confidence level.
 *
 * @param confidence Clamped confidence (0.0-1.0)
 * @return Pair of (label text, color)
 */
private fun getConfidenceTextAndColor(confidence: Float): Pair<String, Color> {
    return if (confidence >= 0.70f) {
        // Medium confidence: amber warning
        "Вероятно" to FinutsColors.Warning
    } else {
        // Low confidence: muted/tertiary
        "Проверьте" to FinutsColors.TextTertiary
    }
}

/**
 * State helper for testing and composing with other components.
 */
object AIConfidenceHelper {
    /**
     * Check if badge should be visible for given confidence.
     */
    fun shouldShowBadge(confidence: Float): Boolean {
        return confidence.coerceIn(0f, 1f) < 0.85f
    }

    /**
     * Get label text for confidence level.
     */
    fun getLabelText(confidence: Float): String? {
        val clamped = confidence.coerceIn(0f, 1f)
        if (clamped >= 0.85f) return null
        return if (clamped >= 0.70f) "Вероятно" else "Проверьте"
    }
}

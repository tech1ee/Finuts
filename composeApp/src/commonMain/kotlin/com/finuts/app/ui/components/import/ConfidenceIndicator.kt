package com.finuts.app.ui.components.import

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsTypography

/**
 * Confidence level for categorization.
 */
enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Get confidence level from float value.
 */
fun getConfidenceLevel(confidence: Float): ConfidenceLevel {
    return when {
        confidence >= 0.8f -> ConfidenceLevel.HIGH
        confidence >= 0.5f -> ConfidenceLevel.MEDIUM
        else -> ConfidenceLevel.LOW
    }
}

/**
 * Confidence Indicator - Shows AI categorization confidence.
 *
 * Visual:
 * High (>0.8):    ████████████  95%   ← Green
 * Medium (0.5-0.8): ████████░░░░  62%   ← Amber
 * Low (<0.5):     ████░░░░░░░░  35%   ← Red/muted
 *
 * Specs:
 * - Width: 48dp
 * - Height: 4dp
 * - Radius: 2dp
 * - Background: FinutsColors.ProgressBackground
 * - Fill colors by threshold:
 *   - >0.8: FinutsColors.ProgressOnTrack (green)
 *   - 0.5-0.8: FinutsColors.ProgressBehind (amber)
 *   - <0.5: FinutsColors.ProgressOverdue (red)
 */
@Composable
fun ConfidenceIndicator(
    confidence: Float,
    showPercentage: Boolean = true,
    modifier: Modifier = Modifier
) {
    val clampedConfidence = confidence.coerceIn(0f, 1f)
    val level = getConfidenceLevel(clampedConfidence)
    val percentage = (clampedConfidence * 100).toInt()

    val fillColor: Color = when (level) {
        ConfidenceLevel.HIGH -> FinutsColors.ProgressOnTrack
        ConfidenceLevel.MEDIUM -> FinutsColors.ProgressBehind
        ConfidenceLevel.LOW -> FinutsColors.ProgressOverdue
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Progress bar
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(FinutsColors.ProgressBackground)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp * clampedConfidence)
                    .clip(RoundedCornerShape(2.dp))
                    .background(fillColor)
            )
        }

        // Percentage text
        if (showPercentage) {
            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "$percentage%",
                style = FinutsTypography.labelSmall,
                color = FinutsColors.TextTertiary
            )
        }
    }
}

/**
 * Compact confidence indicator - just the bar, no text.
 */
@Composable
fun ConfidenceBar(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    ConfidenceIndicator(
        confidence = confidence,
        showPercentage = false,
        modifier = modifier
    )
}

package com.finuts.app.ui.components.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing

/**
 * Budget Progress Bar - Linear progress indicator with status colors.
 *
 * Design specs:
 * - Height: 8dp (FinutsSpacing.progressHeight)
 * - Radius: 4dp (FinutsSpacing.progressRadius)
 * - Colors: Green (0-79%), Amber (80-99%), Red (100%+)
 * - Animation: 200ms tween (design system: <300ms)
 */

private val ProgressShape = RoundedCornerShape(FinutsSpacing.progressRadius)
private const val ANIMATION_DURATION_MS = 200

@Composable
fun BudgetProgressBar(
    percentUsed: Float,
    modifier: Modifier = Modifier
) {
    val progressFraction = clampProgress(percentUsed / 100f)
    val progressColor = getBudgetProgressColor(percentUsed)

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
        label = "budgetProgressAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FinutsSpacing.progressHeight)
            .clip(ProgressShape)
            .background(FinutsColors.ProgressBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(FinutsSpacing.progressHeight)
                .clip(ProgressShape)
                .background(progressColor)
        )
    }
}

/**
 * Clamps progress fraction to valid range [0, 1].
 */
fun clampProgress(fraction: Float): Float = fraction.coerceIn(0f, 1f)

/**
 * Returns the appropriate color based on budget usage percentage.
 *
 * @param percentUsed Percentage of budget used (0-100+)
 * @return Green for on-track (0-79%), Amber for warning (80-99%), Red for over (100%+)
 */
fun getBudgetProgressColor(percentUsed: Float): Color = when {
    percentUsed < 80f -> FinutsColors.ProgressOnTrack
    percentUsed < 100f -> FinutsColors.ProgressBehind
    else -> FinutsColors.ProgressOverdue
}

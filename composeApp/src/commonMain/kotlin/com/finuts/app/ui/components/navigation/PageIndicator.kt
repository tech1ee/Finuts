package com.finuts.app.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing

/**
 * Page indicator with animated dots for pagination.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    ● ○ ○ ○ ○                                │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Specs:
 * - Active dot: 8dp, accent color
 * - Inactive dot: 6dp, muted color
 * - Gap between dots: 8dp
 * - Smooth animations for size and color
 */
@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    val validPageCount = maxOf(1, pageCount)
    val validCurrentPage = currentPage.coerceIn(0, validPageCount - 1)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(validPageCount) { index ->
            IndicatorDot(
                isActive = index == validCurrentPage
            )
        }
    }
}

@Composable
private fun IndicatorDot(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val size by animateDpAsState(
        targetValue = if (isActive) 8.dp else 6.dp,
        animationSpec = tween(durationMillis = 200),
        label = "dot_size"
    )

    val color by animateColorAsState(
        targetValue = if (isActive) FinutsColors.Accent else FinutsColors.TextTertiary,
        animationSpec = tween(durationMillis = 200),
        label = "dot_color"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

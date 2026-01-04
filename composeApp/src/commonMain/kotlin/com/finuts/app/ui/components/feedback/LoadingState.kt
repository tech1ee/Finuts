package com.finuts.app.ui.components.feedback

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsShapes
import com.finuts.app.theme.FinutsSpacing

/**
 * Loading State - Skeleton shimmer loading
 *
 * Provides reusable shimmer effect and skeleton components
 * for loading states throughout the app.
 *
 * Specs:
 * - Skeleton Color: SurfaceVariant (#F5F5F5)
 * - Animation: Shimmer 1000ms loop
 * - Corner Radius: 8dp (card)
 */

@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        FinutsColors.SurfaceVariant.copy(alpha = 0.6f),
        FinutsColors.SurfaceVariant.copy(alpha = 0.2f),
        FinutsColors.SurfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation, y = translateAnimation)
    )
}

/**
 * Skeleton Box - Basic skeleton placeholder
 */
@Composable
fun SkeletonBox(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(FinutsShapes.card)
            .background(shimmerBrush())
    )
}

/**
 * Skeleton Circle - Circular skeleton placeholder
 */
@Composable
fun SkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(shimmerBrush())
    )
}

/**
 * Skeleton Line - Text line placeholder
 */
@Composable
fun SkeletonLine(
    width: Dp,
    height: Dp = FinutsSpacing.sm,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(FinutsShapes.xs)
            .background(shimmerBrush())
    )
}

/**
 * Transaction Skeleton - Loading placeholder for transaction list item
 */
@Composable
fun TransactionSkeleton(
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FinutsSpacing.listItemHeight)
                .padding(
                    horizontal = FinutsSpacing.md,
                    vertical = FinutsSpacing.sm
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonCircle(size = FinutsSpacing.iconLarge)

            Spacer(modifier = Modifier.width(FinutsSpacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                SkeletonLine(width = FinutsSpacing.xxxl)
                Spacer(modifier = Modifier.height(FinutsSpacing.xs))
                SkeletonLine(width = FinutsSpacing.xxl)
            }

            Spacer(modifier = Modifier.width(FinutsSpacing.sm))

            SkeletonLine(width = FinutsSpacing.xl)
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(
                    start = FinutsSpacing.md + FinutsSpacing.iconLarge + FinutsSpacing.sm
                ),
                thickness = FinutsSpacing.none,
                color = FinutsColors.BorderSubtleLight
            )
        }
    }
}

/**
 * Account Card Skeleton - Loading placeholder for account card
 */
@Composable
fun AccountCardSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(FinutsSpacing.accountCardWidth)
            .height(FinutsSpacing.accountCardHeight)
            .clip(FinutsShapes.card)
            .background(shimmerBrush())
            .padding(FinutsSpacing.cardPaddingCompact)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonCircle(size = FinutsSpacing.iconMedium)
                Spacer(modifier = Modifier.width(FinutsSpacing.xs))
                Column {
                    SkeletonLine(width = FinutsSpacing.xl)
                    Spacer(modifier = Modifier.height(FinutsSpacing.xs))
                    SkeletonLine(width = FinutsSpacing.lg)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            SkeletonLine(width = FinutsSpacing.xxl)
        }
    }
}

/**
 * Hero Card Skeleton - Loading placeholder for balance hero card
 */
@Composable
fun HeroCardSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FinutsSpacing.heroHeight)
            .clip(FinutsShapes.xl)
            .background(Color(0xFF2C2C2E))
            .padding(FinutsSpacing.heroCardPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SkeletonLine(width = FinutsSpacing.xl, height = FinutsSpacing.sm)
            Spacer(modifier = Modifier.height(FinutsSpacing.sm))
            SkeletonLine(width = FinutsSpacing.xxxl, height = FinutsSpacing.lg)
        }
    }
}

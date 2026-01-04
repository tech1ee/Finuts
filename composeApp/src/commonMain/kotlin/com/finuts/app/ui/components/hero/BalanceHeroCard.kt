package com.finuts.app.ui.components.hero

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMoneyTypography
import com.finuts.app.theme.FinutsMotion
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Balance Hero Card - Premium dark balance display
 *
 * Design: Sophisticated minimalism (Linear/Copilot Money style)
 *
 * Layout:
 * ┌─────────────────────────────────────────────────┐
 * │                                                  │
 * │  Total Balance                                   │  ← bodySmall, tertiary
 * │  ₸1,780,000.00                                  │  ← displayLarge, white
 * │                                                  │
 * │  ┌─────────┐ ┌─────────┐ ┌─────────┐           │
 * │  │ + Add   │ │ ↑ Send  │ │ ↓ Receive│           │  ← Pill buttons, glass
 * │  └─────────┘ └─────────┘ └─────────┘           │
 * │                                                  │
 * └─────────────────────────────────────────────────┘
 *
 * Specs:
 * - Background: Gradient #0A0A0A → #141414
 * - Border: 1px white 6% opacity
 * - Corner radius: 24dp
 * - Padding: 24dp
 * - Action buttons: Pill shape, glass effect (8% white)
 */

private val CardShape = RoundedCornerShape(FinutsSpacing.heroCornerRadius)
private val ButtonShape = RoundedCornerShape(FinutsSpacing.heroActionButtonRadius)

@Composable
fun BalanceHeroCard(
    balance: String,
    balanceLabel: String = "Total Balance",
    onAddClick: () -> Unit = {},
    onSendClick: () -> Unit = {},
    onReceiveClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        FinutsColors.HeroGradientStart,
                        FinutsColors.HeroGradientEnd
                    )
                )
            )
            .border(
                width = 1.dp,
                color = FinutsColors.GlassBorder,
                shape = CardShape
            )
            .padding(FinutsSpacing.heroCardPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Balance Label - left aligned, subtle
            Text(
                text = balanceLabel,
                style = FinutsTypography.bodySmall,
                color = FinutsColors.HeroTextSecondary
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.xs))

            // Balance Amount - large, prominent
            Text(
                text = balance,
                style = FinutsMoneyTypography.displayLarge,
                color = FinutsColors.HeroText
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.heroCardPadding))

            // Action Buttons Row (8dp gap between buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.carouselItemGap)
            ) {
                HeroActionButton(
                    icon = Icons.Rounded.Add,
                    label = "Add",
                    onClick = onAddClick,
                    modifier = Modifier.weight(1f)
                )
                HeroActionButton(
                    icon = Icons.Rounded.ArrowUpward,
                    label = "Send",
                    onClick = onSendClick,
                    modifier = Modifier.weight(1f)
                )
                HeroActionButton(
                    icon = Icons.Rounded.ArrowDownward,
                    label = "Receive",
                    onClick = onReceiveClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Glass-effect action button
 *
 * Specs:
 * - Height: 48dp
 * - Background: 8% white (glass effect)
 * - Border radius: 12dp
 * - Icon: 18dp, white
 * - Label: bodyMedium, white
 * - Press: scale 0.98
 */
@Composable
private fun HeroActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) FinutsMotion.cardPressScale else 1f,
        animationSpec = FinutsMotion.microTween(),
        label = "buttonScale"
    )

    Box(
        modifier = modifier
            .height(FinutsSpacing.heroActionButtonHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(ButtonShape)
            .background(
                if (isPressed) FinutsColors.HeroButtonBgHover
                else FinutsColors.HeroButtonBg
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = FinutsColors.HeroText,
                modifier = Modifier.size(FinutsSpacing.heroIconSize)
            )
            Spacer(modifier = Modifier.width(FinutsSpacing.xs))
            Text(
                text = label,
                style = FinutsTypography.labelLarge,
                color = FinutsColors.HeroText
            )
        }
    }
}

// ============================================================
// LEGACY SUPPORT (for existing code compatibility)
// ============================================================

data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

/**
 * Legacy version for backward compatibility
 * Maps old parameters to new implementation
 */
@Composable
fun BalanceHeroCard(
    balance: String,
    balanceLabel: String = "Total Balance",
    onAddTransaction: () -> Unit = {},
    onSend: () -> Unit = {},
    onReceive: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BalanceHeroCard(
        balance = balance,
        balanceLabel = balanceLabel,
        onAddClick = onAddTransaction,
        onSendClick = onSend,
        onReceiveClick = onReceive,
        modifier = modifier
    )
}

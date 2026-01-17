package com.finuts.app.ui.components.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMoneyTypography
import com.finuts.app.theme.FinutsMotion
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.icons.getAccountIcon

private val CardShape = RoundedCornerShape(FinutsSpacing.accountCardRadius)
private val AccentBarShape = RoundedCornerShape(
    topStart = FinutsSpacing.accountCardRadius,
    bottomStart = FinutsSpacing.accountCardRadius
)

/** Ordered accent colors by AccountType ordinal. */
private val AccentColors
    @Composable get() = listOf(
        FinutsColors.Accent,    // CASH
        FinutsColors.Transfer,  // DEBIT_CARD
        FinutsColors.Expense,   // CREDIT_CARD
        FinutsColors.Income,    // SAVINGS
        FinutsColors.Tertiary   // INVESTMENT
    )

/** Get accent color for account type. */
@Composable
private fun getAccentColor(type: AccountType) = AccentColors[accountTypeToAccentColorIndex(type)]

/**
 * Full-width Account Card for vertical lists
 *
 * Specs: 64dp height, 4dp accent bar, 40dp icon, 12dp icon-to-text gap
 */
@Composable
fun AccountListItem(
    name: String,
    type: AccountType,
    balance: String,
    balanceLabel: String = "Available",
    logoContent: @Composable () -> Unit = { DefaultBankLogo(type) },
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) FinutsMotion.listItemPressScale else 1f,
        animationSpec = FinutsMotion.microTween(),
        label = "listItemScale"
    )

    val accentColor = getAccentColor(type)

    Box(
        modifier = modifier
            .height(FinutsSpacing.transactionItemHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CardShape)
            .background(FinutsColors.Surface)
            .border(
                width = 1.dp,
                color = FinutsColors.Border,
                shape = CardShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Accent bar on left (4dp)
        Box(
            modifier = Modifier
                .width(FinutsSpacing.accountAccentBarWidth)
                .fillMaxHeight()
                .clip(AccentBarShape)
                .background(accentColor)
        )

        // Content - horizontal layout for list
        Row(
            modifier = Modifier
                .padding(start = FinutsSpacing.accountAccentBarWidth)
                .padding(FinutsSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo (40dp)
            Box(
                modifier = Modifier
                    .size(FinutsSpacing.transactionIconSize)
                    .clip(RoundedCornerShape(FinutsSpacing.transactionIconRadius))
                    .background(FinutsColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                logoContent()
            }

            Spacer(modifier = Modifier.width(FinutsSpacing.iconToTextGap))

            // Name and type
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = FinutsTypography.titleMedium,
                    color = FinutsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatAccountTypeName(type.name),
                    style = FinutsTypography.bodySmall,
                    color = FinutsColors.TextTertiary
                )
            }

            // Balance on right
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = balance,
                    style = FinutsMoneyTypography.title,
                    color = FinutsColors.TextPrimary
                )
                Text(
                    text = balanceLabel,
                    style = FinutsTypography.bodySmall,
                    color = FinutsColors.TextTertiary
                )
            }
        }
    }
}

@Composable
private fun DefaultBankLogo(type: AccountType) {
    Icon(
        imageVector = getAccountIcon(type),
        contentDescription = type.name,
        modifier = Modifier.size(20.dp),
        tint = FinutsColors.TextSecondary
    )
}

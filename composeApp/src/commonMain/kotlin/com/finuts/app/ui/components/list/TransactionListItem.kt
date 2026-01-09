package com.finuts.app.ui.components.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMoneyTypography
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTheme
import com.finuts.app.theme.FinutsTypography

/**
 * Transaction List Item - Flat design (no Card wrapper)
 *
 * Layout:
 * ┌────────────────────────────────────────────────────────────┐
 * │ 16dp │ [40dp] │ 12dp │ Merchant Name      │    │ -$2,500 │ 16dp │
 * │      │  Logo  │      │ Category • 12:30   │    │         │      │
 * ├──────────────────────────────────────────────────────────────────┤
 *
 * Calculated Specs (from DESIGN_SYSTEM.md):
 * - Height: 64dp (optimized from 72dp)
 * - Icon container: 40dp × 40dp, 10dp radius
 * - Icon to text gap: 12dp
 * - Horizontal padding: 16dp
 * - Vertical padding: 12dp (centered: (64-40)/2 = 12)
 * - Primary text: 16sp Medium
 * - Secondary text: 14sp Regular, tertiary color
 * - Amount: 16sp SemiBold, semantic color
 * - Divider inset: 68dp (16 + 40 + 12)
 */

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

@Composable
fun TransactionListItem(
    merchantName: String,
    category: String,
    time: String,
    amount: String,
    transactionType: TransactionType,
    merchantInitial: Char? = null,
    avatarColor: Color = FinutsColors.Primary,
    isAISuggested: Boolean = false,
    onClick: () -> Unit = {},
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    val semanticColors = FinutsTheme.semanticColors

    val amountColor = when (transactionType) {
        TransactionType.INCOME -> semanticColors.income
        TransactionType.EXPENSE -> semanticColors.expense
        TransactionType.TRANSFER -> semanticColors.transfer
    }

    val amountPrefix = when (transactionType) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> ""
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FinutsSpacing.transactionItemHeight) // 64dp
                .clickable(onClick = onClick)
                .padding(horizontal = FinutsSpacing.screenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Merchant Avatar / Initial (40dp with 10dp radius)
            Box(
                modifier = Modifier
                    .size(FinutsSpacing.transactionIconSize)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (merchantInitial ?: merchantName.firstOrNull() ?: 'T').toString(),
                    style = FinutsTypography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(FinutsSpacing.iconToTextGap)) // 12dp

            // Merchant Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = merchantName,
                    style = FinutsTypography.titleMedium,
                    color = FinutsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(FinutsSpacing.xxs)) // 2dp

                // Secondary line: Category (with AI indicator) • Time
                val categoryText = if (isAISuggested) "✨ $category" else category
                Text(
                    text = "$categoryText • $time",
                    style = FinutsTypography.bodyMedium,
                    color = FinutsColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Amount (right-aligned)
            Text(
                text = "$amountPrefix$amount",
                style = FinutsMoneyTypography.title, // 18sp SemiBold
                color = amountColor
            )
        }

        // Divider with proper inset (68dp)
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = FinutsSpacing.listItemDividerInset),
                color = FinutsColors.BorderSubtle
            )
        }
    }
}

package com.finuts.app.ui.components.import

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMoneyTypography
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Display status for duplicate detection in UI.
 */
enum class TransactionDuplicateDisplayStatus {
    UNIQUE,
    PROBABLE_DUPLICATE,
    EXACT_DUPLICATE;

    val isDuplicate: Boolean
        get() = this != UNIQUE
}

/**
 * Transaction Review Item - Individual transaction row in review list.
 *
 * Normal transaction layout:
 * ┌───────────────────────────────────────────────────────────┐
 * │ ☑ 🍔 Kaspi Gold | Вкусно и точка              -2,500 ₸   │
 * │      15 янв                                               │
 * └───────────────────────────────────────────────────────────┘
 *
 * Duplicate transaction layout (warning state):
 * ┌───────────────────────────────────────────────────────────┐
 * │ ⚠️ ☐ 🍔 Kaspi Gold | Вкусно и точка           -2,500 ₸   │
 * │        14 янв                                             │
 * │        Похож на: транзакцию 15 янв                        │
 * └───────────────────────────────────────────────────────────┘
 *
 * Specs:
 * - Normal row height: 72dp
 * - Duplicate row height: 88dp (extra line for match info)
 * - Checkbox: 24dp
 * - Category icon: 40dp
 * - Duplicate background: FinutsColors.Warning.copy(alpha = 0.08f)
 */
@Composable
fun TransactionReviewItem(
    categoryEmoji: String,
    description: String,
    date: String,
    amount: Long,
    duplicateStatus: TransactionDuplicateDisplayStatus,
    matchInfo: String?,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDuplicate = duplicateStatus.isDuplicate
    val rowHeight = if (isDuplicate && matchInfo != null) 88.dp else 72.dp

    val backgroundColor = if (isDuplicate) {
        FinutsColors.Warning.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(backgroundColor)
            .padding(horizontal = FinutsSpacing.md, vertical = FinutsSpacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        // Warning icon for duplicates
        if (isDuplicate) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Duplicate warning",
                tint = FinutsColors.Warning,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        // Checkbox
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChange,
            colors = CheckboxDefaults.colors(
                checkedColor = FinutsColors.Accent,
                uncheckedColor = FinutsColors.TextTertiary
            )
        )

        Spacer(modifier = Modifier.width(FinutsSpacing.sm))

        // Category emoji
        Text(
            text = categoryEmoji,
            modifier = Modifier.size(40.dp),
            style = FinutsTypography.headlineMedium
        )

        Spacer(modifier = Modifier.width(FinutsSpacing.iconToTextGap))

        // Description and date
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = description,
                style = FinutsTypography.titleMedium,
                color = FinutsColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = date,
                style = FinutsTypography.bodySmall,
                color = FinutsColors.TextTertiary
            )

            // Match info for duplicates
            if (isDuplicate && matchInfo != null) {
                Text(
                    text = matchInfo,
                    style = FinutsTypography.bodySmall,
                    color = FinutsColors.Warning
                )
            }
        }

        // Amount
        Text(
            text = formatAmount(amount),
            style = FinutsMoneyTypography.body,
            color = if (amount >= 0) FinutsColors.Income else FinutsColors.Expense
        )
    }
}

/**
 * Format amount with currency symbol.
 */
private fun formatAmount(amount: Long): String {
    val absAmount = kotlin.math.abs(amount)
    val formatted = formatNumber(absAmount)
    val sign = if (amount >= 0) "+" else "-"
    return "$sign$formatted ₸"
}

/**
 * Format number with thousand separators.
 */
private fun formatNumber(value: Long): String {
    return value.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
}

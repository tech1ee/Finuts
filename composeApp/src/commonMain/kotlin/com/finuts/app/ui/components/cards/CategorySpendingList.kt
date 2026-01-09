package com.finuts.app.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.feedback.EmptyStatePrompt
import com.finuts.app.ui.icons.CategoryIcon
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.add_transaction
import finuts.composeapp.generated.resources.no_spending_data
import finuts.composeapp.generated.resources.no_spending_data_desc
import org.jetbrains.compose.resources.stringResource

/**
 * Category Spending List - Top Spending Categories
 *
 * Architecture v3.0 component for Overview screen (STATE).
 * Shows top spending categories with mini bar charts.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │[ic]│ Food         ████████████  ₸45,000  (36%)             │
 * ├─────────────────────────────────────────────────────────────┤
 * │[ic]│ Shopping     █████████     ₸30,000  (24%)             │
 * ├─────────────────────────────────────────────────────────────┤
 * │[ic]│ Transport    ██████        ₸15,000  (12%)             │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Specs:
 * - Item height: 56dp each
 * - Icon container: 32dp × 32dp
 * - Bar max width: 100dp
 * - Bar height: 4dp
 */

data class CategorySpending(
    val id: String,
    val name: String,
    val icon: String,
    val amount: Long,
    val percentage: Float,
    val color: Color
)

private val IconShape = RoundedCornerShape(8.dp)
private val BarShape = RoundedCornerShape(2.dp)

private val CardShape = RoundedCornerShape(12.dp)

@Composable
fun CategorySpendingList(
    categories: List<CategorySpending>,
    currencySymbol: String = "₸",
    onAddTransaction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        CategorySpendingEmptyState(
            onAddTransaction = onAddTransaction ?: {},
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        categories.forEachIndexed { index, category ->
            CategorySpendingItem(
                category = category,
                currencySymbol = currencySymbol
            )
            if (index < categories.lastIndex) {
                Spacer(modifier = Modifier.height(FinutsSpacing.sm)) // 8dp between items
            }
        }
    }
}

/**
 * Empty state for CategorySpendingList when no spending data.
 */
@Composable
private fun CategorySpendingEmptyState(
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(FinutsColors.Surface)
            .border(
                width = 1.dp,
                color = FinutsColors.BorderSubtle,
                shape = CardShape
            )
    ) {
        EmptyStatePrompt(
            title = stringResource(Res.string.no_spending_data),
            description = stringResource(Res.string.no_spending_data_desc),
            actionLabel = stringResource(Res.string.add_transaction),
            onAction = onAddTransaction
        )
    }
}

@Composable
private fun CategorySpendingItem(
    category: CategorySpending,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FinutsSpacing.categoryItemHeight), // 56dp
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container with CategoryIcon
        CategoryIcon(
            iconKey = category.id,
            size = 18.dp,
            tint = category.color,
            showContainer = true,
            containerSize = 32.dp,
            containerColor = category.color.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.width(FinutsSpacing.iconToTextGap)) // 12dp

        // Content column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Category name
            Text(
                text = category.name,
                style = FinutsTypography.titleMedium,
                color = FinutsColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.xs)) // 4dp

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f) // Max 60% of available width
                    .height(4.dp)
                    .clip(BarShape)
                    .background(FinutsColors.SurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(category.percentage / 100f)
                        .height(4.dp)
                        .clip(BarShape)
                        .background(category.color)
                )
            }
        }

        // Amount and percentage
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatMoney(category.amount, currencySymbol),
                style = FinutsTypography.titleMedium,
                color = FinutsColors.TextPrimary
            )
            Text(
                text = "${category.percentage.toInt()}%",
                style = FinutsTypography.bodySmall,
                color = FinutsColors.TextTertiary
            )
        }
    }
}

private fun formatMoney(amount: Long, currencySymbol: String): String {
    val whole = amount / 100
    val formattedWhole = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$currencySymbol$formattedWhole"
}

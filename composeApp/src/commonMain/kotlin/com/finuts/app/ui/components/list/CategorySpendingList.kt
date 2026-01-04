package com.finuts.app.ui.components.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Category Spending List - Top Spending Categories
 *
 * Architecture v3.0 component for Overview screen (STATE).
 * Shows top 3-5 spending categories with mini bar charts.
 *
 * Layout per item:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ ┌────┐ │ Food          │ flex │ ₸45,000 (36%) │
 * │ │icon│ │ ████████████░░░░░░░░░░░░░░░░░░░░░   │
 * │ └────┘ │                                      │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Specs:
 * - Item height: 56dp
 * - Icon container: 32dp × 32dp, 8dp radius
 * - Bar chart height: 4dp, radius 2dp
 * - Bar max width: 150dp
 */

data class CategorySpending(
    val id: String,
    val name: String,
    val icon: String,
    val amount: Long,
    val percentage: Float,
    val color: Color
)

private val IconContainerShape = RoundedCornerShape(FinutsSpacing.sm) // 8dp
private val BarShape = RoundedCornerShape(2.dp)

@Composable
fun CategorySpendingList(
    categories: List<CategorySpending>,
    currencySymbol: String = "₸",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FinutsSpacing.sm) // 8dp between items
    ) {
        categories.forEach { category ->
            CategorySpendingItem(
                category = category,
                currencySymbol = currencySymbol
            )
        }
    }
}

@Composable
private fun CategorySpendingItem(
    category: CategorySpending,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (category.percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400),
        label = "categoryProgress"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = FinutsSpacing.md), // 16dp
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Container (32dp × 32dp)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(IconContainerShape)
                .background(category.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.icon,
                style = FinutsTypography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.width(FinutsSpacing.iconToTextGap)) // 12dp

        // Content column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Name + Amount row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.name,
                    style = FinutsTypography.titleMedium,
                    color = FinutsColors.TextPrimary
                )
                Text(
                    text = "${formatMoney(category.amount, currencySymbol)} (${category.percentage.toInt()}%)",
                    style = FinutsTypography.bodyMedium,
                    color = FinutsColors.TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Progress bar (4dp height, max 150dp width)
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(4.dp)
                    .clip(BarShape)
                    .background(FinutsColors.SurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(4.dp)
                        .clip(BarShape)
                        .background(category.color)
                )
            }
        }
    }
}

private fun formatMoney(amount: Long, currencySymbol: String): String {
    val whole = amount / 100
    val formattedWhole = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$currencySymbol$formattedWhole"
}

package com.finuts.app.feature.reports.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.ui.utils.formatAsMoney
import com.finuts.domain.entity.CategorySpending
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi

/**
 * Donut chart showing category spending breakdown.
 * Uses KoalaPlot for pie chart rendering with custom colors.
 */
@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun CategoryDonutChart(
    categories: List<CategorySpending>,
    totalExpense: Long,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        EmptyChartPlaceholder(modifier)
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            PieChart(
                values = categories.map { it.amount.toFloat() },
                modifier = Modifier.size(180.dp),
                slice = { index ->
                    DefaultSlice(
                        color = getCategoryColor(index)
                    )
                },
                holeSize = 0.6f,
                label = { _ -> }
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalExpense.formatAsMoney(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Total Expense",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CategoryLegend(
            categories = categories.take(5),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Legend showing category colors and percentages.
 */
@Composable
private fun CategoryLegend(
    categories: List<CategorySpending>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEachIndexed { index, categorySpending ->
            CategoryLegendItem(
                name = categorySpending.category.name,
                amount = categorySpending.amount,
                percentage = categorySpending.percentage,
                color = getCategoryColor(index)
            )
        }
    }
}

/**
 * Single legend item with color indicator, name, and percentage.
 */
@Composable
private fun CategoryLegendItem(
    name: String,
    amount: Long,
    percentage: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = amount.formatAsMoney(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp)
        )
    }
}

/**
 * Placeholder when no category data is available.
 */
@Composable
private fun EmptyChartPlaceholder(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Text(
            text = "No expense data",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Get color for category at given index.
 * Cycles through chart colors if more categories than colors.
 */
private fun getCategoryColor(index: Int): Color {
    val colors = listOf(
        FinutsColors.CategoryFood,
        FinutsColors.CategoryTransport,
        FinutsColors.CategoryShopping,
        FinutsColors.CategoryHealth,
        FinutsColors.CategoryEntertainment,
        FinutsColors.CategoryUtilities,
        FinutsColors.CategoryEducation,
        FinutsColors.CategoryInvestment,
        FinutsColors.CategoryIncome,
        FinutsColors.CategoryOther
    )
    return colors[index % colors.size]
}

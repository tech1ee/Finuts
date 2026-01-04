package com.finuts.app.ui.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing

/**
 * Category icon component with optional colored background container.
 *
 * Displays a Lucide icon based on the category key.
 * Supports both standalone icons and icons with background containers.
 *
 * @param iconKey The category identifier (e.g., "food", "transport", "shopping")
 * @param modifier Modifier for the icon or container
 * @param tint Icon color (default: TextSecondary)
 * @param size Icon size in dp (default: 24dp)
 * @param showContainer Whether to show a rounded background container
 * @param containerSize Size of the background container (default: 32dp)
 * @param containerColor Background color for the container
 */
@Composable
fun CategoryIcon(
    iconKey: String,
    modifier: Modifier = Modifier,
    tint: Color = FinutsColors.TextSecondary,
    size: Dp = 24.dp,
    showContainer: Boolean = false,
    containerSize: Dp = 32.dp,
    containerColor: Color = tint.copy(alpha = 0.15f)
) {
    val icon = getCategoryIcon(iconKey)

    if (showContainer) {
        Box(
            modifier = modifier
                .size(containerSize)
                .clip(RoundedCornerShape(FinutsSpacing.sm))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconKey,
                modifier = Modifier.size(size),
                tint = tint
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = iconKey,
            modifier = modifier.size(size),
            tint = tint
        )
    }
}

/**
 * Maps category key strings to Lucide icons.
 *
 * Supports multiple naming conventions:
 * - Lowercase: "food", "transport"
 * - With underscores: "food_dining", "health_medical"
 * - Database IDs: matches partial strings
 */
fun getCategoryIcon(key: String): ImageVector {
    return when (key.lowercase()) {
        // Food & Dining
        "food", "food_dining", "restaurant", "dining" -> FinutsIcons.Food
        "groceries", "grocery", "supermarket", "market" -> FinutsIcons.Groceries
        "coffee", "cafe", "drinks" -> FinutsIcons.Coffee

        // Transport
        "transport", "transportation", "car", "auto", "vehicle" -> FinutsIcons.Transport
        "travel", "vacation", "trip", "flight" -> FinutsIcons.Travel

        // Shopping
        "shopping", "shop", "retail", "clothes", "clothing" -> FinutsIcons.Shopping

        // Bills & Utilities
        "utilities", "utility", "bills", "bill" -> FinutsIcons.Utilities
        "rent", "housing", "home", "mortgage" -> FinutsIcons.Rent
        "insurance" -> FinutsIcons.Insurance
        "subscriptions", "subscription", "recurring" -> FinutsIcons.Subscriptions

        // Entertainment & Lifestyle
        "entertainment", "fun", "leisure", "movies" -> FinutsIcons.Entertainment
        "fitness", "gym", "sports", "exercise" -> FinutsIcons.Fitness

        // Health & Education
        "health", "medical", "healthcare", "doctor", "pharmacy" -> FinutsIcons.Health
        "education", "school", "learning", "courses", "books" -> FinutsIcons.Education

        // Income & Work
        "salary", "income", "work", "paycheck", "wages" -> FinutsIcons.Salary
        "gift", "gifts", "presents" -> FinutsIcons.Gift

        // Financial
        "investment", "investments", "stocks", "crypto" -> FinutsIcons.Investment
        "savings", "saving" -> FinutsIcons.Savings
        "transfer", "transfers" -> FinutsIcons.Transfer

        // Default
        "other", "misc", "miscellaneous" -> FinutsIcons.Other
        else -> FinutsIcons.Other
    }
}

/**
 * Get the recommended color for a category based on its key.
 * Returns a semantic color that visually represents the category.
 */
fun getCategoryColor(key: String): Color {
    return when (key.lowercase()) {
        "food", "food_dining", "restaurant", "dining", "groceries", "grocery" -> FinutsColors.CategoryFood
        "transport", "transportation", "car", "travel", "vacation" -> FinutsColors.CategoryTransport
        "shopping", "shop", "retail", "clothes" -> FinutsColors.CategoryShopping
        "utilities", "utility", "bills", "rent", "insurance", "subscriptions" -> FinutsColors.CategoryUtilities
        "entertainment", "fun", "leisure", "fitness" -> FinutsColors.CategoryEntertainment
        "health", "medical", "healthcare" -> FinutsColors.CategoryHealth
        "education", "school", "learning" -> FinutsColors.CategoryEducation
        "salary", "income", "work", "gift" -> FinutsColors.Income
        "investment", "savings" -> FinutsColors.CategoryInvestment
        else -> FinutsColors.CategoryOther
    }
}

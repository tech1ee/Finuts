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
 * - IconRegistry keys: "basket", "coffee", "pizza", etc.
 */
fun getCategoryIcon(key: String): ImageVector {
    return when (key.lowercase()) {
        // Food & Dining
        "food", "food_dining", "restaurant", "dining", "tools_kitchen" -> FinutsIcons.Food
        "groceries", "grocery", "supermarket", "market", "basket" -> FinutsIcons.Groceries
        "coffee", "cafe", "drinks", "mug" -> FinutsIcons.Coffee
        "pizza" -> FinutsIcons.Restaurant
        "truck_delivery", "food_delivery" -> FinutsIcons.Food

        // Transport
        "transport", "transportation", "car", "auto", "vehicle" -> FinutsIcons.Transport
        "travel", "vacation", "trip", "flight", "plane" -> FinutsIcons.Travel
        "bus", "gas_station", "taxi" -> FinutsIcons.Transport

        // Shopping
        "shopping", "shop", "retail", "clothes", "clothing", "shopping_cart" -> FinutsIcons.Shopping
        "shirt", "bag" -> FinutsIcons.Shopping
        "device_mobile" -> FinutsIcons.Other

        // Bills & Utilities
        "utilities", "utility", "bills", "bill", "bolt" -> FinutsIcons.Utilities
        "droplet", "wifi", "phone", "flame" -> FinutsIcons.Utilities
        "rent", "housing", "home", "mortgage" -> FinutsIcons.Rent
        "insurance", "shield" -> FinutsIcons.Insurance
        "subscriptions", "subscription", "recurring", "repeat" -> FinutsIcons.Subscriptions

        // Entertainment & Lifestyle
        "entertainment", "fun", "leisure", "movies", "movie" -> FinutsIcons.Entertainment
        "music", "gamepad", "ticket", "device_tv" -> FinutsIcons.Entertainment
        "fitness", "gym", "sports", "exercise", "run", "activity" -> FinutsIcons.Fitness

        // Health & Education
        "health", "medical", "healthcare", "doctor", "pharmacy", "heart" -> FinutsIcons.Health
        "pill", "stethoscope" -> FinutsIcons.Health
        "education", "school", "learning", "courses", "books" -> FinutsIcons.Education
        "book", "certificate", "bulb", "pencil" -> FinutsIcons.Education

        // Housing
        "building", "key", "tool", "sofa" -> FinutsIcons.Rent

        // Personal
        "scissors", "sparkles", "user" -> FinutsIcons.Other

        // Income & Work
        "salary", "income", "work", "paycheck", "wages", "briefcase" -> FinutsIcons.Salary
        "laptop", "freelance" -> FinutsIcons.Salary
        "gift", "gifts", "presents" -> FinutsIcons.Gift

        // Financial
        "investment", "investments", "stocks", "crypto", "trending_up" -> FinutsIcons.Investment
        "savings", "saving", "coin" -> FinutsIcons.Savings
        "transfer", "transfers", "arrows_left_right" -> FinutsIcons.Transfer
        "credit_card", "card" -> FinutsIcons.Card
        "wallet" -> FinutsIcons.Wallet
        "cash" -> FinutsIcons.Cash
        "rotate_ccw" -> FinutsIcons.Transfer
        "plus" -> FinutsIcons.Add

        // Time-related
        "calendar" -> FinutsIcons.Calendar
        "clock" -> FinutsIcons.Clock

        // Other
        "other", "misc", "miscellaneous", "package" -> FinutsIcons.Other
        "tag" -> FinutsIcons.Tag
        "star", "flag", "circle" -> FinutsIcons.Other

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

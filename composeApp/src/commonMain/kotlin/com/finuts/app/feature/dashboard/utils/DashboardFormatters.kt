package com.finuts.app.feature.dashboard.utils

import androidx.compose.ui.graphics.Color
import com.finuts.app.theme.FinutsColors
import com.finuts.app.ui.utils.MoneyFormatter

/**
 * Formatting utilities for Dashboard display values.
 */

/**
 * Format amount in cents to displayable money string.
 * Example: 150000L, "₸" -> "₸ 1 500.00"
 *
 * @deprecated Use MoneyFormatter.formatWithFraction() directly
 */
fun formatMoney(amount: Long, currencySymbol: String): String =
    MoneyFormatter.formatWithFraction(amount, currencySymbol)

/**
 * Format account type enum name to display string.
 * Example: "CREDIT_CARD" -> "Credit card"
 */
fun formatAccountType(type: String): String {
    return type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * Parse hex color string to Compose Color.
 * Supports 6-char (#RRGGBB) and 8-char (#AARRGGBB) formats.
 */
fun hexToColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    return try {
        val colorInt = cleanHex.toLong(16)
        when (cleanHex.length) {
            6 -> Color(0xFF000000 or colorInt)
            8 -> Color(colorInt)
            else -> FinutsColors.CategoryOther
        }
    } catch (e: Exception) {
        FinutsColors.CategoryOther
    }
}

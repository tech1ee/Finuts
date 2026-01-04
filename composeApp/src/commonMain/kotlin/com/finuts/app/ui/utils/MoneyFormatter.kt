package com.finuts.app.ui.utils

import kotlin.math.abs

/**
 * Centralized money formatting utilities.
 * Use these functions instead of duplicating money formatting logic.
 */
object MoneyFormatter {

    /**
     * Format amount in cents to displayable money string with fraction.
     * Example: 150000L, "₸" -> "₸ 1 500.00"
     *
     * @param amountCents Amount in cents (1 dollar = 100 cents)
     * @param currencySymbol Currency symbol to display
     * @param thousandsSeparator Separator for thousands (default: space)
     */
    fun formatWithFraction(
        amountCents: Long,
        currencySymbol: String,
        thousandsSeparator: String = " "
    ): String {
        val whole = amountCents / 100
        val fraction = abs(amountCents % 100)
        val formattedWhole = formatThousands(whole, thousandsSeparator)
        return "$currencySymbol $formattedWhole.${fraction.toString().padStart(2, '0')}"
    }

    /**
     * Format amount in cents to compact money string without fraction.
     * Example: 150000L, "₸" -> "₸1,500"
     *
     * @param amountCents Amount in cents
     * @param currencySymbol Currency symbol to display
     * @param thousandsSeparator Separator for thousands (default: comma)
     */
    fun formatCompact(
        amountCents: Long,
        currencySymbol: String,
        thousandsSeparator: String = ","
    ): String {
        val whole = amountCents / 100
        val formattedWhole = formatThousands(whole, thousandsSeparator)
        return "$currencySymbol$formattedWhole"
    }

    /**
     * Format amount in cents with sign prefix.
     * Example: 150000L, "₸", true -> "+₸1,500"
     * Example: 150000L, "₸", false -> "-₸1,500"
     *
     * @param amountCents Amount in cents (always positive, sign determined by isPositive)
     * @param currencySymbol Currency symbol to display
     * @param isPositive Whether to show + or - prefix
     * @param thousandsSeparator Separator for thousands (default: comma)
     */
    fun formatWithSign(
        amountCents: Long,
        currencySymbol: String,
        isPositive: Boolean,
        thousandsSeparator: String = ","
    ): String {
        val whole = abs(amountCents) / 100
        val formattedWhole = formatThousands(whole, thousandsSeparator)
        val sign = if (isPositive) "+" else "-"
        return "$sign$currencySymbol$formattedWhole"
    }

    private fun formatThousands(value: Long, separator: String): String {
        return value.toString()
            .reversed()
            .chunked(3)
            .joinToString(separator)
            .reversed()
    }
}

/**
 * Extension function for common formatting with space separator and fraction.
 */
fun Long.formatAsMoney(currencySymbol: String): String =
    MoneyFormatter.formatWithFraction(this, currencySymbol)

/**
 * Extension function for compact formatting (no fraction).
 */
fun Long.formatAsCompactMoney(currencySymbol: String): String =
    MoneyFormatter.formatCompact(this, currencySymbol)

/**
 * Extension function for formatting with sign (auto-detect from amount).
 * Positive amounts get +, negative get -.
 */
fun Long.formatWithSign(currencySymbol: String): String =
    MoneyFormatter.formatWithSign(abs(this), currencySymbol, this >= 0)

/**
 * Extension function for formatting with default tenge symbol.
 */
fun Long.formatAsMoney(): String =
    MoneyFormatter.formatWithFraction(this, "₸")

/**
 * Extension function for formatting with sign and default tenge symbol.
 */
fun Long.formatAsMoney(withSign: Boolean): String =
    if (withSign) MoneyFormatter.formatWithSign(abs(this), "₸", this >= 0)
    else MoneyFormatter.formatWithFraction(this, "₸")

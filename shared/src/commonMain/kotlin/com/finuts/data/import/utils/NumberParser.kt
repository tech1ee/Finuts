package com.finuts.data.import.utils

/**
 * Locale-aware number parser for bank statement amounts.
 * Handles different number formats used across regions:
 * - US: 1,000.00
 * - EU: 1.000,00
 * - RU_KZ: 1 000,00
 * - INDIAN: 1,00,000.00
 *
 * All amounts are returned as Long in minor units (cents/kopecks).
 */
class NumberParser : NumberParserInterface {

    private val CURRENCY_SYMBOLS = setOf(
        '$', '€', '£', '¥', '₽', '₸', '₴', '₾', '₼', '₿',
        '฿', '₫', '₹', '₩', '₪', '₱', '₡', '₢', '₣', '₤', '₥', '₦', '₧', '₨'
    )

    private val CURRENCY_CODES = listOf(
        "USD", "EUR", "GBP", "JPY", "RUB", "KZT", "UAH", "GEL", "AZN",
        "CNY", "INR", "KRW", "BTC", "ETH"
    )

    private val SPACE_CHARS = setOf(' ', '\u00A0', '\u202F', '\u2009')

    /**
     * Parse a number string to Long in minor units (cents).
     * Assumes 2 decimal places if not present.
     *
     * @param text The number string to parse
     * @param locale The number format locale
     * @return Amount in minor units (e.g., cents)
     * @throws NumberParseException if the string cannot be parsed
     */
    override fun parse(text: String, locale: NumberLocale): Long {
        val cleaned = cleanInput(text)

        if (cleaned.isEmpty()) {
            throw NumberParseException.empty()
        }

        val effectiveLocale = if (locale == NumberLocale.AUTO) {
            detectLocale(cleaned)
        } else {
            locale
        }

        return parseWithLocale(cleaned, effectiveLocale)
    }

    /**
     * Detect the number format locale from a sample string.
     *
     * @param text Sample number string
     * @return Detected locale (defaults to US if ambiguous)
     */
    override fun detectLocale(text: String): NumberLocale {
        val cleaned = cleanInput(text)

        // Check for space as thousands separator (RU_KZ)
        if (SPACE_CHARS.any { cleaned.contains(it) }) {
            return NumberLocale.RU_KZ
        }

        val dotIndex = cleaned.lastIndexOf('.')
        val commaIndex = cleaned.lastIndexOf(',')

        return when {
            // No separators - default to US
            dotIndex == -1 && commaIndex == -1 -> NumberLocale.US

            // Only dot - likely US decimal
            dotIndex != -1 && commaIndex == -1 -> NumberLocale.US

            // Only comma - could be EU decimal or US thousands
            dotIndex == -1 && commaIndex != -1 -> {
                // If comma is followed by exactly 2 digits at end, it's EU decimal
                val afterComma = cleaned.substring(commaIndex + 1)
                if (afterComma.length <= 2 && afterComma.all { it.isDigit() }) {
                    NumberLocale.EU
                } else {
                    NumberLocale.US
                }
            }

            // Both dot and comma present
            else -> {
                // Last separator is the decimal separator
                if (dotIndex > commaIndex) {
                    // Pattern: 1,234.56 (US) or 1,00,000.00 (INDIAN)
                    // Check for Indian pattern (2-digit groups)
                    val beforeDot = cleaned.substring(0, dotIndex)
                    if (isIndianFormat(beforeDot)) {
                        NumberLocale.INDIAN
                    } else {
                        NumberLocale.US
                    }
                } else {
                    // Pattern: 1.234,56 (EU)
                    NumberLocale.EU
                }
            }
        }
    }

    private fun cleanInput(text: String): String {
        var result = text.trim()

        // Remove currency symbols
        CURRENCY_SYMBOLS.forEach { symbol ->
            result = result.replace(symbol.toString(), "")
        }

        // Remove currency codes
        CURRENCY_CODES.forEach { code ->
            result = result.replace(code, "", ignoreCase = true)
        }

        return result.trim()
    }

    private fun parseWithLocale(text: String, locale: NumberLocale): Long {
        // Handle negative numbers
        val (isNegative, numericText) = extractSign(text)

        val normalized = when (locale) {
            NumberLocale.US, NumberLocale.INDIAN -> normalizeUS(numericText)
            NumberLocale.EU -> normalizeEU(numericText)
            NumberLocale.RU_KZ -> normalizeRuKz(numericText)
            NumberLocale.AUTO -> throw IllegalStateException("AUTO should be resolved")
        }

        if (normalized.isEmpty() || !normalized.any { it.isDigit() }) {
            throw NumberParseException.noDigits(text)
        }

        val cents = parseNormalizedToCents(normalized)
        return if (isNegative) -cents else cents
    }

    private fun extractSign(text: String): Pair<Boolean, String> {
        val trimmed = text.trim()

        // Check for parentheses (accounting negative)
        if (trimmed.startsWith('(') && trimmed.endsWith(')')) {
            return true to trimmed.substring(1, trimmed.length - 1)
        }

        // Check for minus sign
        if (trimmed.startsWith('-')) {
            return true to trimmed.substring(1)
        }

        // Check for plus sign (positive)
        if (trimmed.startsWith('+')) {
            return false to trimmed.substring(1)
        }

        return false to trimmed
    }

    private fun normalizeUS(text: String): String {
        // Remove commas (thousands separator)
        return text.replace(",", "")
    }

    private fun normalizeEU(text: String): String {
        // Remove dots (thousands separator), replace comma with dot (decimal)
        return text.replace(".", "").replace(",", ".")
    }

    private fun normalizeRuKz(text: String): String {
        // Remove all space characters (thousands separator), replace comma with dot
        var result = text
        SPACE_CHARS.forEach { space ->
            result = result.replace(space.toString(), "")
        }
        return result.replace(",", ".")
    }

    private fun parseNormalizedToCents(normalized: String): Long {
        val dotIndex = normalized.indexOf('.')

        return if (dotIndex == -1) {
            // No decimal point - multiply by 100
            normalized.toLongOrNull()?.times(100)
                ?: throw NumberParseException.invalidFormat(normalized)
        } else {
            val integerPart = normalized.substring(0, dotIndex)
            val decimalPart = normalized.substring(dotIndex + 1).padEnd(2, '0').take(2)

            val integer = if (integerPart.isEmpty()) 0L else {
                integerPart.toLongOrNull()
                    ?: throw NumberParseException.invalidFormat(normalized)
            }

            val decimal = decimalPart.toLongOrNull()
                ?: throw NumberParseException.invalidFormat(normalized)

            integer * 100 + decimal
        }
    }

    private fun isIndianFormat(beforeDecimal: String): Boolean {
        // Indian format has pattern: X,XX,XX,XXX (2-digit groups after first 3)
        val parts = beforeDecimal.split(',')
        if (parts.size < 2) return false

        // First part can be 1-3 digits, rest should be 2 digits
        val firstPart = parts.first()
        val restParts = parts.drop(1)

        return firstPart.length in 1..3 &&
            restParts.all { it.length == 2 }
    }
}

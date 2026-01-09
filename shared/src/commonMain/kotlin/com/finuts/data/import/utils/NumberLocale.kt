package com.finuts.data.import.utils

/**
 * Number format locale for parsing amounts in bank statements.
 * Different regions use different conventions for decimal and thousands separators.
 */
enum class NumberLocale {
    /**
     * Auto-detect locale from the number format.
     * Uses heuristics to determine the most likely format.
     */
    AUTO,

    /**
     * US format: comma as thousands separator, dot as decimal.
     * Example: 1,234.56
     */
    US,

    /**
     * European format: dot as thousands separator, comma as decimal.
     * Example: 1.234,56
     */
    EU,

    /**
     * Russian/Kazakh format: space as thousands separator, comma as decimal.
     * Example: 1 234,56
     */
    RU_KZ,

    /**
     * Indian format: lakh/crore grouping (2-digit groups after thousands), dot as decimal.
     * Example: 1,00,000.00 (one lakh)
     */
    INDIAN
}

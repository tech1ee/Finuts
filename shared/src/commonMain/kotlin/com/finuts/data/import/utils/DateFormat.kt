package com.finuts.data.import.utils

/**
 * Date format types supported by DateParser.
 * Used for parsing dates in bank statements from different regions.
 */
enum class DateFormat {
    /**
     * Auto-detect format from the input string.
     */
    AUTO,

    /**
     * ISO 8601 format: YYYY-MM-DD
     * Example: 2024-01-15
     */
    ISO,

    /**
     * Compact ISO format: YYYYMMDD
     * Example: 20240115
     */
    ISO_COMPACT,

    /**
     * European format: DD.MM.YYYY or DD/MM/YYYY or DD-MM-YYYY
     * Example: 15.01.2024
     */
    EU,

    /**
     * Compact European format: DDMMYYYY
     * Example: 15012024
     */
    EU_COMPACT,

    /**
     * US format: MM/DD/YYYY or MM-DD-YYYY
     * Example: 01/15/2024
     */
    US,

    /**
     * Russian text format with month name
     * Example: 15 января 2024
     */
    RUSSIAN_TEXT,

    /**
     * English text format with month name
     * Example: January 15, 2024
     */
    ENGLISH_TEXT
}

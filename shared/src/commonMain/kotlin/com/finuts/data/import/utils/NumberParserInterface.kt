package com.finuts.data.import.utils

/**
 * Interface for locale-aware number parsing operations.
 * Implementations handle different number formats used across regions
 * (US, EU, RU/KZ, Indian).
 */
interface NumberParserInterface {

    /**
     * Parse a number string to Long in minor units (cents).
     * Assumes 2 decimal places if not present.
     *
     * @param text The number string to parse
     * @param locale The number format locale (default: AUTO)
     * @return Amount in minor units (e.g., cents)
     * @throws NumberParseException if the string cannot be parsed
     */
    fun parse(text: String, locale: NumberLocale = NumberLocale.AUTO): Long

    /**
     * Detect the number format locale from a sample string.
     *
     * @param text Sample number string
     * @return Detected locale (defaults to US if ambiguous)
     */
    fun detectLocale(text: String): NumberLocale
}

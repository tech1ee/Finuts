package com.finuts.data.import.utils

import kotlinx.datetime.LocalDate

/**
 * Interface for date parsing operations.
 * Implementations handle various date formats used across different banks and regions.
 */
interface DateParserInterface {

    /**
     * Parse a date string to LocalDate.
     *
     * @param text The date string to parse
     * @param format The expected date format (default: AUTO)
     * @return Parsed LocalDate
     * @throws DateParseException if the string cannot be parsed
     */
    fun parse(text: String, format: DateFormat = DateFormat.AUTO): LocalDate

    /**
     * Try to parse a date string, returning null on failure.
     *
     * @param text The date string to parse
     * @param format The expected date format (default: AUTO)
     * @return Parsed LocalDate or null if parsing fails
     */
    fun parseOrNull(text: String, format: DateFormat = DateFormat.AUTO): LocalDate?

    /**
     * Detect the date format from a sample string.
     *
     * @param text Sample date string
     * @return Detected format
     */
    fun detectFormat(text: String): DateFormat
}

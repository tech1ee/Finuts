package com.finuts.data.import.utils

import kotlinx.datetime.LocalDate

/**
 * Multi-format date parser for bank statements.
 * Handles various date formats used across different banks and regions.
 */
object DateParser {

    private val RUSSIAN_MONTHS = mapOf(
        // Nominative
        "январь" to 1, "февраль" to 2, "март" to 3, "апрель" to 4,
        "май" to 5, "июнь" to 6, "июль" to 7, "август" to 8,
        "сентябрь" to 9, "октябрь" to 10, "ноябрь" to 11, "декабрь" to 12,
        // Genitive
        "января" to 1, "февраля" to 2, "марта" to 3, "апреля" to 4,
        "мая" to 5, "июня" to 6, "июля" to 7, "августа" to 8,
        "сентября" to 9, "октября" to 10, "ноября" to 11, "декабря" to 12,
        // Abbreviated
        "янв" to 1, "фев" to 2, "мар" to 3, "апр" to 4,
        "июн" to 6, "июл" to 7, "авг" to 8,
        "сен" to 9, "окт" to 10, "ноя" to 11, "дек" to 12
    )

    private val ENGLISH_MONTHS = mapOf(
        "january" to 1, "february" to 2, "march" to 3, "april" to 4,
        "may" to 5, "june" to 6, "july" to 7, "august" to 8,
        "september" to 9, "october" to 10, "november" to 11, "december" to 12,
        // Abbreviated
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
        "jun" to 6, "jul" to 7, "aug" to 8,
        "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
    )

    private val ISO_REGEX = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})$""")
    private val EU_REGEX = Regex("""^(\d{1,2})[./\-](\d{1,2})[./\-](\d{2,4})$""")
    private val COMPACT_REGEX = Regex("""^(\d{8})$""")
    private val TEXT_DATE_REGEX = Regex("""(\d{1,2})\s+(\p{L}+)\s+(\d{4})""")
    private val EN_TEXT_DATE_REGEX = Regex("""(\p{L}+)\s+(\d{1,2}),?\s+(\d{4})""")

    /**
     * Parse a date string to LocalDate.
     *
     * @param text The date string to parse
     * @param format The expected date format (default: AUTO)
     * @return Parsed LocalDate
     * @throws DateParseException if the string cannot be parsed
     */
    fun parse(text: String, format: DateFormat = DateFormat.AUTO): LocalDate {
        val trimmed = text.trim()

        if (trimmed.isEmpty()) {
            throw DateParseException.empty()
        }

        val effectiveFormat = if (format == DateFormat.AUTO) {
            detectFormat(trimmed)
        } else {
            format
        }

        return parseWithFormat(trimmed, effectiveFormat)
    }

    /**
     * Try to parse a date string, returning null on failure.
     *
     * @param text The date string to parse
     * @param format The expected date format (default: AUTO)
     * @return Parsed LocalDate or null if parsing fails
     */
    fun parseOrNull(text: String, format: DateFormat = DateFormat.AUTO): LocalDate? {
        return try {
            parse(text, format)
        } catch (_: DateParseException) {
            null
        }
    }

    /**
     * Detect the date format from a sample string.
     *
     * @param text Sample date string
     * @return Detected format
     */
    fun detectFormat(text: String): DateFormat {
        val trimmed = text.trim()

        // Check ISO format first (YYYY-MM-DD)
        if (ISO_REGEX.matches(trimmed)) {
            return DateFormat.ISO
        }

        // Check compact format (8 digits)
        if (COMPACT_REGEX.matches(trimmed)) {
            // If starts with 19 or 20, it's ISO compact (YYYYMMDD)
            return if (trimmed.startsWith("19") || trimmed.startsWith("20")) {
                DateFormat.ISO_COMPACT
            } else {
                DateFormat.EU_COMPACT
            }
        }

        // Check for Russian month names
        val lowerText = trimmed.lowercase()
        if (RUSSIAN_MONTHS.keys.any { lowerText.contains(it) }) {
            return DateFormat.RUSSIAN_TEXT
        }

        // Check for English month names
        if (ENGLISH_MONTHS.keys.any { lowerText.contains(it) }) {
            return DateFormat.ENGLISH_TEXT
        }

        // Check EU format (DD.MM.YYYY or similar)
        if (EU_REGEX.matches(trimmed)) {
            return DateFormat.EU
        }

        return DateFormat.AUTO
    }

    private fun parseWithFormat(text: String, format: DateFormat): LocalDate {
        return when (format) {
            DateFormat.ISO -> parseIso(text)
            DateFormat.ISO_COMPACT -> parseIsoCompact(text)
            DateFormat.EU -> parseEu(text)
            DateFormat.EU_COMPACT -> parseEuCompact(text)
            DateFormat.US -> parseUs(text)
            DateFormat.RUSSIAN_TEXT -> parseRussianText(text)
            DateFormat.ENGLISH_TEXT -> parseEnglishText(text)
            DateFormat.AUTO -> tryAllFormats(text)
        }
    }

    private fun parseIso(text: String): LocalDate {
        val match = ISO_REGEX.find(text)
            ?: throw DateParseException.invalidFormat(text)

        val year = match.groupValues[1].toInt()
        val month = match.groupValues[2].toInt()
        val day = match.groupValues[3].toInt()

        return createDate(text, year, month, day)
    }

    private fun parseIsoCompact(text: String): LocalDate {
        if (text.length != 8) {
            throw DateParseException.invalidFormat(text)
        }

        val year = text.substring(0, 4).toIntOrNull()
            ?: throw DateParseException.invalidFormat(text)
        val month = text.substring(4, 6).toIntOrNull()
            ?: throw DateParseException.invalidFormat(text)
        val day = text.substring(6, 8).toIntOrNull()
            ?: throw DateParseException.invalidFormat(text)

        return createDate(text, year, month, day)
    }

    private fun parseEu(text: String): LocalDate {
        val match = EU_REGEX.find(text)
            ?: throw DateParseException.invalidFormat(text)

        val day = match.groupValues[1].toInt()
        val month = match.groupValues[2].toInt()
        val year = normalizeYear(match.groupValues[3].toInt())

        return createDate(text, year, month, day)
    }

    private fun parseEuCompact(text: String): LocalDate {
        if (text.length != 8) {
            throw DateParseException.invalidFormat(text)
        }

        val day = text.substring(0, 2).toIntOrNull()
            ?: throw DateParseException.invalidFormat(text)
        val month = text.substring(2, 4).toIntOrNull()
            ?: throw DateParseException.invalidFormat(text)
        val year = text.substring(4, 8).toIntOrNull()
            ?: throw DateParseException.invalidFormat(text)

        return createDate(text, year, month, day)
    }

    private fun parseUs(text: String): LocalDate {
        val match = EU_REGEX.find(text)
            ?: throw DateParseException.invalidFormat(text)

        // In US format: MM/DD/YYYY
        val month = match.groupValues[1].toInt()
        val day = match.groupValues[2].toInt()
        val year = normalizeYear(match.groupValues[3].toInt())

        return createDate(text, year, month, day)
    }

    private fun parseRussianText(text: String): LocalDate {
        val normalized = text.lowercase().replace(Regex("\\s+"), " ").trim()

        val match = TEXT_DATE_REGEX.find(normalized)
            ?: throw DateParseException.invalidFormat(text)

        val day = match.groupValues[1].toInt()
        val monthName = match.groupValues[2]
        val year = match.groupValues[3].toInt()

        val month = RUSSIAN_MONTHS[monthName]
            ?: throw DateParseException.invalidFormat(text)

        return createDate(text, year, month, day)
    }

    private fun parseEnglishText(text: String): LocalDate {
        val normalized = text.lowercase().replace(Regex("\\s+"), " ").trim()

        // Try "Month DD, YYYY" format first
        val matchMonthFirst = EN_TEXT_DATE_REGEX.find(normalized)
        if (matchMonthFirst != null) {
            val monthName = matchMonthFirst.groupValues[1]
            val day = matchMonthFirst.groupValues[2].toInt()
            val year = matchMonthFirst.groupValues[3].toInt()

            val month = ENGLISH_MONTHS[monthName]
                ?: throw DateParseException.invalidFormat(text)

            return createDate(text, year, month, day)
        }

        // Try "DD Month YYYY" format
        val matchDayFirst = TEXT_DATE_REGEX.find(normalized)
        if (matchDayFirst != null) {
            val day = matchDayFirst.groupValues[1].toInt()
            val monthName = matchDayFirst.groupValues[2]
            val year = matchDayFirst.groupValues[3].toInt()

            val month = ENGLISH_MONTHS[monthName]
                ?: throw DateParseException.invalidFormat(text)

            return createDate(text, year, month, day)
        }

        throw DateParseException.invalidFormat(text)
    }

    private fun tryAllFormats(text: String): LocalDate {
        // Try each format in order of specificity
        val formats = listOf(
            DateFormat.ISO,
            DateFormat.ISO_COMPACT,
            DateFormat.RUSSIAN_TEXT,
            DateFormat.ENGLISH_TEXT,
            DateFormat.EU
        )

        for (format in formats) {
            try {
                return parseWithFormat(text, format)
            } catch (_: DateParseException) {
                // Try next format
            }
        }

        throw DateParseException.invalidFormat(text)
    }

    private fun normalizeYear(year: Int): Int {
        return when {
            year >= 100 -> year
            year >= 50 -> 1900 + year
            else -> 2000 + year
        }
    }

    private fun createDate(input: String, year: Int, month: Int, day: Int): LocalDate {
        return try {
            LocalDate(year, month, day)
        } catch (e: IllegalArgumentException) {
            throw DateParseException.invalidDate(input, day, month, year)
        }
    }
}

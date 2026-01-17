package com.finuts.data.import.utils

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Tests for DateParser - multi-format date parsing for bank statements.
 * Handles various date formats used across different banks and regions.
 */
class DateParserTest {

    private val parser = DateParser()

    // ==================== ISO Format Tests ====================

    @Test
    fun `parse ISO format YYYY-MM-DD`() {
        val result = parser.parse("2024-01-15")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    @Test
    fun `parse ISO format YYYY-MM-DD with leading zeros`() {
        val result = parser.parse("2024-01-05")
        assertEquals(LocalDate(2024, 1, 5), result)
    }

    // ==================== European Format Tests (DD.MM.YYYY) ====================

    @Test
    fun `parse EU format DD_MM_YYYY with dots`() {
        val result = parser.parse("15.01.2024")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    @Test
    fun `parse EU format D_M_YYYY with single digits`() {
        val result = parser.parse("5.1.2024")
        assertEquals(LocalDate(2024, 1, 5), result)
    }

    @Test
    fun `parse EU format DD_MM_YY with two-digit year`() {
        val result = parser.parse("15.01.24")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    @Test
    fun `parse EU format DD_MM_YYYY with slashes`() {
        val result = parser.parse("15/01/2024")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    @Test
    fun `parse EU format DD-MM-YYYY with dashes`() {
        val result = parser.parse("15-01-2024")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    // ==================== US Format Tests (MM/DD/YYYY) ====================

    @Test
    fun `parse US format MM_DD_YYYY with slashes`() {
        val result = parser.parse("01/15/2024", DateFormat.US)
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    @Test
    fun `parse US format M_D_YYYY with single digits`() {
        val result = parser.parse("1/5/2024", DateFormat.US)
        assertEquals(LocalDate(2024, 1, 5), result)
    }

    @Test
    fun `parse US format MM_DD_YY with two-digit year`() {
        val result = parser.parse("01/15/24", DateFormat.US)
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    // ==================== Russian Format Tests ====================

    @Test
    fun `parse Russian format with month name`() {
        val result = parser.parse("15 января 2024")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    @Test
    fun `parse Russian format with abbreviated month`() {
        val result = parser.parse("15 янв 2024")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    @Test
    fun `parse Russian format with genitive month`() {
        val result = parser.parse("15 февраля 2024")
        assertEquals(LocalDate(2024, 2, 15), result)
    }

    @Test
    fun `parse Russian format with dots`() {
        val result = parser.parse("15.01.2024")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    // ==================== English Format Tests ====================

    @Test
    fun `parse English format with month name`() {
        val result = parser.parse("January 15, 2024")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    @Test
    fun `parse English format with abbreviated month`() {
        val result = parser.parse("Jan 15, 2024")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    @Test
    fun `parse English format without comma`() {
        val result = parser.parse("15 January 2024")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    @Test
    fun `parse English format with abbreviated month no comma`() {
        val result = parser.parse("15 Jan 2024")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    // ==================== Compact Format Tests ====================

    @Test
    fun `parse compact format YYYYMMDD`() {
        val result = parser.parse("20240115")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    @Test
    fun `parse compact format DDMMYYYY`() {
        val result = parser.parse("15012024", DateFormat.EU_COMPACT)
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    // ==================== All Month Names Tests ====================

    @Test
    fun `parse all Russian months`() {
        val months = listOf(
            "январь" to 1, "февраль" to 2, "март" to 3, "апрель" to 4,
            "май" to 5, "июнь" to 6, "июль" to 7, "август" to 8,
            "сентябрь" to 9, "октябрь" to 10, "ноябрь" to 11, "декабрь" to 12
        )
        months.forEach { (name, num) ->
            val result = parser.parse("15 $name 2024")
            assertEquals(LocalDate(2024, num, 15), result, "Failed for $name")
        }
    }

    @Test
    fun `parse all Russian genitive months`() {
        val months = listOf(
            "января" to 1, "февраля" to 2, "марта" to 3, "апреля" to 4,
            "мая" to 5, "июня" to 6, "июля" to 7, "августа" to 8,
            "сентября" to 9, "октября" to 10, "ноября" to 11, "декабря" to 12
        )
        months.forEach { (name, num) ->
            val result = parser.parse("15 $name 2024")
            assertEquals(LocalDate(2024, num, 15), result, "Failed for $name")
        }
    }

    @Test
    fun `parse all English months`() {
        val months = listOf(
            "January" to 1, "February" to 2, "March" to 3, "April" to 4,
            "May" to 5, "June" to 6, "July" to 7, "August" to 8,
            "September" to 9, "October" to 10, "November" to 11, "December" to 12
        )
        months.forEach { (name, num) ->
            val result = parser.parse("$name 15, 2024")
            assertEquals(LocalDate(2024, num, 15), result, "Failed for $name")
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `parse with extra whitespace`() {
        val result = parser.parse("  15.01.2024  ")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    @Test
    fun `parse with multiple spaces between parts`() {
        val result = parser.parse("15  января   2024")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    @Test
    fun `parse year in 20xx century`() {
        val result = parser.parse("15.01.99")
        assertEquals(LocalDate(1999, 1, 15), result)
    }

    @Test
    fun `parse year in 19xx century`() {
        val result = parser.parse("15.01.50")
        assertEquals(LocalDate(1950, 1, 15), result)
    }

    @Test
    fun `parse first day of month`() {
        val result = parser.parse("01.01.2024")
        assertEquals(LocalDate(2024, 1, 1), result)
    }

    @Test
    fun `parse last day of month`() {
        val result = parser.parse("31.12.2024")
        assertEquals(LocalDate(2024, 12, 31), result)
    }

    @Test
    fun `parse leap year February 29`() {
        val result = parser.parse("29.02.2024")
        assertEquals(LocalDate(2024, 2, 29), result)
    }

    // ==================== Auto Detection Tests ====================

    @Test
    fun `auto detect ISO format`() {
        val format = parser.detectFormat("2024-01-15")
        assertEquals(DateFormat.ISO, format)
    }

    @Test
    fun `auto detect EU format with dots`() {
        val format = parser.detectFormat("15.01.2024")
        assertEquals(DateFormat.EU, format)
    }

    @Test
    fun `auto detect compact format 8 digits starting with 20`() {
        val format = parser.detectFormat("20240115")
        assertEquals(DateFormat.ISO_COMPACT, format)
    }

    @Test
    fun `auto detect Russian month name`() {
        val format = parser.detectFormat("15 января 2024")
        assertEquals(DateFormat.RUSSIAN_TEXT, format)
    }

    @Test
    fun `auto detect English month name`() {
        val format = parser.detectFormat("January 15, 2024")
        assertEquals(DateFormat.ENGLISH_TEXT, format)
    }

    // ==================== Nullable Parse Tests ====================

    @Test
    fun `parseOrNull returns null for invalid date`() {
        val result = parser.parseOrNull("not a date")
        assertNull(result)
    }

    @Test
    fun `parseOrNull returns null for empty string`() {
        val result = parser.parseOrNull("")
        assertNull(result)
    }

    @Test
    fun `parseOrNull returns date for valid input`() {
        val result = parser.parseOrNull("15.01.2024")
        assertEquals(LocalDate(2024, 1, 15), result)
    }

    // ==================== Error Cases ====================

    @Test
    fun `parse empty string throws exception`() {
        assertFailsWith<DateParseException> {
            parser.parse("")
        }
    }

    @Test
    fun `parse invalid date throws exception`() {
        assertFailsWith<DateParseException> {
            parser.parse("not a date")
        }
    }

    @Test
    fun `parse invalid day throws exception`() {
        assertFailsWith<DateParseException> {
            parser.parse("32.01.2024")
        }
    }

    @Test
    fun `parse invalid month throws exception`() {
        assertFailsWith<DateParseException> {
            parser.parse("15.13.2024")
        }
    }

    @Test
    fun `parse non-leap year February 29 throws exception`() {
        assertFailsWith<DateParseException> {
            parser.parse("29.02.2023")
        }
    }
}

package com.finuts.data.import.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for NumberParser - locale-aware number parsing for bank statements.
 * Handles different number formats:
 * - US: 1,000.00 (comma as thousands separator, dot as decimal)
 * - EU: 1.000,00 (dot as thousands separator, comma as decimal)
 * - RU_KZ: 1 000,00 (space as thousands separator, comma as decimal)
 * - INDIAN: 1,00,000.00 (lakh/crore grouping)
 */
class NumberParserTest {

    private val parser = NumberParser()

    // ==================== US Format Tests ====================

    @Test
    fun `parse US format with comma thousands separator`() {
        val result = parser.parse("1,000.00", NumberLocale.US)
        assertEquals(100000L, result) // 1000.00 in cents
    }

    @Test
    fun `parse US format without thousands separator`() {
        val result = parser.parse("1000.00", NumberLocale.US)
        assertEquals(100000L, result)
    }

    @Test
    fun `parse US format with multiple thousands separators`() {
        val result = parser.parse("1,234,567.89", NumberLocale.US)
        assertEquals(123456789L, result)
    }

    @Test
    fun `parse US format integer without decimals`() {
        val result = parser.parse("1,000", NumberLocale.US)
        assertEquals(100000L, result) // Assumes 2 decimal places
    }

    @Test
    fun `parse US format with single decimal place`() {
        val result = parser.parse("1,000.5", NumberLocale.US)
        assertEquals(100050L, result)
    }

    // ==================== EU Format Tests ====================

    @Test
    fun `parse EU format with dot thousands separator`() {
        val result = parser.parse("1.000,00", NumberLocale.EU)
        assertEquals(100000L, result)
    }

    @Test
    fun `parse EU format without thousands separator`() {
        val result = parser.parse("1000,00", NumberLocale.EU)
        assertEquals(100000L, result)
    }

    @Test
    fun `parse EU format with multiple thousands separators`() {
        val result = parser.parse("1.234.567,89", NumberLocale.EU)
        assertEquals(123456789L, result)
    }

    @Test
    fun `parse EU format integer without decimals`() {
        val result = parser.parse("1.000", NumberLocale.EU)
        assertEquals(100000L, result)
    }

    // ==================== RU_KZ Format Tests ====================

    @Test
    fun `parse RU_KZ format with space thousands separator`() {
        val result = parser.parse("1 000,00", NumberLocale.RU_KZ)
        assertEquals(100000L, result)
    }

    @Test
    fun `parse RU_KZ format with non-breaking space`() {
        val result = parser.parse("1\u00A0000,00", NumberLocale.RU_KZ)
        assertEquals(100000L, result)
    }

    @Test
    fun `parse RU_KZ format with narrow no-break space`() {
        val result = parser.parse("1\u202F000,00", NumberLocale.RU_KZ)
        assertEquals(100000L, result)
    }

    @Test
    fun `parse RU_KZ format without thousands separator`() {
        val result = parser.parse("1000,00", NumberLocale.RU_KZ)
        assertEquals(100000L, result)
    }

    @Test
    fun `parse RU_KZ format with multiple thousands separators`() {
        val result = parser.parse("1 234 567,89", NumberLocale.RU_KZ)
        assertEquals(123456789L, result)
    }

    @Test
    fun `parse RU_KZ format integer without decimals`() {
        val result = parser.parse("1 000", NumberLocale.RU_KZ)
        assertEquals(100000L, result)
    }

    // ==================== INDIAN Format Tests ====================

    @Test
    fun `parse INDIAN format with lakh grouping`() {
        val result = parser.parse("1,00,000.00", NumberLocale.INDIAN)
        assertEquals(10000000L, result) // 100,000.00 in paise
    }

    @Test
    fun `parse INDIAN format with crore grouping`() {
        val result = parser.parse("1,00,00,000.00", NumberLocale.INDIAN)
        assertEquals(1000000000L, result) // 10,000,000.00 in paise
    }

    @Test
    fun `parse INDIAN format simple number`() {
        val result = parser.parse("1,000.50", NumberLocale.INDIAN)
        assertEquals(100050L, result)
    }

    // ==================== AUTO Detection Tests ====================

    @Test
    fun `auto detect US format`() {
        val result = parser.parse("1,234.56", NumberLocale.AUTO)
        assertEquals(123456L, result)
    }

    @Test
    fun `auto detect EU format`() {
        val result = parser.parse("1.234,56", NumberLocale.AUTO)
        assertEquals(123456L, result)
    }

    @Test
    fun `auto detect RU_KZ format`() {
        val result = parser.parse("1 234,56", NumberLocale.AUTO)
        assertEquals(123456L, result)
    }

    @Test
    fun `auto detect simple integer`() {
        val result = parser.parse("1234", NumberLocale.AUTO)
        assertEquals(123400L, result)
    }

    @Test
    fun `auto detect simple decimal with dot`() {
        val result = parser.parse("1234.56", NumberLocale.AUTO)
        assertEquals(123456L, result)
    }

    @Test
    fun `auto detect simple decimal with comma`() {
        val result = parser.parse("1234,56", NumberLocale.AUTO)
        assertEquals(123456L, result)
    }

    // ==================== Negative Numbers ====================

    @Test
    fun `parse negative number with minus sign`() {
        val result = parser.parse("-1,000.00", NumberLocale.US)
        assertEquals(-100000L, result)
    }

    @Test
    fun `parse negative number with parentheses`() {
        val result = parser.parse("(1,000.00)", NumberLocale.US)
        assertEquals(-100000L, result)
    }

    @Test
    fun `parse negative RU_KZ format`() {
        val result = parser.parse("-1 000,00", NumberLocale.RU_KZ)
        assertEquals(-100000L, result)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `parse zero`() {
        val result = parser.parse("0.00", NumberLocale.US)
        assertEquals(0L, result)
    }

    @Test
    fun `parse small amount`() {
        val result = parser.parse("0.01", NumberLocale.US)
        assertEquals(1L, result)
    }

    @Test
    fun `parse large amount`() {
        val result = parser.parse("999,999,999.99", NumberLocale.US)
        assertEquals(99999999999L, result)
    }

    @Test
    fun `parse with leading zeros in decimal`() {
        val result = parser.parse("10.05", NumberLocale.US)
        assertEquals(1005L, result)
    }

    @Test
    fun `parse with currency symbol prefix`() {
        val result = parser.parse("$1,000.00", NumberLocale.US)
        assertEquals(100000L, result)
    }

    @Test
    fun `parse with currency symbol suffix`() {
        val result = parser.parse("1 000,00 ₸", NumberLocale.RU_KZ)
        assertEquals(100000L, result)
    }

    @Test
    fun `parse with currency code`() {
        val result = parser.parse("USD 1,000.00", NumberLocale.US)
        assertEquals(100000L, result)
    }

    @Test
    fun `parse with whitespace`() {
        val result = parser.parse("  1,000.00  ", NumberLocale.US)
        assertEquals(100000L, result)
    }

    @Test
    fun `parse with plus sign`() {
        val result = parser.parse("+1,000.00", NumberLocale.US)
        assertEquals(100000L, result)
    }

    // ==================== Error Cases ====================

    @Test
    fun `parse empty string throws exception`() {
        assertFailsWith<NumberParseException> {
            parser.parse("", NumberLocale.US)
        }
    }

    @Test
    fun `parse non-numeric string throws exception`() {
        assertFailsWith<NumberParseException> {
            parser.parse("abc", NumberLocale.US)
        }
    }

    @Test
    fun `parse only whitespace throws exception`() {
        assertFailsWith<NumberParseException> {
            parser.parse("   ", NumberLocale.US)
        }
    }

    // ==================== Locale Detection ====================

    @Test
    fun `detectLocale identifies US format`() {
        val locale = parser.detectLocale("1,234.56")
        assertEquals(NumberLocale.US, locale)
    }

    @Test
    fun `detectLocale identifies EU format`() {
        val locale = parser.detectLocale("1.234,56")
        assertEquals(NumberLocale.EU, locale)
    }

    @Test
    fun `detectLocale identifies RU_KZ format`() {
        val locale = parser.detectLocale("1 234,56")
        assertEquals(NumberLocale.RU_KZ, locale)
    }

    @Test
    fun `detectLocale defaults to US for ambiguous`() {
        val locale = parser.detectLocale("1234")
        assertEquals(NumberLocale.US, locale)
    }
}

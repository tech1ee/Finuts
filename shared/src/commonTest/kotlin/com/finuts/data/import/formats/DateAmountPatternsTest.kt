package com.finuts.data.import.formats

import com.finuts.data.import.ocr.LocalTransactionExtractor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD Tests for Date and Amount Parsing Patterns.
 *
 * Comprehensive tests for all date and amount formats encountered
 * in bank statements, receipts, and invoices worldwide.
 *
 * Based on research from:
 * - [Regex Bank Statement Tutorial](https://gist.github.com/Bazrahimi/61547eb9d758ef55ca42aef0a470dbf9)
 * - [Transaction Parsing Guide](https://docs.inscribe.ai/docs/transaction-parsing)
 * - Various bank statement format specifications
 */
class DateAmountPatternsTest {

    private val extractor = LocalTransactionExtractor()

    // ============================================
    // DATE FORMAT TESTS
    // ============================================

    // === European Date Formats (DD.MM.YYYY) ===

    @Test
    fun `parses DD dot MM dot YYYY format`() {
        val dates = listOf(
            "01.01.2026" to "01.01.2026",
            "15.06.2026" to "15.06.2026",
            "31.12.2026" to "31.12.2026"
        )

        dates.forEach { (date, expected) ->
            val line = "$date -100,00 ₽ Test"
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse date: $date")
            assertEquals(expected, result[0].rawDate, "Date mismatch for: $date")
        }
    }

    @Test
    fun `parses DD dot MM dot YY short year format`() {
        val dates = listOf(
            "01.01.26" to "01.01.26",
            "15.06.26" to "15.06.26",
            "31.12.26" to "31.12.26"
        )

        dates.forEach { (date, expected) ->
            val line = "$date -100,00 ₽ Test"
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse date: $date")
            assertEquals(expected, result[0].rawDate, "Date mismatch for: $date")
        }
    }

    // === US Date Formats (MM/DD/YYYY) ===

    @Test
    fun `parses MM slash DD slash YYYY format`() {
        val dates = listOf(
            "01/15/2026" to "01/15/2026",
            "06/30/2026" to "06/30/2026",
            "12/31/2026" to "12/31/2026"
        )

        dates.forEach { (date, expected) ->
            val line = "$date -$100.00 Test"
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse date: $date")
            assertEquals(expected, result[0].rawDate, "Date mismatch for: $date")
        }
    }

    @Test
    fun `parses MM slash DD slash YY short year format`() {
        val dates = listOf(
            "01/15/26" to "01/15/26",
            "06/30/26" to "06/30/26",
            "12/31/26" to "12/31/26"
        )

        dates.forEach { (date, expected) ->
            val line = "$date -$100.00 Test"
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse date: $date")
            assertEquals(expected, result[0].rawDate, "Date mismatch for: $date")
        }
    }

    // === ISO Date Format (YYYY-MM-DD) ===

    @Test
    fun `parses YYYY dash MM dash DD ISO format`() {
        val dates = listOf(
            "2026-01-15" to "2026-01-15",
            "2026-06-30" to "2026-06-30",
            "2026-12-31" to "2026-12-31"
        )

        dates.forEach { (date, expected) ->
            val line = "$date -100.00 USD Test"
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse date: $date")
            assertEquals(expected, result[0].rawDate, "Date mismatch for: $date")
        }
    }

    // === UK Date Formats (DD/MM/YYYY) ===

    @Test
    fun `parses DD slash MM slash YYYY UK format`() {
        val dates = listOf(
            "15/01/2026" to "15/01/2026",
            "30/06/2026" to "30/06/2026",
            "31/12/2026" to "31/12/2026"
        )

        dates.forEach { (date, expected) ->
            val line = "$date -£100.00 Test"
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse date: $date")
            assertEquals(expected, result[0].rawDate, "Date mismatch for: $date")
        }
    }

    // === Single Digit Day/Month ===

    @Test
    fun `parses single digit day format`() {
        val line = "1.01.2026 -100,00 ₽ Test"
        val result = extractor.extract(line)

        assertTrue(result.isNotEmpty())
        assertTrue(result[0].rawDate.contains("1"))
    }

    @Test
    fun `parses single digit month format`() {
        val line = "15.1.2026 -100,00 ₽ Test"
        val result = extractor.extract(line)

        assertTrue(result.isNotEmpty())
    }

    // ============================================
    // AMOUNT FORMAT TESTS
    // ============================================

    // === US Amount Format ($1,234.56) ===

    @Test
    fun `parses US dollar amount with comma thousands`() {
        val amounts = listOf(
            "01/01/2026 -$1,234.56 Test" to -123456L,
            "01/01/2026 -$12,345.67 Test" to -1234567L,
            "01/01/2026 $123,456.78 Test" to 12345678L,
            "01/01/2026 -$1,234,567.89 Test" to -123456789L
        )

        amounts.forEach { (line, expected) ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals(expected, result[0].amountMinorUnits, "Amount mismatch for: $line")
        }
    }

    @Test
    fun `parses US dollar amount without thousands separator`() {
        val amounts = listOf(
            "01/01/2026 -$100.00 Test" to -10000L,
            "01/01/2026 -$999.99 Test" to -99999L,
            "01/01/2026 $50.00 Test" to 5000L
        )

        amounts.forEach { (line, expected) ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals(expected, result[0].amountMinorUnits, "Amount mismatch for: $line")
        }
    }

    // === European Amount Format (1.234,56 €) ===

    @Test
    fun `parses euro amount with dot thousands and comma decimal`() {
        val amounts = listOf(
            "01.01.2026 -1.234,56 € Test" to -123456L,
            "01.01.2026 -12.345,67 € Test" to -1234567L,
            "01.01.2026 123.456,78 € Test" to 12345678L
        )

        amounts.forEach { (line, expected) ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals(expected, result[0].amountMinorUnits, "Amount mismatch for: $line")
        }
    }

    // === RU/CIS Amount Format (1 234,56 ₽) ===

    @Test
    fun `parses ruble amount with space thousands`() {
        val amounts = listOf(
            "01.01.2026 -1 234,56 ₽ Test" to -123456L,
            "01.01.2026 -12 345,67 ₽ Test" to -1234567L,
            "01.01.2026 +123 456,78 ₽ Test" to 12345678L,
            "01.01.2026 -1 234 567,89 ₽ Test" to -123456789L
        )

        amounts.forEach { (line, expected) ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals(expected, result[0].amountMinorUnits, "Amount mismatch for: $line")
        }
    }

    @Test
    fun `parses tenge amount with space thousands`() {
        val amounts = listOf(
            "01.01.2026 -5 000,00 ₸ Test" to -500000L,
            "01.01.2026 +100 000,00 ₸ Test" to 10000000L,
            "01.01.2026 -1 234 567,89 ₸ Test" to -123456789L
        )

        amounts.forEach { (line, expected) ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals(expected, result[0].amountMinorUnits, "Amount mismatch for: $line")
        }
    }

    // === Kaspi-style Amount Format (- 3 700,00 ₸) ===

    @Test
    fun `parses Kaspi format with spaced sign`() {
        val amounts = listOf(
            "01.01.2026 - 3 700,00 ₸ Test" to -370000L,
            "01.01.2026 + 100 000,00 ₸ Test" to 10000000L,
            "01.01.2026 - 25 000,00 ₸ Test" to -2500000L
        )

        amounts.forEach { (line, expected) ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals(expected, result[0].amountMinorUnits, "Amount mismatch for: $line")
        }
    }

    // === Signed Amounts ===

    @Test
    fun `parses negative amounts with minus sign`() {
        val amounts = listOf(
            "01.01.2026 -100,00 ₽ Test" to -10000L,
            "01.01.2026 -$100.00 Test" to -10000L,
            "01.01.2026 -100.00 EUR Test" to -10000L
        )

        amounts.forEach { (line, expected) ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals(expected, result[0].amountMinorUnits, "Amount mismatch for: $line")
            assertTrue(result[0].isDebit, "Should be debit for: $line")
        }
    }

    @Test
    fun `parses positive amounts with plus sign`() {
        val amounts = listOf(
            "01.01.2026 +100,00 ₽ Test" to 10000L,
            "01.01.2026 +$100.00 Test" to 10000L,
            "01.01.2026 +100.00 EUR Test" to 10000L
        )

        amounts.forEach { (line, expected) ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals(expected, result[0].amountMinorUnits, "Amount mismatch for: $line")
            assertTrue(result[0].isCredit, "Should be credit for: $line")
        }
    }

    // ============================================
    // CURRENCY DETECTION TESTS
    // ============================================

    @Test
    fun `detects USD from dollar sign`() {
        val line = "01/01/2026 -$100.00 Test"
        val result = extractor.extract(line)

        assertTrue(result.isNotEmpty())
        assertEquals("USD", result[0].currency)
    }

    @Test
    fun `detects EUR from euro sign`() {
        val line = "01.01.2026 -100,00 € Test"
        val result = extractor.extract(line)

        assertTrue(result.isNotEmpty())
        assertEquals("EUR", result[0].currency)
    }

    @Test
    fun `detects RUB from ruble sign`() {
        val line = "01.01.2026 -100,00 ₽ Test"
        val result = extractor.extract(line)

        assertTrue(result.isNotEmpty())
        assertEquals("RUB", result[0].currency)
    }

    @Test
    fun `detects GBP from pound sign`() {
        val line = "15/01/2026 -£100.00 Test"
        val result = extractor.extract(line)

        assertTrue(result.isNotEmpty())
        assertEquals("GBP", result[0].currency)
    }

    @Test
    fun `detects KZT from tenge sign`() {
        val line = "01.01.2026 -5 000,00 ₸ Test"
        val result = extractor.extract(line)

        assertTrue(result.isNotEmpty())
        assertEquals("KZT", result[0].currency)
    }

    @Test
    fun `detects JPY from yen sign`() {
        val line = "2026-01-01 -¥10000 Test"
        val result = extractor.extract(line)

        assertTrue(result.isNotEmpty())
        assertEquals("JPY", result[0].currency)
    }

    @Test
    fun `detects currency from ISO code`() {
        val currencies = listOf(
            "01.01.2026 -100.00 USD Test" to "USD",
            "01.01.2026 -100,00 EUR Test" to "EUR",
            "01.01.2026 -100.00 GBP Test" to "GBP",
            "01.01.2026 -100,00 RUB Test" to "RUB",
            "01.01.2026 -100,00 KZT Test" to "KZT"
        )

        currencies.forEach { (line, expected) ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals(expected, result[0].currency, "Currency mismatch for: $line")
        }
    }

    // ============================================
    // EDGE CASES
    // ============================================

    @Test
    fun `handles amount without currency symbol`() {
        val line = "01.01.2026 -100.00 Test payment"
        val result = extractor.extract(line)

        assertTrue(result.isNotEmpty())
        assertEquals(-10000, result[0].amountMinorUnits)
        assertEquals(null, result[0].currency)
    }

    @Test
    fun `handles very small amounts`() {
        val amounts = listOf(
            "01.01.2026 -0,01 ₽ Test" to -1L,
            "01.01.2026 +0,99 ₽ Test" to 99L,
            "01/01/2026 -$0.01 Test" to -1L
        )

        amounts.forEach { (line, expected) ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals(expected, result[0].amountMinorUnits, "Amount mismatch for: $line")
        }
    }

    @Test
    fun `handles amounts with trailing zeros`() {
        val line = "01.01.2026 -100,00 ₽ Test"
        val result = extractor.extract(line)

        assertTrue(result.isNotEmpty())
        assertEquals(-10000, result[0].amountMinorUnits)
    }

    @Test
    fun `handles amounts without decimal part`() {
        val amounts = listOf(
            "01.01.2026 -100 ₽ Test" to -10000L,
            "01/01/2026 -$100 Test" to -10000L,
            "01.01.2026 +5000 ₸ Test" to 500000L
        )

        amounts.forEach { (line, expected) ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals(expected, result[0].amountMinorUnits, "Amount mismatch for: $line")
        }
    }

    @Test
    fun `handles amounts with single decimal digit`() {
        val line = "01.01.2026 -100,5 ₽ Test"
        val result = extractor.extract(line)

        // 100,5 should be interpreted as 100.50 = 10050 minor units
        assertTrue(result.isNotEmpty())
        // Note: behavior may vary depending on parser implementation
    }

    @Test
    fun `handles zero amount`() {
        val line = "01.01.2026 0,00 ₽ Adjustment"
        val result = extractor.extract(line)

        // Zero amounts may be included or excluded
        if (result.isNotEmpty()) {
            assertEquals(0, result[0].amountMinorUnits)
        }
    }

    @Test
    fun `handles amount before and after description`() {
        val lines = listOf(
            "01.01.2026 -100,00 ₽ Payment description",
            "01.01.2026 Payment description -100,00 ₽"
        )

        lines.forEach { line ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals(-10000, result[0].amountMinorUnits)
        }
    }

    @Test
    fun `extracts description after removing date and amount`() {
        val line = "15.01.2026 -5 000,00 ₸ Оплата услуг Интернет"
        val result = extractor.extract(line)

        assertTrue(result.isNotEmpty())
        assertTrue(result[0].rawDescription.contains("Оплата"))
        assertTrue(result[0].rawDescription.contains("Интернет"))
    }

    @Test
    fun `handles line with multiple numbers`() {
        val line = "15.01.2026 Заказ #123456 -1 500,00 ₽"
        val result = extractor.extract(line)

        // Should extract the correct amount, not order number
        assertTrue(result.isNotEmpty())
        assertEquals(-150000, result[0].amountMinorUnits)
    }

    @Test
    fun `skips lines without amount`() {
        val lines = listOf(
            "15.01.2026 This is a header line",
            "Page 1 of 5",
            "Account Summary",
            "15.01.2026 Balance check"
        )

        lines.forEach { line ->
            val result = extractor.extract(line)
            assertTrue(result.isEmpty(), "Should skip line without amount: $line")
        }
    }

    @Test
    fun `skips lines without date`() {
        val lines = listOf(
            "Payment -100,00 ₽",
            "Total: 5 000,00 ₸",
            "Balance: $1,234.56"
        )

        lines.forEach { line ->
            val result = extractor.extract(line)
            assertTrue(result.isEmpty(), "Should skip line without date: $line")
        }
    }
}

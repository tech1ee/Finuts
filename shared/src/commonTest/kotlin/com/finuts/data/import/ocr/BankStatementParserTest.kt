package com.finuts.data.import.ocr

import com.finuts.domain.entity.import.ImportSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BankStatementParserTest {

    private val parser = BankStatementParser()

    // === Basic Parsing ===

    @Test
    fun `parses simple date-description-amount line`() {
        val text = "15.01.2026 Вкусно и точка -2500.00"
        val transactions = parser.parseText(text, null)

        assertEquals(1, transactions.size)
        assertEquals(-250000L, transactions[0].amount)
        assertTrue(transactions[0].description.contains("Вкусно"))
    }

    @Test
    fun `parses multiple transactions from text`() {
        val text = """
            15.01.2026 Вкусно и точка -2500.00
            16.01.2026 Kaspi Перевод +5000.00
            17.01.2026 Магнит -1234.56
        """.trimIndent()

        val transactions = parser.parseText(text, null)
        assertEquals(3, transactions.size)
    }

    @Test
    fun `returns empty list for unparseable text`() {
        val text = "Random text without transactions"
        val transactions = parser.parseText(text, null)
        assertTrue(transactions.isEmpty())
    }

    // === Date Formats ===

    @Test
    fun `parses EU date format DD_MM_YYYY`() {
        val text = "15.01.2026 Покупка -100.00"
        val transactions = parser.parseText(text, null)

        assertEquals(1, transactions.size)
        assertEquals(15, transactions[0].date.dayOfMonth)
        assertEquals(1, transactions[0].date.monthNumber)
        assertEquals(2026, transactions[0].date.year)
    }

    @Test
    fun `parses ISO date format YYYY-MM-DD`() {
        val text = "2026-01-15 Покупка -100.00"
        val transactions = parser.parseText(text, null)

        assertEquals(1, transactions.size)
        assertEquals(15, transactions[0].date.dayOfMonth)
    }

    @Test
    fun `parses date with slash separator`() {
        val text = "15/01/2026 Покупка -100.00"
        val transactions = parser.parseText(text, null)

        assertEquals(1, transactions.size)
        assertEquals(15, transactions[0].date.dayOfMonth)
    }

    // === Amount Formats ===

    @Test
    fun `parses negative amount with minus sign`() {
        val text = "15.01.2026 Покупка -2500.00"
        val transactions = parser.parseText(text, null)

        assertEquals(-250000L, transactions[0].amount)
    }

    @Test
    fun `parses positive amount with plus sign`() {
        val text = "15.01.2026 Зарплата +50000.00"
        val transactions = parser.parseText(text, null)

        assertEquals(5000000L, transactions[0].amount)
    }

    @Test
    fun `parses amount without sign`() {
        val text = "15.01.2026 Покупка 2500.00"
        val transactions = parser.parseText(text, null)

        assertEquals(250000L, transactions[0].amount)
    }

    @Test
    fun `parses Russian number format with comma as decimal`() {
        val text = "15.01.2026 Покупка -2 500,50"
        val transactions = parser.parseText(text, null)

        assertEquals(1, transactions.size)
        assertEquals(-250050L, transactions[0].amount)
    }

    @Test
    fun `parses amount with currency symbol`() {
        val text = "15.01.2026 Покупка -2500.00 ₸"
        val transactions = parser.parseText(text, null)

        assertEquals(-250000L, transactions[0].amount)
    }

    // === Description Extraction ===

    @Test
    fun `extracts description between date and amount`() {
        val text = "15.01.2026 Покупка в магазине Магнит -500.00"
        val transactions = parser.parseText(text, null)

        assertEquals("Покупка в магазине Магнит", transactions[0].description)
    }

    @Test
    fun `handles cyrillic characters in description`() {
        val text = "15.01.2026 Ресторан Пушкин -10000.00"
        val transactions = parser.parseText(text, null)

        assertTrue(transactions[0].description.contains("Ресторан"))
        assertTrue(transactions[0].description.contains("Пушкин"))
    }

    // === Merchant Extraction ===

    @Test
    fun `extracts merchant from description`() {
        val text = "15.01.2026 Магнит | Покупка продуктов -500.00"
        val transactions = parser.parseText(text, null)

        assertNotNull(transactions[0].merchant)
        assertEquals("Магнит", transactions[0].merchant)
    }

    // === Source and Confidence ===

    @Test
    fun `sets ImportSource to DOCUMENT_AI`() {
        val text = "15.01.2026 Покупка -100.00"
        val transactions = parser.parseText(text, null)

        assertEquals(ImportSource.DOCUMENT_AI, transactions[0].source)
    }

    @Test
    fun `sets confidence based on match quality`() {
        val text = "15.01.2026 Покупка -100.00"
        val transactions = parser.parseText(text, null)

        assertTrue(transactions[0].confidence > 0f)
        assertTrue(transactions[0].confidence <= 1f)
    }

    // === Edge Cases ===

    @Test
    fun `skips empty lines`() {
        val text = """
            15.01.2026 Покупка -100.00

            16.01.2026 Перевод +200.00
        """.trimIndent()

        val transactions = parser.parseText(text, null)
        assertEquals(2, transactions.size)
    }

    @Test
    fun `handles multiline description`() {
        val text = "15.01.2026 Покупка в интернет магазине -500.00"
        val transactions = parser.parseText(text, null)

        assertEquals(1, transactions.size)
        assertTrue(transactions[0].description.isNotBlank())
    }

    @Test
    fun `handles page break markers`() {
        val text = """
            15.01.2026 Покупка -100.00
            --- Page Break ---
            16.01.2026 Перевод +200.00
        """.trimIndent()

        val transactions = parser.parseText(text, null)
        assertEquals(2, transactions.size)
    }

    @Test
    fun `parses transaction with balance after amount`() {
        val text = "15.01.2026 Покупка -100.00 Остаток: 5000.00"
        val transactions = parser.parseText(text, null)

        assertEquals(1, transactions.size)
        assertEquals(-10000L, transactions[0].amount)
    }

    // === Bank-Specific Patterns ===

    @Test
    fun `parses Kaspi-style statement line`() {
        val text = "15 января 2026 Kaspi Gold | Перевод -5000.00 ₸"
        val transactions = parser.parseText(text, "kaspi")

        assertEquals(1, transactions.size)
        assertTrue(transactions[0].description.contains("Kaspi") ||
            transactions[0].description.contains("Перевод"))
    }
}

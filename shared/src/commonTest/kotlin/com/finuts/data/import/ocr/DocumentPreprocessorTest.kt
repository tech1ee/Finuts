package com.finuts.data.import.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocumentPreprocessorTest {

    private val preprocessor = DocumentPreprocessor()

    // === Token Reduction Tests ===

    @Test
    fun `keeps lines with dates`() {
        val text = """
            Header text
            15.01.2026 Payment to store
            Some unrelated text
            2026-01-15 Another transaction
        """.trimIndent()

        val result = preprocessor.process(text)

        assertTrue(result.cleanedText.contains("15.01.2026"))
        assertTrue(result.cleanedText.contains("2026-01-15"))
    }

    @Test
    fun `keeps lines with amounts`() {
        val text = """
            Company info
            Transaction -5000 KZT
            Address line
            Payment 12,500.00 USD
        """.trimIndent()

        val result = preprocessor.process(text)

        assertTrue(result.cleanedText.contains("-5000"))
        assertTrue(result.cleanedText.contains("12,500.00"))
    }

    @Test
    fun `removes page numbers`() {
        val text = """
            15.01.2026 Payment 1000
            Page 1 of 3
            16.01.2026 Payment 2000
            - 2 -
            17.01.2026 Payment 3000
        """.trimIndent()

        val result = preprocessor.process(text)

        assertTrue(result.cleanedText.contains("15.01.2026"))
        assertTrue(result.cleanedText.contains("16.01.2026"))
        assertTrue(result.cleanedText.contains("17.01.2026"))
        assertTrue(!result.cleanedText.contains("Page 1 of 3"))
        assertTrue(!result.cleanedText.contains("- 2 -"))
    }

    @Test
    fun `removes headers and footers`() {
        val text = """
            KASPI BANK Statement
            Period: 01.01.2026 - 31.01.2026
            15.01.2026 -5000 Payment
            16.01.2026 +1000 Income
            Generated on 01.02.2026
            Confidential
        """.trimIndent()

        val result = preprocessor.process(text)

        assertTrue(result.cleanedText.contains("15.01.2026"))
        assertTrue(result.cleanedText.contains("16.01.2026"))
    }

    @Test
    fun `reduces token count by at least 30 percent`() {
        val text = """
            KASPI BANK
            Customer Statement
            Account: 1234567890
            Period: 01.01.2026 - 31.01.2026

            Date        Description                 Amount      Balance
            ----        -----------                 ------      -------
            15.01.2026  Grocery shopping            -5000       95000
            16.01.2026  Salary deposit              +100000     195000
            17.01.2026  Utility payment             -15000      180000

            Total transactions: 3
            Closing balance: 180000 KZT

            Thank you for using Kaspi Bank!
            Page 1 of 1
        """.trimIndent()

        val result = preprocessor.process(text)
        val originalLength = text.length
        val cleanedLength = result.cleanedText.length

        assertTrue(cleanedLength < originalLength * 0.7,
            "Expected at least 30% reduction, got ${100 - (cleanedLength * 100 / originalLength)}%")
    }

    // === Document Type Detection ===

    @Test
    fun `detects bank statement type`() {
        val text = """
            Bank Statement
            Account: 1234567890
            15.01.2026 -5000 Payment
        """.trimIndent()

        val result = preprocessor.process(text)

        assertEquals(DocumentType.BANK_STATEMENT, result.hints.type)
    }

    @Test
    fun `detects receipt type`() {
        val text = """
            Магазин "Продукты"
            ИИН: 123456789012
            Чек №12345
            Хлеб      150
            Молоко    450
            ИТОГО:    600
        """.trimIndent()

        val result = preprocessor.process(text)

        assertEquals(DocumentType.RECEIPT, result.hints.type)
    }

    @Test
    fun `detects invoice type`() {
        val text = """
            INVOICE
            Invoice #INV-2026-001
            Bill to: Customer LLC
            Amount due: 50000 KZT
        """.trimIndent()

        val result = preprocessor.process(text)

        assertEquals(DocumentType.INVOICE, result.hints.type)
    }

    // === Language Detection ===

    @Test
    fun `detects Russian language`() {
        val text = """
            Выписка по счету
            15.01.2026 Перевод Иванову -5000
        """.trimIndent()

        val result = preprocessor.process(text)

        assertEquals("ru", result.hints.language)
    }

    @Test
    fun `detects Kazakh language`() {
        val text = """
            Шот бойынша үзінді
            15.01.2026 Аударым -5000
        """.trimIndent()

        val result = preprocessor.process(text)

        assertEquals("kk", result.hints.language)
    }

    @Test
    fun `detects English language`() {
        val text = """
            Bank Statement
            15.01.2026 Transfer to John Smith -5000
        """.trimIndent()

        val result = preprocessor.process(text)

        assertEquals("en", result.hints.language)
    }

    // === Edge Cases ===

    @Test
    fun `handles empty text`() {
        val result = preprocessor.process("")

        assertEquals("", result.cleanedText)
        assertEquals(DocumentType.UNKNOWN, result.hints.type)
    }

    @Test
    fun `handles text without transactions`() {
        val text = """
            Company header
            Some random text
            Another line
        """.trimIndent()

        val result = preprocessor.process(text)

        assertEquals(DocumentType.UNKNOWN, result.hints.type)
    }

    @Test
    fun `preserves transaction order`() {
        val text = """
            15.01.2026 First transaction
            16.01.2026 Second transaction
            17.01.2026 Third transaction
        """.trimIndent()

        val result = preprocessor.process(text)
        val lines = result.cleanedText.lines()

        assertTrue(lines[0].contains("First"))
        assertTrue(lines[1].contains("Second"))
        assertTrue(lines[2].contains("Third"))
    }

    // === Real Bank Statement Samples ===

    @Test
    fun `preprocesses Kaspi statement format`() {
        val text = """
            KASPI BANK
            Выписка по карте *1234
            За период: 01.01.2026 - 31.01.2026

            07.01.26 - 3 700,00 ₸ Перевод Shukhrat S.
            08.01.26 + 50 000,00 ₸ Пополнение наличными
            09.01.26 - 1 500,00 ₸ Glovo

            Остаток: 45 800,00 ₸
            Дата формирования: 01.02.2026
        """.trimIndent()

        val result = preprocessor.process(text)

        assertTrue(result.cleanedText.contains("07.01.26"))
        assertTrue(result.cleanedText.contains("08.01.26"))
        assertTrue(result.cleanedText.contains("09.01.26"))
        assertEquals("ru", result.hints.language)
    }
}

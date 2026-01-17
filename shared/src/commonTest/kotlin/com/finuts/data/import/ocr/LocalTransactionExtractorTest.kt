package com.finuts.data.import.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalTransactionExtractorTest {

    private val extractor = LocalTransactionExtractor()

    // === Date Extraction ===

    @Test
    fun `extracts date in DD_MM_YYYY format`() {
        val text = "15.01.2026 Payment to store -5000"
        val transactions = extractor.extract(text)

        assertEquals(1, transactions.size)
        assertEquals("15.01.2026", transactions[0].rawDate)
    }

    @Test
    fun `extracts date in DD_MM_YY format`() {
        val text = "07.01.26 - 3 700,00 ₸ Перевод"
        val transactions = extractor.extract(text)

        assertEquals(1, transactions.size)
        assertEquals("07.01.26", transactions[0].rawDate)
    }

    @Test
    fun `extracts date in YYYY-MM-DD format`() {
        val text = "2026-01-15 Transfer -10000"
        val transactions = extractor.extract(text)

        assertEquals(1, transactions.size)
        assertEquals("2026-01-15", transactions[0].rawDate)
    }

    @Test
    fun `extracts date in DD slash MM slash YYYY format`() {
        val text = "15/01/2026 Salary +100000"
        val transactions = extractor.extract(text)

        assertEquals(1, transactions.size)
        assertEquals("15/01/2026", transactions[0].rawDate)
    }

    // === Amount Extraction ===

    @Test
    fun `extracts positive amount with plus sign`() {
        val text = "15.01.2026 +50000 Income"
        val transactions = extractor.extract(text)

        assertEquals(1, transactions.size)
        assertEquals(5000000, transactions[0].amountMinorUnits) // 50000 * 100
        assertTrue(transactions[0].isCredit)
    }

    @Test
    fun `extracts negative amount with minus sign`() {
        val text = "15.01.2026 -5000 Payment"
        val transactions = extractor.extract(text)

        assertEquals(1, transactions.size)
        assertEquals(-500000, transactions[0].amountMinorUnits) // -5000 * 100
        assertTrue(transactions[0].isDebit)
    }

    @Test
    fun `extracts amount with decimal`() {
        val text = "15.01.2026 -1234.56 Payment"
        val transactions = extractor.extract(text)

        assertEquals(1, transactions.size)
        assertEquals(-123456, transactions[0].amountMinorUnits)
    }

    @Test
    fun `extracts amount with comma decimal separator`() {
        val text = "15.01.2026 -3 700,00 ₸ Перевод"
        val transactions = extractor.extract(text)

        assertEquals(1, transactions.size)
        assertEquals(-370000, transactions[0].amountMinorUnits)
    }

    @Test
    fun `extracts amount with thousand separator`() {
        val text = "15.01.2026 -1 000 000 KZT Payment"
        val transactions = extractor.extract(text)

        assertEquals(1, transactions.size)
        assertEquals(-100000000, transactions[0].amountMinorUnits) // 1000000 * 100
    }

    // === Currency Detection ===

    @Test
    fun `detects KZT currency from symbol`() {
        val text = "15.01.2026 -5000 ₸ Payment"
        val transactions = extractor.extract(text)

        assertEquals("KZT", transactions[0].currency)
    }

    @Test
    fun `detects USD currency from symbol`() {
        val text = "15.01.2026 $100 Payment"
        val transactions = extractor.extract(text)

        assertEquals("USD", transactions[0].currency)
    }

    @Test
    fun `detects EUR currency from symbol`() {
        val text = "15.01.2026 €500 Payment"
        val transactions = extractor.extract(text)

        assertEquals("EUR", transactions[0].currency)
    }

    @Test
    fun `detects RUB currency from symbol`() {
        val text = "15.01.2026 -5000₽ Payment"
        val transactions = extractor.extract(text)

        assertEquals("RUB", transactions[0].currency)
    }

    @Test
    fun `detects currency from ISO code`() {
        val text = "15.01.2026 -5000 KZT Payment"
        val transactions = extractor.extract(text)

        assertEquals("KZT", transactions[0].currency)
    }

    // === Raw Description ===

    @Test
    fun `captures raw description`() {
        val text = "15.01.2026 -5000 Payment to grocery store"
        val transactions = extractor.extract(text)

        assertEquals("Payment to grocery store", transactions[0].rawDescription)
    }

    @Test
    fun `captures description with Cyrillic`() {
        val text = "07.01.26 - 3 700,00 ₸ Перевод Shukhrat S."
        val transactions = extractor.extract(text)

        assertTrue(transactions[0].rawDescription.contains("Перевод"))
    }

    // === Multiple Transactions ===

    @Test
    fun `extracts multiple transactions`() {
        val text = """
            15.01.2026 -5000 Payment one
            16.01.2026 +10000 Income
            17.01.2026 -2500 Payment two
        """.trimIndent()

        val transactions = extractor.extract(text)

        assertEquals(3, transactions.size)
        assertEquals(-500000, transactions[0].amountMinorUnits)
        assertEquals(1000000, transactions[1].amountMinorUnits)
        assertEquals(-250000, transactions[2].amountMinorUnits)
    }

    @Test
    fun `preserves transaction order`() {
        val text = """
            15.01.2026 -1000 First
            16.01.2026 -2000 Second
            17.01.2026 -3000 Third
        """.trimIndent()

        val transactions = extractor.extract(text)

        assertTrue(transactions[0].rawDescription.contains("First"))
        assertTrue(transactions[1].rawDescription.contains("Second"))
        assertTrue(transactions[2].rawDescription.contains("Third"))
    }

    // === Edge Cases ===

    @Test
    fun `handles empty text`() {
        val transactions = extractor.extract("")

        assertTrue(transactions.isEmpty())
    }

    @Test
    fun `handles text without transactions`() {
        val text = "Some random text without dates or amounts"
        val transactions = extractor.extract(text)

        assertTrue(transactions.isEmpty())
    }

    @Test
    fun `skips lines without amounts`() {
        val text = """
            15.01.2026 Balance: N/A
            16.01.2026 -5000 Real transaction
        """.trimIndent()

        val transactions = extractor.extract(text)

        assertEquals(1, transactions.size)
        assertEquals(-500000, transactions[0].amountMinorUnits)
    }

    // === Real Bank Statement Samples ===

    @Test
    fun `parses Kaspi bank format`() {
        val text = """
            07.01.26 - 3 700,00 ₸ Перевод Shukhrat S.
            08.01.26 + 50 000,00 ₸ Пополнение наличными
            09.01.26 - 1 500,00 ₸ Glovo
        """.trimIndent()

        val transactions = extractor.extract(text)

        assertEquals(3, transactions.size)
        assertEquals(-370000, transactions[0].amountMinorUnits)
        assertEquals(5000000, transactions[1].amountMinorUnits)
        assertEquals(-150000, transactions[2].amountMinorUnits)

        transactions.forEach { tx ->
            assertEquals("KZT", tx.currency)
        }
    }

    @Test
    fun `parses mixed currency statement`() {
        val text = """
            15.01.2026 -5000 KZT Оплата
            16.01.2026 $100 USD Payment
            17.01.2026 €50 EUR Transfer
        """.trimIndent()

        val transactions = extractor.extract(text)

        assertEquals(3, transactions.size)
        assertEquals("KZT", transactions[0].currency)
        assertEquals("USD", transactions[1].currency)
        assertEquals("EUR", transactions[2].currency)
    }

    @Test
    fun `parses RU amount with space thousands and currency symbol`() {
        // Test regex patterns directly
        val ruPattern = Regex("-?\\d{1,3}(\\s\\d{3})+(,\\d{2})?\\s*[₽₸]")
        val currencyPattern = Regex("[₸€₽£¥\$]\\s*[\\d,]+(?:[.]\\d{1,2})?|[\\d,.]+\\s*[₸€₽£¥\$]")

        val input = "45 990,00 ₸"

        val ruMatch = ruPattern.find(input)
        val currMatch = currencyPattern.find(input)

        // Debug: verify RU pattern matches correctly
        assertTrue(ruMatch != null, "RU pattern should match '$input'")
        assertEquals("45 990,00 ₸", ruMatch?.value, "RU match should be the entire amount")

        // Check what currency pattern matches
        if (currMatch != null) {
            assertTrue(
                ruMatch != null && ruMatch.range.first <= currMatch.range.first,
                "RU pattern should match before currency pattern. RU='${ruMatch?.value}' at ${ruMatch?.range}, Currency='${currMatch.value}' at ${currMatch.range}"
            )
        }

        // Now test through extractor
        val transactions = extractor.extract("15.01.2026\n$input")

        assertEquals(1, transactions.size, "Should extract 1 transaction")
        assertEquals(-4599000, transactions[0].amountMinorUnits) // 45990.00 * 100, negative in receipt mode
        assertEquals("KZT", transactions[0].currency)
    }

    @Test
    fun `parses Kaspi QR receipt amount`() {
        // Exact format from Kaspi QR receipt
        val text = """
            15.01.2026 12:45
            Сумма: 45 990,00 ₸
        """.trimIndent()

        val transactions = extractor.extract(text)

        assertEquals(1, transactions.size)
        assertEquals(-4599000, transactions[0].amountMinorUnits)
    }
}

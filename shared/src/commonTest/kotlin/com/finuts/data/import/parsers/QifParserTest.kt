package com.finuts.data.import.parsers

import com.finuts.domain.entity.import.DocumentType
import com.finuts.domain.entity.import.ImportResult
import com.finuts.domain.entity.import.ImportSource
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for QifParser - QIF (Quicken Interchange Format) parsing.
 */
class QifParserTest {

    private val parser = QifParser()

    @Test
    fun `parse simple QIF with Bank type`() {
        val qif = """
            !Type:Bank
            D01/15/2024
            T-100.00
            PTest Merchant
            MTest memo
            ^
        """.trimIndent()

        val result = parser.parse(qif, DocumentType.Qif("Bank"))

        assertIs<ImportResult.Success>(result)
        assertEquals(1, result.transactions.size)
    }

    @Test
    fun `parse QIF with CCard type`() {
        val qif = """
            !Type:CCard
            D01/15/2024
            T-50.00
            PCredit card purchase
            ^
        """.trimIndent()

        val result = parser.parse(qif, DocumentType.Qif("CCard"))

        assertIs<ImportResult.Success>(result)
        assertEquals(1, result.transactions.size)
    }

    @Test
    fun `parse date in US format`() {
        val qif = """
            !Type:Bank
            D01/15/2024
            T-100.00
            PTest
            ^
        """.trimIndent()

        val result = parser.parse(qif, DocumentType.Qif("Bank"))

        assertIs<ImportResult.Success>(result)
        assertEquals(LocalDate(2024, 1, 15), result.transactions[0].date)
    }

    @Test
    fun `parse date in EU format with hint`() {
        val qif = """
            !Type:Bank
            D15/01/2024
            T-100.00
            PTest
            ^
        """.trimIndent()

        val result = parser.parse(qif, DocumentType.Qif("Bank"))

        assertIs<ImportResult.Success>(result)
        // Should parse as January 15, 2024
        assertEquals(LocalDate(2024, 1, 15), result.transactions[0].date)
    }

    @Test
    fun `parse negative amount (expense)`() {
        val qif = """
            !Type:Bank
            D01/15/2024
            T-500.00
            PExpense
            ^
        """.trimIndent()

        val result = parser.parse(qif, DocumentType.Qif("Bank"))

        assertIs<ImportResult.Success>(result)
        assertEquals(-50000L, result.transactions[0].amount)
    }

    @Test
    fun `parse positive amount (income)`() {
        val qif = """
            !Type:Bank
            D01/15/2024
            T1000.00
            PIncome
            ^
        """.trimIndent()

        val result = parser.parse(qif, DocumentType.Qif("Bank"))

        assertIs<ImportResult.Success>(result)
        assertEquals(100000L, result.transactions[0].amount)
    }

    @Test
    fun `parse payee as merchant`() {
        val qif = """
            !Type:Bank
            D01/15/2024
            T-50.00
            PAmazon
            ^
        """.trimIndent()

        val result = parser.parse(qif, DocumentType.Qif("Bank"))

        assertIs<ImportResult.Success>(result)
        assertEquals("Amazon", result.transactions[0].merchant)
    }

    @Test
    fun `parse memo as description`() {
        val qif = """
            !Type:Bank
            D01/15/2024
            T-50.00
            PVendor
            MMonthly subscription
            ^
        """.trimIndent()

        val result = parser.parse(qif, DocumentType.Qif("Bank"))

        assertIs<ImportResult.Success>(result)
        assertEquals("Monthly subscription", result.transactions[0].description)
    }

    @Test
    fun `parse multiple transactions`() {
        val qif = """
            !Type:Bank
            D01/15/2024
            T-100.00
            PMerchant 1
            ^
            D01/16/2024
            T200.00
            PMerchant 2
            ^
            D01/17/2024
            T-50.00
            PMerchant 3
            ^
        """.trimIndent()

        val result = parser.parse(qif, DocumentType.Qif("Bank"))

        assertIs<ImportResult.Success>(result)
        assertEquals(3, result.transactions.size)
    }

    @Test
    fun `parse amount with comma decimal`() {
        val qif = """
            !Type:Bank
            D01/15/2024
            T-100,50
            PTest
            ^
        """.trimIndent()

        val result = parser.parse(qif, DocumentType.Qif("Bank"))

        assertIs<ImportResult.Success>(result)
        assertEquals(-10050L, result.transactions[0].amount)
    }

    @Test
    fun `transactions have RULE_BASED source`() {
        val qif = """
            !Type:Bank
            D01/15/2024
            T-50.00
            PTest
            ^
        """.trimIndent()

        val result = parser.parse(qif, DocumentType.Qif("Bank"))

        assertIs<ImportResult.Success>(result)
        assertEquals(ImportSource.RULE_BASED, result.transactions[0].source)
    }

    @Test
    fun `transactions have high confidence`() {
        val qif = """
            !Type:Bank
            D01/15/2024
            T-50.00
            PTest
            ^
        """.trimIndent()

        val result = parser.parse(qif, DocumentType.Qif("Bank"))

        assertIs<ImportResult.Success>(result)
        assertTrue(result.transactions[0].confidence >= 0.9f)
    }

    @Test
    fun `return error for empty content`() {
        val result = parser.parse("", DocumentType.Qif("Bank"))

        assertIs<ImportResult.Error>(result)
    }

    @Test
    fun `return error for invalid QIF`() {
        val result = parser.parse("not valid qif", DocumentType.Qif("Bank"))

        assertIs<ImportResult.Error>(result)
    }

    @Test
    fun `skip transactions without required fields`() {
        val qif = """
            !Type:Bank
            D01/15/2024
            T-100.00
            PValid
            ^
            D01/16/2024
            PNo amount
            ^
            D01/17/2024
            T-50.00
            PAnother valid
            ^
        """.trimIndent()

        val result = parser.parse(qif, DocumentType.Qif("Bank"))

        assertIs<ImportResult.Success>(result)
        assertEquals(2, result.transactions.size)
    }
}

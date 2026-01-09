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
 * Tests for CsvParser - CSV parsing with column auto-detection.
 */
class CsvParserTest {

    private val parser = CsvParser()

    @Test
    fun `parse simple CSV with header`() {
        val csv = "Date,Amount,Description\n2024-01-15,1000.00,Test payment\n2024-01-16,-500.50,Another payment"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(2, result.transactions.size)
    }

    @Test
    fun `parse CSV with semicolon delimiter`() {
        val csv = "Date;Amount;Description\n2024-01-15;1000,00;Test payment"

        val result = parser.parse(csv, DocumentType.Csv(';', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(1, result.transactions.size)
    }

    @Test
    fun `parse CSV with tab delimiter`() {
        val csv = "Date\tAmount\tDescription\n2024-01-15\t1000.00\tTest"

        val result = parser.parse(csv, DocumentType.Csv('\t', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(1, result.transactions.size)
    }

    @Test
    fun `parse CSV with quoted fields containing comma`() {
        val csv = "Date,Amount,Description\n2024-01-15,1000.00,\"Payment, with comma\""

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals("Payment, with comma", result.transactions[0].description)
    }

    @Test
    fun `auto-detect date column by Russian name`() {
        val csv = "Дата,Сумма,Описание\n15.01.2024,1000.00,Test"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(LocalDate(2024, 1, 15), result.transactions[0].date)
    }

    @Test
    fun `auto-detect amount column by name`() {
        val csv = "Date,Sum,Description\n2024-01-15,1000.50,Test"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(100050L, result.transactions[0].amount)
    }

    @Test
    fun `detect balance column when present`() {
        val csv = "Date,Amount,Balance,Description\n2024-01-15,-100,900,Test"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(90000L, result.transactions[0].balance)
    }

    @Test
    fun `detect merchant column when present`() {
        val csv = "Date,Amount,Merchant,Description\n2024-01-15,-100,Amazon,Shopping"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals("Amazon", result.transactions[0].merchant)
    }

    @Test
    fun `parse positive amount`() {
        val csv = "Date,Amount,Description\n2024-01-15,1000.00,Income"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(100000L, result.transactions[0].amount)
    }

    @Test
    fun `parse negative amount with minus`() {
        val csv = "Date,Amount,Description\n2024-01-15,-500.00,Expense"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(-50000L, result.transactions[0].amount)
    }

    @Test
    fun `parse amount with comma as decimal`() {
        val csv = "Date;Amount;Description\n15.01.2024;1000,50;Test"

        val result = parser.parse(csv, DocumentType.Csv(';', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(100050L, result.transactions[0].amount)
    }

    @Test
    fun `parse amount with space as thousands separator`() {
        val csv = "Date;Amount;Description\n15.01.2024;1 000,50;Test"

        val result = parser.parse(csv, DocumentType.Csv(';', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(100050L, result.transactions[0].amount)
    }

    @Test
    fun `parse ISO date format`() {
        val csv = "Date,Amount,Description\n2024-01-15,100,Test"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(LocalDate(2024, 1, 15), result.transactions[0].date)
    }

    @Test
    fun `parse European date format`() {
        val csv = "Date,Amount,Description\n15.01.2024,100,Test"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(LocalDate(2024, 1, 15), result.transactions[0].date)
    }

    @Test
    fun `skip empty lines`() {
        val csv = "Date,Amount,Description\n2024-01-15,100,Test1\n\n2024-01-16,200,Test2"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(2, result.transactions.size)
    }

    @Test
    fun `handle trailing delimiter`() {
        val csv = "Date,Amount,Description,\n2024-01-15,100,Test,"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(1, result.transactions.size)
    }

    @Test
    fun `handle BOM in UTF-8`() {
        val bom = "\uFEFF"
        val csv = bom + "Date,Amount,Description\n2024-01-15,100,Test"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(1, result.transactions.size)
    }

    @Test
    fun `return error for empty content`() {
        val result = parser.parse("", DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Error>(result)
        assertTrue(result.message.isNotEmpty())
    }

    @Test
    fun `return error for header only`() {
        val csv = "Date,Amount,Description"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Error>(result)
    }

    @Test
    fun `return NeedsUserInput for unrecognized columns`() {
        val csv = "Col1,Col2,Col3\nValue1,Value2,Value3"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.NeedsUserInput>(result)
    }

    @Test
    fun `skip rows with invalid date`() {
        val csv = "Date,Amount,Description\n2024-01-15,100,Good row\ninvalid,200,Bad date\n2024-01-16,300,Another good row"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(2, result.transactions.size)
    }

    @Test
    fun `transactions have RULE_BASED source`() {
        val csv = "Date,Amount,Description\n2024-01-15,100,Test"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertEquals(ImportSource.RULE_BASED, result.transactions[0].source)
    }

    @Test
    fun `transactions have confidence score`() {
        val csv = "Date,Amount,Description\n2024-01-15,100,Test"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertTrue(result.transactions[0].confidence in 0.0f..1.0f)
    }

    @Test
    fun `result includes document type`() {
        val docType = DocumentType.Csv(',', "UTF-8")
        val csv = "Date,Amount,Description\n2024-01-15,100,Test"

        val result = parser.parse(csv, docType)

        assertIs<ImportResult.Success>(result)
        assertEquals(docType, result.documentType)
    }

    @Test
    fun `transactions store raw data`() {
        val csv = "Date,Amount,Description\n2024-01-15,100,Test"

        val result = parser.parse(csv, DocumentType.Csv(',', "UTF-8"))

        assertIs<ImportResult.Success>(result)
        assertTrue(result.transactions[0].rawData.isNotEmpty())
    }
}

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
 * Tests for OfxParser - OFX/QFX format parsing.
 */
class OfxParserTest {

    private val parser = OfxParser()

    @Test
    fun `parse OFX SGML format`() {
        val ofx = """
            OFXHEADER:100
            DATA:OFXSGML
            VERSION:102
            <OFX>
            <BANKMSGSRSV1>
            <STMTTRNRS>
            <STMTRS>
            <BANKTRANLIST>
            <STMTTRN>
            <TRNTYPE>DEBIT
            <DTPOSTED>20240115
            <TRNAMT>-100.00
            <NAME>Test Merchant
            <MEMO>Test payment
            </STMTTRN>
            </BANKTRANLIST>
            </STMTRS>
            </STMTTRNRS>
            </BANKMSGSRSV1>
            </OFX>
        """.trimIndent()

        val result = parser.parse(ofx, DocumentType.Ofx("1.0"))

        assertIs<ImportResult.Success>(result)
        assertEquals(1, result.transactions.size)
    }

    @Test
    fun `parse OFX XML format`() {
        val ofx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <?OFX OFXHEADER="200" VERSION="220"?>
            <OFX>
            <BANKMSGSRSV1>
            <STMTTRNRS>
            <STMTRS>
            <BANKTRANLIST>
            <STMTTRN>
            <TRNTYPE>DEBIT</TRNTYPE>
            <DTPOSTED>20240115</DTPOSTED>
            <TRNAMT>-100.00</TRNAMT>
            <NAME>Test Merchant</NAME>
            <MEMO>Test payment</MEMO>
            </STMTTRN>
            </BANKTRANLIST>
            </STMTRS>
            </STMTTRNRS>
            </BANKMSGSRSV1>
            </OFX>
        """.trimIndent()

        val result = parser.parse(ofx, DocumentType.Ofx("2.2"))

        assertIs<ImportResult.Success>(result)
        assertEquals(1, result.transactions.size)
    }

    @Test
    fun `parse transaction with DEBIT type`() {
        val ofx = createOfxWithTransaction("DEBIT", "-50.00")

        val result = parser.parse(ofx, DocumentType.Ofx("2.2"))

        assertIs<ImportResult.Success>(result)
        assertEquals(-5000L, result.transactions[0].amount)
    }

    @Test
    fun `parse transaction with CREDIT type`() {
        val ofx = createOfxWithTransaction("CREDIT", "100.00")

        val result = parser.parse(ofx, DocumentType.Ofx("2.2"))

        assertIs<ImportResult.Success>(result)
        assertEquals(10000L, result.transactions[0].amount)
    }

    @Test
    fun `parse transaction date YYYYMMDD format`() {
        val ofx = createOfxWithTransaction("DEBIT", "-50.00", date = "20240115")

        val result = parser.parse(ofx, DocumentType.Ofx("2.2"))

        assertIs<ImportResult.Success>(result)
        assertEquals(LocalDate(2024, 1, 15), result.transactions[0].date)
    }

    @Test
    fun `parse transaction date with time YYYYMMDDHHMMSS format`() {
        val ofx = createOfxWithTransaction("DEBIT", "-50.00", date = "20240115120000")

        val result = parser.parse(ofx, DocumentType.Ofx("2.2"))

        assertIs<ImportResult.Success>(result)
        assertEquals(LocalDate(2024, 1, 15), result.transactions[0].date)
    }

    @Test
    fun `parse transaction with NAME as merchant`() {
        val ofx = createOfxWithTransaction("DEBIT", "-50.00", name = "Amazon")

        val result = parser.parse(ofx, DocumentType.Ofx("2.2"))

        assertIs<ImportResult.Success>(result)
        assertEquals("Amazon", result.transactions[0].merchant)
    }

    @Test
    fun `parse transaction with MEMO as description`() {
        val ofx = createOfxWithTransaction("DEBIT", "-50.00", memo = "Monthly subscription")

        val result = parser.parse(ofx, DocumentType.Ofx("2.2"))

        assertIs<ImportResult.Success>(result)
        assertEquals("Monthly subscription", result.transactions[0].description)
    }

    @Test
    fun `parse multiple transactions`() {
        val ofx = """
            <OFX>
            <BANKMSGSRSV1>
            <STMTTRNRS>
            <STMTRS>
            <BANKTRANLIST>
            <STMTTRN>
            <TRNTYPE>DEBIT</TRNTYPE>
            <DTPOSTED>20240115</DTPOSTED>
            <TRNAMT>-100.00</TRNAMT>
            <NAME>Merchant 1</NAME>
            </STMTTRN>
            <STMTTRN>
            <TRNTYPE>CREDIT</TRNTYPE>
            <DTPOSTED>20240116</DTPOSTED>
            <TRNAMT>200.00</TRNAMT>
            <NAME>Merchant 2</NAME>
            </STMTTRN>
            </BANKTRANLIST>
            </STMTRS>
            </STMTTRNRS>
            </BANKMSGSRSV1>
            </OFX>
        """.trimIndent()

        val result = parser.parse(ofx, DocumentType.Ofx("2.2"))

        assertIs<ImportResult.Success>(result)
        assertEquals(2, result.transactions.size)
    }

    @Test
    fun `transactions have RULE_BASED source`() {
        val ofx = createOfxWithTransaction("DEBIT", "-50.00")

        val result = parser.parse(ofx, DocumentType.Ofx("2.2"))

        assertIs<ImportResult.Success>(result)
        assertEquals(ImportSource.RULE_BASED, result.transactions[0].source)
    }

    @Test
    fun `transactions have high confidence`() {
        val ofx = createOfxWithTransaction("DEBIT", "-50.00")

        val result = parser.parse(ofx, DocumentType.Ofx("2.2"))

        assertIs<ImportResult.Success>(result)
        assertTrue(result.transactions[0].confidence >= 0.9f)
    }

    @Test
    fun `return error for empty content`() {
        val result = parser.parse("", DocumentType.Ofx("2.2"))

        assertIs<ImportResult.Error>(result)
    }

    @Test
    fun `return error for invalid OFX`() {
        val result = parser.parse("not valid ofx", DocumentType.Ofx("2.2"))

        assertIs<ImportResult.Error>(result)
    }

    @Test
    fun `return error for OFX without transactions`() {
        val ofx = """
            <OFX>
            <BANKMSGSRSV1>
            <STMTTRNRS>
            <STMTRS>
            <BANKTRANLIST>
            </BANKTRANLIST>
            </STMTRS>
            </STMTTRNRS>
            </BANKMSGSRSV1>
            </OFX>
        """.trimIndent()

        val result = parser.parse(ofx, DocumentType.Ofx("2.2"))

        assertIs<ImportResult.Error>(result)
    }

    private fun createOfxWithTransaction(
        type: String,
        amount: String,
        date: String = "20240115",
        name: String = "Test Merchant",
        memo: String = "Test memo"
    ): String = """
        <OFX>
        <BANKMSGSRSV1>
        <STMTTRNRS>
        <STMTRS>
        <BANKTRANLIST>
        <STMTTRN>
        <TRNTYPE>$type</TRNTYPE>
        <DTPOSTED>$date</DTPOSTED>
        <TRNAMT>$amount</TRNAMT>
        <NAME>$name</NAME>
        <MEMO>$memo</MEMO>
        </STMTTRN>
        </BANKTRANLIST>
        </STMTRS>
        </STMTTRNRS>
        </BANKMSGSRSV1>
        </OFX>
    """.trimIndent()
}

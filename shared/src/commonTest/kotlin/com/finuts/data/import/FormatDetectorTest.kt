package com.finuts.data.import

import com.finuts.domain.entity.import.DocumentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests for FormatDetector - auto-detection of document types.
 */
class FormatDetectorTest {

    // ==================== Extension Detection Tests ====================

    @Test
    fun `detect CSV from extension lowercase`() {
        val result = FormatDetector.detectFromExtension("statement.csv")
        assertIs<DocumentType.Csv>(result)
    }

    @Test
    fun `detect CSV from extension uppercase`() {
        val result = FormatDetector.detectFromExtension("STATEMENT.CSV")
        assertIs<DocumentType.Csv>(result)
    }

    @Test
    fun `detect PDF from extension`() {
        val result = FormatDetector.detectFromExtension("statement.pdf")
        assertIs<DocumentType.Pdf>(result)
    }

    @Test
    fun `detect OFX from extension`() {
        val result = FormatDetector.detectFromExtension("transactions.ofx")
        assertIs<DocumentType.Ofx>(result)
    }

    @Test
    fun `detect QFX from extension`() {
        val result = FormatDetector.detectFromExtension("transactions.qfx")
        assertIs<DocumentType.Ofx>(result)
    }

    @Test
    fun `detect QIF from extension`() {
        val result = FormatDetector.detectFromExtension("transactions.qif")
        assertIs<DocumentType.Qif>(result)
    }

    @Test
    fun `detect JPEG image from extension`() {
        val result = FormatDetector.detectFromExtension("receipt.jpg")
        assertIs<DocumentType.Image>(result)
        assertEquals("JPEG", result.format)
    }

    @Test
    fun `detect PNG image from extension`() {
        val result = FormatDetector.detectFromExtension("receipt.png")
        assertIs<DocumentType.Image>(result)
        assertEquals("PNG", result.format)
    }

    @Test
    fun `detect HEIC image from extension`() {
        val result = FormatDetector.detectFromExtension("receipt.heic")
        assertIs<DocumentType.Image>(result)
        assertEquals("HEIC", result.format)
    }

    @Test
    fun `unknown extension returns Unknown`() {
        val result = FormatDetector.detectFromExtension("document.xyz")
        assertIs<DocumentType.Unknown>(result)
    }

    @Test
    fun `no extension returns Unknown`() {
        val result = FormatDetector.detectFromExtension("document")
        assertIs<DocumentType.Unknown>(result)
    }

    // ==================== Content Detection Tests ====================

    @Test
    fun `detect CSV from comma-separated content`() {
        val content = "Date,Amount,Description\n2024-01-15,1000,Test"
        val result = FormatDetector.detectFromContent(content.encodeToByteArray())
        assertIs<DocumentType.Csv>(result)
        assertEquals(',', result.delimiter)
    }

    @Test
    fun `detect CSV from semicolon-separated content`() {
        val content = "Date;Amount;Description\n2024-01-15;1000;Test"
        val result = FormatDetector.detectFromContent(content.encodeToByteArray())
        assertIs<DocumentType.Csv>(result)
        assertEquals(';', result.delimiter)
    }

    @Test
    fun `detect CSV from tab-separated content`() {
        val content = "Date\tAmount\tDescription\n2024-01-15\t1000\tTest"
        val result = FormatDetector.detectFromContent(content.encodeToByteArray())
        assertIs<DocumentType.Csv>(result)
        assertEquals('\t', result.delimiter)
    }

    @Test
    fun `detect OFX from header`() {
        val content = """
            OFXHEADER:100
            DATA:OFXSGML
            VERSION:102
        """.trimIndent()
        val result = FormatDetector.detectFromContent(content.encodeToByteArray())
        assertIs<DocumentType.Ofx>(result)
    }

    @Test
    fun `detect OFX from XML format`() {
        val content = """
            <?xml version="1.0"?>
            <?OFX OFXHEADER="200"?>
            <OFX>
        """.trimIndent()
        val result = FormatDetector.detectFromContent(content.encodeToByteArray())
        assertIs<DocumentType.Ofx>(result)
    }

    @Test
    fun `detect QIF from header`() {
        val content = """
            !Type:Bank
            D01/15/2024
            T1000.00
        """.trimIndent()
        val result = FormatDetector.detectFromContent(content.encodeToByteArray())
        assertIs<DocumentType.Qif>(result)
    }

    @Test
    fun `detect PDF from magic bytes`() {
        val content = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D) // %PDF-
        val result = FormatDetector.detectFromContent(content)
        assertIs<DocumentType.Pdf>(result)
    }

    @Test
    fun `detect PNG from magic bytes`() {
        val content = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val result = FormatDetector.detectFromContent(content)
        assertIs<DocumentType.Image>(result)
        assertEquals("PNG", result.format)
    }

    @Test
    fun `detect JPEG from magic bytes FFD8FF`() {
        val content = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        val result = FormatDetector.detectFromContent(content)
        assertIs<DocumentType.Image>(result)
        assertEquals("JPEG", result.format)
    }

    @Test
    fun `empty content returns Unknown`() {
        val result = FormatDetector.detectFromContent(byteArrayOf())
        assertIs<DocumentType.Unknown>(result)
    }

    // ==================== Bank Signature Detection Tests ====================

    @Test
    fun `detect Kaspi bank signature from content`() {
        val content = """
            АО «Kaspi Bank»
            Выписка по счету
            Период: 01.01.2024 - 31.01.2024
        """.trimIndent()
        val bankSignature = FormatDetector.detectBankSignature(content)
        assertEquals("kaspi", bankSignature)
    }

    @Test
    fun `detect Halyk bank signature from content`() {
        val content = """
            АО "Народный Банк Казахстана"
            Halyk Bank
            Statement
        """.trimIndent()
        val bankSignature = FormatDetector.detectBankSignature(content)
        assertEquals("halyk", bankSignature)
    }

    @Test
    fun `detect Jusan bank signature from content`() {
        val content = """
            АО "Jusan Bank"
            Выписка
        """.trimIndent()
        val bankSignature = FormatDetector.detectBankSignature(content)
        assertEquals("jusan", bankSignature)
    }

    @Test
    fun `detect Forte bank signature from content`() {
        val content = """
            АО "ForteBank"
            Account statement
        """.trimIndent()
        val bankSignature = FormatDetector.detectBankSignature(content)
        assertEquals("forte", bankSignature)
    }

    @Test
    fun `detect Sberbank signature from content`() {
        val content = """
            ПАО Сбербанк
            Выписка по счету
        """.trimIndent()
        val bankSignature = FormatDetector.detectBankSignature(content)
        assertEquals("sberbank", bankSignature)
    }

    @Test
    fun `detect Tinkoff signature from content`() {
        val content = """
            АО «Тинькофф Банк»
            Выписка
        """.trimIndent()
        val bankSignature = FormatDetector.detectBankSignature(content)
        assertEquals("tinkoff", bankSignature)
    }

    @Test
    fun `unknown bank returns null`() {
        val content = "Some random bank statement"
        val bankSignature = FormatDetector.detectBankSignature(content)
        assertNull(bankSignature)
    }

    // ==================== Combined Detection Tests ====================

    @Test
    fun `detect with both extension and content prefers content analysis`() {
        val content = "Date;Amount;Description\n2024-01-15;1000;Test"
        val result = FormatDetector.detect("file.csv", content.encodeToByteArray())
        assertIs<DocumentType.Csv>(result)
        assertEquals(';', result.delimiter) // Content shows semicolon, not comma
    }

    @Test
    fun `detect falls back to extension when content unclear`() {
        val content = "Some unclear text data"
        val result = FormatDetector.detect("file.csv", content.encodeToByteArray())
        assertIs<DocumentType.Csv>(result)
    }

    @Test
    fun `detect with null content uses extension only`() {
        val result = FormatDetector.detect("file.pdf", null)
        assertIs<DocumentType.Pdf>(result)
    }

    // ==================== CSV Delimiter Detection Tests ====================

    @Test
    fun `detectCsvDelimiter prefers most common delimiter`() {
        val content = "a,b,c,d\n1,2,3,4\na;b\n"
        val delimiter = FormatDetector.detectCsvDelimiter(content)
        assertEquals(',', delimiter)
    }

    @Test
    fun `detectCsvDelimiter handles quoted fields`() {
        val content = "\"Name, with comma\",Amount\n\"Test\",1000"
        val delimiter = FormatDetector.detectCsvDelimiter(content)
        assertEquals(',', delimiter)
    }

    @Test
    fun `detectCsvDelimiter defaults to comma`() {
        val content = "NoDelimitersHere"
        val delimiter = FormatDetector.detectCsvDelimiter(content)
        assertEquals(',', delimiter)
    }

    // ==================== Encoding Detection Tests ====================

    @Test
    fun `detect UTF-8 encoding from BOM`() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val content = bom + "Test".encodeToByteArray()
        val encoding = FormatDetector.detectEncoding(content)
        assertEquals("UTF-8", encoding)
    }

    @Test
    fun `detect UTF-16 LE encoding from BOM`() {
        val bom = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        val encoding = FormatDetector.detectEncoding(bom)
        assertEquals("UTF-16LE", encoding)
    }

    @Test
    fun `detect UTF-16 BE encoding from BOM`() {
        val bom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        val encoding = FormatDetector.detectEncoding(bom)
        assertEquals("UTF-16BE", encoding)
    }

    @Test
    fun `default to UTF-8 when no BOM`() {
        val content = "Test content".encodeToByteArray()
        val encoding = FormatDetector.detectEncoding(content)
        assertEquals("UTF-8", encoding)
    }
}

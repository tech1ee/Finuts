package com.finuts.domain.entity.import

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for DocumentType sealed interface and its implementations.
 */
class DocumentTypeTest {

    @Test
    fun `Csv can be created with delimiter and encoding`() {
        val csv = DocumentType.Csv(delimiter = ',', encoding = "UTF-8")

        assertEquals(',', csv.delimiter)
        assertEquals("UTF-8", csv.encoding)
    }

    @Test
    fun `Csv with semicolon delimiter`() {
        val csv = DocumentType.Csv(delimiter = ';', encoding = "windows-1251")

        assertEquals(';', csv.delimiter)
        assertEquals("windows-1251", csv.encoding)
    }

    @Test
    fun `Csv with tab delimiter`() {
        val csv = DocumentType.Csv(delimiter = '\t', encoding = "UTF-8")

        assertEquals('\t', csv.delimiter)
    }

    @Test
    fun `Pdf can be created with bank signature`() {
        val pdf = DocumentType.Pdf(bankSignature = "kaspi")

        assertEquals("kaspi", pdf.bankSignature)
    }

    @Test
    fun `Pdf allows null bank signature for unknown banks`() {
        val pdf = DocumentType.Pdf(bankSignature = null)

        assertNull(pdf.bankSignature)
    }

    @Test
    fun `Ofx can be created with version`() {
        val ofx = DocumentType.Ofx(version = "2.2")

        assertEquals("2.2", ofx.version)
    }

    @Test
    fun `Ofx with OFX version 1`() {
        val ofx = DocumentType.Ofx(version = "1.6")

        assertEquals("1.6", ofx.version)
    }

    @Test
    fun `Qif can be created with account type`() {
        val qif = DocumentType.Qif(accountType = "Bank")

        assertEquals("Bank", qif.accountType)
    }

    @Test
    fun `Image can be created with format`() {
        val image = DocumentType.Image(format = "JPEG")

        assertEquals("JPEG", image.format)
    }

    @Test
    fun `Image supports PNG format`() {
        val image = DocumentType.Image(format = "PNG")

        assertEquals("PNG", image.format)
    }

    @Test
    fun `Unknown is a singleton`() {
        val unknown1 = DocumentType.Unknown
        val unknown2 = DocumentType.Unknown

        assertTrue(unknown1 === unknown2)
    }

    @Test
    fun `all DocumentType implementations can be used polymorphically`() {
        val types: List<DocumentType> = listOf(
            DocumentType.Csv(delimiter = ',', encoding = "UTF-8"),
            DocumentType.Pdf(bankSignature = "halyk"),
            DocumentType.Ofx(version = "2.2"),
            DocumentType.Qif(accountType = "CCard"),
            DocumentType.Image(format = "HEIC"),
            DocumentType.Unknown
        )

        assertEquals(6, types.size)
    }

    @Test
    fun `when expression covers all types`() {
        val types = listOf(
            DocumentType.Csv(',', "UTF-8"),
            DocumentType.Pdf(null),
            DocumentType.Ofx("2.2"),
            DocumentType.Qif("Bank"),
            DocumentType.Image("PNG"),
            DocumentType.Unknown
        )

        types.forEach { type ->
            val result = when (type) {
                is DocumentType.Csv -> "csv"
                is DocumentType.Pdf -> "pdf"
                is DocumentType.Ofx -> "ofx"
                is DocumentType.Qif -> "qif"
                is DocumentType.Image -> "image"
                is DocumentType.Unknown -> "unknown"
            }
            assertTrue(result.isNotEmpty())
        }
    }

    @Test
    fun `smart cast works for DocumentType`() {
        val type: DocumentType = DocumentType.Csv(delimiter = ';', encoding = "UTF-8")

        when (type) {
            is DocumentType.Csv -> {
                assertEquals(';', type.delimiter)
                assertEquals("UTF-8", type.encoding)
            }
            else -> throw AssertionError("Expected Csv")
        }
    }

    @Test
    fun `DocumentType Csv copy works`() {
        val original = DocumentType.Csv(delimiter = ',', encoding = "UTF-8")
        val modified = original.copy(delimiter = ';')

        assertEquals(',', original.delimiter)
        assertEquals(';', modified.delimiter)
        assertEquals(original.encoding, modified.encoding)
    }
}

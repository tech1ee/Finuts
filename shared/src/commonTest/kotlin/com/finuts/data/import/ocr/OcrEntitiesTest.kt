package com.finuts.data.import.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OcrEntitiesTest {

    // === PdfPage Tests ===

    @Test
    fun `PdfPage equals returns true for same content`() {
        val page1 = PdfPage(0, 100, 200, byteArrayOf(1, 2, 3))
        val page2 = PdfPage(0, 100, 200, byteArrayOf(1, 2, 3))
        assertEquals(page1, page2)
    }

    @Test
    fun `PdfPage equals returns false for different imageData`() {
        val page1 = PdfPage(0, 100, 200, byteArrayOf(1, 2, 3))
        val page2 = PdfPage(0, 100, 200, byteArrayOf(1, 2, 4))
        assertFalse(page1 == page2)
    }

    @Test
    fun `PdfPage hashCode is consistent for same content`() {
        val page1 = PdfPage(0, 100, 200, byteArrayOf(1, 2, 3))
        val page2 = PdfPage(0, 100, 200, byteArrayOf(1, 2, 3))
        assertEquals(page1.hashCode(), page2.hashCode())
    }

    // === OcrResult Tests ===

    @Test
    fun `OcrResult isEmpty returns true for blank text`() {
        val result = OcrResult("   ", emptyList(), 0.9f)
        assertTrue(result.isEmpty)
    }

    @Test
    fun `OcrResult isEmpty returns false for non-blank text`() {
        val result = OcrResult("Hello", emptyList(), 0.9f)
        assertFalse(result.isEmpty)
    }

    @Test
    fun `OcrResult hasHighConfidence returns true for 0_8 or higher`() {
        val result = OcrResult("text", emptyList(), 0.85f)
        assertTrue(result.hasHighConfidence)
    }

    @Test
    fun `OcrResult hasHighConfidence returns false for below 0_8`() {
        val result = OcrResult("text", emptyList(), 0.79f)
        assertFalse(result.hasHighConfidence)
    }

    @Test
    fun `OcrResult hasMediumConfidence returns true for 0_5 to 0_8`() {
        val result = OcrResult("text", emptyList(), 0.65f)
        assertTrue(result.hasMediumConfidence)
    }

    @Test
    fun `OcrResult hasMediumConfidence returns false for below 0_5`() {
        val result = OcrResult("text", emptyList(), 0.45f)
        assertFalse(result.hasMediumConfidence)
    }

    // === BoundingBox Tests ===

    @Test
    fun `BoundingBox right calculated correctly`() {
        val box = BoundingBox(0.1f, 0.2f, 0.3f, 0.4f)
        assertEquals(0.4f, box.right, 0.001f)
    }

    @Test
    fun `BoundingBox bottom calculated correctly`() {
        val box = BoundingBox(0.1f, 0.2f, 0.3f, 0.4f)
        assertEquals(0.6f, box.bottom, 0.001f)
    }

    @Test
    fun `BoundingBox centerX calculated correctly`() {
        val box = BoundingBox(0.1f, 0.2f, 0.4f, 0.6f)
        assertEquals(0.3f, box.centerX, 0.001f)
    }

    @Test
    fun `BoundingBox centerY calculated correctly`() {
        val box = BoundingBox(0.1f, 0.2f, 0.4f, 0.6f)
        assertEquals(0.5f, box.centerY, 0.001f)
    }

    // === OcrBlock Tests ===

    @Test
    fun `OcrBlock stores text and confidence`() {
        val block = OcrBlock(
            text = "Привет",
            confidence = 0.95f,
            boundingBox = BoundingBox(0f, 0f, 1f, 1f)
        )
        assertEquals("Привет", block.text)
        assertEquals(0.95f, block.confidence, 0.001f)
    }

    // === Exception Tests ===

    @Test
    fun `OcrException stores message`() {
        val exception = OcrException("OCR failed")
        assertEquals("OCR failed", exception.message)
    }

    @Test
    fun `PdfExtractionException stores message`() {
        val exception = PdfExtractionException("PDF invalid")
        assertEquals("PDF invalid", exception.message)
    }
}

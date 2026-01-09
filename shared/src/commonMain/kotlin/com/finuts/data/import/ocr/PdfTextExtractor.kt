package com.finuts.data.import.ocr

/**
 * Platform-specific PDF page extractor.
 *
 * Extracts pages from a PDF document as images for OCR processing.
 *
 * Android: Uses PdfRenderer
 * iOS: Uses PDFKit
 */
expect class PdfTextExtractor {
    /**
     * Extract all pages from a PDF as images.
     *
     * @param pdfData Raw PDF file bytes
     * @return List of extracted pages with image data
     * @throws PdfExtractionException if PDF cannot be processed
     */
    suspend fun extractPages(pdfData: ByteArray): List<PdfPage>
}

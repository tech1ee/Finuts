package com.finuts.data.import.ocr

/**
 * Platform-specific OCR (Optical Character Recognition) service.
 *
 * Recognizes text from images with support for multiple languages
 * including Russian (Cyrillic) and English.
 *
 * Android: Uses Tesseract4Android (supports Cyrillic)
 * iOS: Uses Vision Framework VNRecognizeTextRequest
 */
expect class OcrService {
    /**
     * Recognize text from an image.
     *
     * @param imageData PNG/JPEG image bytes
     * @return OCR result with full text, blocks, and confidence
     * @throws OcrException if recognition fails
     */
    suspend fun recognizeText(imageData: ByteArray): OcrResult
}

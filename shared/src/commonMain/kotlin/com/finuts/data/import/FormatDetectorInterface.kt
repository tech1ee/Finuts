package com.finuts.data.import

import com.finuts.domain.entity.import.DocumentType

/**
 * Interface for document type detection.
 * Implementations detect document types from file extensions and content.
 */
interface FormatDetectorInterface {

    /**
     * Detect document type from filename and optional content.
     *
     * @param filename The filename including extension
     * @param content Optional file content for deeper analysis
     * @return Detected DocumentType
     */
    fun detect(filename: String, content: ByteArray?): DocumentType

    /**
     * Detect document type from file extension.
     *
     * @param filename The filename including extension
     * @return Detected DocumentType
     */
    fun detectFromExtension(filename: String): DocumentType

    /**
     * Detect document type from file content.
     *
     * @param content File content as bytes
     * @return Detected DocumentType
     */
    fun detectFromContent(content: ByteArray): DocumentType

    /**
     * Detect the most likely CSV delimiter from content.
     *
     * @param content CSV content as string
     * @return Detected delimiter character
     */
    fun detectCsvDelimiter(content: String): Char

    /**
     * Detect bank signature from document content.
     *
     * @param content Document text content
     * @return Bank identifier or null if not recognized
     */
    fun detectBankSignature(content: String): String?

    /**
     * Detect text encoding from BOM or content analysis.
     *
     * @param content File content as bytes
     * @return Detected encoding name
     */
    fun detectEncoding(content: ByteArray): String
}

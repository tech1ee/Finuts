package com.finuts.test.fakes

import com.finuts.data.import.FormatDetectorInterface
import com.finuts.domain.entity.import.DocumentType

/**
 * Fake implementation of FormatDetectorInterface for testing.
 * Provides configurable responses for format detection.
 */
class FakeFormatDetector : FormatDetectorInterface {

    private var detectResponses = mutableMapOf<String, DocumentType>()
    private var extensionResponses = mutableMapOf<String, DocumentType>()
    private var contentResponses = mutableMapOf<Int, DocumentType>()
    private var delimiterResponses = mutableMapOf<String, Char>()
    private var bankSignatureResponses = mutableMapOf<String, String?>()
    private var encodingResponses = mutableMapOf<Int, String>()

    private var defaultDocumentType: DocumentType = DocumentType.Unknown
    private var defaultDelimiter: Char = ','
    private var defaultEncoding: String = "UTF-8"

    override fun detect(filename: String, content: ByteArray?): DocumentType {
        return detectResponses[filename] ?: defaultDocumentType
    }

    override fun detectFromExtension(filename: String): DocumentType {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return extensionResponses[extension] ?: defaultDocumentType
    }

    override fun detectFromContent(content: ByteArray): DocumentType {
        return contentResponses[content.contentHashCode()] ?: defaultDocumentType
    }

    override fun detectCsvDelimiter(content: String): Char {
        return delimiterResponses[content] ?: defaultDelimiter
    }

    override fun detectBankSignature(content: String): String? {
        return bankSignatureResponses[content]
    }

    override fun detectEncoding(content: ByteArray): String {
        return encodingResponses[content.contentHashCode()] ?: defaultEncoding
    }

    // Test helpers

    fun setDetectResponse(filename: String, documentType: DocumentType) {
        detectResponses[filename] = documentType
    }

    fun setExtensionResponse(extension: String, documentType: DocumentType) {
        extensionResponses[extension.lowercase()] = documentType
    }

    fun setContentResponse(content: ByteArray, documentType: DocumentType) {
        contentResponses[content.contentHashCode()] = documentType
    }

    fun setDelimiterResponse(content: String, delimiter: Char) {
        delimiterResponses[content] = delimiter
    }

    fun setBankSignatureResponse(content: String, bankId: String?) {
        bankSignatureResponses[content] = bankId
    }

    fun setEncodingResponse(content: ByteArray, encoding: String) {
        encodingResponses[content.contentHashCode()] = encoding
    }

    fun setDefaultDocumentType(documentType: DocumentType) {
        defaultDocumentType = documentType
    }

    fun setDefaultDelimiter(delimiter: Char) {
        defaultDelimiter = delimiter
    }

    fun setDefaultEncoding(encoding: String) {
        defaultEncoding = encoding
    }

    fun reset() {
        detectResponses.clear()
        extensionResponses.clear()
        contentResponses.clear()
        delimiterResponses.clear()
        bankSignatureResponses.clear()
        encodingResponses.clear()
        defaultDocumentType = DocumentType.Unknown
        defaultDelimiter = ','
        defaultEncoding = "UTF-8"
    }
}

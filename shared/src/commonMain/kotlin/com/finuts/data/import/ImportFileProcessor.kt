package com.finuts.data.import

import co.touchlab.kermit.Logger
import com.finuts.data.import.ocr.PdfParser
import com.finuts.data.import.parsers.CsvParser
import com.finuts.data.import.parsers.OfxParser
import com.finuts.data.import.parsers.QifParser
import com.finuts.domain.entity.import.DocumentType
import com.finuts.domain.entity.import.ImportResult

/**
 * Processes import files by detecting format and parsing content.
 * Orchestrates format detection and delegates to appropriate parsers.
 */
class ImportFileProcessor(
    private val formatDetector: FormatDetectorInterface,
    private val csvParser: CsvParser,
    private val ofxParser: OfxParser,
    private val qifParser: QifParser,
    private val pdfParser: PdfParser? = null
) {
    private val log = Logger.withTag("ImportFileProcessor")

    /**
     * Process a file and extract transactions.
     *
     * @param filename The original filename (used for format detection)
     * @param bytes The file content as bytes
     * @return ImportResult with parsed transactions or error
     */
    suspend fun process(filename: String, bytes: ByteArray): ImportResult {
        log.d { "process() called: filename=$filename, size=${bytes.size}" }
        val documentType = formatDetector.detect(filename, bytes)
        log.d { "Detected type: $documentType" }

        return when (documentType) {
            is DocumentType.Csv -> csvParser.parse(bytes.decodeToString(), documentType)
            is DocumentType.Ofx -> ofxParser.parse(bytes.decodeToString(), documentType)
            is DocumentType.Qif -> qifParser.parse(bytes.decodeToString(), documentType)
            is DocumentType.Pdf -> {
                log.d { "Processing PDF..." }
                processPdf(bytes, documentType)
            }
            is DocumentType.Image -> processImage(bytes, documentType)
            is DocumentType.Unknown -> ImportResult.Error(
                message = "Unknown file format: ${filename.substringAfterLast('.')}",
                documentType = documentType,
                partialTransactions = emptyList()
            )
        }
    }

    private suspend fun processPdf(
        bytes: ByteArray,
        documentType: DocumentType.Pdf
    ): ImportResult {
        log.d { "processPdf() - pdfParser=${if (pdfParser != null) "available" else "NULL"}" }
        if (pdfParser == null) {
            log.e { "ERROR: pdfParser is null!" }
            return ImportResult.Error(
                message = "PDF parsing is not available on this platform",
                documentType = documentType,
                partialTransactions = emptyList()
            )
        }
        log.d { "Calling pdfParser.parsePdf()..." }
        val result = pdfParser.parsePdf(bytes, documentType)
        log.d { "pdfParser.parsePdf() returned: ${result::class.simpleName}" }
        return result
    }

    private suspend fun processImage(
        bytes: ByteArray,
        documentType: DocumentType.Image
    ): ImportResult {
        if (pdfParser == null) {
            return ImportResult.Error(
                message = "Image OCR is not available on this platform",
                documentType = documentType,
                partialTransactions = emptyList()
            )
        }
        return pdfParser.parseImage(bytes, documentType)
    }
}

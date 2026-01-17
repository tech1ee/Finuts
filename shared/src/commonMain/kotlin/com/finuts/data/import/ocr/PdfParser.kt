package com.finuts.data.import.ocr

import co.touchlab.kermit.Logger
import com.finuts.domain.entity.import.DocumentType
import com.finuts.domain.entity.import.ImportResult
import com.finuts.domain.entity.import.ImportSource
import com.finuts.domain.entity.import.ImportedTransaction

/**
 * Orchestrates PDF and image document parsing through OCR.
 *
 * Pipeline:
 * 1. Extract pages from PDF (or use image directly)
 * 2. Run OCR on each page
 * 3. Combine text from all pages
 * 4. Parse bank statement text into transactions
 *
 * @property pdfExtractor Platform-specific PDF page extractor
 * @property ocrService Platform-specific OCR service
 * @property bankStatementParser Text-to-transactions parser
 */
class PdfParser(
    private val pdfExtractor: PdfTextExtractor,
    private val ocrService: OcrService,
    private val bankStatementParser: BankStatementParser
) {
    private val log = Logger.withTag("PdfParser")

    companion object {
        private const val PAGE_BREAK_MARKER = "\n\n--- Page Break ---\n\n"
        private const val LOW_CONFIDENCE_THRESHOLD = 0.5f
        private const val MIN_TRANSACTIONS_THRESHOLD = 1
    }

    /**
     * Parse a PDF document and extract transactions.
     *
     * @param pdfData Raw PDF file bytes
     * @param documentType Document type with bank signature
     * @return ImportResult with parsed transactions
     */
    suspend fun parsePdf(
        pdfData: ByteArray,
        documentType: DocumentType.Pdf
    ): ImportResult {
        log.d { "parsePdf: Starting PDF parsing, size=${pdfData.size} bytes" }
        return try {
            // 1. Extract pages as images
            log.d { "parsePdf: Step 1 - Extracting pages from PDF" }
            val pages = pdfExtractor.extractPages(pdfData)
            log.d { "parsePdf: Extracted ${pages.size} pages" }

            if (pages.isEmpty()) {
                log.w { "parsePdf: No pages found in PDF" }
                return ImportResult.Error(
                    message = "PDF не содержит страниц",
                    documentType = documentType,
                    partialTransactions = emptyList()
                )
            }

            // 2. OCR each page
            log.d { "parsePdf: Step 2 - Running OCR on ${pages.size} pages" }
            val ocrResults = mutableListOf<OcrResult>()
            for ((index, page) in pages.withIndex()) {
                try {
                    log.d { "parsePdf: OCR page ${index + 1}/${pages.size}, imageSize=${page.imageData.size}" }
                    val result = ocrService.recognizeText(page.imageData)
                    log.d { "parsePdf: OCR page ${index + 1} done, text=${result.fullText.take(100)}..." }
                    ocrResults.add(result)
                } catch (e: OcrException) {
                    log.e(e) { "parsePdf: OCR failed for page ${index + 1}: ${e.message}" }
                    continue
                }
            }

            if (ocrResults.isEmpty()) {
                log.e { "parsePdf: All OCR attempts failed" }
                return ImportResult.Error(
                    message = "Не удалось распознать текст в PDF",
                    documentType = documentType,
                    partialTransactions = emptyList()
                )
            }

            // 3. Reconstruct table layout using bounding boxes
            // OCR often reads columns instead of rows - we need to use Y coordinates
            // to group text blocks into table rows, then sort by X within each row
            log.d { "parsePdf: Step 3 - Reconstructing table layout from ${ocrResults.size} pages" }
            val fullText = try {
                ocrResults
                    .mapIndexed { pageIndex, result ->
                        log.d { "parsePdf: Processing page $pageIndex: ${result.blocks.size} blocks" }
                        val reconstructed = reconstructTableText(result.blocks, pageIndex)
                        log.d { "parsePdf: Page $pageIndex reconstructed ${result.blocks.size} blocks into ${reconstructed.lines().size} lines" }
                        reconstructed
                    }
                    .joinToString(PAGE_BREAK_MARKER)
            } catch (e: Exception) {
                log.e(e) { "parsePdf: ERROR in reconstructTableText: ${e::class.simpleName}: ${e.message}" }
                throw e
            }
            log.d { "parsePdf: Step 3 - Combined text length=${fullText.length}" }
            // Skip verbose log - large strings can cause iOS logging to block
            log.d { "parsePdf: Step 3.5 - Text preview: ${fullText.take(200)}..." }

            log.d { "parsePdf: Step 3.6 - Calculating OCR confidence..." }
            val avgOcrConfidence = ocrResults
                .map { it.overallConfidence }
                .average()
                .toFloat()
            log.d { "parsePdf: Step 3.7 - OCR confidence=$avgOcrConfidence" }

            // 4. Parse transactions from text
            log.d { "parsePdf: Step 4 - Parsing transactions from text (length=${fullText.length})..." }
            val transactions = bankStatementParser.parseText(
                text = fullText,
                bankSignature = documentType.bankSignature
            )
            log.d { "parsePdf: Step 4 DONE - Parsed ${transactions.size} transactions" }

            // 5. Adjust confidence based on OCR quality
            val adjustedTransactions = transactions.map { tx ->
                tx.copy(confidence = tx.confidence * avgOcrConfidence)
            }

            val result = buildResult(adjustedTransactions, documentType, avgOcrConfidence)
            log.i { "parsePdf: Completed, result=${result::class.simpleName}, txCount=${adjustedTransactions.size}" }
            result
        } catch (e: PdfExtractionException) {
            log.e(e) { "parsePdf: PDF extraction failed: ${e.message}" }
            ImportResult.Error(
                message = "Ошибка извлечения страниц: ${e.message}",
                documentType = documentType,
                partialTransactions = emptyList()
            )
        } catch (e: Exception) {
            log.e(e) { "parsePdf: Unexpected error: ${e.message}" }
            ImportResult.Error(
                message = "Ошибка обработки PDF: ${e.message}",
                documentType = documentType,
                partialTransactions = emptyList()
            )
        }
    }

    /**
     * Parse an image document (photo of statement) using OCR.
     *
     * @param imageData Raw image bytes (PNG/JPEG)
     * @param documentType Document type information
     * @return ImportResult with parsed transactions
     */
    suspend fun parseImage(
        imageData: ByteArray,
        documentType: DocumentType.Image
    ): ImportResult {
        return try {
            // 1. Run OCR on image
            val ocrResult = ocrService.recognizeText(imageData)

            if (ocrResult.isEmpty) {
                return ImportResult.Error(
                    message = "Не удалось распознать текст на изображении",
                    documentType = documentType,
                    partialTransactions = emptyList()
                )
            }

            // 2. Parse transactions from OCR text
            val transactions = bankStatementParser.parseText(
                text = ocrResult.fullText,
                bankSignature = null
            )

            // 3. Adjust confidence
            val adjustedTransactions = transactions.map { tx ->
                tx.copy(confidence = tx.confidence * ocrResult.overallConfidence)
            }

            buildImageResult(adjustedTransactions, documentType, ocrResult.overallConfidence)
        } catch (e: OcrException) {
            ImportResult.Error(
                message = "Ошибка OCR: ${e.message}",
                documentType = documentType,
                partialTransactions = emptyList()
            )
        } catch (e: Exception) {
            ImportResult.Error(
                message = "Ошибка обработки изображения: ${e.message}",
                documentType = documentType,
                partialTransactions = emptyList()
            )
        }
    }

    private fun buildResult(
        transactions: List<ImportedTransaction>,
        documentType: DocumentType.Pdf,
        ocrConfidence: Float
    ): ImportResult {
        return when {
            transactions.isEmpty() -> {
                ImportResult.NeedsUserInput(
                    transactions = emptyList(),
                    documentType = documentType,
                    issues = listOf(
                        "Не удалось распознать транзакции в PDF.",
                        "Попробуйте загрузить файл в формате CSV или OFX."
                    )
                )
            }

            ocrConfidence < LOW_CONFIDENCE_THRESHOLD -> {
                ImportResult.NeedsUserInput(
                    transactions = transactions,
                    documentType = documentType,
                    issues = listOf(
                        "Низкое качество распознавания (${(ocrConfidence * 100).toInt()}%).",
                        "Проверьте распознанные транзакции."
                    )
                )
            }

            else -> {
                ImportResult.Success(
                    transactions = transactions,
                    documentType = documentType,
                    totalConfidence = calculateTotalConfidence(transactions, ocrConfidence)
                )
            }
        }
    }

    private fun buildImageResult(
        transactions: List<ImportedTransaction>,
        documentType: DocumentType.Image,
        ocrConfidence: Float
    ): ImportResult {
        return when {
            transactions.isEmpty() -> {
                ImportResult.NeedsUserInput(
                    transactions = emptyList(),
                    documentType = documentType,
                    issues = listOf(
                        "Не удалось распознать транзакции на изображении.",
                        "Убедитесь, что изображение содержит банковскую выписку."
                    )
                )
            }

            ocrConfidence < LOW_CONFIDENCE_THRESHOLD -> {
                ImportResult.NeedsUserInput(
                    transactions = transactions,
                    documentType = documentType,
                    issues = listOf(
                        "Низкое качество распознавания (${(ocrConfidence * 100).toInt()}%).",
                        "Попробуйте сделать более чёткое фото."
                    )
                )
            }

            else -> {
                ImportResult.Success(
                    transactions = transactions,
                    documentType = documentType,
                    totalConfidence = calculateTotalConfidence(transactions, ocrConfidence)
                )
            }
        }
    }

    private fun calculateTotalConfidence(
        transactions: List<ImportedTransaction>,
        ocrConfidence: Float
    ): Float {
        if (transactions.isEmpty()) return 0f

        val avgTxConfidence = transactions
            .map { it.confidence }
            .average()
            .toFloat()

        // Weight: 60% transaction parsing, 40% OCR quality
        return (avgTxConfidence * 0.6f + ocrConfidence * 0.4f).coerceIn(0f, 1f)
    }

    /**
     * Reconstructs table text from OCR blocks using spatial positioning.
     *
     * OCR engines often read PDFs column-by-column instead of row-by-row.
     * This function uses bounding box Y coordinates to group blocks into rows,
     * then sorts blocks within each row by X coordinate (left to right).
     *
     * This produces properly formatted lines like:
     *   "28.12.25 Покупка Starbucks - 699,00 T"
     * Instead of scattered:
     *   "28.12.25"
     *   "Покупка"
     *   "Starbucks"
     *   "- 699,00 T"
     *
     * @param blocks OCR blocks with text and bounding boxes
     * @param pageIndex Page number for logging
     * @return Reconstructed text with proper row ordering
     */
    private fun reconstructTableText(blocks: List<OcrBlock>, pageIndex: Int): String {
        log.d { "reconstructTableText START: page=$pageIndex, blocks=${blocks.size}" }

        if (blocks.isEmpty()) {
            log.d { "reconstructTableText: blocks is EMPTY, returning empty string" }
            return ""
        }

        // Log first few blocks for debugging
        blocks.take(5).forEachIndexed { idx, block ->
            log.v { "Block[$idx]: text='${block.text.take(30)}', y=${block.boundingBox.y}, centerY=${block.boundingBox.centerY}, h=${block.boundingBox.height}" }
        }

        // Y-coordinate tolerance for grouping blocks into same row
        // Blocks within this vertical distance are considered same row
        // Using percentage of average block height for robustness
        val avgHeight = blocks.map { it.boundingBox.height }.average().toFloat()
        val rowTolerance = avgHeight * 0.5f
        log.d { "avgHeight=$avgHeight, rowTolerance=$rowTolerance" }

        // Group blocks by Y coordinate (centerY for better accuracy)
        val rows = mutableListOf<MutableList<OcrBlock>>()

        for (block in blocks.sortedBy { it.boundingBox.centerY }) {
            // Find existing row that this block belongs to
            val existingRow = rows.find { row ->
                val rowCenterY = row.map { it.boundingBox.centerY }.average().toFloat()
                kotlin.math.abs(block.boundingBox.centerY - rowCenterY) <= rowTolerance
            }

            if (existingRow != null) {
                existingRow.add(block)
            } else {
                rows.add(mutableListOf(block))
            }
        }

        log.d { "reconstructTableText: Page $pageIndex - grouped ${blocks.size} blocks into ${rows.size} rows" }

        // Log first few rows for debugging
        rows.take(3).forEachIndexed { idx, row ->
            val rowText = row.sortedBy { it.boundingBox.x }.joinToString(" | ") { it.text.take(20) }
            log.v { "Row[$idx]: $rowText" }
        }

        // Sort rows by Y (top to bottom), blocks within row by X (left to right)
        val result = rows
            .sortedBy { row -> row.minOfOrNull { it.boundingBox.y } ?: 0f }
            .map { row ->
                row.sortedBy { it.boundingBox.x }
                    .joinToString(" ") { it.text.trim() }
            }
            .joinToString("\n")

        log.d { "reconstructTableText DONE: result length=${result.length}, lines=${result.lines().size}" }
        return result
    }
}

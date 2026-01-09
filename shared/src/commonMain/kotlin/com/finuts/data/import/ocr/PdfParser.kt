package com.finuts.data.import.ocr

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
        return try {
            // 1. Extract pages as images
            val pages = pdfExtractor.extractPages(pdfData)

            if (pages.isEmpty()) {
                return ImportResult.Error(
                    message = "PDF не содержит страниц",
                    documentType = documentType,
                    partialTransactions = emptyList()
                )
            }

            // 2. OCR each page
            val ocrResults = mutableListOf<OcrResult>()
            for (page in pages) {
                try {
                    val result = ocrService.recognizeText(page.imageData)
                    ocrResults.add(result)
                } catch (e: OcrException) {
                    // Continue with other pages if one fails
                    continue
                }
            }

            if (ocrResults.isEmpty()) {
                return ImportResult.Error(
                    message = "Не удалось распознать текст в PDF",
                    documentType = documentType,
                    partialTransactions = emptyList()
                )
            }

            // 3. Combine text from all pages
            val fullText = ocrResults
                .joinToString(PAGE_BREAK_MARKER) { it.fullText }

            val avgOcrConfidence = ocrResults
                .map { it.overallConfidence }
                .average()
                .toFloat()

            // 4. Parse transactions from text
            val transactions = bankStatementParser.parseText(
                text = fullText,
                bankSignature = documentType.bankSignature
            )

            // 5. Adjust confidence based on OCR quality
            val adjustedTransactions = transactions.map { tx ->
                tx.copy(confidence = tx.confidence * avgOcrConfidence)
            }

            buildResult(adjustedTransactions, documentType, avgOcrConfidence)
        } catch (e: PdfExtractionException) {
            ImportResult.Error(
                message = "Ошибка извлечения страниц: ${e.message}",
                documentType = documentType,
                partialTransactions = emptyList()
            )
        } catch (e: Exception) {
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
}

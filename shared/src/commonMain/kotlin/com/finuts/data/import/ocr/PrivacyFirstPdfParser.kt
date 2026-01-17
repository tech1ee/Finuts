package com.finuts.data.import.ocr

import co.touchlab.kermit.Logger
import com.finuts.ai.privacy.PIIAnonymizer
import com.finuts.domain.entity.import.DocumentType as DomainDocumentType
import com.finuts.domain.entity.import.ImportResult
import com.finuts.domain.entity.import.ImportSource
import com.finuts.domain.entity.import.ImportedTransaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDate

/**
 * Privacy-First PDF Parser Orchestrator.
 *
 * Implements the complete privacy-first parsing pipeline:
 * 1. OCR → Extract text locally (100% on device)
 * 2. Preprocess → Reduce tokens, detect document type
 * 3. Local Extract → Extract dates/amounts (100% on device)
 * 4. Anonymize → Replace PII before cloud call
 * 5. Cloud Enhance → Get merchant/category from LLM
 * 6. Deanonymize → Restore PII locally
 *
 * Privacy Guarantees:
 * - Raw OCR text never leaves device
 * - PII is always anonymized before cloud calls
 * - LLM only sees: dates, amounts, anonymized descriptions
 */
class PrivacyFirstPdfParser(
    private val pdfExtractor: PdfTextExtractor,
    private val ocrService: OcrService,
    private val preprocessor: DocumentPreprocessor,
    private val localExtractor: LocalTransactionExtractor,
    private val anonymizer: PIIAnonymizer,
    private val cloudEnhancer: CloudTransactionEnhancer?
) {
    private val log = Logger.withTag("PrivacyFirstPdfParser")

    /**
     * Parse PDF with privacy-first approach.
     */
    suspend fun parsePdf(
        pdfData: ByteArray,
        documentType: DomainDocumentType.Pdf
    ): ImportResult {
        log.d { "parsePdf: Starting privacy-first parsing, size=${pdfData.size}" }

        return try {
            // Step 1: Extract pages from PDF (LOCAL)
            val pages = pdfExtractor.extractPages(pdfData)
            if (pages.isEmpty()) {
                return ImportResult.Error(
                    message = "PDF не содержит страниц",
                    documentType = documentType,
                    partialTransactions = emptyList()
                )
            }

            // Step 2: OCR each page (LOCAL)
            val ocrText = performOcr(pages)
            if (ocrText.isBlank()) {
                return ImportResult.Error(
                    message = "Не удалось распознать текст в PDF",
                    documentType = documentType,
                    partialTransactions = emptyList()
                )
            }

            // Process the OCR text through the pipeline
            processText(ocrText, documentType)
        } catch (e: PdfExtractionException) {
            log.e(e) { "PDF extraction failed" }
            ImportResult.Error(
                message = "Ошибка извлечения: ${e.message}",
                documentType = documentType,
                partialTransactions = emptyList()
            )
        } catch (e: Exception) {
            log.e(e) { "Unexpected error" }
            ImportResult.Error(
                message = "Ошибка обработки PDF: ${e.message}",
                documentType = documentType,
                partialTransactions = emptyList()
            )
        }
    }

    /**
     * Parse image with privacy-first approach.
     */
    suspend fun parseImage(
        imageData: ByteArray,
        documentType: DomainDocumentType.Image
    ): ImportResult {
        log.d { "parseImage: Starting privacy-first parsing, size=${imageData.size}" }

        return try {
            // Step 1: OCR (LOCAL)
            val ocrResult = ocrService.recognizeText(imageData)
            if (ocrResult.isEmpty) {
                return ImportResult.Error(
                    message = "Не удалось распознать текст на изображении",
                    documentType = documentType,
                    partialTransactions = emptyList()
                )
            }

            // Process through pipeline
            processText(ocrResult.fullText, documentType)
        } catch (e: OcrException) {
            log.e(e) { "OCR failed" }
            ImportResult.Error(
                message = "Ошибка OCR: ${e.message}",
                documentType = documentType,
                partialTransactions = emptyList()
            )
        } catch (e: Exception) {
            log.e(e) { "Unexpected error" }
            ImportResult.Error(
                message = "Ошибка обработки изображения: ${e.message}",
                documentType = documentType,
                partialTransactions = emptyList()
            )
        }
    }

    /**
     * Core processing pipeline (shared between PDF and Image).
     */
    private suspend fun processText(
        ocrText: String,
        documentType: DomainDocumentType
    ): ImportResult {
        // Step 3: Preprocess (LOCAL) - reduce tokens
        log.d { "Step 3: Preprocessing (token reduction)" }
        val preprocessed = preprocessor.process(ocrText)
        log.d { "Preprocessed: ${preprocessed.hints.type}, ${preprocessed.hints.language}" }

        // Step 4: Local extraction (LOCAL) - dates and amounts
        log.d { "Step 4: Local extraction (dates/amounts)" }
        val partialTransactions = localExtractor.extract(preprocessed.cleanedText)
        log.d { "Extracted ${partialTransactions.size} partial transactions" }

        if (partialTransactions.isEmpty()) {
            return createEmptyResult(documentType)
        }

        // Step 5: Anonymize (LOCAL) - replace PII
        log.d { "Step 5: Anonymizing PII before cloud call" }
        val anonymizationResult = anonymizer.anonymize(preprocessed.cleanedText)
        log.d { "Anonymized ${anonymizationResult.piiCount} PII items" }

        // Step 6: Cloud enhancement (if available)
        val enhancedTransactions = if (cloudEnhancer != null) {
            log.d { "Step 6: Cloud enhancement (merchant/category)" }
            try {
                cloudEnhancer.enhance(partialTransactions)
            } catch (e: Exception) {
                log.w(e) { "Cloud enhancement failed, using local data" }
                partialTransactions.map { toEnhancedWithoutLlm(it) }
            }
        } else {
            log.d { "Step 6: Skipped (no cloud enhancer configured)" }
            partialTransactions.map { toEnhancedWithoutLlm(it) }
        }

        // Step 7: Deanonymize (LOCAL) - restore PII in results
        log.d { "Step 7: Deanonymizing results" }
        val usedLlm = cloudEnhancer != null
        val finalTransactions = enhancedTransactions.map { enhanced ->
            val deanonymizedDescription = anonymizer.deanonymize(
                enhanced.rawDescription,
                anonymizationResult.mapping
            )
            val deanonymizedCounterparty = enhanced.counterpartyName?.let {
                anonymizer.deanonymize(it, anonymizationResult.mapping)
            }

            toImportedTransaction(
                enhanced = enhanced,
                deanonymizedDescription = deanonymizedDescription,
                deanonymizedCounterparty = deanonymizedCounterparty,
                usedLlm = usedLlm
            )
        }

        return createSuccessResult(finalTransactions, documentType)
    }

    private suspend fun performOcr(pages: List<PdfPage>): String {
        val texts = mutableListOf<String>()
        for ((index, page) in pages.withIndex()) {
            try {
                val result = ocrService.recognizeText(page.imageData)
                texts.add(result.fullText)
                log.d { "OCR page ${index + 1}/${pages.size}: ${result.fullText.take(50)}..." }
            } catch (e: OcrException) {
                log.w(e) { "OCR failed for page ${index + 1}" }
            }
        }
        return texts.joinToString("\n\n")
    }

    private fun toEnhancedWithoutLlm(partial: PartialTransaction): EnhancedTransaction {
        return EnhancedTransaction(
            rawDate = partial.rawDate,
            amountMinorUnits = partial.amountMinorUnits,
            currency = partial.currency,
            rawDescription = partial.rawDescription,
            isCredit = partial.isCredit,
            isDebit = partial.isDebit,
            merchant = null,
            counterpartyName = null,
            categoryHint = null,
            transactionType = null
        )
    }

    private fun toImportedTransaction(
        enhanced: EnhancedTransaction,
        deanonymizedDescription: String,
        deanonymizedCounterparty: String?,
        usedLlm: Boolean
    ): ImportedTransaction {
        val description = buildDescription(
            deanonymizedDescription,
            deanonymizedCounterparty,
            enhanced.merchant
        )

        return ImportedTransaction(
            date = parseDate(enhanced.rawDate),
            amount = enhanced.amountMinorUnits,
            description = description,
            merchant = enhanced.merchant,
            category = enhanced.categoryHint,
            confidence = ENHANCED_CONFIDENCE,
            source = if (usedLlm) ImportSource.LLM_ENHANCED else ImportSource.DOCUMENT_AI
        )
    }

    private fun buildDescription(
        rawDescription: String,
        counterparty: String?,
        merchant: String?
    ): String {
        return when {
            merchant != null -> rawDescription
            counterparty != null -> "Перевод: $counterparty"
            else -> rawDescription
        }
    }

    private fun parseDate(rawDate: String): LocalDate {
        return try {
            // Try ISO format first
            if (rawDate.contains("-")) {
                rawDate.toLocalDate()
            } else {
                // Parse DD.MM.YYYY or DD/MM/YYYY
                val parts = rawDate.split(Regex("[./]"))
                if (parts.size == 3) {
                    val day = parts[0].toInt()
                    val month = parts[1].toInt()
                    var year = parts[2].toInt()
                    if (year < 100) year += 2000
                    LocalDate(year, month, day)
                } else {
                    LocalDate(2025, 1, 1) // Fallback
                }
            }
        } catch (e: Exception) {
            LocalDate(2025, 1, 1) // Fallback
        }
    }

    private fun createEmptyResult(documentType: DomainDocumentType): ImportResult {
        return ImportResult.NeedsUserInput(
            transactions = emptyList(),
            documentType = documentType,
            issues = listOf(
                "Не удалось распознать транзакции.",
                "Попробуйте загрузить файл в формате CSV."
            )
        )
    }

    private fun createSuccessResult(
        transactions: List<ImportedTransaction>,
        documentType: DomainDocumentType
    ): ImportResult {
        val avgConfidence = transactions.map { it.confidence }.average().toFloat()

        return if (avgConfidence < LOW_CONFIDENCE_THRESHOLD) {
            ImportResult.NeedsUserInput(
                transactions = transactions,
                documentType = documentType,
                issues = listOf(
                    "Низкая уверенность (${(avgConfidence * 100).toInt()}%).",
                    "Проверьте распознанные транзакции."
                )
            )
        } else {
            ImportResult.Success(
                transactions = transactions,
                documentType = documentType,
                totalConfidence = avgConfidence
            )
        }
    }

    companion object {
        private const val LOW_CONFIDENCE_THRESHOLD = 0.5f
        private const val ENHANCED_CONFIDENCE = 0.9f
    }
}

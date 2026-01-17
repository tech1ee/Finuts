package com.finuts.data.import.ocr

import com.finuts.ai.privacy.AnonymizationResult
import com.finuts.ai.privacy.PIIAnonymizer
import com.finuts.ai.privacy.DetectedPII
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the Privacy-First PDF parsing pipeline components.
 *
 * Note: Full integration tests with expect/actual classes (OcrService, PdfTextExtractor)
 * are run on each platform. These tests focus on the pipeline logic.
 */
class PrivacyFirstPdfParserTest {

    // === Pipeline Logic Tests ===

    @Test
    fun `preprocessor reduces text and keeps transactions`() {
        val preprocessor = DocumentPreprocessor()
        val text = """
            Bank Statement
            15.01.2026 -5000 KZT Glovo
            16.01.2026 +10000 KZT Salary
            Footer info
        """.trimIndent()

        val result = preprocessor.process(text)

        assertTrue(result.cleanedText.contains("15.01.2026"))
        assertTrue(result.cleanedText.contains("16.01.2026"))
    }

    @Test
    fun `local extractor parses transactions`() {
        val extractor = LocalTransactionExtractor()
        val text = """
            15.01.2026 -5000 KZT Glovo
            16.01.2026 +10000 KZT Salary
        """.trimIndent()

        val transactions = extractor.extract(text)

        assertEquals(2, transactions.size)
        assertEquals(-500000, transactions[0].amountMinorUnits)
        assertEquals(1000000, transactions[1].amountMinorUnits)
    }

    @Test
    fun `cloud enhancer adds merchant info`() = runTest {
        val llmProvider = FakeLLMProvider(
            response = """[{"index": 0, "merchant": "Glovo", "categoryHint": "food_delivery"}]"""
        )
        val enhancer = CloudTransactionEnhancer(llmProvider)

        val partial = PartialTransaction(
            rawDate = "15.01.2026",
            amountMinorUnits = -500000,
            currency = "KZT",
            rawDescription = "Glovo order",
            isCredit = false,
            isDebit = true
        )

        val enhanced = enhancer.enhance(listOf(partial))

        assertEquals("Glovo", enhanced[0].merchant)
        assertEquals("food_delivery", enhanced[0].categoryHint)
    }

    @Test
    fun `anonymizer replaces PII and allows deanonymization`() {
        val anonymizer = com.finuts.ai.privacy.RegexPIIAnonymizer()
        val text = "Transfer to Иванов А.С."

        val result = anonymizer.anonymize(text)
        assertTrue(result.wasModified)
        assertTrue(result.anonymizedText.contains("[PERSON_NAME_"))

        val restored = anonymizer.deanonymize(result.anonymizedText, result.mapping)
        assertEquals(text, restored)
    }

    // === Full Pipeline Integration (without expect classes) ===

    @Test
    fun `full pipeline processes OCR text to transactions`() = runTest {
        // Simulate OCR result
        val ocrText = """
            15.01.2026 -5000 KZT Glovo
            16.01.2026 +10000 KZT Salary
        """.trimIndent()

        // Step 1: Preprocess
        val preprocessor = DocumentPreprocessor()
        val preprocessed = preprocessor.process(ocrText)

        // Step 2: Local extract
        val extractor = LocalTransactionExtractor()
        val partial = extractor.extract(preprocessed.cleanedText)
        assertEquals(2, partial.size)

        // Step 3: Anonymize (nothing to anonymize in this case)
        val anonymizer = com.finuts.ai.privacy.RegexPIIAnonymizer()
        val anonymized = anonymizer.anonymize(preprocessed.cleanedText)

        // Step 4: Cloud enhance
        val llmProvider = FakeLLMProvider(
            response = """[
                {"index": 0, "merchant": "Glovo"},
                {"index": 1, "merchant": null, "categoryHint": "income"}
            ]"""
        )
        val enhancer = CloudTransactionEnhancer(llmProvider)
        val enhanced = enhancer.enhance(partial)

        // Verify
        assertEquals(2, enhanced.size)
        assertEquals("Glovo", enhanced[0].merchant)
        assertEquals("income", enhanced[1].categoryHint)
    }

    @Test
    fun `pipeline handles PII correctly`() = runTest {
        val ocrText = "15.01.2026 -5000 Перевод Иванов А.С."

        // Preprocess
        val preprocessor = DocumentPreprocessor()
        val preprocessed = preprocessor.process(ocrText)

        // Anonymize
        val anonymizer = com.finuts.ai.privacy.RegexPIIAnonymizer()
        val anonymized = anonymizer.anonymize(preprocessed.cleanedText)

        // Verify PII is replaced
        assertTrue(anonymized.wasModified)
        assertTrue(!anonymized.anonymizedText.contains("Иванов"))
        assertTrue(anonymized.anonymizedText.contains("[PERSON_NAME_"))

        // Extract with anonymized text
        val extractor = LocalTransactionExtractor()
        val partial = extractor.extract(preprocessed.cleanedText)

        // Enhance
        val llmProvider = FakeLLMProvider(
            response = """[{"index": 0, "counterpartyName": "[PERSON_NAME_1]"}]"""
        )
        val enhancer = CloudTransactionEnhancer(llmProvider)
        val enhanced = enhancer.enhance(partial)

        // Deanonymize the counterpartyName
        val restored = anonymizer.deanonymize(
            enhanced[0].counterpartyName ?: "",
            anonymized.mapping
        )
        assertEquals("Иванов А.С.", restored)
    }

    @Test
    fun `pipeline continues when LLM fails`() = runTest {
        val ocrText = "15.01.2026 -5000 KZT Payment"

        val preprocessor = DocumentPreprocessor()
        val preprocessed = preprocessor.process(ocrText)

        val extractor = LocalTransactionExtractor()
        val partial = extractor.extract(preprocessed.cleanedText)

        // LLM fails
        val llmProvider = FakeLLMProvider(shouldFail = true)
        val enhancer = CloudTransactionEnhancer(llmProvider)
        val enhanced = enhancer.enhance(partial)

        // Should still have the transaction, just without enhancements
        assertEquals(1, enhanced.size)
        assertEquals(-500000, enhanced[0].amountMinorUnits)
        assertEquals(null, enhanced[0].merchant)
    }

    // === Component Integration Tests ===

    @Test
    fun `preprocessor detects bank statement type`() {
        val preprocessor = DocumentPreprocessor()
        val text = """
            Выписка по счёту
            Период: 01.01.2026 - 31.01.2026
            15.01.2026 -5000 KZT Glovo
        """.trimIndent()

        val result = preprocessor.process(text)

        assertEquals(DocumentType.BANK_STATEMENT, result.hints.type)
    }

    @Test
    fun `preprocessor detects Russian language`() {
        val preprocessor = DocumentPreprocessor()
        val text = "Перевод на карту 15.01.2026 -5000 KZT"

        val result = preprocessor.process(text)

        assertEquals("ru", result.hints.language)
    }

    @Test
    fun `preprocessor detects Kazakh language`() {
        val preprocessor = DocumentPreprocessor()
        val text = "Төлем жасау 15.01.2026 -5000 KZT қазақстан"

        val result = preprocessor.process(text)

        assertEquals("kk", result.hints.language)
    }

    @Test
    fun `extractor handles various amount formats`() {
        val extractor = LocalTransactionExtractor()

        // Kaspi format with spaces
        val text1 = "15.01.2026 - 3 700,00 ₸ Glovo"
        val result1 = extractor.extract(text1)
        assertEquals(1, result1.size)
        assertEquals(-370000, result1[0].amountMinorUnits)

        // Standard format
        val text2 = "15.01.2026 +5000.00 USD Transfer"
        val result2 = extractor.extract(text2)
        assertEquals(1, result2.size)
        assertEquals(500000, result2[0].amountMinorUnits)
    }

    @Test
    fun `extractor detects currencies`() {
        val extractor = LocalTransactionExtractor()

        val text = "15.01.2026 -5000 ₸ Purchase"
        val result = extractor.extract(text)

        assertEquals("KZT", result[0].currency)
    }

    @Test
    fun `enhancer preserves original data on LLM failure`() = runTest {
        val llmProvider = FakeLLMProvider(shouldFail = true)
        val enhancer = CloudTransactionEnhancer(llmProvider)

        val original = PartialTransaction(
            rawDate = "15.01.2026",
            amountMinorUnits = -500000,
            currency = "KZT",
            rawDescription = "Test description",
            isCredit = false,
            isDebit = true
        )

        val result = enhancer.enhance(listOf(original))

        assertEquals(original.rawDate, result[0].rawDate)
        assertEquals(original.amountMinorUnits, result[0].amountMinorUnits)
        assertEquals(original.currency, result[0].currency)
        assertEquals(original.rawDescription, result[0].rawDescription)
    }

    // === Edge Cases ===

    @Test
    fun `handles empty input`() {
        val preprocessor = DocumentPreprocessor()
        val extractor = LocalTransactionExtractor()

        val preprocessed = preprocessor.process("")
        val extracted = extractor.extract("")

        assertEquals("", preprocessed.cleanedText)
        assertTrue(extracted.isEmpty())
    }

    @Test
    fun `handles text without transactions`() {
        val preprocessor = DocumentPreprocessor()
        val extractor = LocalTransactionExtractor()

        val text = "This is just some random text without any financial data."

        val preprocessed = preprocessor.process(text)
        val extracted = extractor.extract(preprocessed.cleanedText)

        assertTrue(extracted.isEmpty())
    }

    @Test
    fun `handles multiple currencies in same document`() {
        val extractor = LocalTransactionExtractor()

        val text = """
            15.01.2026 -5000 ₸ Glovo
            16.01.2026 -100 $ Amazon
            17.01.2026 +50 € Refund
        """.trimIndent()

        val result = extractor.extract(text)

        assertEquals(3, result.size)
        assertEquals("KZT", result[0].currency)
        assertEquals("USD", result[1].currency)
        assertEquals("EUR", result[2].currency)
    }
}

private class FakePIIAnonymizer : PIIAnonymizer {
    override fun anonymize(text: String): AnonymizationResult {
        return AnonymizationResult(
            anonymizedText = text,
            mapping = emptyMap(),
            detectedPII = emptyList(),
            wasModified = false
        )
    }

    override fun deanonymize(text: String, mapping: Map<String, String>): String = text

    override fun detectPII(text: String) = emptyList<DetectedPII>()
}

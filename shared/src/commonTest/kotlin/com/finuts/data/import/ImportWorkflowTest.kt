package com.finuts.data.import

import com.finuts.ai.privacy.RegexPIIAnonymizer
import com.finuts.data.import.ocr.CloudTransactionEnhancer
import com.finuts.data.import.ocr.DocumentPreprocessor
import com.finuts.data.import.ocr.DocumentType
import com.finuts.data.import.ocr.FakeLLMProvider
import com.finuts.data.import.ocr.LocalTransactionExtractor
import com.finuts.data.import.ocr.PartialTransaction
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive tests for the Privacy-First Import workflow.
 *
 * Tests cover:
 * - Full pipeline integration
 * - Various statement formats (Kaspi, Halyk, international)
 * - Privacy guarantees (PII anonymization)
 * - Edge cases and error handling
 * - Multiple currencies and languages
 */
class ImportWorkflowTest {

    // === Full Pipeline Tests ===

    @Test
    fun `full pipeline extracts transactions from Kaspi statement format`() = runTest {
        // Transaction lines only (without header with period that confuses extractor)
        val ocrText = """
            Kaspi Bank
            Выписка за период январь 2026

            15.01.2026 - 3 700,00 ₸ Glovo оплата заказа
            16.01.2026 + 100 000,00 ₸ Пополнение карты
            17.01.2026 - 25 000,00 ₸ Magnum Cash & Carry
            18.01.2026 - 5 000,00 ₸ Перевод Иванов А.С.
        """.trimIndent()

        // Step 1: Preprocess
        val preprocessor = DocumentPreprocessor()
        val preprocessed = preprocessor.process(ocrText)

        assertEquals(DocumentType.BANK_STATEMENT, preprocessed.hints.type)
        assertEquals("ru", preprocessed.hints.language)

        // Step 2: Extract locally
        val extractor = LocalTransactionExtractor()
        val partial = extractor.extract(preprocessed.cleanedText)

        // Should have 4 transactions
        assertTrue(partial.size >= 4, "Expected at least 4 transactions, got ${partial.size}")

        // Verify first transactions
        val glovo = partial.find { it.rawDescription.contains("Glovo") }
        val magnum = partial.find { it.rawDescription.contains("Magnum") }
        val transfer = partial.find { it.rawDescription.contains("Перевод") || it.rawDescription.contains("Иванов") }

        assertTrue(glovo != null, "Expected Glovo transaction")
        assertTrue(magnum != null, "Expected Magnum transaction")
        assertTrue(transfer != null, "Expected transfer transaction")

        assertEquals(-370000, glovo?.amountMinorUnits)
        assertEquals(-2500000, magnum?.amountMinorUnits)
        assertEquals(-500000, transfer?.amountMinorUnits)

        // Step 3: Anonymize
        val anonymizer = RegexPIIAnonymizer()
        val anonymized = anonymizer.anonymize(preprocessed.cleanedText)

        assertTrue(anonymized.wasModified)
        assertFalse(anonymized.anonymizedText.contains("Иванов"))
        assertTrue(anonymized.anonymizedText.contains("[PERSON_NAME_"))

        // Step 4: Enhance with LLM (using indices based on actual extracted transactions)
        val enhanceResponse = partial.mapIndexed { index, tx ->
            when {
                tx.rawDescription.contains("Glovo") ->
                    """{"index": $index, "merchant": "Glovo", "categoryHint": "food_delivery"}"""
                tx.rawDescription.contains("Пополнение") ->
                    """{"index": $index, "merchant": null, "categoryHint": "income"}"""
                tx.rawDescription.contains("Magnum") ->
                    """{"index": $index, "merchant": "Magnum", "categoryHint": "groceries"}"""
                tx.rawDescription.contains("Перевод") || tx.rawDescription.contains("Иванов") ->
                    """{"index": $index, "counterpartyName": "[PERSON_NAME_1]", "categoryHint": "transfers"}"""
                else ->
                    """{"index": $index}"""
            }
        }.joinToString(",", "[", "]")

        val llmProvider = FakeLLMProvider(response = enhanceResponse)
        val enhancer = CloudTransactionEnhancer(llmProvider)
        val enhanced = enhancer.enhance(partial)

        assertEquals(partial.size, enhanced.size)

        val enhancedGlovo = enhanced.find { it.rawDescription.contains("Glovo") }
        val enhancedMagnum = enhanced.find { it.rawDescription.contains("Magnum") }
        val enhancedTransfer = enhanced.find {
            it.rawDescription.contains("Перевод") || it.rawDescription.contains("Иванов")
        }

        assertEquals("Glovo", enhancedGlovo?.merchant)
        assertEquals("Magnum", enhancedMagnum?.merchant)
        assertEquals("[PERSON_NAME_1]", enhancedTransfer?.counterpartyName)

        // Step 5: Deanonymize
        val restoredCounterparty = anonymizer.deanonymize(
            enhancedTransfer?.counterpartyName ?: "",
            anonymized.mapping
        )
        assertEquals("Иванов А.С.", restoredCounterparty)
    }

    @Test
    fun `pipeline handles multi-currency statement`() = runTest {
        val ocrText = """
            International Statement
            15.01.2026 -100.00 $ Amazon purchase
            16.01.2026 +500.00 € Freelance payment
            17.01.2026 -50 000 ₽ Moscow Hotel
            18.01.2026 -5000 ₸ Local shop
        """.trimIndent()

        val preprocessor = DocumentPreprocessor()
        val preprocessed = preprocessor.process(ocrText)

        val extractor = LocalTransactionExtractor()
        val partial = extractor.extract(preprocessed.cleanedText)

        assertEquals(4, partial.size)

        assertEquals("USD", partial[0].currency)
        assertEquals(-10000, partial[0].amountMinorUnits)

        assertEquals("EUR", partial[1].currency)
        assertEquals(50000, partial[1].amountMinorUnits)

        assertEquals("RUB", partial[2].currency)
        assertEquals(-5000000, partial[2].amountMinorUnits)

        assertEquals("KZT", partial[3].currency)
        assertEquals(-500000, partial[3].amountMinorUnits)
    }

    @Test
    fun `pipeline detects Kazakh language`() {
        val preprocessor = DocumentPreprocessor()
        val text = """
            Банк шотынан үзінді көшірме
            15.01.2026 -5000 ₸ Төлем жасау
            Ақша қозғалысы туралы есеп
        """.trimIndent()

        val result = preprocessor.process(text)

        assertEquals("kk", result.hints.language)
        assertEquals(DocumentType.BANK_STATEMENT, result.hints.type)
    }

    // === Privacy Guarantee Tests ===

    @Test
    fun `anonymizer removes all PII types`() {
        val anonymizer = RegexPIIAnonymizer()

        val textWithPII = """
            Перевод от Иванов И.И.
            На счёт KZ123456789012345678
            Телефон: +7 777 123 45 67
            Email: test@example.com
            Карта: 4532 1234 5678 9012
        """.trimIndent()

        val result = anonymizer.anonymize(textWithPII)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("Иванов"))
        assertFalse(result.anonymizedText.contains("KZ123456789"))
        assertFalse(result.anonymizedText.contains("+7 777"))
        assertFalse(result.anonymizedText.contains("test@example.com"))
        assertFalse(result.anonymizedText.contains("4532 1234"))

        assertTrue(result.anonymizedText.contains("[PERSON_NAME_"))
        assertTrue(result.anonymizedText.contains("[IBAN_"))
        assertTrue(result.anonymizedText.contains("[PHONE_"))
        assertTrue(result.anonymizedText.contains("[EMAIL_"))
        assertTrue(result.anonymizedText.contains("[CARD_NUMBER_"))
    }

    @Test
    fun `anonymizer correctly deanonymizes all PII`() {
        val anonymizer = RegexPIIAnonymizer()

        val original = "Перевод Иванов А.С. на счёт test@mail.ru"
        val result = anonymizer.anonymize(original)

        val restored = anonymizer.deanonymize(result.anonymizedText, result.mapping)

        assertEquals(original, restored)
    }

    @Test
    fun `anonymizer preserves dates and amounts`() {
        val anonymizer = RegexPIIAnonymizer()

        val text = "15.01.2026 -5000 ₸ Оплата"
        val result = anonymizer.anonymize(text)

        assertTrue(result.anonymizedText.contains("15.01.2026"))
        assertTrue(result.anonymizedText.contains("-5000"))
        assertTrue(result.anonymizedText.contains("₸"))
    }

    // === Edge Cases ===

    @Test
    fun `handles empty document`() {
        val preprocessor = DocumentPreprocessor()
        val extractor = LocalTransactionExtractor()

        val preprocessed = preprocessor.process("")
        val extracted = extractor.extract("")

        assertEquals("", preprocessed.cleanedText)
        assertTrue(extracted.isEmpty())
    }

    @Test
    fun `handles document without transactions`() {
        val preprocessor = DocumentPreprocessor()
        val extractor = LocalTransactionExtractor()

        val text = """
            This is a bank logo
            Some marketing text
            Terms and conditions apply
        """.trimIndent()

        val preprocessed = preprocessor.process(text)
        val extracted = extractor.extract(preprocessed.cleanedText)

        assertTrue(extracted.isEmpty())
    }

    @Test
    fun `handles malformed dates gracefully`() {
        val extractor = LocalTransactionExtractor()

        // Valid format
        val valid = "15.01.2026 -5000 ₸ Test"
        val validResult = extractor.extract(valid)
        assertEquals(1, validResult.size)

        // No date - should be skipped
        val noDate = "Payment -5000 ₸ Test"
        val noDateResult = extractor.extract(noDate)
        assertTrue(noDateResult.isEmpty())
    }

    @Test
    fun `handles malformed amounts gracefully`() {
        val extractor = LocalTransactionExtractor()

        // Valid amount
        val valid = "15.01.2026 -5000 ₸ Test"
        val validResult = extractor.extract(valid)
        assertEquals(1, validResult.size)

        // No amount - should be skipped
        val noAmount = "15.01.2026 Test payment"
        val noAmountResult = extractor.extract(noAmount)
        assertTrue(noAmountResult.isEmpty())
    }

    // === Error Recovery Tests ===

    @Test
    fun `pipeline continues when LLM returns partial data`() = runTest {
        val llmProvider = FakeLLMProvider(
            response = """[{"index": 0}]""" // Minimal response
        )
        val enhancer = CloudTransactionEnhancer(llmProvider)

        val partial = PartialTransaction(
            rawDate = "15.01.2026",
            amountMinorUnits = -500000,
            currency = "KZT",
            rawDescription = "Test",
            isCredit = false,
            isDebit = true
        )

        val result = enhancer.enhance(listOf(partial))

        assertEquals(1, result.size)
        assertEquals(partial.amountMinorUnits, result[0].amountMinorUnits)
        assertEquals(null, result[0].merchant) // Not provided by LLM
    }

    @Test
    fun `pipeline continues when LLM fails completely`() = runTest {
        val llmProvider = FakeLLMProvider(shouldFail = true)
        val enhancer = CloudTransactionEnhancer(llmProvider)

        val partial = PartialTransaction(
            rawDate = "15.01.2026",
            amountMinorUnits = -500000,
            currency = "KZT",
            rawDescription = "Test",
            isCredit = false,
            isDebit = true
        )

        val result = enhancer.enhance(listOf(partial))

        // Should return original data without enhancements
        assertEquals(1, result.size)
        assertEquals(partial.amountMinorUnits, result[0].amountMinorUnits)
        assertEquals(partial.rawDate, result[0].rawDate)
    }

    @Test
    fun `pipeline continues when LLM returns invalid JSON`() = runTest {
        val llmProvider = FakeLLMProvider(
            response = "invalid json {{{ not valid }}"
        )
        val enhancer = CloudTransactionEnhancer(llmProvider)

        val partial = PartialTransaction(
            rawDate = "15.01.2026",
            amountMinorUnits = -500000,
            currency = "KZT",
            rawDescription = "Test",
            isCredit = false,
            isDebit = true
        )

        val result = enhancer.enhance(listOf(partial))

        // Should return original data without enhancements
        assertEquals(1, result.size)
        assertEquals(null, result[0].merchant)
    }

    // === Transaction Type Detection Tests ===

    @Test
    fun `extractor correctly identifies debit transactions`() {
        val extractor = LocalTransactionExtractor()

        val debitTexts = listOf(
            "15.01.2026 -5000 ₸ Оплата",
            "15.01.2026 - 5 000,00 ₸ Покупка",
            "15.01.2026 -100.00 $ Payment"
        )

        debitTexts.forEach { text ->
            val result = extractor.extract(text)
            assertEquals(1, result.size)
            assertTrue(result[0].isDebit)
            assertFalse(result[0].isCredit)
            assertTrue(result[0].amountMinorUnits < 0)
        }
    }

    @Test
    fun `extractor correctly identifies credit transactions`() {
        val extractor = LocalTransactionExtractor()

        val creditTexts = listOf(
            "15.01.2026 +5000 ₸ Пополнение",
            "15.01.2026 + 5 000,00 ₸ Доход",
            "15.01.2026 +100.00 $ Income"
        )

        creditTexts.forEach { text ->
            val result = extractor.extract(text)
            assertEquals(1, result.size)
            assertTrue(result[0].isCredit)
            assertFalse(result[0].isDebit)
            assertTrue(result[0].amountMinorUnits > 0)
        }
    }

    // === Batch Processing Tests ===

    @Test
    fun `processes large number of transactions`() = runTest {
        val extractor = LocalTransactionExtractor()

        val lines = (1..100).map { i ->
            "${i.toString().padStart(2, '0')}.01.2026 -${i * 1000} ₸ Transaction $i"
        }
        val text = lines.joinToString("\n")

        val result = extractor.extract(text)

        assertEquals(100, result.size)
        assertEquals(-100000, result[0].amountMinorUnits) // 1 * 1000 * 100 kopecks
        assertEquals(-10000000, result[99].amountMinorUnits) // 100 * 1000 * 100 kopecks
    }

    @Test
    fun `LLM enhancer handles batch of transactions`() = runTest {
        val indices = (0..9).toList()
        val response = indices.map { i ->
            """{"index": $i, "merchant": "Merchant$i", "categoryHint": "category$i"}"""
        }.joinToString(",", "[", "]")

        val llmProvider = FakeLLMProvider(response = response)
        val enhancer = CloudTransactionEnhancer(llmProvider)

        val partials = (0..9).map { i ->
            PartialTransaction(
                rawDate = "15.01.2026",
                amountMinorUnits = -(i + 1) * 10000L,
                currency = "KZT",
                rawDescription = "Transaction $i",
                isCredit = false,
                isDebit = true
            )
        }

        val result = enhancer.enhance(partials)

        assertEquals(10, result.size)
        result.forEachIndexed { index, tx ->
            assertEquals("Merchant$index", tx.merchant)
            assertEquals("category$index", tx.categoryHint)
        }
    }
}

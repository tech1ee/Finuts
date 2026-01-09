package com.finuts.domain.entity.import

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for ImportResult sealed interface and its implementations.
 */
class ImportResultTest {

    @Test
    fun `Success can be created with transactions and document type`() {
        val transactions = listOf(createTestTransaction())
        val result = ImportResult.Success(
            transactions = transactions,
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.95f
        )

        assertEquals(1, result.transactions.size)
        assertIs<DocumentType.Csv>(result.documentType)
        assertEquals(0.95f, result.totalConfidence)
    }

    @Test
    fun `Success with multiple transactions`() {
        val transactions = listOf(
            createTestTransaction(amount = 10000L),
            createTestTransaction(amount = 20000L),
            createTestTransaction(amount = 30000L)
        )
        val result = ImportResult.Success(
            transactions = transactions,
            documentType = DocumentType.Pdf("kaspi"),
            totalConfidence = 0.85f
        )

        assertEquals(3, result.transactions.size)
        assertEquals(0.85f, result.totalConfidence)
    }

    @Test
    fun `Success with empty transactions list`() {
        val result = ImportResult.Success(
            transactions = emptyList(),
            documentType = DocumentType.Ofx("2.2"),
            totalConfidence = 1.0f
        )

        assertTrue(result.transactions.isEmpty())
    }

    @Test
    fun `Success with different document types`() {
        val types = listOf(
            DocumentType.Csv(';', "windows-1251"),
            DocumentType.Pdf(null),
            DocumentType.Ofx("1.6"),
            DocumentType.Qif("Bank"),
            DocumentType.Image("PNG")
        )

        types.forEach { docType ->
            val result = ImportResult.Success(
                transactions = listOf(createTestTransaction()),
                documentType = docType,
                totalConfidence = 0.9f
            )
            assertEquals(docType, result.documentType)
        }
    }

    @Test
    fun `Error can be created with message and document type`() {
        val result = ImportResult.Error(
            message = "Failed to parse file",
            documentType = DocumentType.Pdf("unknown"),
            partialTransactions = emptyList()
        )

        assertEquals("Failed to parse file", result.message)
        assertIs<DocumentType.Pdf>(result.documentType)
        assertTrue(result.partialTransactions.isEmpty())
    }

    @Test
    fun `Error with partial transactions`() {
        val partial = listOf(createTestTransaction(confidence = 0.3f))
        val result = ImportResult.Error(
            message = "Some rows could not be parsed",
            documentType = DocumentType.Csv(',', "UTF-8"),
            partialTransactions = partial
        )

        assertEquals(1, result.partialTransactions.size)
        assertEquals(0.3f, result.partialTransactions.first().confidence)
    }

    @Test
    fun `Error with null document type for unrecognized format`() {
        val result = ImportResult.Error(
            message = "Unknown file format",
            documentType = null,
            partialTransactions = emptyList()
        )

        assertEquals(null, result.documentType)
    }

    @Test
    fun `NeedsUserInput can be created with ambiguous transactions`() {
        val transactions = listOf(
            createTestTransaction(confidence = 0.5f),
            createTestTransaction(confidence = 0.4f)
        )
        val result = ImportResult.NeedsUserInput(
            transactions = transactions,
            documentType = DocumentType.Pdf(null),
            issues = listOf("Date format ambiguous", "Amount unclear")
        )

        assertEquals(2, result.transactions.size)
        assertEquals(2, result.issues.size)
        assertTrue("Date format ambiguous" in result.issues)
    }

    @Test
    fun `NeedsUserInput with single issue`() {
        val result = ImportResult.NeedsUserInput(
            transactions = listOf(createTestTransaction()),
            documentType = DocumentType.Image("JPEG"),
            issues = listOf("OCR confidence too low")
        )

        assertEquals(1, result.issues.size)
    }

    @Test
    fun `all ImportResult implementations can be used polymorphically`() {
        val results: List<ImportResult> = listOf(
            ImportResult.Success(
                transactions = listOf(createTestTransaction()),
                documentType = DocumentType.Csv(',', "UTF-8"),
                totalConfidence = 0.9f
            ),
            ImportResult.Error(
                message = "Parse error",
                documentType = DocumentType.Unknown,
                partialTransactions = emptyList()
            ),
            ImportResult.NeedsUserInput(
                transactions = listOf(createTestTransaction()),
                documentType = DocumentType.Pdf(null),
                issues = listOf("Review needed")
            )
        )

        assertEquals(3, results.size)
    }

    @Test
    fun `when expression covers all ImportResult types`() {
        val results = listOf(
            ImportResult.Success(emptyList(), DocumentType.Csv(',', "UTF-8"), 1.0f),
            ImportResult.Error("error", null, emptyList()),
            ImportResult.NeedsUserInput(emptyList(), DocumentType.Unknown, emptyList())
        )

        results.forEach { result ->
            val type = when (result) {
                is ImportResult.Success -> "success"
                is ImportResult.Error -> "error"
                is ImportResult.NeedsUserInput -> "needs_input"
            }
            assertTrue(type.isNotEmpty())
        }
    }

    @Test
    fun `smart cast works for ImportResult`() {
        val result: ImportResult = ImportResult.Success(
            transactions = listOf(createTestTransaction()),
            documentType = DocumentType.Ofx("2.2"),
            totalConfidence = 0.88f
        )

        when (result) {
            is ImportResult.Success -> {
                assertEquals(1, result.transactions.size)
                assertEquals(0.88f, result.totalConfidence)
            }
            else -> throw AssertionError("Expected Success")
        }
    }

    @Test
    fun `ImportResult Success with high confidence from rule-based parsing`() {
        val tx = createTestTransaction(
            source = ImportSource.RULE_BASED,
            confidence = 0.99f
        )
        val result = ImportResult.Success(
            transactions = listOf(tx),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.99f
        )

        assertEquals(ImportSource.RULE_BASED, result.transactions.first().source)
    }

    @Test
    fun `ImportResult Success with LLM enhanced parsing`() {
        val tx = createTestTransaction(
            source = ImportSource.LLM_ENHANCED,
            confidence = 0.75f
        )
        val result = ImportResult.Success(
            transactions = listOf(tx),
            documentType = DocumentType.Pdf("unknown"),
            totalConfidence = 0.75f
        )

        assertEquals(ImportSource.LLM_ENHANCED, result.transactions.first().source)
    }

    // Helper function
    private fun createTestTransaction(
        date: LocalDate = LocalDate(2024, 1, 15),
        amount: Long = 10000L,
        description: String = "Test",
        merchant: String? = null,
        balance: Long? = null,
        category: String? = null,
        confidence: Float = 0.9f,
        source: ImportSource = ImportSource.RULE_BASED,
        rawData: Map<String, String> = emptyMap()
    ) = ImportedTransaction(
        date = date,
        amount = amount,
        description = description,
        merchant = merchant,
        balance = balance,
        category = category,
        confidence = confidence,
        source = source,
        rawData = rawData
    )
}

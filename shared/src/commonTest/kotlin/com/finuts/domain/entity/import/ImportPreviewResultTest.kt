package com.finuts.domain.entity.import

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ImportPreviewResult data class.
 * Tests the preview result shown to user before confirming import.
 */
class ImportPreviewResultTest {

    private val sampleTransaction = ReviewableTransaction(
        index = 0,
        transaction = ImportedTransaction(
            date = LocalDate(2026, 1, 8),
            amount = -5000L,
            description = "Coffee Shop",
            confidence = 0.9f,
            source = ImportSource.RULE_BASED
        ),
        duplicateStatus = DuplicateStatus.Unique,
        isSelected = true,
        categoryOverride = null
    )

    @Test
    fun `ImportPreviewResult contains all required fields`() {
        val docType = DocumentType.Csv(',', "UTF-8")
        val warnings = listOf("Future date detected")

        val result = ImportPreviewResult(
            transactions = listOf(sampleTransaction),
            documentType = docType,
            duplicateCount = 0,
            validationWarnings = warnings
        )

        assertEquals(1, result.transactions.size)
        assertEquals(docType, result.documentType)
        assertEquals(0, result.duplicateCount)
        assertEquals(warnings, result.validationWarnings)
    }

    @Test
    fun `totalCount returns total number of transactions`() {
        val transactions = listOf(
            sampleTransaction,
            sampleTransaction.copy(index = 1),
            sampleTransaction.copy(index = 2)
        )

        val result = ImportPreviewResult(
            transactions = transactions,
            documentType = DocumentType.Unknown,
            duplicateCount = 0,
            validationWarnings = emptyList()
        )

        assertEquals(3, result.totalCount)
    }

    @Test
    fun `selectedCount returns count of selected transactions`() {
        val transactions = listOf(
            sampleTransaction.copy(index = 0, isSelected = true),
            sampleTransaction.copy(index = 1, isSelected = false),
            sampleTransaction.copy(index = 2, isSelected = true)
        )

        val result = ImportPreviewResult(
            transactions = transactions,
            documentType = DocumentType.Unknown,
            duplicateCount = 0,
            validationWarnings = emptyList()
        )

        assertEquals(2, result.selectedCount)
    }

    @Test
    fun `hasWarnings returns true when warnings exist`() {
        val result = ImportPreviewResult(
            transactions = listOf(sampleTransaction),
            documentType = DocumentType.Unknown,
            duplicateCount = 0,
            validationWarnings = listOf("Warning 1")
        )

        assertTrue(result.hasWarnings)
    }

    @Test
    fun `hasWarnings returns false when no warnings`() {
        val result = ImportPreviewResult(
            transactions = listOf(sampleTransaction),
            documentType = DocumentType.Unknown,
            duplicateCount = 0,
            validationWarnings = emptyList()
        )

        assertFalse(result.hasWarnings)
    }

    @Test
    fun `hasDuplicates returns true when duplicates exist`() {
        val result = ImportPreviewResult(
            transactions = listOf(sampleTransaction),
            documentType = DocumentType.Unknown,
            duplicateCount = 5,
            validationWarnings = emptyList()
        )

        assertTrue(result.hasDuplicates)
    }

    @Test
    fun `hasDuplicates returns false when no duplicates`() {
        val result = ImportPreviewResult(
            transactions = listOf(sampleTransaction),
            documentType = DocumentType.Unknown,
            duplicateCount = 0,
            validationWarnings = emptyList()
        )

        assertFalse(result.hasDuplicates)
    }

    @Test
    fun `totalIncome calculates sum of positive amounts for selected`() {
        val transactions = listOf(
            sampleTransaction.copy(
                index = 0,
                transaction = sampleTransaction.transaction.copy(amount = 10000L),
                isSelected = true
            ),
            sampleTransaction.copy(
                index = 1,
                transaction = sampleTransaction.transaction.copy(amount = 5000L),
                isSelected = true
            ),
            sampleTransaction.copy(
                index = 2,
                transaction = sampleTransaction.transaction.copy(amount = 3000L),
                isSelected = false
            )
        )

        val result = ImportPreviewResult(
            transactions = transactions,
            documentType = DocumentType.Unknown,
            duplicateCount = 0,
            validationWarnings = emptyList()
        )

        assertEquals(15000L, result.totalIncome)
    }

    @Test
    fun `totalExpenses calculates sum of negative amounts for selected`() {
        val transactions = listOf(
            sampleTransaction.copy(
                index = 0,
                transaction = sampleTransaction.transaction.copy(amount = -10000L),
                isSelected = true
            ),
            sampleTransaction.copy(
                index = 1,
                transaction = sampleTransaction.transaction.copy(amount = -5000L),
                isSelected = true
            ),
            sampleTransaction.copy(
                index = 2,
                transaction = sampleTransaction.transaction.copy(amount = -3000L),
                isSelected = false
            )
        )

        val result = ImportPreviewResult(
            transactions = transactions,
            documentType = DocumentType.Unknown,
            duplicateCount = 0,
            validationWarnings = emptyList()
        )

        assertEquals(-15000L, result.totalExpenses)
    }
}

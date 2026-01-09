package com.finuts.domain.entity.import

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ImportProgress sealed interface.
 * Tests all possible progress states during import.
 */
class ImportProgressTest {

    @Test
    fun `Idle is initial state`() {
        val progress: ImportProgress = ImportProgress.Idle

        assertIs<ImportProgress.Idle>(progress)
        assertFalse(progress.isTerminal)
    }

    @Test
    fun `DetectingFormat contains filename and file size`() {
        val filename = "statement.csv"
        val fileSize = 1024L

        val progress = ImportProgress.DetectingFormat(filename, fileSize)

        assertEquals(filename, progress.filename)
        assertEquals(fileSize, progress.fileSizeBytes)
        assertFalse(progress.isTerminal)
    }

    @Test
    fun `Parsing contains document type`() {
        val docType = DocumentType.Csv(delimiter = ',', encoding = "UTF-8")

        val progress = ImportProgress.Parsing(docType)

        assertEquals(docType, progress.documentType)
        assertFalse(progress.isTerminal)
    }

    @Test
    fun `Validating contains total and processed counts`() {
        val total = 50
        val processed = 25

        val progress = ImportProgress.Validating(total, processed)

        assertEquals(total, progress.totalTransactions)
        assertEquals(processed, progress.processedCount)
        assertFalse(progress.isTerminal)
    }

    @Test
    fun `Deduplicating contains counts and duplicates found`() {
        val total = 100
        val processed = 50
        val duplicates = 5

        val progress = ImportProgress.Deduplicating(total, processed, duplicates)

        assertEquals(total, progress.totalTransactions)
        assertEquals(processed, progress.processedCount)
        assertEquals(duplicates, progress.duplicatesFound)
        assertFalse(progress.isTerminal)
    }

    @Test
    fun `Categorizing contains counts and current tier`() {
        val total = 100
        val categorized = 80
        val tier = CategorizationTier.RULE_BASED

        val progress = ImportProgress.Categorizing(total, categorized, tier)

        assertEquals(total, progress.totalTransactions)
        assertEquals(categorized, progress.categorizedCount)
        assertEquals(tier, progress.currentTier)
        assertFalse(progress.isTerminal)
    }

    @Test
    fun `AwaitingConfirmation contains preview result`() {
        val previewResult = ImportPreviewResult(
            transactions = emptyList(),
            documentType = DocumentType.Csv(',', "UTF-8"),
            duplicateCount = 0,
            validationWarnings = emptyList()
        )

        val progress = ImportProgress.AwaitingConfirmation(previewResult)

        assertEquals(previewResult, progress.result)
        assertFalse(progress.isTerminal)
    }

    @Test
    fun `Saving contains total and saved counts`() {
        val total = 50
        val saved = 30

        val progress = ImportProgress.Saving(total, saved)

        assertEquals(total, progress.totalTransactions)
        assertEquals(saved, progress.savedCount)
        assertFalse(progress.isTerminal)
    }

    @Test
    fun `Completed is terminal state with counts`() {
        val saved = 45
        val skipped = 3
        val duplicates = 2

        val progress = ImportProgress.Completed(saved, skipped, duplicates)

        assertEquals(saved, progress.savedCount)
        assertEquals(skipped, progress.skippedCount)
        assertEquals(duplicates, progress.duplicateCount)
        assertTrue(progress.isTerminal)
    }

    @Test
    fun `Failed is terminal state with error info`() {
        val message = "Parse error"
        val recoverable = true

        val progress = ImportProgress.Failed(message, recoverable, null)

        assertEquals(message, progress.message)
        assertEquals(recoverable, progress.recoverable)
        assertNull(progress.partialResult)
        assertTrue(progress.isTerminal)
    }

    @Test
    fun `Failed can contain partial result`() {
        val partialResult = ImportPreviewResult(
            transactions = emptyList(),
            documentType = DocumentType.Unknown,
            duplicateCount = 0,
            validationWarnings = emptyList()
        )

        val progress = ImportProgress.Failed(
            message = "Partial failure",
            recoverable = false,
            partialResult = partialResult
        )

        assertEquals(partialResult, progress.partialResult)
    }

    @Test
    fun `Cancelled is terminal state`() {
        val progress: ImportProgress = ImportProgress.Cancelled

        assertIs<ImportProgress.Cancelled>(progress)
        assertTrue(progress.isTerminal)
    }
}

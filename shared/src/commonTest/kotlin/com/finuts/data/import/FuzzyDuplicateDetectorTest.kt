package com.finuts.data.import

import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.entity.import.DuplicateStatus
import com.finuts.domain.entity.import.ImportSource
import com.finuts.domain.entity.import.ImportedTransaction
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for FuzzyDuplicateDetector.
 * Tests the algorithm for detecting duplicate transactions.
 */
class FuzzyDuplicateDetectorTest {

    private val detector = FuzzyDuplicateDetector()
    private val tz = TimeZone.currentSystemDefault()

    private fun LocalDate.toInstant(): Instant = atStartOfDayIn(tz)

    private fun createImported(
        date: LocalDate = LocalDate(2026, 1, 8),
        amount: Long = -5000L,
        description: String = "Coffee Shop"
    ) = ImportedTransaction(
        date = date,
        amount = amount,
        description = description,
        confidence = 0.9f,
        source = ImportSource.RULE_BASED
    )

    private fun createExisting(
        id: String = "tx-123",
        date: LocalDate = LocalDate(2026, 1, 8),
        amount: Long = -5000L,
        description: String = "Coffee Shop"
    ): Transaction {
        val now = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
        return Transaction(
            id = id,
            accountId = "acc-1",
            date = date.toInstant(),
            amount = amount,
            description = description,
            type = TransactionType.EXPENSE,
            categoryId = "food",
            merchant = null,
            note = null,
            createdAt = now,
            updatedAt = now
        )
    }

    // === Exact Duplicate Detection ===

    @Test
    fun `detects exact duplicate with same date amount and description`() {
        val imported = createImported()
        val existing = listOf(createExisting())

        val status = detector.checkDuplicate(imported, existing)

        assertIs<DuplicateStatus.ExactDuplicate>(status)
        assertEquals("tx-123", status.matchingTransactionId)
    }

    @Test
    fun `returns Unique when no matching transaction exists`() {
        val imported = createImported()
        val existing = listOf(
            createExisting(id = "tx-1", date = LocalDate(2026, 1, 1), amount = -1000L)
        )

        val status = detector.checkDuplicate(imported, existing)

        assertIs<DuplicateStatus.Unique>(status)
    }

    @Test
    fun `returns Unique when existing list is empty`() {
        val imported = createImported()

        val status = detector.checkDuplicate(imported, emptyList())

        assertIs<DuplicateStatus.Unique>(status)
    }

    // === Probable Duplicate Detection ===

    @Test
    fun `detects probable duplicate with similar description`() {
        val imported = createImported(description = "Coffee Shop Payment")
        val existing = listOf(createExisting(description = "Coffee Shop"))

        val status = detector.checkDuplicate(imported, existing)

        assertIs<DuplicateStatus.ProbableDuplicate>(status)
        assertTrue(status.similarity >= 0.5f)
    }

    @Test
    fun `detects probable duplicate with date plus minus one day`() {
        val imported = createImported(date = LocalDate(2026, 1, 9))
        val existing = listOf(createExisting(date = LocalDate(2026, 1, 8)))

        val status = detector.checkDuplicate(imported, existing)

        assertTrue(status.isDuplicate)
    }

    @Test
    fun `returns Unique when date differs by more than one day`() {
        val imported = createImported(date = LocalDate(2026, 1, 15))
        val existing = listOf(createExisting(date = LocalDate(2026, 1, 8)))

        val status = detector.checkDuplicate(imported, existing)

        assertIs<DuplicateStatus.Unique>(status)
    }

    @Test
    fun `returns Unique when amount differs`() {
        val imported = createImported(amount = -10000L)
        val existing = listOf(createExisting(amount = -5000L))

        val status = detector.checkDuplicate(imported, existing)

        assertIs<DuplicateStatus.Unique>(status)
    }

    // === Description Normalization ===

    @Test
    fun `normalizes description case for comparison`() {
        val imported = createImported(description = "COFFEE SHOP")
        val existing = listOf(createExisting(description = "coffee shop"))

        val status = detector.checkDuplicate(imported, existing)

        assertIs<DuplicateStatus.ExactDuplicate>(status)
    }

    @Test
    fun `normalizes extra whitespace in descriptions`() {
        val imported = createImported(description = "Coffee   Shop")
        val existing = listOf(createExisting(description = "Coffee Shop"))

        val status = detector.checkDuplicate(imported, existing)

        assertIs<DuplicateStatus.ExactDuplicate>(status)
    }

    @Test
    fun `removes special characters for comparison`() {
        val imported = createImported(description = "Coffee Shop*123")
        val existing = listOf(createExisting(description = "Coffee Shop"))

        val status = detector.checkDuplicate(imported, existing)

        assertTrue(status.isDuplicate)
    }

    // === Batch Processing ===

    @Test
    fun `checkDuplicates processes multiple transactions`() {
        val imported = listOf(
            createImported(description = "Transaction 1"),
            createImported(description = "Transaction 2"),
            createImported(description = "Coffee Shop")
        )
        val existing = listOf(createExisting())

        val results = detector.checkDuplicates(imported, existing)

        assertEquals(3, results.size)
        assertIs<DuplicateStatus.Unique>(results[0])
        assertIs<DuplicateStatus.Unique>(results[1])
        assertIs<DuplicateStatus.ExactDuplicate>(results[2])
    }

    @Test
    fun `checkDuplicates returns correct indices as keys`() {
        val imported = listOf(
            createImported(description = "Coffee Shop"),
            createImported(description = "Unique Transaction")
        )
        val existing = listOf(createExisting())

        val results = detector.checkDuplicates(imported, existing)

        assertTrue(results.containsKey(0))
        assertTrue(results.containsKey(1))
        assertIs<DuplicateStatus.ExactDuplicate>(results[0])
        assertIs<DuplicateStatus.Unique>(results[1])
    }

    // === Similarity Scoring ===

    @Test
    fun `similarity score is 1 point 0 for exact matches`() {
        val imported = createImported()
        val existing = listOf(createExisting())

        val status = detector.checkDuplicate(imported, existing)

        assertIs<DuplicateStatus.ExactDuplicate>(status)
        assertEquals(1.0f, status.similarity)
    }

    @Test
    fun `similarity score reflects description difference`() {
        val imported = createImported(description = "Coffee Shop Downtown")
        val existing = listOf(createExisting(description = "Coffee Shop"))

        val status = detector.checkDuplicate(imported, existing)

        assertIs<DuplicateStatus.ProbableDuplicate>(status)
        assertTrue(status.similarity in 0.5f..0.94f)
    }
}

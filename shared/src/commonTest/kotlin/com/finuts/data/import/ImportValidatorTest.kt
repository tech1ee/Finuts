package com.finuts.data.import

import com.finuts.domain.entity.import.ImportSource
import com.finuts.domain.entity.import.ImportedTransaction
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ImportValidator.
 * Tests validation of imported transactions.
 */
class ImportValidatorTest {

    private val validator = ImportValidator()
    private val today: LocalDate = run {
        val now = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
        now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    private fun createTransaction(
        date: LocalDate = today,
        amount: Long = -5000L,
        description: String = "Test Transaction"
    ) = ImportedTransaction(
        date = date,
        amount = amount,
        description = description,
        confidence = 0.9f,
        source = ImportSource.RULE_BASED
    )

    // === Date Validation ===

    @Test
    fun `detects future date as warning`() {
        val futureDate = LocalDate(today.year + 1, 1, 15)
        val transaction = createTransaction(date = futureDate)

        val result = validator.validate(listOf(transaction))

        assertTrue(result.hasWarnings)
        assertTrue(result.warnings.any { it.contains("future", ignoreCase = true) })
    }

    @Test
    fun `accepts past dates without warning`() {
        val pastDate = LocalDate(today.year - 1, 1, 15)
        val transaction = createTransaction(date = pastDate)

        val result = validator.validate(listOf(transaction))

        assertFalse(result.warnings.any { it.contains("future", ignoreCase = true) })
    }

    @Test
    fun `accepts today date without warning`() {
        val transaction = createTransaction(date = today)

        val result = validator.validate(listOf(transaction))

        assertFalse(result.warnings.any { it.contains("future", ignoreCase = true) })
    }

    // === Amount Validation ===

    @Test
    fun `detects unusually large amount as warning`() {
        val transaction = createTransaction(amount = -10_000_000_00L) // 10 million

        val result = validator.validate(listOf(transaction))

        assertTrue(result.hasWarnings)
        assertTrue(result.warnings.any { it.contains("amount") || it.contains("сумм") })
    }

    @Test
    fun `accepts normal amounts without warning`() {
        val transaction = createTransaction(amount = -50000L) // 500

        val result = validator.validate(listOf(transaction))

        assertFalse(result.warnings.any { it.contains("amount") || it.contains("сумм") })
    }

    // === Description Validation ===

    @Test
    fun `detects empty description as warning`() {
        val transaction = createTransaction(description = "")

        val result = validator.validate(listOf(transaction))

        assertTrue(result.hasWarnings)
    }

    @Test
    fun `detects whitespace-only description as warning`() {
        val transaction = createTransaction(description = "   ")

        val result = validator.validate(listOf(transaction))

        assertTrue(result.hasWarnings)
    }

    // === Batch Validation ===

    @Test
    fun `validates multiple transactions and aggregates warnings`() {
        val transactions = listOf(
            createTransaction(description = ""),
            createTransaction(date = LocalDate(today.year + 1, 1, 1)),
            createTransaction() // valid
        )

        val result = validator.validate(transactions)

        assertTrue(result.hasWarnings)
        assertTrue(result.warnings.size >= 2)
    }

    @Test
    fun `returns valid result for correct transactions`() {
        val transactions = listOf(
            createTransaction(description = "Transaction 1"),
            createTransaction(description = "Transaction 2")
        )

        val result = validator.validate(transactions)

        assertTrue(result.isValid)
    }

    @Test
    fun `validation result contains warning count`() {
        val transactions = listOf(
            createTransaction(description = ""),
            createTransaction(description = "  ")
        )

        val result = validator.validate(transactions)

        assertEquals(2, result.warningCount)
    }
}

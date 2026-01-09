package com.finuts.domain.entity.import

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ReviewableTransaction data class.
 * Tests the wrapper for transactions during review step.
 */
class ReviewableTransactionTest {

    private val baseTransaction = ImportedTransaction(
        date = LocalDate(2026, 1, 8),
        amount = -5000L,
        description = "Coffee Shop",
        category = "food",
        confidence = 0.9f,
        source = ImportSource.RULE_BASED
    )

    @Test
    fun `ReviewableTransaction wraps ImportedTransaction with index`() {
        val reviewable = ReviewableTransaction(
            index = 5,
            transaction = baseTransaction,
            duplicateStatus = DuplicateStatus.Unique,
            isSelected = true,
            categoryOverride = null
        )

        assertEquals(5, reviewable.index)
        assertEquals(baseTransaction, reviewable.transaction)
    }

    @Test
    fun `isSelected defaults to true for unique transactions`() {
        val reviewable = ReviewableTransaction(
            index = 0,
            transaction = baseTransaction,
            duplicateStatus = DuplicateStatus.Unique,
            isSelected = true,
            categoryOverride = null
        )

        assertTrue(reviewable.isSelected)
    }

    @Test
    fun `categoryOverride is null by default`() {
        val reviewable = ReviewableTransaction(
            index = 0,
            transaction = baseTransaction,
            duplicateStatus = DuplicateStatus.Unique,
            isSelected = true,
            categoryOverride = null
        )

        assertNull(reviewable.categoryOverride)
    }

    @Test
    fun `categoryOverride can be set to different category`() {
        val reviewable = ReviewableTransaction(
            index = 0,
            transaction = baseTransaction,
            duplicateStatus = DuplicateStatus.Unique,
            isSelected = true,
            categoryOverride = "transport"
        )

        assertEquals("transport", reviewable.categoryOverride)
    }

    @Test
    fun `effectiveCategory returns override when set`() {
        val reviewable = ReviewableTransaction(
            index = 0,
            transaction = baseTransaction,
            duplicateStatus = DuplicateStatus.Unique,
            isSelected = true,
            categoryOverride = "entertainment"
        )

        assertEquals("entertainment", reviewable.effectiveCategory)
    }

    @Test
    fun `effectiveCategory returns original category when no override`() {
        val reviewable = ReviewableTransaction(
            index = 0,
            transaction = baseTransaction,
            duplicateStatus = DuplicateStatus.Unique,
            isSelected = true,
            categoryOverride = null
        )

        assertEquals("food", reviewable.effectiveCategory)
    }

    @Test
    fun `isProbableDuplicate returns true for ProbableDuplicate status`() {
        val reviewable = ReviewableTransaction(
            index = 0,
            transaction = baseTransaction,
            duplicateStatus = DuplicateStatus.ProbableDuplicate(
                matchingTransactionId = "tx-123",
                similarity = 0.8f,
                reason = "Same date and amount"
            ),
            isSelected = false,
            categoryOverride = null
        )

        assertTrue(reviewable.isProbableDuplicate)
    }

    @Test
    fun `isProbableDuplicate returns false for Unique status`() {
        val reviewable = ReviewableTransaction(
            index = 0,
            transaction = baseTransaction,
            duplicateStatus = DuplicateStatus.Unique,
            isSelected = true,
            categoryOverride = null
        )

        assertFalse(reviewable.isProbableDuplicate)
    }

    @Test
    fun `isExactDuplicate returns true for ExactDuplicate status`() {
        val reviewable = ReviewableTransaction(
            index = 0,
            transaction = baseTransaction,
            duplicateStatus = DuplicateStatus.ExactDuplicate(
                matchingTransactionId = "tx-456"
            ),
            isSelected = false,
            categoryOverride = null
        )

        assertTrue(reviewable.isExactDuplicate)
    }

    @Test
    fun `isExactDuplicate returns false for ProbableDuplicate status`() {
        val reviewable = ReviewableTransaction(
            index = 0,
            transaction = baseTransaction,
            duplicateStatus = DuplicateStatus.ProbableDuplicate(
                matchingTransactionId = "tx-789",
                similarity = 0.75f,
                reason = "Similar"
            ),
            isSelected = false,
            categoryOverride = null
        )

        assertFalse(reviewable.isExactDuplicate)
    }
}

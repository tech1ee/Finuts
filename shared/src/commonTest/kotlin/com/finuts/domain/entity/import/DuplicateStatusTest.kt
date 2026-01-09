package com.finuts.domain.entity.import

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for DuplicateStatus sealed interface.
 * Tests duplicate detection result states.
 */
class DuplicateStatusTest {

    @Test
    fun `Unique status indicates no duplicate found`() {
        val status: DuplicateStatus = DuplicateStatus.Unique

        assertIs<DuplicateStatus.Unique>(status)
    }

    @Test
    fun `Unique status isDuplicate returns false`() {
        val status = DuplicateStatus.Unique

        assertFalse(status.isDuplicate)
    }

    @Test
    fun `ExactDuplicate contains matching transaction id`() {
        val matchingId = "tx-123"
        val status = DuplicateStatus.ExactDuplicate(matchingTransactionId = matchingId)

        assertEquals(matchingId, status.matchingTransactionId)
    }

    @Test
    fun `ExactDuplicate isDuplicate returns true`() {
        val status = DuplicateStatus.ExactDuplicate(matchingTransactionId = "tx-123")

        assertTrue(status.isDuplicate)
    }

    @Test
    fun `ExactDuplicate has maximum similarity of 1 point 0`() {
        val status = DuplicateStatus.ExactDuplicate(matchingTransactionId = "tx-123")

        assertEquals(1.0f, status.similarity)
    }

    @Test
    fun `ProbableDuplicate contains matching transaction id and similarity`() {
        val matchingId = "tx-456"
        val similarity = 0.85f
        val reason = "Same date and amount"

        val status = DuplicateStatus.ProbableDuplicate(
            matchingTransactionId = matchingId,
            similarity = similarity,
            reason = reason
        )

        assertEquals(matchingId, status.matchingTransactionId)
        assertEquals(similarity, status.similarity)
        assertEquals(reason, status.reason)
    }

    @Test
    fun `ProbableDuplicate isDuplicate returns true`() {
        val status = DuplicateStatus.ProbableDuplicate(
            matchingTransactionId = "tx-456",
            similarity = 0.75f,
            reason = "Similar description"
        )

        assertTrue(status.isDuplicate)
    }

    @Test
    fun `ProbableDuplicate similarity must be between 0 and 1`() {
        val status = DuplicateStatus.ProbableDuplicate(
            matchingTransactionId = "tx-789",
            similarity = 0.7f,
            reason = "Date match"
        )

        assertTrue(status.similarity in 0f..1f)
    }
}

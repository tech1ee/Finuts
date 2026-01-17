package com.finuts.data.categorization

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for TransactionForCategorization data class.
 */
class TransactionForCategorizationTest {

    @Test
    fun `TransactionForCategorization stores all fields`() {
        val tx = TransactionForCategorization(
            id = "tx-123",
            description = "STARBUCKS COFFEE",
            amount = -450L
        )

        assertEquals("tx-123", tx.id)
        assertEquals("STARBUCKS COFFEE", tx.description)
        assertEquals(-450L, tx.amount)
    }

    @Test
    fun `formattedAmount formats positive amount correctly`() {
        val tx = TransactionForCategorization(
            id = "tx-1",
            description = "TEST",
            amount = 12345L
        )

        assertEquals("123.45", tx.formattedAmount)
    }

    @Test
    fun `formattedAmount formats negative amount with sign`() {
        val tx = TransactionForCategorization(
            id = "tx-1",
            description = "TEST",
            amount = -12345L
        )

        assertEquals("-123.45", tx.formattedAmount)
    }

    @Test
    fun `formattedAmount handles zero`() {
        val tx = TransactionForCategorization(
            id = "tx-1",
            description = "TEST",
            amount = 0L
        )

        assertEquals("0.00", tx.formattedAmount)
    }

    @Test
    fun `formattedAmount pads single digit cents`() {
        val tx = TransactionForCategorization(
            id = "tx-1",
            description = "TEST",
            amount = 105L // 1.05
        )

        assertEquals("1.05", tx.formattedAmount)
    }

    @Test
    fun `formattedAmount handles large amounts`() {
        val tx = TransactionForCategorization(
            id = "tx-1",
            description = "TEST",
            amount = 10000000L // 100,000.00
        )

        assertEquals("100000.00", tx.formattedAmount)
    }

    @Test
    fun `formattedAmount handles cents only`() {
        val tx = TransactionForCategorization(
            id = "tx-1",
            description = "TEST",
            amount = 99L // 0.99
        )

        assertEquals("0.99", tx.formattedAmount)
    }

    @Test
    fun `TransactionForCategorization copy works correctly`() {
        val original = TransactionForCategorization(
            id = "tx-1",
            description = "ORIGINAL",
            amount = 1000L
        )

        val copy = original.copy(description = "MODIFIED", amount = 2000L)

        assertEquals("tx-1", copy.id)
        assertEquals("MODIFIED", copy.description)
        assertEquals(2000L, copy.amount)
    }

    @Test
    fun `TransactionForCategorization equals works correctly`() {
        val tx1 = TransactionForCategorization(
            id = "tx-1",
            description = "TEST",
            amount = 1000L
        )
        val tx2 = TransactionForCategorization(
            id = "tx-1",
            description = "TEST",
            amount = 1000L
        )

        assertEquals(tx1, tx2)
        assertEquals(tx1.hashCode(), tx2.hashCode())
    }
}

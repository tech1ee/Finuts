package com.finuts.domain.entity

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransferTest {

    private val now = Instant.fromEpochMilliseconds(1704067200000) // 2024-01-01

    @Test
    fun `transfer should have correct from and to accounts`() {
        val transfer = createTransfer(
            fromAccountId = "acc-1",
            fromAccountName = "Wallet",
            toAccountId = "acc-2",
            toAccountName = "Savings"
        )

        assertEquals("acc-1", transfer.fromAccountId)
        assertEquals("Wallet", transfer.fromAccountName)
        assertEquals("acc-2", transfer.toAccountId)
        assertEquals("Savings", transfer.toAccountName)
    }

    @Test
    fun `transfer should have correct amount`() {
        val transfer = createTransfer(amount = 50000L)

        assertEquals(50000L, transfer.amount)
    }

    @Test
    fun `transfer should have linked transaction ids`() {
        val transfer = createTransfer(
            outgoingTransactionId = "tx-out-1",
            incomingTransactionId = "tx-in-1"
        )

        assertEquals("tx-out-1", transfer.outgoingTransactionId)
        assertEquals("tx-in-1", transfer.incomingTransactionId)
    }

    @Test
    fun `transfer should have date`() {
        val transfer = createTransfer(date = now)

        assertEquals(now, transfer.date)
    }

    @Test
    fun `transfer should have optional note`() {
        val transferWithNote = createTransfer(note = "Monthly savings")
        val transferWithoutNote = createTransfer(note = null)

        assertEquals("Monthly savings", transferWithNote.note)
        assertEquals(null, transferWithoutNote.note)
    }

    @Test
    fun `transfer id should be based on outgoing transaction`() {
        val transfer = createTransfer(outgoingTransactionId = "tx-123")

        assertEquals("tx-123", transfer.id)
    }

    @Test
    fun `isValid should return true when from and to accounts are different`() {
        val transfer = createTransfer(
            fromAccountId = "acc-1",
            toAccountId = "acc-2"
        )

        assertTrue(transfer.isValid)
    }

    @Test
    fun `isValid should return false when from and to accounts are same`() {
        val transfer = createTransfer(
            fromAccountId = "acc-1",
            toAccountId = "acc-1"
        )

        assertFalse(transfer.isValid)
    }

    @Test
    fun `isValid should return false when amount is zero`() {
        val transfer = createTransfer(amount = 0L)

        assertFalse(transfer.isValid)
    }

    @Test
    fun `isValid should return false when amount is negative`() {
        val transfer = createTransfer(amount = -1000L)

        assertFalse(transfer.isValid)
    }

    @Test
    fun `displayDescription should show transfer between accounts`() {
        val transfer = createTransfer(
            fromAccountName = "Wallet",
            toAccountName = "Savings"
        )

        assertEquals("Wallet → Savings", transfer.displayDescription)
    }

    @Test
    fun `transfer should be equal when outgoing transaction ids match`() {
        val transfer1 = createTransfer(outgoingTransactionId = "tx-1")
        val transfer2 = createTransfer(outgoingTransactionId = "tx-1")

        assertEquals(transfer1.id, transfer2.id)
    }

    @Test
    fun `transfer createdAt should match date by default`() {
        val transfer = createTransfer(date = now, createdAt = now)

        assertEquals(now, transfer.createdAt)
    }

    private fun createTransfer(
        outgoingTransactionId: String = "tx-out-default",
        incomingTransactionId: String = "tx-in-default",
        fromAccountId: String = "from-acc",
        fromAccountName: String = "From Account",
        toAccountId: String = "to-acc",
        toAccountName: String = "To Account",
        amount: Long = 10000L,
        date: Instant = now,
        note: String? = null,
        createdAt: Instant = now
    ) = Transfer(
        outgoingTransactionId = outgoingTransactionId,
        incomingTransactionId = incomingTransactionId,
        fromAccountId = fromAccountId,
        fromAccountName = fromAccountName,
        toAccountId = toAccountId,
        toAccountName = toAccountName,
        amount = amount,
        date = date,
        note = note,
        createdAt = createdAt
    )
}

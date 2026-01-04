package com.finuts.domain.usecase

import com.finuts.domain.entity.Transfer
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.test.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CreateTransferUseCaseTest {

    private lateinit var repository: FakeTransactionRepository
    private lateinit var useCase: CreateTransferUseCase

    private val now = Instant.fromEpochMilliseconds(1704067200000) // 2024-01-01

    @BeforeTest
    fun setup() {
        repository = FakeTransactionRepository()
        useCase = CreateTransferUseCase(repository, clock = { now })
    }

    @Test
    fun `execute should create two linked transactions`() = runTest {
        val result = useCase.execute(
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = 50000L,
            date = now,
            note = "Test transfer"
        )

        assertTrue(result.isSuccess)
        val transactions = repository.getAllTransactions().first()
        assertEquals(2, transactions.size)
    }

    @Test
    fun `execute should create outgoing transaction with negative impact`() = runTest {
        useCase.execute(
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = 50000L,
            date = now,
            note = null
        )

        val transactions = repository.getAllTransactions().first()
        val outgoing = transactions.find { it.accountId == "acc-1" }

        assertNotNull(outgoing)
        assertEquals(TransactionType.TRANSFER, outgoing.type)
        assertEquals(50000L, outgoing.amount)
        assertEquals("acc-2", outgoing.transferAccountId)
    }

    @Test
    fun `execute should create incoming transaction with positive impact`() = runTest {
        useCase.execute(
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = 50000L,
            date = now,
            note = null
        )

        val transactions = repository.getAllTransactions().first()
        val incoming = transactions.find { it.accountId == "acc-2" }

        assertNotNull(incoming)
        assertEquals(TransactionType.TRANSFER, incoming.type)
        assertEquals(50000L, incoming.amount)
        assertEquals("acc-1", incoming.transferAccountId)
    }

    @Test
    fun `execute should link transactions to each other`() = runTest {
        useCase.execute(
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = 50000L,
            date = now,
            note = null
        )

        val transactions = repository.getAllTransactions().first()
        val outgoing = transactions.find { it.accountId == "acc-1" }
        val incoming = transactions.find { it.accountId == "acc-2" }

        assertNotNull(outgoing)
        assertNotNull(incoming)
        assertEquals(incoming.id, outgoing.linkedTransactionId)
        assertEquals(outgoing.id, incoming.linkedTransactionId)
    }

    @Test
    fun `execute should return transfer with correct data`() = runTest {
        val result = useCase.execute(
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = 50000L,
            date = now,
            note = "Monthly savings"
        )

        assertTrue(result.isSuccess)
        val transfer = result.getOrNull()
        assertNotNull(transfer)
        assertEquals("acc-1", transfer.fromAccountId)
        assertEquals("acc-2", transfer.toAccountId)
        assertEquals(50000L, transfer.amount)
        assertEquals(now, transfer.date)
        assertEquals("Monthly savings", transfer.note)
    }

    @Test
    fun `execute should fail when from and to accounts are same`() = runTest {
        val result = useCase.execute(
            fromAccountId = "acc-1",
            toAccountId = "acc-1",
            amount = 50000L,
            date = now,
            note = null
        )

        assertTrue(result.isFailure)
        assertEquals(0, repository.getAllTransactions().first().size)
    }

    @Test
    fun `execute should fail when amount is zero`() = runTest {
        val result = useCase.execute(
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = 0L,
            date = now,
            note = null
        )

        assertTrue(result.isFailure)
        assertEquals(0, repository.getAllTransactions().first().size)
    }

    @Test
    fun `execute should fail when amount is negative`() = runTest {
        val result = useCase.execute(
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = -1000L,
            date = now,
            note = null
        )

        assertTrue(result.isFailure)
        assertEquals(0, repository.getAllTransactions().first().size)
    }

    @Test
    fun `execute should set correct timestamps`() = runTest {
        useCase.execute(
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = 50000L,
            date = now,
            note = null
        )

        val transactions = repository.getAllTransactions().first()
        transactions.forEach { tx ->
            assertEquals(now, tx.createdAt)
            assertEquals(now, tx.updatedAt)
        }
    }

    @Test
    fun `execute should set transfer description`() = runTest {
        useCase.execute(
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = 50000L,
            date = now,
            note = null
        )

        val transactions = repository.getAllTransactions().first()
        val outgoing = transactions.find { it.accountId == "acc-1" }
        val incoming = transactions.find { it.accountId == "acc-2" }

        assertEquals("Transfer", outgoing?.description)
        assertEquals("Transfer", incoming?.description)
    }

    @Test
    fun `execute should generate unique ids for transactions`() = runTest {
        useCase.execute(
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = 50000L,
            date = now,
            note = null
        )

        val transactions = repository.getAllTransactions().first()
        val ids = transactions.map { it.id }

        assertEquals(2, ids.distinct().size)
    }
}

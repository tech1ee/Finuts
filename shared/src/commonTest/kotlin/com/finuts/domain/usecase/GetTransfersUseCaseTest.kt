package com.finuts.domain.usecase

import com.finuts.domain.entity.Account
import com.finuts.domain.entity.AccountType
import com.finuts.domain.entity.Currency
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.test.fakes.FakeAccountRepository
import com.finuts.test.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetTransfersUseCaseTest {

    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var useCase: GetTransfersUseCase

    private val now = Instant.fromEpochMilliseconds(1704067200000) // 2024-01-01

    private val kzt = Currency(code = "KZT", symbol = "₸", name = "Tenge")

    private val account1 = Account(
        id = "acc-1",
        name = "Wallet",
        type = AccountType.CASH,
        currency = kzt,
        balance = 100000L,
        color = "#4CAF50",
        icon = "wallet",
        isArchived = false,
        createdAt = now,
        updatedAt = now
    )

    private val account2 = Account(
        id = "acc-2",
        name = "Savings",
        type = AccountType.SAVINGS,
        currency = kzt,
        balance = 500000L,
        color = "#2196F3",
        icon = "savings",
        isArchived = false,
        createdAt = now,
        updatedAt = now
    )

    @BeforeTest
    fun setup() {
        transactionRepository = FakeTransactionRepository()
        accountRepository = FakeAccountRepository()
        accountRepository.setAccounts(listOf(account1, account2))
        useCase = GetTransfersUseCase(transactionRepository, accountRepository)
    }

    @Test
    fun `execute should return empty list when no transfers exist`() = runTest {
        val transfers = useCase.execute().first()
        assertTrue(transfers.isEmpty())
    }

    @Test
    fun `execute should return transfer for linked transactions`() = runTest {
        val outgoing = createTransferTransaction(
            id = "tx-out-1",
            accountId = "acc-1",
            linkedTransactionId = "tx-in-1",
            transferAccountId = "acc-2"
        )
        val incoming = createTransferTransaction(
            id = "tx-in-1",
            accountId = "acc-2",
            linkedTransactionId = "tx-out-1",
            transferAccountId = "acc-1"
        )
        transactionRepository.setTransactions(listOf(outgoing, incoming))

        val transfers = useCase.execute().first()

        assertEquals(1, transfers.size)
    }

    @Test
    fun `execute should populate account names`() = runTest {
        val outgoing = createTransferTransaction(
            id = "tx-out-1",
            accountId = "acc-1",
            linkedTransactionId = "tx-in-1",
            transferAccountId = "acc-2"
        )
        val incoming = createTransferTransaction(
            id = "tx-in-1",
            accountId = "acc-2",
            linkedTransactionId = "tx-out-1",
            transferAccountId = "acc-1"
        )
        transactionRepository.setTransactions(listOf(outgoing, incoming))

        val transfers = useCase.execute().first()
        val transfer = transfers.first()

        assertEquals("Wallet", transfer.fromAccountName)
        assertEquals("Savings", transfer.toAccountName)
    }

    @Test
    fun `execute should set correct transfer amount`() = runTest {
        val outgoing = createTransferTransaction(
            id = "tx-out-1",
            accountId = "acc-1",
            amount = 50000L,
            linkedTransactionId = "tx-in-1",
            transferAccountId = "acc-2"
        )
        val incoming = createTransferTransaction(
            id = "tx-in-1",
            accountId = "acc-2",
            amount = 50000L,
            linkedTransactionId = "tx-out-1",
            transferAccountId = "acc-1"
        )
        transactionRepository.setTransactions(listOf(outgoing, incoming))

        val transfers = useCase.execute().first()

        assertEquals(50000L, transfers.first().amount)
    }

    @Test
    fun `execute should include transfer note`() = runTest {
        val outgoing = createTransferTransaction(
            id = "tx-out-1",
            accountId = "acc-1",
            note = "Monthly savings",
            linkedTransactionId = "tx-in-1",
            transferAccountId = "acc-2"
        )
        val incoming = createTransferTransaction(
            id = "tx-in-1",
            accountId = "acc-2",
            note = "Monthly savings",
            linkedTransactionId = "tx-out-1",
            transferAccountId = "acc-1"
        )
        transactionRepository.setTransactions(listOf(outgoing, incoming))

        val transfers = useCase.execute().first()

        assertEquals("Monthly savings", transfers.first().note)
    }

    @Test
    fun `execute should not include non-transfer transactions`() = runTest {
        val expense = Transaction(
            id = "tx-expense",
            accountId = "acc-1",
            amount = 1000L,
            type = TransactionType.EXPENSE,
            categoryId = "cat-1",
            description = "Coffee",
            merchant = null,
            note = null,
            date = now,
            createdAt = now,
            updatedAt = now
        )
        transactionRepository.setTransactions(listOf(expense))

        val transfers = useCase.execute().first()

        assertTrue(transfers.isEmpty())
    }

    @Test
    fun `execute should return multiple transfers sorted by date descending`() = runTest {
        val day1 = Instant.fromEpochMilliseconds(1704067200000) // Jan 1
        val day2 = Instant.fromEpochMilliseconds(1704153600000) // Jan 2

        val transfer1Out = createTransferTransaction(
            id = "tx-out-1",
            accountId = "acc-1",
            date = day1,
            linkedTransactionId = "tx-in-1",
            transferAccountId = "acc-2"
        )
        val transfer1In = createTransferTransaction(
            id = "tx-in-1",
            accountId = "acc-2",
            date = day1,
            linkedTransactionId = "tx-out-1",
            transferAccountId = "acc-1"
        )
        val transfer2Out = createTransferTransaction(
            id = "tx-out-2",
            accountId = "acc-1",
            date = day2,
            linkedTransactionId = "tx-in-2",
            transferAccountId = "acc-2"
        )
        val transfer2In = createTransferTransaction(
            id = "tx-in-2",
            accountId = "acc-2",
            date = day2,
            linkedTransactionId = "tx-out-2",
            transferAccountId = "acc-1"
        )

        transactionRepository.setTransactions(listOf(transfer1Out, transfer1In, transfer2Out, transfer2In))

        val transfers = useCase.execute().first()

        assertEquals(2, transfers.size)
        assertEquals(day2, transfers[0].date)
        assertEquals(day1, transfers[1].date)
    }

    @Test
    fun `execute should use unknown for missing account names`() = runTest {
        val outgoing = createTransferTransaction(
            id = "tx-out-1",
            accountId = "acc-unknown",
            linkedTransactionId = "tx-in-1",
            transferAccountId = "acc-2"
        )
        val incoming = createTransferTransaction(
            id = "tx-in-1",
            accountId = "acc-2",
            linkedTransactionId = "tx-out-1",
            transferAccountId = "acc-unknown"
        )
        transactionRepository.setTransactions(listOf(outgoing, incoming))

        val transfers = useCase.execute().first()
        val transfer = transfers.first()

        assertEquals("Unknown", transfer.fromAccountName)
    }

    private fun createTransferTransaction(
        id: String,
        accountId: String,
        amount: Long = 10000L,
        date: Instant = now,
        note: String? = null,
        linkedTransactionId: String,
        transferAccountId: String
    ) = Transaction(
        id = id,
        accountId = accountId,
        amount = amount,
        type = TransactionType.TRANSFER,
        categoryId = null,
        description = "Transfer",
        merchant = null,
        note = note,
        date = date,
        createdAt = now,
        updatedAt = now,
        linkedTransactionId = linkedTransactionId,
        transferAccountId = transferAccountId
    )
}

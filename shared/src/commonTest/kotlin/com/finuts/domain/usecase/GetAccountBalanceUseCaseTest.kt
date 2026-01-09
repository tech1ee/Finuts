package com.finuts.domain.usecase

import com.finuts.domain.entity.TransactionType
import com.finuts.test.TestData
import com.finuts.test.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD tests for GetAccountBalanceUseCase.
 * Verifies calculated balance logic based on initialBalance + transactions.
 */
class GetAccountBalanceUseCaseTest {

    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var useCase: GetAccountBalanceUseCase

    private val accountId = "account-1"

    @BeforeTest
    fun setup() {
        transactionRepository = FakeTransactionRepository()
        useCase = GetAccountBalanceUseCase(transactionRepository)
    }

    // --- Basic Balance Calculations ---

    @Test
    fun `balance is zero when no transactions and no initial balance`() = runTest {
        val balance = useCase.execute(accountId, initialBalance = 0L).first()
        assertEquals(0L, balance)
    }

    @Test
    fun `balance equals initial balance when no transactions`() = runTest {
        val balance = useCase.execute(accountId, initialBalance = 100_000L).first()
        assertEquals(100_000L, balance)
    }

    @Test
    fun `income transaction increases balance`() = runTest {
        transactionRepository.setTransactions(listOf(
            TestData.transaction(
                id = "tx-1",
                accountId = accountId,
                amount = 50_000L,
                type = TransactionType.INCOME
            )
        ))

        val balance = useCase.execute(accountId, initialBalance = 0L).first()
        assertEquals(50_000L, balance)
    }

    @Test
    fun `expense transaction decreases balance`() = runTest {
        transactionRepository.setTransactions(listOf(
            TestData.transaction(
                id = "tx-1",
                accountId = accountId,
                amount = 30_000L,
                type = TransactionType.EXPENSE
            )
        ))

        val balance = useCase.execute(accountId, initialBalance = 100_000L).first()
        assertEquals(70_000L, balance)
    }

    @Test
    fun `multiple income transactions sum correctly`() = runTest {
        transactionRepository.setTransactions(listOf(
            TestData.transaction(id = "tx-1", accountId = accountId, amount = 10_000L, type = TransactionType.INCOME),
            TestData.transaction(id = "tx-2", accountId = accountId, amount = 20_000L, type = TransactionType.INCOME),
            TestData.transaction(id = "tx-3", accountId = accountId, amount = 30_000L, type = TransactionType.INCOME)
        ))

        val balance = useCase.execute(accountId, initialBalance = 0L).first()
        assertEquals(60_000L, balance)
    }

    @Test
    fun `multiple expense transactions sum correctly`() = runTest {
        transactionRepository.setTransactions(listOf(
            TestData.transaction(id = "tx-1", accountId = accountId, amount = 5_000L, type = TransactionType.EXPENSE),
            TestData.transaction(id = "tx-2", accountId = accountId, amount = 15_000L, type = TransactionType.EXPENSE),
            TestData.transaction(id = "tx-3", accountId = accountId, amount = 10_000L, type = TransactionType.EXPENSE)
        ))

        val balance = useCase.execute(accountId, initialBalance = 100_000L).first()
        assertEquals(70_000L, balance)
    }

    @Test
    fun `mixed income and expense calculates correctly`() = runTest {
        transactionRepository.setTransactions(listOf(
            TestData.transaction(id = "tx-1", accountId = accountId, amount = 100_000L, type = TransactionType.INCOME),
            TestData.transaction(id = "tx-2", accountId = accountId, amount = 30_000L, type = TransactionType.EXPENSE),
            TestData.transaction(id = "tx-3", accountId = accountId, amount = 20_000L, type = TransactionType.INCOME),
            TestData.transaction(id = "tx-4", accountId = accountId, amount = 50_000L, type = TransactionType.EXPENSE)
        ))

        val balance = useCase.execute(accountId, initialBalance = 0L).first()
        // 100_000 - 30_000 + 20_000 - 50_000 = 40_000
        assertEquals(40_000L, balance)
    }

    // --- Transfer Transactions ---

    @Test
    fun `outgoing transfer decreases balance`() = runTest {
        transactionRepository.setTransactions(listOf(
            TestData.transaction(
                id = "tx-1",
                accountId = accountId,
                amount = 25_000L,
                type = TransactionType.TRANSFER,
                linkedTransactionId = "tx-2",
                transferAccountId = "account-2"
            )
        ))

        val balance = useCase.execute(accountId, initialBalance = 100_000L).first()
        assertEquals(75_000L, balance)
    }

    @Test
    fun `incoming transfer increases balance`() = runTest {
        // Incoming transfer: transaction is created on SOURCE account
        // with transferAccountId pointing to OUR account (destination)
        transactionRepository.setTransactions(listOf(
            TestData.transaction(
                id = "tx-2",
                accountId = "account-2",  // Source account (not ours)
                amount = 25_000L,
                type = TransactionType.TRANSFER,
                linkedTransactionId = "tx-1",
                transferAccountId = accountId  // Our account (destination)
            )
        ))

        // For incoming transfer: amount is added to our account
        val balance = useCase.execute(accountId, initialBalance = 100_000L).first()
        assertEquals(125_000L, balance)
    }

    // --- Account Isolation ---

    @Test
    fun `only transactions for specified account are counted`() = runTest {
        transactionRepository.setTransactions(listOf(
            TestData.transaction(id = "tx-1", accountId = accountId, amount = 50_000L, type = TransactionType.INCOME),
            TestData.transaction(id = "tx-2", accountId = "other-account", amount = 100_000L, type = TransactionType.INCOME),
            TestData.transaction(id = "tx-3", accountId = accountId, amount = 10_000L, type = TransactionType.EXPENSE)
        ))

        val balance = useCase.execute(accountId, initialBalance = 0L).first()
        // Only tx-1 and tx-3 counted: 50_000 - 10_000 = 40_000
        assertEquals(40_000L, balance)
    }

    // --- Edge Cases ---

    @Test
    fun `balance can be negative`() = runTest {
        transactionRepository.setTransactions(listOf(
            TestData.transaction(id = "tx-1", accountId = accountId, amount = 50_000L, type = TransactionType.EXPENSE)
        ))

        val balance = useCase.execute(accountId, initialBalance = 0L).first()
        assertEquals(-50_000L, balance)
    }

    @Test
    fun `initial balance can be negative`() = runTest {
        val balance = useCase.execute(accountId, initialBalance = -20_000L).first()
        assertEquals(-20_000L, balance)
    }

    @Test
    fun `large amounts calculate correctly`() = runTest {
        transactionRepository.setTransactions(listOf(
            TestData.transaction(id = "tx-1", accountId = accountId, amount = 1_000_000_000L, type = TransactionType.INCOME),
            TestData.transaction(id = "tx-2", accountId = accountId, amount = 500_000_000L, type = TransactionType.EXPENSE)
        ))

        val balance = useCase.execute(accountId, initialBalance = 0L).first()
        assertEquals(500_000_000L, balance)
    }

    // --- Reactive Updates ---

    @Test
    fun `balance updates when new transaction added`() = runTest {
        val balanceFlow = useCase.execute(accountId, initialBalance = 100_000L)

        // Initial balance
        assertEquals(100_000L, balanceFlow.first())

        // Add transaction
        transactionRepository.createTransaction(
            TestData.transaction(id = "tx-1", accountId = accountId, amount = 20_000L, type = TransactionType.EXPENSE)
        )

        // Updated balance
        assertEquals(80_000L, balanceFlow.first())
    }

    @Test
    fun `balance updates when transaction deleted`() = runTest {
        transactionRepository.setTransactions(listOf(
            TestData.transaction(id = "tx-1", accountId = accountId, amount = 30_000L, type = TransactionType.EXPENSE)
        ))

        val balanceFlow = useCase.execute(accountId, initialBalance = 100_000L)
        assertEquals(70_000L, balanceFlow.first())

        // Delete transaction
        transactionRepository.deleteTransaction("tx-1")

        // Balance restored
        assertEquals(100_000L, balanceFlow.first())
    }
}

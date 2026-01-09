package com.finuts.domain.usecase

import com.finuts.domain.entity.TransactionType
import com.finuts.test.TestData
import com.finuts.test.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD tests for ReconcileAccountUseCase.
 * Verifies account reconciliation creates adjustment transactions.
 */
class ReconcileAccountUseCaseTest {

    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var getBalanceUseCase: GetAccountBalanceUseCase
    private lateinit var useCase: ReconcileAccountUseCase

    private val accountId = "account-1"
    private val now = Instant.fromEpochMilliseconds(1704067200000) // 2024-01-01

    @BeforeTest
    fun setup() {
        transactionRepository = FakeTransactionRepository()
        getBalanceUseCase = GetAccountBalanceUseCase(transactionRepository)
        useCase = ReconcileAccountUseCase(transactionRepository, clock = { now })
    }

    // --- Basic Reconciliation ---

    @Test
    fun `reconcile creates positive adjustment when target is higher`() = runTest {
        // Current balance: 100_000, Target: 150_000
        // Needs +50_000 adjustment (INCOME)
        transactionRepository.setTransactions(listOf(
            TestData.transaction(id = "tx-1", accountId = accountId, amount = 100_000L, type = TransactionType.INCOME)
        ))

        val result = useCase.execute(
            accountId = accountId,
            currentBalance = 100_000L,
            targetBalance = 150_000L,
            note = "Bank statement reconciliation"
        )

        assertTrue(result.isSuccess)
        val adjustment = result.getOrNull()
        assertNotNull(adjustment)
        assertEquals(50_000L, adjustment.amount)
        assertEquals(TransactionType.INCOME, adjustment.type)
        assertEquals("Balance Adjustment", adjustment.description)
        assertEquals("Bank statement reconciliation", adjustment.note)
    }

    @Test
    fun `reconcile creates negative adjustment when target is lower`() = runTest {
        // Current balance: 100_000, Target: 70_000
        // Needs -30_000 adjustment (EXPENSE)
        transactionRepository.setTransactions(listOf(
            TestData.transaction(id = "tx-1", accountId = accountId, amount = 100_000L, type = TransactionType.INCOME)
        ))

        val result = useCase.execute(
            accountId = accountId,
            currentBalance = 100_000L,
            targetBalance = 70_000L,
            note = null
        )

        assertTrue(result.isSuccess)
        val adjustment = result.getOrNull()
        assertNotNull(adjustment)
        assertEquals(30_000L, adjustment.amount)
        assertEquals(TransactionType.EXPENSE, adjustment.type)
    }

    @Test
    fun `reconcile returns failure when balances match`() = runTest {
        val result = useCase.execute(
            accountId = accountId,
            currentBalance = 100_000L,
            targetBalance = 100_000L,
            note = null
        )

        assertTrue(result.isFailure)
    }

    // --- Adjustment Transaction Properties ---

    @Test
    fun `adjustment transaction has correct account id`() = runTest {
        val result = useCase.execute(
            accountId = accountId,
            currentBalance = 0L,
            targetBalance = 50_000L,
            note = null
        )

        val adjustment = result.getOrNull()
        assertNotNull(adjustment)
        assertEquals(accountId, adjustment.accountId)
    }

    @Test
    fun `adjustment transaction has current timestamp`() = runTest {
        val result = useCase.execute(
            accountId = accountId,
            currentBalance = 0L,
            targetBalance = 50_000L,
            note = null
        )

        val adjustment = result.getOrNull()
        assertNotNull(adjustment)
        assertEquals(now, adjustment.date)
        assertEquals(now, adjustment.createdAt)
        assertEquals(now, adjustment.updatedAt)
    }

    @Test
    fun `adjustment transaction has no category`() = runTest {
        val result = useCase.execute(
            accountId = accountId,
            currentBalance = 0L,
            targetBalance = 50_000L,
            note = null
        )

        val adjustment = result.getOrNull()
        assertNotNull(adjustment)
        assertEquals(null, adjustment.categoryId)
    }

    @Test
    fun `adjustment transaction is saved to repository`() = runTest {
        useCase.execute(
            accountId = accountId,
            currentBalance = 0L,
            targetBalance = 50_000L,
            note = null
        )

        val transactions = transactionRepository.getAllTransactions().first()
        assertEquals(1, transactions.size)
        assertEquals("Balance Adjustment", transactions[0].description)
    }

    // --- Balance Verification ---

    @Test
    fun `balance is correct after positive adjustment`() = runTest {
        useCase.execute(
            accountId = accountId,
            currentBalance = 0L,
            targetBalance = 75_000L,
            note = null
        )

        val newBalance = getBalanceUseCase.execute(accountId, initialBalance = 0L).first()
        assertEquals(75_000L, newBalance)
    }

    @Test
    fun `balance is correct after negative adjustment`() = runTest {
        // Start with some income
        transactionRepository.setTransactions(listOf(
            TestData.transaction(id = "tx-1", accountId = accountId, amount = 100_000L, type = TransactionType.INCOME)
        ))

        useCase.execute(
            accountId = accountId,
            currentBalance = 100_000L,
            targetBalance = 60_000L,
            note = null
        )

        val newBalance = getBalanceUseCase.execute(accountId, initialBalance = 0L).first()
        assertEquals(60_000L, newBalance)
    }

    // --- Edge Cases ---

    @Test
    fun `reconcile handles negative target balance`() = runTest {
        val result = useCase.execute(
            accountId = accountId,
            currentBalance = 50_000L,
            targetBalance = -20_000L,
            note = null
        )

        assertTrue(result.isSuccess)
        val adjustment = result.getOrNull()
        assertNotNull(adjustment)
        assertEquals(70_000L, adjustment.amount) // 50_000 - (-20_000) = 70_000
        assertEquals(TransactionType.EXPENSE, adjustment.type)
    }

    @Test
    fun `reconcile handles negative current balance`() = runTest {
        val result = useCase.execute(
            accountId = accountId,
            currentBalance = -30_000L,
            targetBalance = 20_000L,
            note = null
        )

        assertTrue(result.isSuccess)
        val adjustment = result.getOrNull()
        assertNotNull(adjustment)
        assertEquals(50_000L, adjustment.amount) // 20_000 - (-30_000) = 50_000
        assertEquals(TransactionType.INCOME, adjustment.type)
    }

    @Test
    fun `reconcile handles both negative balances`() = runTest {
        val result = useCase.execute(
            accountId = accountId,
            currentBalance = -100_000L,
            targetBalance = -60_000L,
            note = null
        )

        assertTrue(result.isSuccess)
        val adjustment = result.getOrNull()
        assertNotNull(adjustment)
        assertEquals(40_000L, adjustment.amount) // -60_000 - (-100_000) = 40_000
        assertEquals(TransactionType.INCOME, adjustment.type)
    }

    @Test
    fun `adjustment has unique id`() = runTest {
        useCase.execute(
            accountId = accountId,
            currentBalance = 0L,
            targetBalance = 10_000L,
            note = null
        )
        useCase.execute(
            accountId = accountId,
            currentBalance = 10_000L,
            targetBalance = 25_000L,
            note = null
        )

        val transactions = transactionRepository.getAllTransactions().first()
        assertEquals(2, transactions.size)
        assertTrue(transactions[0].id != transactions[1].id)
    }
}

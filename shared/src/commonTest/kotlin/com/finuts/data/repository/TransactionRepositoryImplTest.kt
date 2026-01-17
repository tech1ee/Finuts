package com.finuts.data.repository

import app.cash.turbine.test
import com.finuts.data.local.entity.TransactionEntity
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.test.fakes.dao.FakeTransactionDao
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransactionRepositoryImplTest {

    private lateinit var fakeDao: FakeTransactionDao
    private lateinit var repository: TransactionRepositoryImpl

    @BeforeTest
    fun setup() {
        fakeDao = FakeTransactionDao()
        repository = TransactionRepositoryImpl(fakeDao)
    }

    // === getAllTransactions Tests ===

    @Test
    fun `getAllTransactions returns empty list when no transactions`() = runTest {
        repository.getAllTransactions().test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllTransactions returns all transactions mapped to domain`() = runTest {
        fakeDao.setTransactions(listOf(
            createTransactionEntity("1", "Coffee", date = 1704153600000L),
            createTransactionEntity("2", "Lunch", date = 1704067200000L)
        ))

        repository.getAllTransactions().test {
            val transactions = awaitItem()
            assertEquals(2, transactions.size)
            // Sorted by date DESC
            assertEquals("Coffee", transactions[0].description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === getTransactionById Tests ===

    @Test
    fun `getTransactionById returns null when not found`() = runTest {
        repository.getTransactionById("non-existent").test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getTransactionById returns transaction when found`() = runTest {
        fakeDao.setTransactions(listOf(createTransactionEntity("1", "Coffee")))

        repository.getTransactionById("1").test {
            val tx = awaitItem()
            assertEquals("1", tx?.id)
            assertEquals("Coffee", tx?.description)
            awaitComplete()
        }
    }

    // === getTransactionsByAccount Tests ===

    @Test
    fun `getTransactionsByAccount filters by account`() = runTest {
        fakeDao.setTransactions(listOf(
            createTransactionEntity("1", "Coffee", accountId = "acc1"),
            createTransactionEntity("2", "Lunch", accountId = "acc2")
        ))

        repository.getTransactionsByAccount("acc1").test {
            val transactions = awaitItem()
            assertEquals(1, transactions.size)
            assertEquals("Coffee", transactions[0].description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === getTransactionsByCategory Tests ===

    @Test
    fun `getTransactionsByCategory filters by category`() = runTest {
        fakeDao.setTransactions(listOf(
            createTransactionEntity("1", "Coffee", categoryId = "food"),
            createTransactionEntity("2", "Bus", categoryId = "transport")
        ))

        repository.getTransactionsByCategory("food").test {
            val transactions = awaitItem()
            assertEquals(1, transactions.size)
            assertEquals("Coffee", transactions[0].description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === getTransactionsByDateRange Tests ===

    @Test
    fun `getTransactionsByDateRange filters by date`() = runTest {
        fakeDao.setTransactions(listOf(
            createTransactionEntity("1", "Jan 1", date = 1704067200000L),  // 2024-01-01
            createTransactionEntity("2", "Jan 15", date = 1705276800000L), // 2024-01-15
            createTransactionEntity("3", "Feb 1", date = 1706745600000L)   // 2024-02-01
        ))

        val start = Instant.fromEpochMilliseconds(1704067200000L)
        val end = Instant.fromEpochMilliseconds(1705363200000L) // 2024-01-16

        repository.getTransactionsByDateRange(start, end).test {
            val transactions = awaitItem()
            assertEquals(2, transactions.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === getTransactionsByType Tests ===

    @Test
    fun `getTransactionsByType filters by expense type`() = runTest {
        fakeDao.setTransactions(listOf(
            createTransactionEntity("1", "Coffee", type = "EXPENSE"),
            createTransactionEntity("2", "Salary", type = "INCOME")
        ))

        repository.getTransactionsByType(TransactionType.EXPENSE).test {
            val transactions = awaitItem()
            assertEquals(1, transactions.size)
            assertEquals("Coffee", transactions[0].description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === createTransaction Tests ===

    @Test
    fun `createTransaction inserts entity into dao`() = runTest {
        val tx = createDomainTransaction("new-id", "New Transaction")

        repository.createTransaction(tx)

        val stored = fakeDao.getTransactionById("new-id")
        assertEquals("New Transaction", stored?.description)
    }

    // === updateTransaction Tests ===

    @Test
    fun `updateTransaction modifies existing entity`() = runTest {
        fakeDao.setTransactions(listOf(createTransactionEntity("1", "Old")))
        val updated = createDomainTransaction("1", "Updated")

        repository.updateTransaction(updated)

        val stored = fakeDao.getTransactionById("1")
        assertEquals("Updated", stored?.description)
    }

    // === deleteTransaction Tests ===

    @Test
    fun `deleteTransaction removes entity from dao`() = runTest {
        fakeDao.setTransactions(listOf(createTransactionEntity("1", "ToDelete")))

        repository.deleteTransaction("1")

        assertNull(fakeDao.getTransactionById("1"))
    }

    // === insertTransfer Tests ===

    @Test
    fun `insertTransfer inserts both transactions`() = runTest {
        val outgoing = createDomainTransaction("out-1", "Transfer Out")
        val incoming = createDomainTransaction("in-1", "Transfer In")

        repository.insertTransfer(outgoing, incoming)

        val all = fakeDao.getAll()
        assertEquals(2, all.size)
        assertTrue(all.any { it.id == "out-1" })
        assertTrue(all.any { it.id == "in-1" })
    }

    // === Helper Functions ===

    private fun createTransactionEntity(
        id: String,
        description: String,
        accountId: String = "acc-1",
        categoryId: String = "groceries",
        type: String = "EXPENSE",
        date: Long = 1704067200000L
    ) = TransactionEntity(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        amount = 1000L,
        type = type,
        description = description,
        merchant = null,
        note = null,
        date = date,
        isRecurring = false,
        recurringRuleId = null,
        attachments = null,
        tags = null,
        createdAt = 1704067200000L,
        updatedAt = 1704067200000L
    )

    private fun createDomainTransaction(
        id: String,
        description: String
    ) = Transaction(
        id = id,
        accountId = "acc-1",
        categoryId = "groceries",
        amount = 1000L,
        type = TransactionType.EXPENSE,
        description = description,
        merchant = null,
        note = null,
        date = Instant.fromEpochMilliseconds(1704067200000L),
        isRecurring = false,
        recurringRuleId = null,
        attachments = emptyList(),
        tags = emptyList(),
        createdAt = Instant.fromEpochMilliseconds(1704067200000L),
        updatedAt = Instant.fromEpochMilliseconds(1704067200000L)
    )
}

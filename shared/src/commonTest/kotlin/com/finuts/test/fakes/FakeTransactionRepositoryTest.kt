package com.finuts.test.fakes

import app.cash.turbine.test
import com.finuts.domain.entity.TransactionType
import com.finuts.test.BaseTest
import com.finuts.test.TestData
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for FakeTransactionRepository to ensure our test fakes work correctly.
 */
class FakeTransactionRepositoryTest : BaseTest() {

    private val repository = FakeTransactionRepository()

    @Test
    fun `getAllTransactions returns empty list initially`() = runTest {
        repository.getAllTransactions().test {
            assertEquals(emptyList(), awaitItem())
        }
    }

    @Test
    fun `createTransaction adds transaction to list`() = runTest {
        val transaction = TestData.transaction(id = "tx-1", description = "Coffee")

        repository.getAllTransactions().test {
            assertEquals(emptyList(), awaitItem())

            repository.createTransaction(transaction)

            val transactions = awaitItem()
            assertEquals(1, transactions.size)
            assertEquals("Coffee", transactions.first().description)
        }
    }

    @Test
    fun `getTransactionById returns transaction when exists`() = runTest {
        val transaction = TestData.transaction(id = "tx-1", description = "Grocery")
        repository.createTransaction(transaction)

        repository.getTransactionById("tx-1").test {
            assertEquals("Grocery", awaitItem()?.description)
        }
    }

    @Test
    fun `getTransactionById returns null when not exists`() = runTest {
        repository.getTransactionById("non-existent").test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `getTransactionsByAccount filters by accountId`() = runTest {
        val tx1 = TestData.transaction(id = "1", accountId = "acc-1")
        val tx2 = TestData.transaction(id = "2", accountId = "acc-2")
        val tx3 = TestData.transaction(id = "3", accountId = "acc-1")

        repository.setTransactions(listOf(tx1, tx2, tx3))

        repository.getTransactionsByAccount("acc-1").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.accountId == "acc-1" })
        }
    }

    @Test
    fun `getTransactionsByCategory filters by categoryId`() = runTest {
        val tx1 = TestData.transaction(id = "1", categoryId = "food")
        val tx2 = TestData.transaction(id = "2", categoryId = "transport")
        val tx3 = TestData.transaction(id = "3", categoryId = "food")

        repository.setTransactions(listOf(tx1, tx2, tx3))

        repository.getTransactionsByCategory("food").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.categoryId == "food" })
        }
    }

    @Test
    fun `getTransactionsByType filters by type`() = runTest {
        val expense = TestData.transaction(id = "1", type = TransactionType.EXPENSE)
        val income = TestData.transaction(id = "2", type = TransactionType.INCOME)
        val transfer = TestData.transaction(id = "3", type = TransactionType.TRANSFER)

        repository.setTransactions(listOf(expense, income, transfer))

        repository.getTransactionsByType(TransactionType.EXPENSE).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(TransactionType.EXPENSE, result.first().type)
        }
    }

    @Test
    fun `getTransactionsByDateRange filters correctly`() = runTest {
        // Fixed dates for deterministic testing
        val jan1 = Instant.parse("2024-01-01T12:00:00Z")
        val jan5 = Instant.parse("2024-01-05T12:00:00Z")
        val jan10 = Instant.parse("2024-01-10T12:00:00Z")
        val jan15 = Instant.parse("2024-01-15T12:00:00Z")

        val earlyTx = TestData.transaction(id = "1", date = jan1)
        val midTx = TestData.transaction(id = "2", date = jan10)
        val lateTx = TestData.transaction(id = "3", date = jan15)

        repository.setTransactions(listOf(earlyTx, midTx, lateTx))

        // Query for jan5 to jan12 - should only return midTx
        repository.getTransactionsByDateRange(
            start = jan5,
            end = Instant.parse("2024-01-12T23:59:59Z")
        ).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("2", result.first().id)
        }
    }

    @Test
    fun `updateTransaction modifies existing transaction`() = runTest {
        val transaction = TestData.transaction(id = "1", description = "Original")
        repository.createTransaction(transaction)

        val updated = transaction.copy(description = "Updated")
        repository.updateTransaction(updated)

        repository.getTransactionById("1").test {
            assertEquals("Updated", awaitItem()?.description)
        }
    }

    @Test
    fun `deleteTransaction removes transaction from list`() = runTest {
        val transaction = TestData.transaction(id = "1")
        repository.createTransaction(transaction)

        repository.deleteTransaction("1")

        repository.getAllTransactions().test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `clear removes all transactions`() = runTest {
        repository.setTransactions(
            listOf(
                TestData.transaction(id = "1"),
                TestData.transaction(id = "2")
            )
        )

        repository.clear()

        repository.getAllTransactions().test {
            assertTrue(awaitItem().isEmpty())
        }
    }
}

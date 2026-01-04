package com.finuts.test.fakes

import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant

/**
 * Fake implementation of TransactionRepository for testing.
 * Stores transactions in memory and provides full control for test scenarios.
 */
class FakeTransactionRepository : TransactionRepository {

    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())

    override fun getAllTransactions(): Flow<List<Transaction>> = transactions

    override fun getTransactionById(id: String): Flow<Transaction?> =
        transactions.map { list -> list.find { it.id == id } }

    override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { it.accountId == accountId } }

    override fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { it.categoryId == categoryId } }

    override fun getTransactionsByDateRange(
        start: Instant,
        end: Instant
    ): Flow<List<Transaction>> = transactions.map { list ->
        list.filter { it.date >= start && it.date <= end }
    }

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { it.type == type } }

    override suspend fun createTransaction(transaction: Transaction) {
        transactions.update { it + transaction }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactions.update { list ->
            list.map { if (it.id == transaction.id) transaction else it }
        }
    }

    override suspend fun deleteTransaction(id: String) {
        transactions.update { list -> list.filter { it.id != id } }
    }

    override suspend fun insertTransfer(outgoing: Transaction, incoming: Transaction) {
        transactions.update { it + outgoing + incoming }
    }

    // Test helpers
    fun setTransactions(newTransactions: List<Transaction>) {
        transactions.value = newTransactions
    }

    fun clear() {
        transactions.value = emptyList()
    }
}

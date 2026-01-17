package com.finuts.test.fakes.dao

import com.finuts.data.local.dao.TransactionDao
import com.finuts.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of TransactionDao for unit testing.
 * Stores entities in memory with full CRUD support.
 */
class FakeTransactionDao : TransactionDao {

    private val transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())

    override fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactions.map { it.sortedByDescending { t -> t.date } }

    override suspend fun getTransactionById(id: String): TransactionEntity? =
        transactions.value.find { it.id == id }

    override fun getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>> =
        transactions.map { list ->
            list.filter { it.accountId == accountId }.sortedByDescending { it.date }
        }

    override fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>> =
        transactions.map { list ->
            list.filter { it.categoryId == categoryId }.sortedByDescending { it.date }
        }

    override fun getTransactionsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionEntity>> = transactions.map { list ->
        list.filter { it.date in startDate..endDate }.sortedByDescending { it.date }
    }

    override fun getTransactionsByType(type: String): Flow<List<TransactionEntity>> =
        transactions.map { list ->
            list.filter { it.type == type }.sortedByDescending { it.date }
        }

    override fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>> =
        transactions.map { list ->
            list.sortedByDescending { it.date }.take(limit)
        }

    override suspend fun insert(transaction: TransactionEntity) {
        transactions.update { list ->
            list.filterNot { it.id == transaction.id } + transaction
        }
    }

    override suspend fun insertAll(transactions: List<TransactionEntity>) {
        transactions.forEach { insert(it) }
    }

    override suspend fun update(transaction: TransactionEntity) {
        transactions.update { list ->
            list.map { if (it.id == transaction.id) transaction else it }
        }
    }

    override suspend fun deleteById(id: String) {
        transactions.update { list -> list.filter { it.id != id } }
    }

    override suspend fun deleteByAccount(accountId: String) {
        transactions.update { list -> list.filter { it.accountId != accountId } }
    }

    override suspend fun insertTransfer(
        outgoing: TransactionEntity,
        incoming: TransactionEntity
    ) {
        insertAll(listOf(outgoing, incoming))
    }

    // Test helpers
    fun setTransactions(newTransactions: List<TransactionEntity>) {
        transactions.value = newTransactions
    }

    fun clear() {
        transactions.value = emptyList()
    }

    fun getAll(): List<TransactionEntity> = transactions.value
}

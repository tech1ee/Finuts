package com.finuts.data.repository

import com.finuts.data.local.dao.TransactionDao
import com.finuts.data.local.mapper.toDomain
import com.finuts.data.local.mapper.toEntity
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTransactionById(id: String): Flow<Transaction?> = flow {
        emit(transactionDao.getTransactionById(id)?.toDomain())
    }

    override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByAccount(accountId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTransactionsByDateRange(
        start: Instant,
        end: Instant
    ): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(
            startDate = start.toEpochMilliseconds(),
            endDate = end.toEpochMilliseconds()
        ).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type.name).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createTransaction(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: String) {
        transactionDao.deleteById(id)
    }

    override suspend fun insertTransfer(outgoing: Transaction, incoming: Transaction) {
        transactionDao.insertTransfer(outgoing.toEntity(), incoming.toEntity())
    }
}

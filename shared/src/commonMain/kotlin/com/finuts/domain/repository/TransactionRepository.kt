package com.finuts.domain.repository

import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionById(id: String): Flow<Transaction?>
    fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>>
    fun getTransactionsByDateRange(start: Instant, end: Instant): Flow<List<Transaction>>
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    suspend fun createTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: String)

    /**
     * Insert a transfer as two linked transactions atomically.
     * @param outgoing The transaction deducting from source account
     * @param incoming The transaction adding to destination account
     */
    suspend fun insertTransfer(outgoing: Transaction, incoming: Transaction)
}

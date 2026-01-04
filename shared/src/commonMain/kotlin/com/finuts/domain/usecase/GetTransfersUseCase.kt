package com.finuts.domain.usecase

import com.finuts.domain.entity.Transfer
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Use case for getting all transfers.
 *
 * Combines linked transfer transactions into Transfer domain entities
 * with populated account names.
 */
class GetTransfersUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    fun execute(): Flow<List<Transfer>> {
        return combine(
            transactionRepository.getTransactionsByType(TransactionType.TRANSFER),
            accountRepository.getAllAccounts()
        ) { transactions, accounts ->
            val accountMap = accounts.associateBy { it.id }
            val processedIds = mutableSetOf<String>()

            buildList {
                for (tx in transactions) {
                    if (tx.linkedTransactionId == null || tx.id in processedIds) continue

                    val incomingId = tx.linkedTransactionId
                    val incoming = transactions.find { it.id == incomingId } ?: continue

                    processedIds.add(tx.id)
                    processedIds.add(incoming.id)

                    val fromAccountId = tx.accountId
                    val toAccountId = tx.transferAccountId ?: incoming.accountId

                    add(
                        Transfer(
                            outgoingTransactionId = tx.id,
                            incomingTransactionId = incoming.id,
                            fromAccountId = fromAccountId,
                            fromAccountName = accountMap[fromAccountId]?.name ?: "Unknown",
                            toAccountId = toAccountId,
                            toAccountName = accountMap[toAccountId]?.name ?: "Unknown",
                            amount = tx.amount,
                            date = tx.date,
                            note = tx.note,
                            createdAt = tx.createdAt
                        )
                    )
                }
            }.sortedByDescending { it.date }
        }
    }
}

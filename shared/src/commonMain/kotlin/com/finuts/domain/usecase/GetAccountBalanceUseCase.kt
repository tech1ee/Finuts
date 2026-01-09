package com.finuts.domain.usecase

import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case for calculating account balance from transactions.
 *
 * Calculates: initialBalance + sum(INCOME) - sum(EXPENSE) +/- TRANSFER
 *
 * For transfers:
 * - If accountId matches our account → outgoing → subtract
 * - If transferAccountId matches our account → incoming → add
 */
class GetAccountBalanceUseCase(
    private val transactionRepository: TransactionRepository
) {
    /**
     * Calculate balance for an account based on its transactions.
     *
     * @param accountId The account to calculate balance for
     * @param initialBalance The starting balance before any transactions
     * @return Flow emitting the current calculated balance
     */
    fun execute(accountId: String, initialBalance: Long): Flow<Long> {
        return transactionRepository.getAllTransactions()
            .map { transactions ->
                var balance = initialBalance

                for (tx in transactions) {
                    when {
                        // Income to this account: add
                        tx.type == TransactionType.INCOME && tx.accountId == accountId -> {
                            balance += tx.amount
                        }
                        // Expense from this account: subtract
                        tx.type == TransactionType.EXPENSE && tx.accountId == accountId -> {
                            balance -= tx.amount
                        }
                        // Outgoing transfer from this account: subtract
                        tx.type == TransactionType.TRANSFER && tx.accountId == accountId -> {
                            balance -= tx.amount
                        }
                        // Incoming transfer to this account: add
                        tx.type == TransactionType.TRANSFER && tx.transferAccountId == accountId -> {
                            balance += tx.amount
                        }
                    }
                }

                balance
            }
    }
}

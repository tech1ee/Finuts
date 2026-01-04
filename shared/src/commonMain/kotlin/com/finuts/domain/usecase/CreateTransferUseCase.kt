package com.finuts.domain.usecase

import com.finuts.domain.entity.Transfer
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.TransactionRepository
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Use case for creating a transfer between two accounts.
 *
 * Creates two linked transactions:
 * - Outgoing: deducts amount from source account
 * - Incoming: adds amount to destination account
 */
class CreateTransferUseCase(
    private val transactionRepository: TransactionRepository,
    private val clock: () -> Instant = {
        Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
    }
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun execute(
        fromAccountId: String,
        toAccountId: String,
        amount: Long,
        date: Instant,
        note: String?
    ): Result<Transfer> {
        // Validate inputs
        if (fromAccountId == toAccountId) {
            return Result.failure(IllegalArgumentException("Cannot transfer to the same account"))
        }
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Transfer amount must be positive"))
        }

        val now = clock()
        val outgoingId = Uuid.random().toString()
        val incomingId = Uuid.random().toString()

        // Create outgoing transaction (deduct from source)
        val outgoing = Transaction(
            id = outgoingId,
            accountId = fromAccountId,
            amount = amount,
            type = TransactionType.TRANSFER,
            categoryId = null,
            description = "Transfer",
            merchant = null,
            note = note,
            date = date,
            createdAt = now,
            updatedAt = now,
            linkedTransactionId = incomingId,
            transferAccountId = toAccountId
        )

        // Create incoming transaction (add to destination)
        val incoming = Transaction(
            id = incomingId,
            accountId = toAccountId,
            amount = amount,
            type = TransactionType.TRANSFER,
            categoryId = null,
            description = "Transfer",
            merchant = null,
            note = note,
            date = date,
            createdAt = now,
            updatedAt = now,
            linkedTransactionId = outgoingId,
            transferAccountId = fromAccountId
        )

        return try {
            transactionRepository.insertTransfer(outgoing, incoming)

            val transfer = Transfer(
                outgoingTransactionId = outgoingId,
                incomingTransactionId = incomingId,
                fromAccountId = fromAccountId,
                fromAccountName = "", // Will be populated by UI layer
                toAccountId = toAccountId,
                toAccountName = "", // Will be populated by UI layer
                amount = amount,
                date = date,
                note = note,
                createdAt = now
            )
            Result.success(transfer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

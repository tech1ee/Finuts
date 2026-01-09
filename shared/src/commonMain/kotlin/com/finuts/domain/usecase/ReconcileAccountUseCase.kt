package com.finuts.domain.usecase

import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.TransactionRepository
import kotlinx.datetime.Instant
import kotlin.math.abs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Use case for reconciling account balance with a target value.
 *
 * Creates an adjustment transaction to correct the balance difference.
 * - If target > current: creates INCOME adjustment
 * - If target < current: creates EXPENSE adjustment
 */
class ReconcileAccountUseCase(
    private val transactionRepository: TransactionRepository,
    private val clock: () -> Instant = {
        Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
    }
) {
    /**
     * Reconcile account balance to match the target balance.
     *
     * @param accountId The account to reconcile
     * @param currentBalance The current calculated balance
     * @param targetBalance The desired balance after reconciliation
     * @param note Optional note for the adjustment transaction
     * @return Result with the created adjustment transaction, or failure if no adjustment needed
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun execute(
        accountId: String,
        currentBalance: Long,
        targetBalance: Long,
        note: String?
    ): Result<Transaction> {
        val difference = targetBalance - currentBalance

        if (difference == 0L) {
            return Result.failure(
                IllegalArgumentException("No adjustment needed - balances already match")
            )
        }

        val now = clock()
        val adjustmentType = if (difference > 0) TransactionType.INCOME else TransactionType.EXPENSE

        val adjustment = Transaction(
            id = Uuid.random().toString(),
            accountId = accountId,
            amount = abs(difference),
            type = adjustmentType,
            categoryId = null,
            description = "Balance Adjustment",
            merchant = null,
            note = note,
            date = now,
            isRecurring = false,
            recurringRuleId = null,
            attachments = emptyList(),
            tags = emptyList(),
            createdAt = now,
            updatedAt = now,
            linkedTransactionId = null,
            transferAccountId = null
        )

        return try {
            transactionRepository.createTransaction(adjustment)
            Result.success(adjustment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

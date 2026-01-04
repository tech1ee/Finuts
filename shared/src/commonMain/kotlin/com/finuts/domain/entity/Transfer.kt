package com.finuts.domain.entity

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a transfer between two accounts.
 *
 * A transfer is implemented as two linked transactions:
 * - Outgoing: TRANSFER type from source account (amount deducted)
 * - Incoming: TRANSFER type to destination account (amount added)
 *
 * This entity provides a unified view of both transactions for UI purposes.
 */
@Serializable
data class Transfer(
    val outgoingTransactionId: String,
    val incomingTransactionId: String,
    val fromAccountId: String,
    val fromAccountName: String,
    val toAccountId: String,
    val toAccountName: String,
    val amount: Long,
    val date: Instant,
    val note: String? = null,
    val createdAt: Instant
) {
    /**
     * Transfer ID is based on the outgoing transaction.
     */
    val id: String get() = outgoingTransactionId

    /**
     * Validates that the transfer is properly configured.
     */
    val isValid: Boolean
        get() = fromAccountId != toAccountId && amount > 0

    /**
     * Human-readable description of the transfer.
     */
    val displayDescription: String
        get() = "$fromAccountName → $toAccountName"
}

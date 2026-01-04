package com.finuts.domain.entity

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val currency: Currency,
    val balance: Long,
    val icon: String?,
    val color: String?,
    val isArchived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class AccountType {
    CASH,
    BANK_ACCOUNT,
    CREDIT_CARD,
    DEBIT_CARD,
    SAVINGS,
    INVESTMENT,
    CRYPTO,
    OTHER
}

@Serializable
data class Currency(
    val code: String,
    val symbol: String,
    val name: String
)

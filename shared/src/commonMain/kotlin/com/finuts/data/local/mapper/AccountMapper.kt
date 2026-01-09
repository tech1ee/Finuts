package com.finuts.data.local.mapper

import com.finuts.data.local.entity.AccountEntity
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.AccountType
import com.finuts.domain.entity.Currency
import kotlinx.datetime.Instant

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = AccountType.valueOf(type),
    currency = Currency(
        code = currencyCode,
        symbol = currencySymbol,
        name = currencyName
    ),
    balance = balance,
    initialBalance = initialBalance,
    icon = icon,
    color = color,
    isArchived = isArchived,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt)
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    type = type.name,
    currencyCode = currency.code,
    currencySymbol = currency.symbol,
    currencyName = currency.name,
    balance = balance,
    initialBalance = initialBalance,
    icon = icon,
    color = color,
    isArchived = isArchived,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds()
)

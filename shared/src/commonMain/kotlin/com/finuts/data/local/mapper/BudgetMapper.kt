package com.finuts.data.local.mapper

import com.finuts.data.local.entity.BudgetEntity
import com.finuts.domain.entity.Budget
import com.finuts.domain.entity.BudgetPeriod
import com.finuts.domain.entity.Currency
import kotlinx.datetime.Instant

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    categoryId = categoryId,
    name = name,
    amount = amount,
    currency = Currency(
        code = currencyCode,
        symbol = currencySymbol,
        name = currencyName
    ),
    period = BudgetPeriod.valueOf(period),
    startDate = Instant.fromEpochMilliseconds(startDate),
    endDate = endDate?.let { Instant.fromEpochMilliseconds(it) },
    isActive = isActive,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt)
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    categoryId = categoryId,
    name = name,
    amount = amount,
    currencyCode = currency.code,
    currencySymbol = currency.symbol,
    currencyName = currency.name,
    period = period.name,
    startDate = startDate.toEpochMilliseconds(),
    endDate = endDate?.toEpochMilliseconds(),
    isActive = isActive,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds()
)

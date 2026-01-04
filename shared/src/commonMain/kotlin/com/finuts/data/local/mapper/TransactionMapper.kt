package com.finuts.data.local.mapper

import com.finuts.data.local.entity.TransactionEntity
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    accountId = accountId,
    amount = amount,
    type = TransactionType.valueOf(type),
    categoryId = categoryId,
    description = description,
    merchant = merchant,
    note = note,
    date = Instant.fromEpochMilliseconds(date),
    isRecurring = isRecurring,
    recurringRuleId = recurringRuleId,
    attachments = attachments?.let { json.decodeFromString<List<String>>(it) } ?: emptyList(),
    tags = tags?.let { json.decodeFromString<List<String>>(it) } ?: emptyList(),
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    linkedTransactionId = linkedTransactionId,
    transferAccountId = transferAccountId
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    amount = amount,
    type = type.name,
    description = description,
    merchant = merchant,
    note = note,
    date = date.toEpochMilliseconds(),
    isRecurring = isRecurring,
    recurringRuleId = recurringRuleId,
    attachments = if (attachments.isEmpty()) null else json.encodeToString(attachments),
    tags = if (tags.isEmpty()) null else json.encodeToString(tags),
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
    linkedTransactionId = linkedTransactionId,
    transferAccountId = transferAccountId
)

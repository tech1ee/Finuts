package com.finuts.data.local.mapper

import com.finuts.data.local.entity.LearnedMerchantEntity
import com.finuts.domain.entity.LearnedMerchant
import com.finuts.domain.entity.LearnedMerchantSource
import kotlinx.datetime.Instant

fun LearnedMerchantEntity.toDomain(): LearnedMerchant = LearnedMerchant(
    id = id,
    merchantPattern = merchantPattern,
    categoryId = categoryId,
    confidence = confidence,
    source = LearnedMerchantSource.valueOf(source),
    sampleCount = sampleCount,
    lastUsedAt = Instant.fromEpochMilliseconds(lastUsedAt),
    createdAt = Instant.fromEpochMilliseconds(createdAt)
)

fun LearnedMerchant.toEntity(): LearnedMerchantEntity = LearnedMerchantEntity(
    id = id,
    merchantPattern = merchantPattern,
    categoryId = categoryId,
    confidence = confidence,
    source = source.name,
    sampleCount = sampleCount,
    lastUsedAt = lastUsedAt.toEpochMilliseconds(),
    createdAt = createdAt.toEpochMilliseconds()
)

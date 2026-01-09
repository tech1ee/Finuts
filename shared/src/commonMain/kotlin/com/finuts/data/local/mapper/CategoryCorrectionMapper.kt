package com.finuts.data.local.mapper

import com.finuts.data.local.entity.CategoryCorrectionEntity
import com.finuts.domain.entity.CategoryCorrection
import kotlinx.datetime.Instant

fun CategoryCorrectionEntity.toDomain(): CategoryCorrection = CategoryCorrection(
    id = id,
    transactionId = transactionId,
    originalCategoryId = originalCategoryId,
    correctedCategoryId = correctedCategoryId,
    merchantName = merchantName,
    merchantNormalized = merchantNormalized,
    createdAt = Instant.fromEpochMilliseconds(createdAt)
)

fun CategoryCorrection.toEntity(): CategoryCorrectionEntity = CategoryCorrectionEntity(
    id = id,
    transactionId = transactionId,
    originalCategoryId = originalCategoryId,
    correctedCategoryId = correctedCategoryId,
    merchantName = merchantName,
    merchantNormalized = merchantNormalized,
    createdAt = createdAt.toEpochMilliseconds()
)

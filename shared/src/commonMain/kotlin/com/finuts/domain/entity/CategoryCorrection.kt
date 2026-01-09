package com.finuts.domain.entity

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Domain entity representing a user's category correction.
 * When a user changes the AI-suggested category, we store this correction
 * to learn from it and improve future predictions.
 */
@Serializable
data class CategoryCorrection(
    val id: String,
    val transactionId: String,
    val originalCategoryId: String?,
    val correctedCategoryId: String,
    val merchantName: String?,
    val merchantNormalized: String?,
    val createdAt: Instant
)

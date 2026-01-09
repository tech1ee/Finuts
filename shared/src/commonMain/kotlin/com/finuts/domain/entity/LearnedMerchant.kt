package com.finuts.domain.entity

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Domain entity representing a learned merchant-to-category mapping.
 * Created from user corrections when the same merchant is corrected
 * to the same category multiple times.
 */
@Serializable
data class LearnedMerchant(
    val id: String,
    val merchantPattern: String,
    val categoryId: String,
    val confidence: Float,
    val source: LearnedMerchantSource,
    val sampleCount: Int,
    val lastUsedAt: Instant,
    val createdAt: Instant
)

/**
 * Source of the learned merchant mapping.
 */
@Serializable
enum class LearnedMerchantSource {
    /** From user corrections (highest priority) */
    USER,
    /** From on-device ML predictions */
    ML,
    /** From collaborative filtering (future) */
    COLLABORATIVE
}

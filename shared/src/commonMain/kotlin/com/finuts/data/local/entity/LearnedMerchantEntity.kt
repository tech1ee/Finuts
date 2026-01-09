package com.finuts.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing learned merchant-to-category mappings.
 * These mappings are created from user corrections and used in Tier 0 categorization.
 */
@Entity(
    tableName = "learned_merchants",
    indices = [
        Index("merchantPattern", unique = true)
    ]
)
data class LearnedMerchantEntity(
    @PrimaryKey
    val id: String,
    val merchantPattern: String,
    val categoryId: String,
    val confidence: Float,
    val source: String, // USER, ML, COLLABORATIVE
    val sampleCount: Int,
    val lastUsedAt: Long,
    val createdAt: Long
)

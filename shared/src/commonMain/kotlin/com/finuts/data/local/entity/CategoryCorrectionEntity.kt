package com.finuts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing user category corrections.
 * Used to learn from user corrections and improve future categorization (Tier 0).
 */
@Entity(
    tableName = "category_corrections",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("transactionId"),
        Index("merchantNormalized")
    ]
)
data class CategoryCorrectionEntity(
    @PrimaryKey
    val id: String,
    val transactionId: String,
    val originalCategoryId: String?,
    val correctedCategoryId: String,
    val merchantName: String?,
    val merchantNormalized: String?,
    val createdAt: Long
)

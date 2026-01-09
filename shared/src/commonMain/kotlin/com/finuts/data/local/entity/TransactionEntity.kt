package com.finuts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("date"),
        Index("linkedTransactionId")
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val accountId: String,
    val categoryId: String?,
    val amount: Long,
    val type: String,
    val description: String?,
    val merchant: String?,
    val note: String?,
    val date: Long,
    val isRecurring: Boolean,
    val recurringRuleId: String?,
    val attachments: String?,
    val tags: String?,
    val createdAt: Long,
    val updatedAt: Long,
    // Transfer fields: links two transactions for double-entry accounting
    val linkedTransactionId: String? = null,
    val transferAccountId: String? = null,
    // AI categorization metadata (Tier 0-3 tracking)
    val categorizationSource: String? = null,
    val categorizationConfidence: Float? = null
)

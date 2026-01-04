package com.finuts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String?,
    val name: String,
    val amount: Long,
    val currencyCode: String,
    val currencySymbol: String,
    val currencyName: String,
    val period: String,
    val startDate: Long,
    val endDate: Long?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

package com.finuts.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val currencyCode: String,
    val currencySymbol: String,
    val currencyName: String,
    val balance: Long,
    /** Initial balance before any transactions. Used for calculated balance. */
    val initialBalance: Long = 0L,
    val icon: String?,
    val color: String?,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

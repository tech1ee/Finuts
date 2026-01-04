package com.finuts.test

import com.finuts.data.local.entity.AccountEntity
import com.finuts.data.local.entity.BudgetEntity
import com.finuts.data.local.entity.CategoryEntity
import com.finuts.data.local.entity.TransactionEntity

/**
 * Test data factory for database entities in tests.
 */
object TestEntityData {

    const val DEFAULT_TIMESTAMP = 1704067200000L // 2024-01-01 00:00:00 UTC

    fun accountEntity(
        id: String = "account-1",
        name: String = "Test Account",
        type: String = "BANK_ACCOUNT",
        currencyCode: String = "KZT",
        currencySymbol: String = "₸",
        currencyName: String = "Kazakhstani Tenge",
        balance: Long = 0L,
        icon: String? = "bank",
        color: String? = "#4CAF50",
        isArchived: Boolean = false,
        createdAt: Long = DEFAULT_TIMESTAMP,
        updatedAt: Long = DEFAULT_TIMESTAMP
    ) = AccountEntity(
        id = id,
        name = name,
        type = type,
        currencyCode = currencyCode,
        currencySymbol = currencySymbol,
        currencyName = currencyName,
        balance = balance,
        icon = icon,
        color = color,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun transactionEntity(
        id: String = "transaction-1",
        accountId: String = "account-1",
        categoryId: String? = "category-1",
        amount: Long = 10000L,
        type: String = "EXPENSE",
        description: String? = "Test Transaction",
        merchant: String? = null,
        note: String? = null,
        date: Long = DEFAULT_TIMESTAMP,
        isRecurring: Boolean = false,
        recurringRuleId: String? = null,
        attachments: String? = null,
        tags: String? = null,
        createdAt: Long = DEFAULT_TIMESTAMP,
        updatedAt: Long = DEFAULT_TIMESTAMP
    ) = TransactionEntity(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        amount = amount,
        type = type,
        description = description,
        merchant = merchant,
        note = note,
        date = date,
        isRecurring = isRecurring,
        recurringRuleId = recurringRuleId,
        attachments = attachments,
        tags = tags,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun categoryEntity(
        id: String = "category-1",
        name: String = "Test Category",
        icon: String = "shopping_cart",
        color: String = "#FF9800",
        type: String = "EXPENSE",
        parentId: String? = null,
        isDefault: Boolean = false,
        sortOrder: Int = 0
    ) = CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        color = color,
        type = type,
        parentId = parentId,
        isDefault = isDefault,
        sortOrder = sortOrder
    )

    fun budgetEntity(
        id: String = "budget-1",
        categoryId: String? = "category-1",
        name: String = "Monthly Food Budget",
        amount: Long = 50000_00L,
        currencyCode: String = "KZT",
        currencySymbol: String = "₸",
        currencyName: String = "Kazakhstani Tenge",
        period: String = "MONTHLY",
        startDate: Long = DEFAULT_TIMESTAMP,
        endDate: Long? = null,
        isActive: Boolean = true,
        createdAt: Long = DEFAULT_TIMESTAMP,
        updatedAt: Long = DEFAULT_TIMESTAMP
    ) = BudgetEntity(
        id = id,
        categoryId = categoryId,
        name = name,
        amount = amount,
        currencyCode = currencyCode,
        currencySymbol = currencySymbol,
        currencyName = currencyName,
        period = period,
        startDate = startDate,
        endDate = endDate,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

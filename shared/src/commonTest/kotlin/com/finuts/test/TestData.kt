package com.finuts.test

import com.finuts.domain.entity.Account
import com.finuts.domain.entity.AccountType
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategoryType
import com.finuts.domain.entity.Currency
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.entity.Budget
import com.finuts.domain.entity.BudgetPeriod
import kotlinx.datetime.Instant

/**
 * Test data factory for creating domain entities in tests.
 * Provides sensible defaults while allowing customization.
 */
object TestData {

    /** Default timestamp for tests - 2024-01-01 00:00:00 UTC */
    val DEFAULT_INSTANT: Instant = Instant.parse("2024-01-01T00:00:00Z")

    val KZT = Currency(code = "KZT", symbol = "₸", name = "Kazakhstani Tenge")
    val USD = Currency(code = "USD", symbol = "$", name = "US Dollar")

    fun account(
        id: String = "account-1",
        name: String = "Test Account",
        type: AccountType = AccountType.BANK_ACCOUNT,
        currency: Currency = KZT,
        balance: Long = 0L,
        icon: String? = "bank",
        color: String? = "#4CAF50",
        isArchived: Boolean = false,
        createdAt: Instant = DEFAULT_INSTANT,
        updatedAt: Instant = DEFAULT_INSTANT
    ) = Account(
        id = id,
        name = name,
        type = type,
        currency = currency,
        balance = balance,
        icon = icon,
        color = color,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun transaction(
        id: String = "transaction-1",
        accountId: String = "account-1",
        amount: Long = 10000L,
        type: TransactionType = TransactionType.EXPENSE,
        categoryId: String? = "category-1",
        description: String? = "Test Transaction",
        merchant: String? = null,
        note: String? = null,
        date: Instant = DEFAULT_INSTANT,
        isRecurring: Boolean = false,
        recurringRuleId: String? = null,
        attachments: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        createdAt: Instant = DEFAULT_INSTANT,
        updatedAt: Instant = DEFAULT_INSTANT
    ) = Transaction(
        id = id,
        accountId = accountId,
        amount = amount,
        type = type,
        categoryId = categoryId,
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

    fun category(
        id: String = "category-1",
        name: String = "Test Category",
        icon: String = "shopping_cart",
        color: String = "#FF9800",
        type: CategoryType = CategoryType.EXPENSE,
        parentId: String? = null,
        isDefault: Boolean = false,
        sortOrder: Int = 0
    ) = Category(
        id = id,
        name = name,
        icon = icon,
        color = color,
        type = type,
        parentId = parentId,
        isDefault = isDefault,
        sortOrder = sortOrder
    )

    fun budget(
        id: String = "budget-1",
        categoryId: String? = "category-1",
        name: String = "Test Budget",
        amount: Long = 50000_00L,
        currency: Currency = KZT,
        period: BudgetPeriod = BudgetPeriod.MONTHLY,
        startDate: Instant = DEFAULT_INSTANT,
        endDate: Instant? = null,
        isActive: Boolean = true,
        createdAt: Instant = DEFAULT_INSTANT,
        updatedAt: Instant = DEFAULT_INSTANT
    ) = Budget(
        id = id,
        categoryId = categoryId,
        name = name,
        amount = amount,
        currency = currency,
        period = period,
        startDate = startDate,
        endDate = endDate,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

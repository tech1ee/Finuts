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
import com.finuts.domain.entity.import.DocumentType
import com.finuts.domain.entity.import.ImportedTransaction
import com.finuts.domain.entity.import.ImportResult
import com.finuts.domain.entity.import.ImportSource
import com.finuts.domain.entity.CategorizationSource
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

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
        initialBalance: Long = 0L,
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
        initialBalance = initialBalance,
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
        updatedAt: Instant = DEFAULT_INSTANT,
        linkedTransactionId: String? = null,
        transferAccountId: String? = null,
        categorizationSource: CategorizationSource? = null,
        categorizationConfidence: Float? = null
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
        updatedAt = updatedAt,
        linkedTransactionId = linkedTransactionId,
        transferAccountId = transferAccountId,
        categorizationSource = categorizationSource,
        categorizationConfidence = categorizationConfidence
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

    // ==================== Import Entities ====================

    /** Default date for import tests - 2024-01-15 */
    val DEFAULT_LOCAL_DATE: LocalDate = LocalDate(2024, 1, 15)

    fun importedTransaction(
        date: LocalDate = DEFAULT_LOCAL_DATE,
        amount: Long = 10000L,
        description: String = "Test Import Transaction",
        merchant: String? = null,
        balance: Long? = null,
        category: String? = null,
        confidence: Float = 0.9f,
        source: ImportSource = ImportSource.RULE_BASED,
        rawData: Map<String, String> = emptyMap()
    ) = ImportedTransaction(
        date = date,
        amount = amount,
        description = description,
        merchant = merchant,
        balance = balance,
        category = category,
        confidence = confidence,
        source = source,
        rawData = rawData
    )

    fun documentTypeCsv(
        delimiter: Char = ',',
        encoding: String = "UTF-8"
    ) = DocumentType.Csv(delimiter, encoding)

    fun documentTypePdf(
        bankSignature: String? = null
    ) = DocumentType.Pdf(bankSignature)

    fun documentTypeOfx(
        version: String = "2.2"
    ) = DocumentType.Ofx(version)

    fun importResultSuccess(
        transactions: List<ImportedTransaction> = listOf(importedTransaction()),
        documentType: DocumentType = documentTypeCsv(),
        totalConfidence: Float = 0.9f
    ) = ImportResult.Success(transactions, documentType, totalConfidence)

    fun importResultError(
        message: String = "Parse error",
        documentType: DocumentType? = null,
        partialTransactions: List<ImportedTransaction> = emptyList()
    ) = ImportResult.Error(message, documentType, partialTransactions)

    fun importResultNeedsInput(
        transactions: List<ImportedTransaction> = listOf(importedTransaction(confidence = 0.5f)),
        documentType: DocumentType = documentTypePdf(),
        issues: List<String> = listOf("Review required")
    ) = ImportResult.NeedsUserInput(transactions, documentType, issues)
}

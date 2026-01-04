package com.finuts.data.local.mapper

import com.finuts.domain.entity.AccountType
import com.finuts.domain.entity.Currency
import com.finuts.test.TestData
import com.finuts.test.TestEntityData
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for AccountMapper extension functions.
 */
class AccountMapperTest {

    @Test
    fun `toDomain maps entity to domain correctly`() {
        val entity = TestEntityData.accountEntity(
            id = "acc-123",
            name = "My Savings",
            type = "SAVINGS",
            currencyCode = "USD",
            currencySymbol = "$",
            currencyName = "US Dollar",
            balance = 150000L,
            icon = "piggy_bank",
            color = "#2196F3",
            isArchived = false,
            createdAt = 1704067200000L,
            updatedAt = 1704153600000L
        )

        val domain = entity.toDomain()

        assertEquals("acc-123", domain.id)
        assertEquals("My Savings", domain.name)
        assertEquals(AccountType.SAVINGS, domain.type)
        assertEquals("USD", domain.currency.code)
        assertEquals("$", domain.currency.symbol)
        assertEquals("US Dollar", domain.currency.name)
        assertEquals(150000L, domain.balance)
        assertEquals("piggy_bank", domain.icon)
        assertEquals("#2196F3", domain.color)
        assertEquals(false, domain.isArchived)
        assertEquals(Instant.fromEpochMilliseconds(1704067200000L), domain.createdAt)
        assertEquals(Instant.fromEpochMilliseconds(1704153600000L), domain.updatedAt)
    }

    @Test
    fun `toDomain maps all account types correctly`() {
        AccountType.entries.forEach { accountType ->
            val entity = TestEntityData.accountEntity(type = accountType.name)
            val domain = entity.toDomain()
            assertEquals(accountType, domain.type)
        }
    }

    @Test
    fun `toDomain handles null icon and color`() {
        val entity = TestEntityData.accountEntity(icon = null, color = null)
        val domain = entity.toDomain()
        assertEquals(null, domain.icon)
        assertEquals(null, domain.color)
    }

    @Test
    fun `toDomain handles archived account`() {
        val entity = TestEntityData.accountEntity(isArchived = true)
        val domain = entity.toDomain()
        assertEquals(true, domain.isArchived)
    }

    @Test
    fun `toEntity maps domain to entity correctly`() {
        val domain = TestData.account(
            id = "acc-456",
            name = "Credit Card",
            type = AccountType.CREDIT_CARD,
            currency = Currency("EUR", "€", "Euro"),
            balance = -50000L,
            icon = "credit_card",
            color = "#F44336",
            isArchived = true,
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2024-01-02T00:00:00Z")
        )

        val entity = domain.toEntity()

        assertEquals("acc-456", entity.id)
        assertEquals("Credit Card", entity.name)
        assertEquals("CREDIT_CARD", entity.type)
        assertEquals("EUR", entity.currencyCode)
        assertEquals("€", entity.currencySymbol)
        assertEquals("Euro", entity.currencyName)
        assertEquals(-50000L, entity.balance)
        assertEquals("credit_card", entity.icon)
        assertEquals("#F44336", entity.color)
        assertEquals(true, entity.isArchived)
        assertEquals(1704067200000L, entity.createdAt)
        assertEquals(1704153600000L, entity.updatedAt)
    }

    @Test
    fun `toEntity maps all account types correctly`() {
        AccountType.entries.forEach { accountType ->
            val domain = TestData.account(type = accountType)
            val entity = domain.toEntity()
            assertEquals(accountType.name, entity.type)
        }
    }

    @Test
    fun `roundtrip entity to domain to entity preserves data`() {
        val originalEntity = TestEntityData.accountEntity(
            id = "roundtrip-1",
            name = "Roundtrip Account",
            type = "INVESTMENT",
            currencyCode = "RUB",
            currencySymbol = "₽",
            currencyName = "Russian Ruble",
            balance = 1000000L,
            icon = "trending_up",
            color = "#9C27B0",
            isArchived = false,
            createdAt = 1704067200000L,
            updatedAt = 1704067200000L
        )

        val domain = originalEntity.toDomain()
        val resultEntity = domain.toEntity()

        assertEquals(originalEntity.id, resultEntity.id)
        assertEquals(originalEntity.name, resultEntity.name)
        assertEquals(originalEntity.type, resultEntity.type)
        assertEquals(originalEntity.currencyCode, resultEntity.currencyCode)
        assertEquals(originalEntity.currencySymbol, resultEntity.currencySymbol)
        assertEquals(originalEntity.currencyName, resultEntity.currencyName)
        assertEquals(originalEntity.balance, resultEntity.balance)
        assertEquals(originalEntity.icon, resultEntity.icon)
        assertEquals(originalEntity.color, resultEntity.color)
        assertEquals(originalEntity.isArchived, resultEntity.isArchived)
        assertEquals(originalEntity.createdAt, resultEntity.createdAt)
        assertEquals(originalEntity.updatedAt, resultEntity.updatedAt)
    }

    @Test
    fun `roundtrip domain to entity to domain preserves data`() {
        val originalDomain = TestData.account(
            id = "roundtrip-2",
            name = "Crypto Wallet",
            type = AccountType.CRYPTO,
            currency = Currency("KZT", "₸", "Kazakhstani Tenge"),
            balance = 5000000L,
            icon = "currency_bitcoin",
            color = "#FF9800",
            isArchived = false
        )

        val entity = originalDomain.toEntity()
        val resultDomain = entity.toDomain()

        assertEquals(originalDomain.id, resultDomain.id)
        assertEquals(originalDomain.name, resultDomain.name)
        assertEquals(originalDomain.type, resultDomain.type)
        assertEquals(originalDomain.currency, resultDomain.currency)
        assertEquals(originalDomain.balance, resultDomain.balance)
        assertEquals(originalDomain.icon, resultDomain.icon)
        assertEquals(originalDomain.color, resultDomain.color)
        assertEquals(originalDomain.isArchived, resultDomain.isArchived)
        assertEquals(originalDomain.createdAt, resultDomain.createdAt)
        assertEquals(originalDomain.updatedAt, resultDomain.updatedAt)
    }

    @Test
    fun `toDomain handles zero balance`() {
        val entity = TestEntityData.accountEntity(balance = 0L)
        val domain = entity.toDomain()
        assertEquals(0L, domain.balance)
    }

    @Test
    fun `toDomain handles negative balance`() {
        val entity = TestEntityData.accountEntity(balance = -100000L)
        val domain = entity.toDomain()
        assertEquals(-100000L, domain.balance)
    }

    @Test
    fun `toDomain handles large balance`() {
        val largeBalance = Long.MAX_VALUE / 2
        val entity = TestEntityData.accountEntity(balance = largeBalance)
        val domain = entity.toDomain()
        assertEquals(largeBalance, domain.balance)
    }
}

package com.finuts.data.local.mapper

import com.finuts.domain.entity.BudgetPeriod
import com.finuts.domain.entity.Currency
import com.finuts.test.TestData
import com.finuts.test.TestEntityData
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for BudgetMapper extension functions.
 */
class BudgetMapperTest {

    @Test
    fun `toDomain maps entity to domain correctly`() {
        val entity = TestEntityData.budgetEntity(
            id = "budget-123",
            categoryId = "cat-1",
            name = "Food Budget",
            amount = 100000_00L,
            currencyCode = "KZT",
            currencySymbol = "₸",
            currencyName = "Kazakhstani Tenge",
            period = "MONTHLY",
            startDate = 1704067200000L,
            endDate = 1706745600000L,
            isActive = true,
            createdAt = 1704067200000L,
            updatedAt = 1704153600000L
        )

        val domain = entity.toDomain()

        assertEquals("budget-123", domain.id)
        assertEquals("cat-1", domain.categoryId)
        assertEquals("Food Budget", domain.name)
        assertEquals(100000_00L, domain.amount)
        assertEquals("KZT", domain.currency.code)
        assertEquals("₸", domain.currency.symbol)
        assertEquals("Kazakhstani Tenge", domain.currency.name)
        assertEquals(BudgetPeriod.MONTHLY, domain.period)
        assertEquals(Instant.fromEpochMilliseconds(1704067200000L), domain.startDate)
        assertEquals(Instant.fromEpochMilliseconds(1706745600000L), domain.endDate)
        assertEquals(true, domain.isActive)
        assertEquals(Instant.fromEpochMilliseconds(1704067200000L), domain.createdAt)
        assertEquals(Instant.fromEpochMilliseconds(1704153600000L), domain.updatedAt)
    }

    @Test
    fun `toDomain maps all budget periods correctly`() {
        BudgetPeriod.entries.forEach { period ->
            val entity = TestEntityData.budgetEntity(period = period.name)
            val domain = entity.toDomain()
            assertEquals(period, domain.period)
        }
    }

    @Test
    fun `toDomain handles null categoryId`() {
        val entity = TestEntityData.budgetEntity(categoryId = null)
        val domain = entity.toDomain()
        assertNull(domain.categoryId)
    }

    @Test
    fun `toDomain handles null endDate`() {
        val entity = TestEntityData.budgetEntity(endDate = null)
        val domain = entity.toDomain()
        assertNull(domain.endDate)
    }

    @Test
    fun `toDomain handles inactive budget`() {
        val entity = TestEntityData.budgetEntity(isActive = false)
        val domain = entity.toDomain()
        assertEquals(false, domain.isActive)
    }

    @Test
    fun `toEntity maps domain to entity correctly`() {
        val domain = TestData.budget(
            id = "budget-456",
            categoryId = "cat-2",
            name = "Entertainment",
            amount = 25000_00L,
            currency = Currency("USD", "$", "US Dollar"),
            period = BudgetPeriod.WEEKLY,
            startDate = Instant.parse("2024-01-01T00:00:00Z"),
            endDate = Instant.parse("2024-03-31T23:59:59Z"),
            isActive = true,
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2024-01-02T00:00:00Z")
        )

        val entity = domain.toEntity()

        assertEquals("budget-456", entity.id)
        assertEquals("cat-2", entity.categoryId)
        assertEquals("Entertainment", entity.name)
        assertEquals(25000_00L, entity.amount)
        assertEquals("USD", entity.currencyCode)
        assertEquals("$", entity.currencySymbol)
        assertEquals("US Dollar", entity.currencyName)
        assertEquals("WEEKLY", entity.period)
        assertEquals(1704067200000L, entity.startDate)
        assertEquals(1711929599000L, entity.endDate)
        assertEquals(true, entity.isActive)
    }

    @Test
    fun `toEntity maps all budget periods correctly`() {
        BudgetPeriod.entries.forEach { period ->
            val domain = TestData.budget(period = period)
            val entity = domain.toEntity()
            assertEquals(period.name, entity.period)
        }
    }

    @Test
    fun `toEntity handles null categoryId`() {
        val domain = TestData.budget(categoryId = null)
        val entity = domain.toEntity()
        assertNull(entity.categoryId)
    }

    @Test
    fun `toEntity handles null endDate`() {
        val domain = TestData.budget(endDate = null)
        val entity = domain.toEntity()
        assertNull(entity.endDate)
    }

    @Test
    fun `roundtrip entity to domain to entity preserves data`() {
        val originalEntity = TestEntityData.budgetEntity(
            id = "roundtrip-1",
            categoryId = "cat-rt",
            name = "Travel Budget",
            amount = 500000_00L,
            currencyCode = "EUR",
            currencySymbol = "€",
            currencyName = "Euro",
            period = "YEARLY",
            startDate = 1704067200000L,
            endDate = 1735689600000L,
            isActive = true,
            createdAt = 1704067200000L,
            updatedAt = 1704067200000L
        )

        val domain = originalEntity.toDomain()
        val resultEntity = domain.toEntity()

        assertEquals(originalEntity.id, resultEntity.id)
        assertEquals(originalEntity.categoryId, resultEntity.categoryId)
        assertEquals(originalEntity.name, resultEntity.name)
        assertEquals(originalEntity.amount, resultEntity.amount)
        assertEquals(originalEntity.currencyCode, resultEntity.currencyCode)
        assertEquals(originalEntity.currencySymbol, resultEntity.currencySymbol)
        assertEquals(originalEntity.currencyName, resultEntity.currencyName)
        assertEquals(originalEntity.period, resultEntity.period)
        assertEquals(originalEntity.startDate, resultEntity.startDate)
        assertEquals(originalEntity.endDate, resultEntity.endDate)
        assertEquals(originalEntity.isActive, resultEntity.isActive)
        assertEquals(originalEntity.createdAt, resultEntity.createdAt)
        assertEquals(originalEntity.updatedAt, resultEntity.updatedAt)
    }

    @Test
    fun `roundtrip domain to entity to domain preserves data`() {
        val originalDomain = TestData.budget(
            id = "roundtrip-2",
            categoryId = "cat-rt2",
            name = "Savings Goal",
            amount = 1000000_00L,
            currency = Currency("RUB", "₽", "Russian Ruble"),
            period = BudgetPeriod.QUARTERLY,
            startDate = Instant.parse("2024-01-01T00:00:00Z"),
            endDate = Instant.parse("2024-12-31T23:59:59Z"),
            isActive = false
        )

        val entity = originalDomain.toEntity()
        val resultDomain = entity.toDomain()

        assertEquals(originalDomain.id, resultDomain.id)
        assertEquals(originalDomain.categoryId, resultDomain.categoryId)
        assertEquals(originalDomain.name, resultDomain.name)
        assertEquals(originalDomain.amount, resultDomain.amount)
        assertEquals(originalDomain.currency, resultDomain.currency)
        assertEquals(originalDomain.period, resultDomain.period)
        assertEquals(originalDomain.startDate, resultDomain.startDate)
        assertEquals(originalDomain.endDate, resultDomain.endDate)
        assertEquals(originalDomain.isActive, resultDomain.isActive)
        assertEquals(originalDomain.createdAt, resultDomain.createdAt)
        assertEquals(originalDomain.updatedAt, resultDomain.updatedAt)
    }

    @Test
    fun `toDomain handles daily period`() {
        val entity = TestEntityData.budgetEntity(period = "DAILY")
        val domain = entity.toDomain()
        assertEquals(BudgetPeriod.DAILY, domain.period)
    }

    @Test
    fun `toDomain handles weekly period`() {
        val entity = TestEntityData.budgetEntity(period = "WEEKLY")
        val domain = entity.toDomain()
        assertEquals(BudgetPeriod.WEEKLY, domain.period)
    }

    @Test
    fun `toDomain handles quarterly period`() {
        val entity = TestEntityData.budgetEntity(period = "QUARTERLY")
        val domain = entity.toDomain()
        assertEquals(BudgetPeriod.QUARTERLY, domain.period)
    }

    @Test
    fun `toDomain handles yearly period`() {
        val entity = TestEntityData.budgetEntity(period = "YEARLY")
        val domain = entity.toDomain()
        assertEquals(BudgetPeriod.YEARLY, domain.period)
    }

    @Test
    fun `toDomain handles zero amount`() {
        val entity = TestEntityData.budgetEntity(amount = 0L)
        val domain = entity.toDomain()
        assertEquals(0L, domain.amount)
    }

    @Test
    fun `toDomain handles large amount`() {
        val largeAmount = Long.MAX_VALUE / 2
        val entity = TestEntityData.budgetEntity(amount = largeAmount)
        val domain = entity.toDomain()
        assertEquals(largeAmount, domain.amount)
    }
}

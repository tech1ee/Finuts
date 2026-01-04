package com.finuts.domain.entity

import com.finuts.test.TestData
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Account entity and related types.
 */
class AccountTest {

    @Test
    fun `Account can be created with all parameters`() {
        val instant = Instant.parse("2024-01-01T00:00:00Z")
        val currency = Currency("KZT", "₸", "Kazakhstani Tenge")

        val account = Account(
            id = "acc-1",
            name = "My Account",
            type = AccountType.BANK_ACCOUNT,
            currency = currency,
            balance = 100000L,
            icon = "bank",
            color = "#4CAF50",
            isArchived = false,
            createdAt = instant,
            updatedAt = instant
        )

        assertEquals("acc-1", account.id)
        assertEquals("My Account", account.name)
        assertEquals(AccountType.BANK_ACCOUNT, account.type)
        assertEquals(currency, account.currency)
        assertEquals(100000L, account.balance)
        assertEquals("bank", account.icon)
        assertEquals("#4CAF50", account.color)
        assertFalse(account.isArchived)
        assertEquals(instant, account.createdAt)
        assertEquals(instant, account.updatedAt)
    }

    @Test
    fun `Account isArchived defaults to false`() {
        val account = TestData.account()
        assertFalse(account.isArchived)
    }

    @Test
    fun `Account allows null icon and color`() {
        val account = TestData.account(icon = null, color = null)
        assertNull(account.icon)
        assertNull(account.color)
    }

    @Test
    fun `Account allows negative balance`() {
        val account = TestData.account(balance = -50000L)
        assertEquals(-50000L, account.balance)
    }

    @Test
    fun `Account allows zero balance`() {
        val account = TestData.account(balance = 0L)
        assertEquals(0L, account.balance)
    }

    @Test
    fun `Account copy works correctly`() {
        val original = TestData.account(name = "Original", balance = 1000L)
        val modified = original.copy(name = "Modified", balance = 2000L)

        assertEquals("Original", original.name)
        assertEquals(1000L, original.balance)
        assertEquals("Modified", modified.name)
        assertEquals(2000L, modified.balance)
        assertEquals(original.id, modified.id)
    }

    @Test
    fun `AccountType has all expected values`() {
        val types = AccountType.entries.map { it.name }
        assertTrue("CASH" in types)
        assertTrue("BANK_ACCOUNT" in types)
        assertTrue("CREDIT_CARD" in types)
        assertTrue("DEBIT_CARD" in types)
        assertTrue("SAVINGS" in types)
        assertTrue("INVESTMENT" in types)
        assertTrue("CRYPTO" in types)
        assertTrue("OTHER" in types)
        assertEquals(8, AccountType.entries.size)
    }

    @Test
    fun `Currency can be created`() {
        val currency = Currency("USD", "$", "US Dollar")
        assertEquals("USD", currency.code)
        assertEquals("$", currency.symbol)
        assertEquals("US Dollar", currency.name)
    }

    @Test
    fun `Currency equality works`() {
        val currency1 = Currency("KZT", "₸", "Kazakhstani Tenge")
        val currency2 = Currency("KZT", "₸", "Kazakhstani Tenge")
        val currency3 = Currency("USD", "$", "US Dollar")

        assertEquals(currency1, currency2)
        assertTrue(currency1 != currency3)
    }

    @Test
    fun `all AccountTypes can be used in Account`() {
        AccountType.entries.forEach { type ->
            val account = TestData.account(type = type)
            assertEquals(type, account.type)
        }
    }
}

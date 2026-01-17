package com.finuts.app.ui.components.cards

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for AccountCard utility functions.
 * TDD: Write tests first for extracted utilities.
 */
class AccountCardUtilsTest {

    // === accountTypeToAccentColorIndex Tests ===

    @Test
    fun `accountTypeToAccentColorIndex returns 0 for CASH`() {
        val result = accountTypeToAccentColorIndex(AccountType.CASH)
        assertEquals(0, result)
    }

    @Test
    fun `accountTypeToAccentColorIndex returns 1 for DEBIT_CARD`() {
        val result = accountTypeToAccentColorIndex(AccountType.DEBIT_CARD)
        assertEquals(1, result)
    }

    @Test
    fun `accountTypeToAccentColorIndex returns 2 for CREDIT_CARD`() {
        val result = accountTypeToAccentColorIndex(AccountType.CREDIT_CARD)
        assertEquals(2, result)
    }

    @Test
    fun `accountTypeToAccentColorIndex returns 3 for SAVINGS`() {
        val result = accountTypeToAccentColorIndex(AccountType.SAVINGS)
        assertEquals(3, result)
    }

    @Test
    fun `accountTypeToAccentColorIndex returns 4 for INVESTMENT`() {
        val result = accountTypeToAccentColorIndex(AccountType.INVESTMENT)
        assertEquals(4, result)
    }

    // === formatAccountTypeName Tests ===

    @Test
    fun `formatAccountTypeName replaces underscores with spaces`() {
        val result = formatAccountTypeName("DEBIT_CARD")
        assertEquals("DEBIT CARD", result)
    }

    @Test
    fun `formatAccountTypeName handles name without underscores`() {
        val result = formatAccountTypeName("CASH")
        assertEquals("CASH", result)
    }

    @Test
    fun `formatAccountTypeName handles multiple underscores`() {
        val result = formatAccountTypeName("SOME_TYPE_NAME")
        assertEquals("SOME TYPE NAME", result)
    }

    @Test
    fun `formatAccountTypeName handles empty string`() {
        val result = formatAccountTypeName("")
        assertEquals("", result)
    }

    // === AccountType enum Tests ===

    @Test
    fun `AccountType has all expected values`() {
        val types = AccountType.entries
        assertEquals(5, types.size)
        assertNotNull(types.find { it == AccountType.CASH })
        assertNotNull(types.find { it == AccountType.DEBIT_CARD })
        assertNotNull(types.find { it == AccountType.CREDIT_CARD })
        assertNotNull(types.find { it == AccountType.SAVINGS })
        assertNotNull(types.find { it == AccountType.INVESTMENT })
    }
}

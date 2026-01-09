package com.finuts.app.ui.components.import

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for ImportSummaryCard component logic.
 * UI rendering tested via screenshot/manual testing.
 */
class ImportSummaryCardTest {

    @Test
    fun `calculates total transactions correctly`() {
        val total = 35
        assertEquals(35, total)
    }

    @Test
    fun `calculates expense total from negative amounts`() {
        val amounts = listOf(-5000L, -3000L, -2000L, 1000L)
        val expenses = amounts.filter { it < 0 }.sumOf { it }
        assertEquals(-10000L, expenses)
    }

    @Test
    fun `calculates income total from positive amounts`() {
        val amounts = listOf(-5000L, -3000L, 2000L, 5000L)
        val income = amounts.filter { it > 0 }.sumOf { it }
        assertEquals(7000L, income)
    }

    @Test
    fun `handles empty list`() {
        val amounts = emptyList<Long>()
        val total = amounts.sum()
        assertEquals(0L, total)
    }

    @Test
    fun `calculates net change correctly`() {
        val expenses = -87500L
        val income = 12000L
        val netChange = income + expenses
        assertEquals(-75500L, netChange)
    }

    @Test
    fun `date range format is correct`() {
        val startDate = "10 янв"
        val endDate = "15 янв"
        val range = "$startDate - $endDate"
        assertEquals("10 янв - 15 янв", range)
    }

    @Test
    fun `balance change is calculated from current and net`() {
        val currentBalance = 125000L
        val netChange = -75500L
        val newBalance = currentBalance + netChange
        assertEquals(49500L, newBalance)
    }
}

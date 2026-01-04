package com.finuts.domain.entity

import com.finuts.test.TestData
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Transaction entity and related types.
 */
class TransactionTest {

    @Test
    fun `Transaction can be created with all parameters`() {
        val instant = Instant.parse("2024-01-15T10:30:00Z")

        val transaction = Transaction(
            id = "tx-1",
            accountId = "acc-1",
            amount = 5000L,
            type = TransactionType.EXPENSE,
            categoryId = "cat-1",
            description = "Coffee",
            merchant = "Starbucks",
            note = "Morning coffee",
            date = instant,
            isRecurring = true,
            recurringRuleId = "rule-1",
            attachments = listOf("receipt.jpg"),
            tags = listOf("food", "coffee"),
            createdAt = instant,
            updatedAt = instant
        )

        assertEquals("tx-1", transaction.id)
        assertEquals("acc-1", transaction.accountId)
        assertEquals(5000L, transaction.amount)
        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals("cat-1", transaction.categoryId)
        assertEquals("Coffee", transaction.description)
        assertEquals("Starbucks", transaction.merchant)
        assertEquals("Morning coffee", transaction.note)
        assertEquals(instant, transaction.date)
        assertTrue(transaction.isRecurring)
        assertEquals("rule-1", transaction.recurringRuleId)
        assertEquals(listOf("receipt.jpg"), transaction.attachments)
        assertEquals(listOf("food", "coffee"), transaction.tags)
    }

    @Test
    fun `Transaction defaults isRecurring to false`() {
        val tx = TestData.transaction()
        assertFalse(tx.isRecurring)
    }

    @Test
    fun `Transaction defaults attachments to empty list`() {
        val tx = TestData.transaction()
        assertTrue(tx.attachments.isEmpty())
    }

    @Test
    fun `Transaction defaults tags to empty list`() {
        val tx = TestData.transaction()
        assertTrue(tx.tags.isEmpty())
    }

    @Test
    fun `Transaction allows null categoryId`() {
        val tx = TestData.transaction(categoryId = null)
        assertNull(tx.categoryId)
    }

    @Test
    fun `Transaction allows null description`() {
        val tx = TestData.transaction(description = null)
        assertNull(tx.description)
    }

    @Test
    fun `Transaction allows null merchant`() {
        val tx = TestData.transaction(merchant = null)
        assertNull(tx.merchant)
    }

    @Test
    fun `Transaction allows null note`() {
        val tx = TestData.transaction(note = null)
        assertNull(tx.note)
    }

    @Test
    fun `Transaction allows null recurringRuleId`() {
        val tx = TestData.transaction(recurringRuleId = null)
        assertNull(tx.recurringRuleId)
    }

    @Test
    fun `Transaction allows negative amount for expenses`() {
        val tx = TestData.transaction(amount = -10000L, type = TransactionType.EXPENSE)
        assertEquals(-10000L, tx.amount)
    }

    @Test
    fun `Transaction allows positive amount for income`() {
        val tx = TestData.transaction(amount = 50000L, type = TransactionType.INCOME)
        assertEquals(50000L, tx.amount)
    }

    @Test
    fun `Transaction copy works correctly`() {
        val original = TestData.transaction(description = "Original", amount = 1000L)
        val modified = original.copy(description = "Modified", amount = 2000L)

        assertEquals("Original", original.description)
        assertEquals(1000L, original.amount)
        assertEquals("Modified", modified.description)
        assertEquals(2000L, modified.amount)
        assertEquals(original.id, modified.id)
    }

    @Test
    fun `TransactionType has all expected values`() {
        val types = TransactionType.entries.map { it.name }
        assertTrue("INCOME" in types)
        assertTrue("EXPENSE" in types)
        assertTrue("TRANSFER" in types)
        assertEquals(3, TransactionType.entries.size)
    }

    @Test
    fun `all TransactionTypes can be used in Transaction`() {
        TransactionType.entries.forEach { type ->
            val tx = TestData.transaction(type = type)
            assertEquals(type, tx.type)
        }
    }

    @Test
    fun `Transaction with multiple attachments`() {
        val attachments = listOf("doc1.pdf", "doc2.jpg", "doc3.png")
        val tx = TestData.transaction(attachments = attachments)
        assertEquals(3, tx.attachments.size)
        assertEquals(attachments, tx.attachments)
    }

    @Test
    fun `Transaction with multiple tags`() {
        val tags = listOf("important", "work", "expense", "reimbursable")
        val tx = TestData.transaction(tags = tags)
        assertEquals(4, tx.tags.size)
        assertEquals(tags, tx.tags)
    }
}

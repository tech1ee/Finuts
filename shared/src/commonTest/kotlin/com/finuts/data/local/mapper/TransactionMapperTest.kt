package com.finuts.data.local.mapper

import com.finuts.domain.entity.TransactionType
import com.finuts.test.TestData
import com.finuts.test.TestEntityData
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for TransactionMapper extension functions.
 * Includes JSON serialization/deserialization tests for attachments and tags.
 */
class TransactionMapperTest {

    @Test
    fun `toDomain maps entity to domain correctly`() {
        val entity = TestEntityData.transactionEntity(
            id = "tx-123",
            accountId = "acc-1",
            categoryId = "cat-1",
            amount = 5000L,
            type = "EXPENSE",
            description = "Coffee",
            merchant = "Starbucks",
            note = "Morning coffee",
            date = 1704067200000L,
            isRecurring = false,
            recurringRuleId = null,
            attachments = null,
            tags = null,
            createdAt = 1704067200000L,
            updatedAt = 1704067200000L
        )

        val domain = entity.toDomain()

        assertEquals("tx-123", domain.id)
        assertEquals("acc-1", domain.accountId)
        assertEquals("cat-1", domain.categoryId)
        assertEquals(5000L, domain.amount)
        assertEquals(TransactionType.EXPENSE, domain.type)
        assertEquals("Coffee", domain.description)
        assertEquals("Starbucks", domain.merchant)
        assertEquals("Morning coffee", domain.note)
        assertEquals(Instant.fromEpochMilliseconds(1704067200000L), domain.date)
        assertEquals(false, domain.isRecurring)
        assertEquals(null, domain.recurringRuleId)
        assertTrue(domain.attachments.isEmpty())
        assertTrue(domain.tags.isEmpty())
    }

    @Test
    fun `toDomain maps all transaction types correctly`() {
        TransactionType.entries.forEach { txType ->
            val entity = TestEntityData.transactionEntity(type = txType.name)
            val domain = entity.toDomain()
            assertEquals(txType, domain.type)
        }
    }

    @Test
    fun `toDomain handles null categoryId`() {
        val entity = TestEntityData.transactionEntity(categoryId = null)
        val domain = entity.toDomain()
        assertEquals(null, domain.categoryId)
    }

    @Test
    fun `toDomain handles null description merchant and note`() {
        val entity = TestEntityData.transactionEntity(
            description = null,
            merchant = null,
            note = null
        )
        val domain = entity.toDomain()
        assertEquals(null, domain.description)
        assertEquals(null, domain.merchant)
        assertEquals(null, domain.note)
    }

    @Test
    fun `toDomain handles recurring transaction`() {
        val entity = TestEntityData.transactionEntity(
            isRecurring = true,
            recurringRuleId = "rule-123"
        )
        val domain = entity.toDomain()
        assertEquals(true, domain.isRecurring)
        assertEquals("rule-123", domain.recurringRuleId)
    }

    @Test
    fun `toDomain deserializes attachments JSON correctly`() {
        val attachmentsJson = """["file1.jpg","file2.pdf","receipt.png"]"""
        val entity = TestEntityData.transactionEntity(attachments = attachmentsJson)
        val domain = entity.toDomain()

        assertEquals(3, domain.attachments.size)
        assertEquals("file1.jpg", domain.attachments[0])
        assertEquals("file2.pdf", domain.attachments[1])
        assertEquals("receipt.png", domain.attachments[2])
    }

    @Test
    fun `toDomain deserializes tags JSON correctly`() {
        val tagsJson = """["food","coffee","work"]"""
        val entity = TestEntityData.transactionEntity(tags = tagsJson)
        val domain = entity.toDomain()

        assertEquals(3, domain.tags.size)
        assertEquals("food", domain.tags[0])
        assertEquals("coffee", domain.tags[1])
        assertEquals("work", domain.tags[2])
    }

    @Test
    fun `toDomain handles null attachments and tags`() {
        val entity = TestEntityData.transactionEntity(attachments = null, tags = null)
        val domain = entity.toDomain()
        assertTrue(domain.attachments.isEmpty())
        assertTrue(domain.tags.isEmpty())
    }

    @Test
    fun `toDomain handles empty JSON arrays`() {
        val entity = TestEntityData.transactionEntity(attachments = "[]", tags = "[]")
        val domain = entity.toDomain()
        assertTrue(domain.attachments.isEmpty())
        assertTrue(domain.tags.isEmpty())
    }

    @Test
    fun `toEntity maps domain to entity correctly`() {
        val domain = TestData.transaction(
            id = "tx-456",
            accountId = "acc-2",
            categoryId = "cat-2",
            amount = 15000L,
            type = TransactionType.INCOME,
            description = "Salary",
            merchant = "Company Inc",
            note = "Monthly payment",
            date = Instant.parse("2024-01-15T10:00:00Z"),
            isRecurring = true,
            recurringRuleId = "monthly-salary",
            attachments = emptyList(),
            tags = emptyList(),
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2024-01-01T00:00:00Z")
        )

        val entity = domain.toEntity()

        assertEquals("tx-456", entity.id)
        assertEquals("acc-2", entity.accountId)
        assertEquals("cat-2", entity.categoryId)
        assertEquals(15000L, entity.amount)
        assertEquals("INCOME", entity.type)
        assertEquals("Salary", entity.description)
        assertEquals("Company Inc", entity.merchant)
        assertEquals("Monthly payment", entity.note)
        assertEquals(true, entity.isRecurring)
        assertEquals("monthly-salary", entity.recurringRuleId)
    }

    @Test
    fun `toEntity serializes attachments to JSON`() {
        val domain = TestData.transaction(
            attachments = listOf("doc1.pdf", "doc2.jpg")
        )
        val entity = domain.toEntity()
        assertEquals("""["doc1.pdf","doc2.jpg"]""", entity.attachments)
    }

    @Test
    fun `toEntity serializes tags to JSON`() {
        val domain = TestData.transaction(
            tags = listOf("important", "review")
        )
        val entity = domain.toEntity()
        assertEquals("""["important","review"]""", entity.tags)
    }

    @Test
    fun `toEntity sets null for empty attachments`() {
        val domain = TestData.transaction(attachments = emptyList())
        val entity = domain.toEntity()
        assertEquals(null, entity.attachments)
    }

    @Test
    fun `toEntity sets null for empty tags`() {
        val domain = TestData.transaction(tags = emptyList())
        val entity = domain.toEntity()
        assertEquals(null, entity.tags)
    }

    @Test
    fun `roundtrip entity to domain to entity preserves data`() {
        val attachmentsJson = """["file.jpg"]"""
        val tagsJson = """["tag1","tag2"]"""
        val originalEntity = TestEntityData.transactionEntity(
            id = "roundtrip-1",
            accountId = "acc-rt",
            categoryId = "cat-rt",
            amount = 99999L,
            type = "TRANSFER",
            description = "Transfer to savings",
            merchant = null,
            note = "Monthly transfer",
            date = 1704067200000L,
            isRecurring = true,
            recurringRuleId = "transfer-rule",
            attachments = attachmentsJson,
            tags = tagsJson,
            createdAt = 1704067200000L,
            updatedAt = 1704153600000L
        )

        val domain = originalEntity.toDomain()
        val resultEntity = domain.toEntity()

        assertEquals(originalEntity.id, resultEntity.id)
        assertEquals(originalEntity.accountId, resultEntity.accountId)
        assertEquals(originalEntity.categoryId, resultEntity.categoryId)
        assertEquals(originalEntity.amount, resultEntity.amount)
        assertEquals(originalEntity.type, resultEntity.type)
        assertEquals(originalEntity.description, resultEntity.description)
        assertEquals(originalEntity.merchant, resultEntity.merchant)
        assertEquals(originalEntity.note, resultEntity.note)
        assertEquals(originalEntity.date, resultEntity.date)
        assertEquals(originalEntity.isRecurring, resultEntity.isRecurring)
        assertEquals(originalEntity.recurringRuleId, resultEntity.recurringRuleId)
        assertEquals(originalEntity.createdAt, resultEntity.createdAt)
        assertEquals(originalEntity.updatedAt, resultEntity.updatedAt)
        // JSON may have different whitespace but same content
        assertEquals(originalEntity.attachments, resultEntity.attachments)
        assertEquals(originalEntity.tags, resultEntity.tags)
    }

    @Test
    fun `roundtrip domain to entity to domain preserves data`() {
        val originalDomain = TestData.transaction(
            id = "roundtrip-2",
            accountId = "acc-rt2",
            categoryId = "cat-rt2",
            amount = 123456L,
            type = TransactionType.EXPENSE,
            description = "Groceries",
            merchant = "Magnum",
            note = "Weekly groceries",
            attachments = listOf("receipt1.jpg", "receipt2.jpg"),
            tags = listOf("food", "essentials")
        )

        val entity = originalDomain.toEntity()
        val resultDomain = entity.toDomain()

        assertEquals(originalDomain.id, resultDomain.id)
        assertEquals(originalDomain.accountId, resultDomain.accountId)
        assertEquals(originalDomain.categoryId, resultDomain.categoryId)
        assertEquals(originalDomain.amount, resultDomain.amount)
        assertEquals(originalDomain.type, resultDomain.type)
        assertEquals(originalDomain.description, resultDomain.description)
        assertEquals(originalDomain.merchant, resultDomain.merchant)
        assertEquals(originalDomain.note, resultDomain.note)
        assertEquals(originalDomain.isRecurring, resultDomain.isRecurring)
        assertEquals(originalDomain.recurringRuleId, resultDomain.recurringRuleId)
        assertEquals(originalDomain.attachments, resultDomain.attachments)
        assertEquals(originalDomain.tags, resultDomain.tags)
        assertEquals(originalDomain.createdAt, resultDomain.createdAt)
        assertEquals(originalDomain.updatedAt, resultDomain.updatedAt)
    }

    @Test
    fun `toDomain handles zero amount`() {
        val entity = TestEntityData.transactionEntity(amount = 0L)
        val domain = entity.toDomain()
        assertEquals(0L, domain.amount)
    }

    @Test
    fun `toDomain handles negative amount for expenses`() {
        val entity = TestEntityData.transactionEntity(amount = -50000L)
        val domain = entity.toDomain()
        assertEquals(-50000L, domain.amount)
    }
}

package com.finuts.domain.entity.import

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ImportedTransaction entity and ImportSource enum.
 */
class ImportedTransactionTest {

    @Test
    fun `ImportedTransaction can be created with all parameters`() {
        val date = LocalDate(2024, 1, 15)
        val rawData = mapOf("original_date" to "15.01.2024", "original_amount" to "1 000,00")

        val tx = ImportedTransaction(
            date = date,
            amount = 100000L,
            description = "Grocery Store",
            merchant = "Magnum",
            balance = 50000000L,
            category = "Food",
            confidence = 0.95f,
            source = ImportSource.DOCUMENT_AI,
            rawData = rawData
        )

        assertEquals(date, tx.date)
        assertEquals(100000L, tx.amount)
        assertEquals("Grocery Store", tx.description)
        assertEquals("Magnum", tx.merchant)
        assertEquals(50000000L, tx.balance)
        assertEquals("Food", tx.category)
        assertEquals(0.95f, tx.confidence)
        assertEquals(ImportSource.DOCUMENT_AI, tx.source)
        assertEquals(rawData, tx.rawData)
    }

    @Test
    fun `ImportedTransaction allows null merchant`() {
        val tx = createTestTransaction(merchant = null)

        assertNull(tx.merchant)
    }

    @Test
    fun `ImportedTransaction allows null balance`() {
        val tx = createTestTransaction(balance = null)

        assertNull(tx.balance)
    }

    @Test
    fun `ImportedTransaction allows null category`() {
        val tx = createTestTransaction(category = null)

        assertNull(tx.category)
    }

    @Test
    fun `ImportedTransaction with zero confidence`() {
        val tx = createTestTransaction(confidence = 0.0f)

        assertEquals(0.0f, tx.confidence)
    }

    @Test
    fun `ImportedTransaction with full confidence`() {
        val tx = createTestTransaction(confidence = 1.0f)

        assertEquals(1.0f, tx.confidence)
    }

    @Test
    fun `ImportedTransaction with negative amount for expense`() {
        val tx = createTestTransaction(amount = -50000L)

        assertEquals(-50000L, tx.amount)
    }

    @Test
    fun `ImportedTransaction with positive amount for income`() {
        val tx = createTestTransaction(amount = 150000L)

        assertEquals(150000L, tx.amount)
    }

    @Test
    fun `ImportedTransaction with empty rawData`() {
        val tx = createTestTransaction(rawData = emptyMap())

        assertTrue(tx.rawData.isEmpty())
    }

    @Test
    fun `ImportedTransaction with complex rawData`() {
        val rawData = mapOf(
            "line_number" to "5",
            "original_date" to "15/01/2024",
            "original_amount" to "-500.00",
            "bank_reference" to "TXN123456"
        )
        val tx = createTestTransaction(rawData = rawData)

        assertEquals(4, tx.rawData.size)
        assertEquals("TXN123456", tx.rawData["bank_reference"])
    }

    @Test
    fun `ImportedTransaction copy works correctly`() {
        val original = createTestTransaction(description = "Original", amount = 1000L)
        val modified = original.copy(description = "Modified", amount = 2000L)

        assertEquals("Original", original.description)
        assertEquals(1000L, original.amount)
        assertEquals("Modified", modified.description)
        assertEquals(2000L, modified.amount)
        assertEquals(original.date, modified.date)
    }

    @Test
    fun `ImportSource has all expected values`() {
        val sources = ImportSource.entries.map { it.name }

        assertTrue("RULE_BASED" in sources)
        assertTrue("DOCUMENT_AI" in sources)
        assertTrue("LLM_ENHANCED" in sources)
        assertTrue("USER_CORRECTED" in sources)
        assertTrue("NATIVE_AI" in sources)
        assertEquals(5, ImportSource.entries.size)
    }

    @Test
    fun `all ImportSource values can be used in ImportedTransaction`() {
        ImportSource.entries.forEach { source ->
            val tx = createTestTransaction(source = source)
            assertEquals(source, tx.source)
        }
    }

    @Test
    fun `ImportedTransaction supports different date formats`() {
        val dates = listOf(
            LocalDate(2024, 1, 1),
            LocalDate(2024, 12, 31),
            LocalDate(2023, 6, 15)
        )

        dates.forEach { date ->
            val tx = createTestTransaction(date = date)
            assertEquals(date, tx.date)
        }
    }

    @Test
    fun `ImportedTransaction with various sources`() {
        val ruleBased = createTestTransaction(source = ImportSource.RULE_BASED)
        val documentAI = createTestTransaction(source = ImportSource.DOCUMENT_AI)
        val llmEnhanced = createTestTransaction(source = ImportSource.LLM_ENHANCED)
        val userCorrected = createTestTransaction(source = ImportSource.USER_CORRECTED)
        val nativeAI = createTestTransaction(source = ImportSource.NATIVE_AI)

        assertEquals(ImportSource.RULE_BASED, ruleBased.source)
        assertEquals(ImportSource.DOCUMENT_AI, documentAI.source)
        assertEquals(ImportSource.LLM_ENHANCED, llmEnhanced.source)
        assertEquals(ImportSource.USER_CORRECTED, userCorrected.source)
        assertEquals(ImportSource.NATIVE_AI, nativeAI.source)
    }

    // Helper function to create test transactions
    private fun createTestTransaction(
        date: LocalDate = LocalDate(2024, 1, 15),
        amount: Long = 10000L,
        description: String = "Test Transaction",
        merchant: String? = "Test Merchant",
        balance: Long? = 50000L,
        category: String? = "Test Category",
        confidence: Float = 0.9f,
        source: ImportSource = ImportSource.RULE_BASED,
        rawData: Map<String, String> = mapOf("test" to "data")
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
}

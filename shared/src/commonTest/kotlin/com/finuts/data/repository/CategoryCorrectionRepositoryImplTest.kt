package com.finuts.data.repository

import app.cash.turbine.test
import com.finuts.data.local.entity.CategoryCorrectionEntity
import com.finuts.domain.entity.CategoryCorrection
import com.finuts.test.fakes.dao.FakeCategoryCorrectionDao
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryCorrectionRepositoryImplTest {

    private lateinit var fakeDao: FakeCategoryCorrectionDao
    private lateinit var repository: CategoryCorrectionRepositoryImpl

    @BeforeTest
    fun setup() {
        fakeDao = FakeCategoryCorrectionDao()
        repository = CategoryCorrectionRepositoryImpl(fakeDao)
    }

    // === save Tests ===

    @Test
    fun `save inserts correction into dao`() = runTest {
        val correction = createDomainCorrection("1", "tx-1", "groceries")

        repository.save(correction)

        val stored = fakeDao.getAll().find { it.id == "1" }
        assertEquals("groceries", stored?.correctedCategoryId)
    }

    // === getByTransactionId Tests ===

    @Test
    fun `getByTransactionId returns null when not found`() = runTest {
        val result = repository.getByTransactionId("non-existent")
        assertNull(result)
    }

    @Test
    fun `getByTransactionId returns correction when found`() = runTest {
        fakeDao.setCorrections(listOf(
            createCorrectionEntity("1", "tx-123", "groceries")
        ))

        val result = repository.getByTransactionId("tx-123")

        assertEquals("tx-123", result?.transactionId)
        assertEquals("groceries", result?.correctedCategoryId)
    }

    // === getByNormalizedMerchant Tests ===

    @Test
    fun `getByNormalizedMerchant returns empty list when no matches`() = runTest {
        fakeDao.setCorrections(listOf(
            createCorrectionEntity("1", "tx-1", "groceries", merchantNormalized = "MAGNUM")
        ))

        val result = repository.getByNormalizedMerchant("GLOVO")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getByNormalizedMerchant returns all corrections for merchant`() = runTest {
        fakeDao.setCorrections(listOf(
            createCorrectionEntity("1", "tx-1", "groceries", merchantNormalized = "MAGNUM"),
            createCorrectionEntity("2", "tx-2", "shopping", merchantNormalized = "MAGNUM"),
            createCorrectionEntity("3", "tx-3", "food", merchantNormalized = "GLOVO")
        ))

        val result = repository.getByNormalizedMerchant("MAGNUM")

        assertEquals(2, result.size)
        assertTrue(result.all { it.merchantNormalized == "MAGNUM" })
    }

    // === getByMerchantAndCategory Tests ===

    @Test
    fun `getByMerchantAndCategory filters by both merchant and category`() = runTest {
        fakeDao.setCorrections(listOf(
            createCorrectionEntity("1", "tx-1", "groceries", merchantNormalized = "MAGNUM"),
            createCorrectionEntity("2", "tx-2", "shopping", merchantNormalized = "MAGNUM"),
            createCorrectionEntity("3", "tx-3", "groceries", merchantNormalized = "SMALL")
        ))

        val result = repository.getByMerchantAndCategory("MAGNUM", "groceries")

        assertEquals(1, result.size)
        assertEquals("groceries", result[0].correctedCategoryId)
        assertEquals("MAGNUM", result[0].merchantNormalized)
    }

    @Test
    fun `getByMerchantAndCategory returns empty when no match`() = runTest {
        fakeDao.setCorrections(listOf(
            createCorrectionEntity("1", "tx-1", "groceries", merchantNormalized = "MAGNUM")
        ))

        val result = repository.getByMerchantAndCategory("MAGNUM", "transport")

        assertTrue(result.isEmpty())
    }

    // === getAllCorrections Tests ===

    @Test
    fun `getAllCorrections returns empty list when no corrections`() = runTest {
        repository.getAllCorrections().test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllCorrections returns all corrections mapped to domain`() = runTest {
        fakeDao.setCorrections(listOf(
            createCorrectionEntity("1", "tx-1", "groceries"),
            createCorrectionEntity("2", "tx-2", "transport"),
            createCorrectionEntity("3", "tx-3", "shopping")
        ))

        repository.getAllCorrections().test {
            val corrections = awaitItem()
            assertEquals(3, corrections.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === countByMerchant Tests ===

    @Test
    fun `countByMerchant returns zero when no corrections for merchant`() = runTest {
        val count = repository.countByMerchant("UNKNOWN")
        assertEquals(0, count)
    }

    @Test
    fun `countByMerchant returns correct count`() = runTest {
        fakeDao.setCorrections(listOf(
            createCorrectionEntity("1", "tx-1", "groceries", merchantNormalized = "MAGNUM"),
            createCorrectionEntity("2", "tx-2", "groceries", merchantNormalized = "MAGNUM"),
            createCorrectionEntity("3", "tx-3", "groceries", merchantNormalized = "GLOVO")
        ))

        val magnumCount = repository.countByMerchant("MAGNUM")
        val glovoCount = repository.countByMerchant("GLOVO")

        assertEquals(2, magnumCount)
        assertEquals(1, glovoCount)
    }

    // === deleteById Tests ===

    @Test
    fun `deleteById removes correction from dao`() = runTest {
        fakeDao.setCorrections(listOf(
            createCorrectionEntity("1", "tx-1", "groceries")
        ))

        repository.deleteById("1")

        assertTrue(fakeDao.getAll().isEmpty())
    }

    @Test
    fun `deleteById only removes specified correction`() = runTest {
        fakeDao.setCorrections(listOf(
            createCorrectionEntity("1", "tx-1", "groceries"),
            createCorrectionEntity("2", "tx-2", "transport")
        ))

        repository.deleteById("1")

        assertEquals(1, fakeDao.getAll().size)
        assertEquals("2", fakeDao.getAll()[0].id)
    }

    // === Helper Functions ===

    private fun createCorrectionEntity(
        id: String,
        transactionId: String,
        correctedCategoryId: String,
        merchantNormalized: String = "MERCHANT"
    ) = CategoryCorrectionEntity(
        id = id,
        transactionId = transactionId,
        originalCategoryId = "other",
        correctedCategoryId = correctedCategoryId,
        merchantName = "Merchant Name",
        merchantNormalized = merchantNormalized,
        createdAt = 1704067200000L
    )

    private fun createDomainCorrection(
        id: String,
        transactionId: String,
        correctedCategoryId: String
    ) = CategoryCorrection(
        id = id,
        transactionId = transactionId,
        originalCategoryId = "other",
        correctedCategoryId = correctedCategoryId,
        merchantName = "Merchant Name",
        merchantNormalized = "MERCHANT",
        createdAt = Instant.fromEpochMilliseconds(1704067200000L)
    )
}

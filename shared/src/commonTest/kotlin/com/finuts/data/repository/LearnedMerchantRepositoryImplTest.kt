package com.finuts.data.repository

import app.cash.turbine.test
import com.finuts.data.categorization.MerchantNormalizerInterface
import com.finuts.data.local.entity.LearnedMerchantEntity
import com.finuts.domain.entity.LearnedMerchant
import com.finuts.domain.entity.LearnedMerchantSource
import com.finuts.test.fakes.dao.FakeLearnedMerchantDao
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearnedMerchantRepositoryImplTest {

    private lateinit var fakeDao: FakeLearnedMerchantDao
    private lateinit var fakeNormalizer: FakeMerchantNormalizer
    private lateinit var repository: LearnedMerchantRepositoryImpl

    @BeforeTest
    fun setup() {
        fakeDao = FakeLearnedMerchantDao()
        fakeNormalizer = FakeMerchantNormalizer()
        repository = LearnedMerchantRepositoryImpl(fakeDao, fakeNormalizer)
    }

    // === save Tests ===

    @Test
    fun `save inserts merchant into dao`() = runTest {
        val merchant = createDomainMerchant("1", "MAGNUM")

        repository.save(merchant)

        val stored = fakeDao.getAll().find { it.id == "1" }
        assertEquals("MAGNUM", stored?.merchantPattern)
    }

    // === update Tests ===

    @Test
    fun `update modifies existing merchant`() = runTest {
        fakeDao.setMerchants(listOf(createMerchantEntity("1", "OLD", confidence = 0.8f)))
        val updated = createDomainMerchant("1", "NEW", confidence = 0.95f)

        repository.update(updated)

        val stored = fakeDao.getAll().find { it.id == "1" }
        assertEquals("NEW", stored?.merchantPattern)
        assertEquals(0.95f, stored?.confidence)
    }

    // === getByPattern Tests ===

    @Test
    fun `getByPattern returns null when not found`() = runTest {
        val result = repository.getByPattern("UNKNOWN")
        assertNull(result)
    }

    @Test
    fun `getByPattern returns merchant when found`() = runTest {
        fakeDao.setMerchants(listOf(createMerchantEntity("1", "MAGNUM")))

        val result = repository.getByPattern("MAGNUM")

        assertEquals("MAGNUM", result?.merchantPattern)
    }

    // === findMatch Tests ===

    @Test
    fun `findMatch returns null for blank normalized description`() = runTest {
        fakeNormalizer.setNormalizedResult("")

        val result = repository.findMatch("   ")

        assertNull(result)
    }

    @Test
    fun `findMatch returns null when no matching merchant`() = runTest {
        fakeNormalizer.setNormalizedResult("UNKNOWN")

        val result = repository.findMatch("Unknown Store")

        assertNull(result)
    }

    @Test
    fun `findMatch returns matching merchant`() = runTest {
        fakeDao.setMerchants(listOf(createMerchantEntity("1", "MAGNUM", confidence = 0.9f)))
        fakeNormalizer.setNormalizedResult("MAGNUM ALMATY")

        val result = repository.findMatch("Magnum Almaty Store")

        assertEquals("MAGNUM", result?.merchantPattern)
        assertEquals(0.9f, result?.confidence)
    }

    @Test
    fun `findMatch returns highest confidence match`() = runTest {
        fakeDao.setMerchants(listOf(
            createMerchantEntity("1", "MAGNUM", confidence = 0.7f),
            createMerchantEntity("2", "MAGNUM", confidence = 0.95f)
        ))
        fakeNormalizer.setNormalizedResult("MAGNUM ALMATY")

        val result = repository.findMatch("Magnum Almaty")

        assertEquals(0.95f, result?.confidence)
    }

    // === getAllMerchants Tests ===

    @Test
    fun `getAllMerchants returns empty list when no merchants`() = runTest {
        repository.getAllMerchants().test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllMerchants returns all merchants mapped to domain`() = runTest {
        fakeDao.setMerchants(listOf(
            createMerchantEntity("1", "MAGNUM"),
            createMerchantEntity("2", "GLOVO")
        ))

        repository.getAllMerchants().test {
            val merchants = awaitItem()
            assertEquals(2, merchants.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === getHighConfidenceMerchants Tests ===

    @Test
    fun `getHighConfidenceMerchants filters by minimum confidence`() = runTest {
        fakeDao.setMerchants(listOf(
            createMerchantEntity("1", "HIGH", confidence = 0.95f),
            createMerchantEntity("2", "LOW", confidence = 0.5f),
            createMerchantEntity("3", "MEDIUM", confidence = 0.8f)
        ))

        val result = repository.getHighConfidenceMerchants(0.8f)

        assertEquals(2, result.size)
        assertTrue(result.all { it.confidence >= 0.8f })
    }

    // === getBySource Tests ===

    @Test
    fun `getBySource filters by source type`() = runTest {
        fakeDao.setMerchants(listOf(
            createMerchantEntity("1", "USER1", source = "USER"),
            createMerchantEntity("2", "ML1", source = "ML"),
            createMerchantEntity("3", "USER2", source = "USER")
        ))

        val userMerchants = repository.getBySource(LearnedMerchantSource.USER)
        val mlMerchants = repository.getBySource(LearnedMerchantSource.ML)

        assertEquals(2, userMerchants.size)
        assertEquals(1, mlMerchants.size)
        assertTrue(userMerchants.all { it.source == LearnedMerchantSource.USER })
    }

    // === deleteById Tests ===

    @Test
    fun `deleteById removes merchant from dao`() = runTest {
        fakeDao.setMerchants(listOf(createMerchantEntity("1", "ToDelete")))

        repository.deleteById("1")

        assertTrue(fakeDao.getAll().isEmpty())
    }

    // === Helper Functions ===

    private fun createMerchantEntity(
        id: String,
        pattern: String,
        confidence: Float = 0.9f,
        source: String = "USER"
    ) = LearnedMerchantEntity(
        id = id,
        merchantPattern = pattern,
        categoryId = "groceries",
        confidence = confidence,
        source = source,
        sampleCount = 1,
        lastUsedAt = 1704067200000L,
        createdAt = 1704067200000L
    )

    private fun createDomainMerchant(
        id: String,
        pattern: String,
        confidence: Float = 0.9f
    ) = LearnedMerchant(
        id = id,
        merchantPattern = pattern,
        categoryId = "groceries",
        confidence = confidence,
        source = LearnedMerchantSource.USER,
        sampleCount = 1,
        lastUsedAt = Instant.fromEpochMilliseconds(1704067200000L),
        createdAt = Instant.fromEpochMilliseconds(1704067200000L)
    )
}

/**
 * Fake implementation of MerchantNormalizerInterface for testing.
 */
private class FakeMerchantNormalizer : MerchantNormalizerInterface {
    private var normalizedResult: String = ""

    fun setNormalizedResult(result: String) {
        normalizedResult = result
    }

    override fun normalize(merchantName: String): String {
        return if (normalizedResult.isNotEmpty()) normalizedResult else merchantName.uppercase().trim()
    }

    override fun extractKeywords(merchantName: String): List<String> {
        return merchantName.split(" ").filter { it.isNotBlank() }
    }

    override fun isSimilar(merchant1: String, merchant2: String): Boolean {
        return merchant1.uppercase() == merchant2.uppercase()
    }

    override fun toPattern(normalizedName: String): String {
        return normalizedName.split(" ").firstOrNull() ?: normalizedName
    }
}

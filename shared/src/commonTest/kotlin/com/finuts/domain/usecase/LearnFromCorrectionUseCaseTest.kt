package com.finuts.domain.usecase

import com.finuts.domain.entity.CategoryCorrection
import com.finuts.domain.entity.LearnedMerchant
import com.finuts.domain.entity.LearnedMerchantSource
import com.finuts.domain.repository.CategoryCorrectionRepository
import com.finuts.domain.repository.LearnedMerchantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for LearnFromCorrectionUseCase.
 * Verifies the learning pipeline from user corrections.
 */
class LearnFromCorrectionUseCaseTest {

    private val fixedTime = Instant.fromEpochMilliseconds(1704067200000) // 2024-01-01

    // --- Test Doubles ---

    private class FakeCorrectionRepository : CategoryCorrectionRepository {
        val savedCorrections = mutableListOf<CategoryCorrection>()
        var correctionsByMerchant = mutableMapOf<String, MutableList<CategoryCorrection>>()

        override suspend fun save(correction: CategoryCorrection) {
            savedCorrections.add(correction)
            val key = "${correction.merchantNormalized}-${correction.correctedCategoryId}"
            correctionsByMerchant.getOrPut(key) { mutableListOf() }.add(correction)
        }

        override suspend fun getByTransactionId(transactionId: String): CategoryCorrection? {
            return savedCorrections.find { it.transactionId == transactionId }
        }

        override suspend fun getByNormalizedMerchant(
            merchantNormalized: String
        ): List<CategoryCorrection> {
            return savedCorrections.filter { it.merchantNormalized == merchantNormalized }
        }

        override suspend fun getByMerchantAndCategory(
            merchantNormalized: String,
            categoryId: String
        ): List<CategoryCorrection> {
            val key = "$merchantNormalized-$categoryId"
            return correctionsByMerchant[key] ?: emptyList()
        }

        override fun getAllCorrections(): Flow<List<CategoryCorrection>> = flowOf(savedCorrections)

        override suspend fun countByMerchant(merchantNormalized: String): Int {
            return savedCorrections.count { it.merchantNormalized == merchantNormalized }
        }

        override suspend fun deleteById(id: String) {
            savedCorrections.removeAll { it.id == id }
        }
    }

    private class FakeMerchantRepository : LearnedMerchantRepository {
        val savedMerchants = mutableMapOf<String, LearnedMerchant>()

        override suspend fun save(merchant: LearnedMerchant) {
            savedMerchants[merchant.merchantPattern] = merchant
        }

        override suspend fun update(merchant: LearnedMerchant) {
            savedMerchants[merchant.merchantPattern] = merchant
        }

        override suspend fun getByPattern(pattern: String): LearnedMerchant? {
            return savedMerchants[pattern]
        }

        override suspend fun findMatch(description: String): LearnedMerchant? {
            return savedMerchants.values.find { description.contains(it.merchantPattern) }
        }

        override fun getAllMerchants(): Flow<List<LearnedMerchant>> =
            flowOf(savedMerchants.values.toList())

        override suspend fun getHighConfidenceMerchants(
            minConfidence: Float
        ): List<LearnedMerchant> {
            return savedMerchants.values.filter { it.confidence >= minConfidence }
        }

        override suspend fun getBySource(source: LearnedMerchantSource): List<LearnedMerchant> {
            return savedMerchants.values.filter { it.source == source }
        }

        override suspend fun deleteById(id: String) {
            savedMerchants.values.removeAll { it.id == id }
        }
    }

    // --- Tests ---

    @Test
    fun `saves correction on first correction`() = runTest {
        val correctionRepo = FakeCorrectionRepository()
        val merchantRepo = FakeMerchantRepository()
        val useCase = LearnFromCorrectionUseCase(correctionRepo, merchantRepo) { fixedTime }

        val result = useCase.execute(
            transactionId = "tx-1",
            originalCategoryId = "other",
            correctedCategoryId = "groceries",
            merchantName = "MAGNUM SUPER"
        )

        assertTrue(result.isSuccess)
        assertEquals(1, correctionRepo.savedCorrections.size)
        assertTrue(result.getOrNull() is LearnFromCorrectionUseCase.LearnResult.CorrectionSaved)
    }

    @Test
    fun `creates mapping after threshold corrections`() = runTest {
        val correctionRepo = FakeCorrectionRepository()
        val merchantRepo = FakeMerchantRepository()
        val useCase = LearnFromCorrectionUseCase(correctionRepo, merchantRepo) { fixedTime }

        // First correction - both merchants normalize to "MAGNUM SUPER"
        useCase.execute(
            transactionId = "tx-1",
            originalCategoryId = "other",
            correctedCategoryId = "groceries",
            merchantName = "MAGNUM SUPER #123"
        )

        // Second correction (reaches threshold) - same normalized merchant
        val result = useCase.execute(
            transactionId = "tx-2",
            originalCategoryId = "other",
            correctedCategoryId = "groceries",
            merchantName = "MAGNUM SUPER *4567"
        )

        assertTrue(result.isSuccess)
        val learnResult = result.getOrNull()
        assertTrue(learnResult is LearnFromCorrectionUseCase.LearnResult.MappingCreated)
        assertEquals(1, merchantRepo.savedMerchants.size)
    }

    @Test
    fun `updates existing mapping with new sample`() = runTest {
        val correctionRepo = FakeCorrectionRepository()
        val merchantRepo = FakeMerchantRepository()
        val useCase = LearnFromCorrectionUseCase(correctionRepo, merchantRepo) { fixedTime }

        // Pre-create a mapping (pattern is now "MAGNUM SUPER" not regex)
        val existingMapping = LearnedMerchant(
            id = "lm-1",
            merchantPattern = "MAGNUM SUPER",
            categoryId = "groceries",
            confidence = 0.90f,
            source = LearnedMerchantSource.USER,
            sampleCount = 2,
            lastUsedAt = fixedTime,
            createdAt = fixedTime
        )
        merchantRepo.save(existingMapping)

        // New correction for same merchant
        val result = useCase.execute(
            transactionId = "tx-3",
            originalCategoryId = "other",
            correctedCategoryId = "groceries",
            merchantName = "MAGNUM SUPER"
        )

        assertTrue(result.isSuccess)
        val learnResult = result.getOrNull()
        assertTrue(learnResult is LearnFromCorrectionUseCase.LearnResult.MappingUpdated)

        val updated = merchantRepo.savedMerchants["MAGNUM SUPER"]!!
        assertEquals(3, updated.sampleCount)
        assertTrue(updated.confidence > 0.90f)
    }

    @Test
    fun `fails with blank merchant name`() = runTest {
        val correctionRepo = FakeCorrectionRepository()
        val merchantRepo = FakeMerchantRepository()
        val useCase = LearnFromCorrectionUseCase(correctionRepo, merchantRepo) { fixedTime }

        val result = useCase.execute(
            transactionId = "tx-1",
            originalCategoryId = "other",
            correctedCategoryId = "groceries",
            merchantName = ""
        )

        assertTrue(result.isFailure)
        assertTrue(correctionRepo.savedCorrections.isEmpty())
    }

    @Test
    fun `fails with null merchant name`() = runTest {
        val correctionRepo = FakeCorrectionRepository()
        val merchantRepo = FakeMerchantRepository()
        val useCase = LearnFromCorrectionUseCase(correctionRepo, merchantRepo) { fixedTime }

        val result = useCase.execute(
            transactionId = "tx-1",
            originalCategoryId = "other",
            correctedCategoryId = "groceries",
            merchantName = null
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `normalizes merchant name before saving`() = runTest {
        val correctionRepo = FakeCorrectionRepository()
        val merchantRepo = FakeMerchantRepository()
        val useCase = LearnFromCorrectionUseCase(correctionRepo, merchantRepo) { fixedTime }

        useCase.execute(
            transactionId = "tx-1",
            originalCategoryId = null,
            correctedCategoryId = "groceries",
            merchantName = "magnum super almaty"
        )

        val correction = correctionRepo.savedCorrections.first()
        assertEquals("MAGNUM SUPER", correction.merchantNormalized)
    }

    @Test
    fun `confidence increases with more samples`() = runTest {
        val correctionRepo = FakeCorrectionRepository()
        val merchantRepo = FakeMerchantRepository()
        val useCase = LearnFromCorrectionUseCase(correctionRepo, merchantRepo) { fixedTime }

        val existingMapping = LearnedMerchant(
            id = "lm-1",
            merchantPattern = "MAGNUM SUPER",
            categoryId = "groceries",
            confidence = 0.90f,
            source = LearnedMerchantSource.USER,
            sampleCount = 2,
            lastUsedAt = fixedTime,
            createdAt = fixedTime
        )
        merchantRepo.save(existingMapping)

        // Add 3 more corrections
        repeat(3) { i ->
            useCase.execute(
                transactionId = "tx-$i",
                originalCategoryId = null,
                correctedCategoryId = "groceries",
                merchantName = "MAGNUM SUPER"
            )
        }

        val finalMapping = merchantRepo.savedMerchants["MAGNUM SUPER"]!!
        assertTrue(finalMapping.confidence > existingMapping.confidence)
        assertEquals(5, finalMapping.sampleCount)
    }

    @Test
    fun `confidence capped at maximum`() = runTest {
        val correctionRepo = FakeCorrectionRepository()
        val merchantRepo = FakeMerchantRepository()
        val useCase = LearnFromCorrectionUseCase(correctionRepo, merchantRepo) { fixedTime }

        val existingMapping = LearnedMerchant(
            id = "lm-1",
            merchantPattern = "MAGNUM SUPER",
            categoryId = "groceries",
            confidence = 0.97f,
            source = LearnedMerchantSource.USER,
            sampleCount = 10,
            lastUsedAt = fixedTime,
            createdAt = fixedTime
        )
        merchantRepo.save(existingMapping)

        // Add many more corrections
        repeat(10) { i ->
            useCase.execute(
                transactionId = "tx-$i",
                originalCategoryId = null,
                correctedCategoryId = "groceries",
                merchantName = "MAGNUM SUPER"
            )
        }

        val finalMapping = merchantRepo.savedMerchants["MAGNUM SUPER"]!!
        assertTrue(finalMapping.confidence <= 0.98f)
    }

    @Test
    fun `handles different categories for same merchant`() = runTest {
        val correctionRepo = FakeCorrectionRepository()
        val merchantRepo = FakeMerchantRepository()
        val useCase = LearnFromCorrectionUseCase(correctionRepo, merchantRepo) { fixedTime }

        // First correction - groceries
        useCase.execute(
            transactionId = "tx-1",
            originalCategoryId = null,
            correctedCategoryId = "groceries",
            merchantName = "AMAZON"
        )

        // Second correction - different category
        useCase.execute(
            transactionId = "tx-2",
            originalCategoryId = null,
            correctedCategoryId = "shopping",
            merchantName = "AMAZON"
        )

        // Should have 2 corrections saved
        assertEquals(2, correctionRepo.savedCorrections.size)
        // But no mapping yet (threshold not reached for either category)
        assertTrue(merchantRepo.savedMerchants.isEmpty())
    }
}

package com.finuts.domain.usecase

import com.finuts.data.categorization.MerchantNormalizerInterface
import com.finuts.domain.entity.CategoryCorrection
import com.finuts.domain.entity.LearnedMerchant
import com.finuts.domain.entity.LearnedMerchantSource
import com.finuts.domain.repository.CategoryCorrectionRepository
import com.finuts.domain.repository.LearnedMerchantRepository
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Use case for learning from user category corrections.
 *
 * When a user corrects a transaction's category:
 * 1. Saves the correction for audit trail
 * 2. Checks if enough similar corrections exist (threshold: 2)
 * 3. Creates or updates a learned merchant mapping for Tier 0
 *
 * This enables automatic categorization of future transactions
 * from the same merchant based on user preferences.
 */
class LearnFromCorrectionUseCase(
    private val correctionRepository: CategoryCorrectionRepository,
    private val merchantRepository: LearnedMerchantRepository,
    private val merchantNormalizer: MerchantNormalizerInterface,
    private val clock: () -> Instant = {
        Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
    }
) {
    companion object {
        /**
         * Minimum corrections before creating a learned mapping.
         *
         * Set to 1 based on industry research (Copilot Money, Monarch Money):
         * - When user explicitly corrects a category, that's a clear signal
         * - Single correction should immediately create a learned mapping
         * - This matches user expectations ("I told the app once, it should remember")
         */
        private const val MIN_CORRECTIONS_THRESHOLD = 1

        /** Initial confidence for user-learned mappings */
        private const val INITIAL_CONFIDENCE = 0.90f

        /** Max confidence for user-learned mappings */
        private const val MAX_CONFIDENCE = 0.98f

        /** Confidence boost per additional correction */
        private const val CONFIDENCE_BOOST_PER_SAMPLE = 0.02f
    }

    /**
     * Result of the learning operation.
     */
    sealed class LearnResult {
        /** Correction saved but not enough samples for learning */
        data class CorrectionSaved(val correctionId: String) : LearnResult()

        /** New merchant mapping created */
        data class MappingCreated(
            val correctionId: String,
            val merchantPattern: String,
            val categoryId: String
        ) : LearnResult()

        /** Existing mapping updated with new sample */
        data class MappingUpdated(
            val correctionId: String,
            val merchantPattern: String,
            val categoryId: String,
            val newConfidence: Float
        ) : LearnResult()
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun execute(
        transactionId: String,
        originalCategoryId: String?,
        correctedCategoryId: String,
        merchantName: String?
    ): Result<LearnResult> {
        if (merchantName.isNullOrBlank()) {
            return Result.failure(
                IllegalArgumentException("Merchant name required for learning")
            )
        }

        val now = clock()
        val normalizedMerchant = merchantNormalizer.normalize(merchantName)

        if (normalizedMerchant.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Cannot normalize merchant name")
            )
        }

        return try {
            // 1. Save the correction
            val correctionId = Uuid.random().toString()
            val correction = CategoryCorrection(
                id = correctionId,
                transactionId = transactionId,
                originalCategoryId = originalCategoryId,
                correctedCategoryId = correctedCategoryId,
                merchantName = merchantName,
                merchantNormalized = normalizedMerchant,
                createdAt = now
            )
            correctionRepository.save(correction)

            // 2. Check existing corrections for this merchant
            val similarCorrections = correctionRepository
                .getByMerchantAndCategory(normalizedMerchant, correctedCategoryId)

            // 3. Check/update learned merchant mapping
            val result = processLearning(
                correctionId = correctionId,
                normalizedMerchant = normalizedMerchant,
                categoryId = correctedCategoryId,
                correctionCount = similarCorrections.size,
                now = now
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun processLearning(
        correctionId: String,
        normalizedMerchant: String,
        categoryId: String,
        correctionCount: Int,
        now: Instant
    ): LearnResult {
        val merchantPattern = merchantNormalizer.toPattern(normalizedMerchant)
        val existingMapping = merchantRepository.getByPattern(merchantPattern)

        return when {
            // Not enough corrections yet
            correctionCount < MIN_CORRECTIONS_THRESHOLD && existingMapping == null -> {
                LearnResult.CorrectionSaved(correctionId)
            }

            // Update existing mapping
            existingMapping != null -> {
                val newSampleCount = existingMapping.sampleCount + 1
                val newConfidence = calculateConfidence(newSampleCount)

                val updatedMapping = existingMapping.copy(
                    categoryId = categoryId,
                    confidence = newConfidence,
                    sampleCount = newSampleCount,
                    lastUsedAt = now
                )
                merchantRepository.update(updatedMapping)

                LearnResult.MappingUpdated(
                    correctionId = correctionId,
                    merchantPattern = merchantPattern,
                    categoryId = categoryId,
                    newConfidence = newConfidence
                )
            }

            // Create new mapping (threshold reached)
            else -> {
                val newMapping = LearnedMerchant(
                    id = Uuid.random().toString(),
                    merchantPattern = merchantPattern,
                    categoryId = categoryId,
                    confidence = INITIAL_CONFIDENCE,
                    source = LearnedMerchantSource.USER,
                    sampleCount = correctionCount,
                    lastUsedAt = now,
                    createdAt = now
                )
                merchantRepository.save(newMapping)

                LearnResult.MappingCreated(
                    correctionId = correctionId,
                    merchantPattern = merchantPattern,
                    categoryId = categoryId
                )
            }
        }
    }

    private fun calculateConfidence(sampleCount: Int): Float {
        val boost = (sampleCount - 1) * CONFIDENCE_BOOST_PER_SAMPLE
        return (INITIAL_CONFIDENCE + boost).coerceAtMost(MAX_CONFIDENCE)
    }
}

package com.finuts.data.categorization

import co.touchlab.kermit.Logger
import com.finuts.domain.entity.CategorizationResult
import com.finuts.domain.entity.CategorizationSource
import com.finuts.domain.entity.LearnedMerchant
import com.finuts.domain.repository.LearnedMerchantRepository
import kotlinx.datetime.Instant

/**
 * Combined transaction categorizer with Tier 0 (user learned) and Tier 1 (rules).
 *
 * Categorization order (highest priority first):
 * 1. Tier 0: User learned merchant mappings (from corrections)
 * 2. Tier 1: Merchant database patterns
 * 3. Tier 1: User history (deprecated, for backward compatibility)
 * 4. Tier 1: Regex rule patterns
 *
 * If no match found, returns null (falls through to Tier 2 LLM).
 */
class TransactionCategorizer(
    private val learnedMerchantRepository: LearnedMerchantRepository,
    private val ruleBasedCategorizer: RuleBasedCategorizer
) {
    private val log = Logger.withTag("TransactionCategorizer")
    companion object {
        private const val USER_LEARNED_BASE_CONFIDENCE = 0.95f
    }

    /**
     * Categorize a transaction using Tier 0 + Tier 1 cascade.
     *
     * @param transactionId Transaction ID for the result
     * @param description Transaction description/merchant name
     * @return CategorizationResult if matched, null to fall through to LLM
     */
    suspend fun categorize(
        transactionId: String,
        description: String
    ): CategorizationResult? {
        val trimmed = description.trim()
        log.d { "categorize: txId=$transactionId, desc='${trimmed.take(50)}'" }

        if (trimmed.isBlank()) {
            log.d { "categorize: SKIP - empty description" }
            return null
        }

        // Tier 0: Check user learned mappings first (highest priority)
        checkUserLearnedMapping(transactionId, trimmed)?.let { result ->
            log.i {
                "categorize: TIER_0_MATCH txId=$transactionId, " +
                    "category=${result.categoryId}, conf=${result.confidence}"
            }
            return result
        }

        // Tier 1: Fall back to rule-based categorizer
        val tier1Result = ruleBasedCategorizer.categorize(transactionId, trimmed)
        if (tier1Result != null) {
            log.i {
                "categorize: TIER_1_MATCH txId=$transactionId, " +
                    "category=${tier1Result.categoryId}, " +
                    "source=${tier1Result.source}, conf=${tier1Result.confidence}"
            }
        } else {
            log.d { "categorize: NO_MATCH txId=$transactionId → fallthrough to LLM" }
        }
        return tier1Result
    }

    /**
     * Check if we have a learned mapping for this merchant.
     * Updates lastUsedAt on match for statistics.
     */
    private suspend fun checkUserLearnedMapping(
        transactionId: String,
        description: String
    ): CategorizationResult? {
        log.d { "checkUserLearnedMapping: searching for '$description'" }

        val learned = learnedMerchantRepository.findMatch(description)
        if (learned == null) {
            log.d { "checkUserLearnedMapping: no learned mapping found" }
            return null
        }

        log.d {
            "checkUserLearnedMapping: FOUND merchant=${learned.merchantPattern}, " +
                "category=${learned.categoryId}, samples=${learned.sampleCount}"
        }

        // Update last used timestamp (fire and forget for perf)
        updateLastUsed(learned)

        return CategorizationResult(
            transactionId = transactionId,
            categoryId = learned.categoryId,
            confidence = learned.confidence.coerceAtLeast(USER_LEARNED_BASE_CONFIDENCE),
            source = CategorizationSource.USER_LEARNED
        )
    }

    private suspend fun updateLastUsed(learned: LearnedMerchant) {
        try {
            val now = Instant.fromEpochMilliseconds(
                kotlin.time.Clock.System.now().toEpochMilliseconds()
            )
            val updated = learned.copy(lastUsedAt = now)
            learnedMerchantRepository.update(updated)
            log.d { "updateLastUsed: updated ${learned.merchantPattern}" }
        } catch (e: Exception) {
            log.w { "updateLastUsed: failed for ${learned.merchantPattern} - ${e.message}" }
        }
    }

    /**
     * Get categorization statistics.
     */
    suspend fun getStats(): CategorizationStats {
        val allLearned = learnedMerchantRepository
            .getHighConfidenceMerchants(0.0f)

        return CategorizationStats(
            totalLearnedMappings = allLearned.size,
            highConfidenceMappings = allLearned.count { it.confidence >= 0.90f },
            totalSamples = allLearned.sumOf { it.sampleCount }
        )
    }
}

/**
 * Statistics about the categorization system.
 */
data class CategorizationStats(
    val totalLearnedMappings: Int,
    val highConfidenceMappings: Int,
    val totalSamples: Int
)

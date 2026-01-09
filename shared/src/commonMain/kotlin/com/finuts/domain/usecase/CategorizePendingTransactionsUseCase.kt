package com.finuts.domain.usecase

import com.finuts.data.categorization.AICategorizer
import com.finuts.data.categorization.RuleBasedCategorizer
import com.finuts.data.categorization.TransactionForCategorization
import com.finuts.domain.entity.CategorizationResult
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType

/**
 * Use case for categorizing pending transactions using 3-tier approach.
 *
 * Tier 1: Rule-based (MerchantDatabase + regex patterns) - FREE
 * Tier 2: LLM batch (GPT-4o-mini) - LOW COST
 * Tier 3: Premium LLM (GPT-4o) - HIGHER COST
 */
class CategorizePendingTransactionsUseCase(
    private val ruleBasedCategorizer: RuleBasedCategorizer,
    private val aiCategorizer: AICategorizer?
) {
    companion object {
        private val DEFAULT_CATEGORIES = listOf(
            "groceries", "food_delivery", "restaurants", "transport",
            "utilities", "entertainment", "shopping", "healthcare",
            "education", "travel", "transfer", "salary", "other"
        )
    }

    /**
     * Filter transactions that need categorization.
     */
    fun filterPendingCategorization(transactions: List<Transaction>): List<Transaction> {
        return transactions.filter { tx ->
            tx.categoryId == null && tx.type != TransactionType.TRANSFER
        }
    }

    /**
     * Categorize transactions using Tier 1 only (rule-based, free).
     */
    fun categorizeTier1(transactions: List<Transaction>): List<CategorizationResult> {
        return transactions.mapNotNull { tx ->
            val description = tx.description ?: tx.merchant ?: ""
            ruleBasedCategorizer.categorize(tx.id, description)
        }
    }

    /**
     * Categorize transactions using all tiers.
     * Returns pair of (categorized results, remaining uncategorized transaction IDs).
     */
    suspend fun categorizeAll(
        transactions: List<Transaction>,
        categories: List<String> = DEFAULT_CATEGORIES
    ): CategorizationBatchResult {
        val pending = filterPendingCategorization(transactions)
        if (pending.isEmpty()) {
            return CategorizationBatchResult(emptyList(), emptyList())
        }

        val results = mutableListOf<CategorizationResult>()
        var remaining = pending

        // Tier 1: Rule-based
        val tier1Results = categorizeTier1(remaining)
        results.addAll(tier1Results)

        val tier1Ids = tier1Results.map { it.transactionId }.toSet()
        remaining = remaining.filter { it.id !in tier1Ids }

        // Tier 2: LLM batch (if available and needed)
        if (remaining.isNotEmpty() && aiCategorizer != null) {
            val tier2Input = remaining.map { tx ->
                TransactionForCategorization(
                    id = tx.id,
                    description = tx.description ?: tx.merchant ?: "",
                    amount = tx.amount
                )
            }

            val tier2Results = aiCategorizer.categorizeTier2(tier2Input, categories)
            val highConfTier2 = tier2Results.filter { it.confidence >= 0.70f }
            results.addAll(highConfTier2)

            val tier2Ids = highConfTier2.map { it.transactionId }.toSet()
            remaining = remaining.filter { it.id !in tier2Ids }
        }

        // Tier 3: Premium LLM (if available and still needed)
        if (remaining.isNotEmpty() && aiCategorizer != null) {
            val tier3Input = remaining.map { tx ->
                TransactionForCategorization(
                    id = tx.id,
                    description = tx.description ?: tx.merchant ?: "",
                    amount = tx.amount
                )
            }

            val tier3Results = aiCategorizer.categorizeTier3(tier3Input, categories)
            results.addAll(tier3Results)

            val tier3Ids = tier3Results.map { it.transactionId }.toSet()
            remaining = remaining.filter { it.id !in tier3Ids }
        }

        val uncategorizedIds = remaining.map { it.id }
        return CategorizationBatchResult(results, uncategorizedIds)
    }
}

/**
 * Result of batch categorization.
 */
data class CategorizationBatchResult(
    val results: List<CategorizationResult>,
    val uncategorizedTransactionIds: List<String>
) {
    val totalCategorized: Int get() = results.size
    val totalUncategorized: Int get() = uncategorizedTransactionIds.size

    val localCount: Int get() = results.count { it.isLocalSource }
    val tier2Count: Int get() = results.count {
        it.source == com.finuts.domain.entity.CategorizationSource.LLM_TIER2
    }
    val tier3Count: Int get() = results.count {
        it.source == com.finuts.domain.entity.CategorizationSource.LLM_TIER3
    }

    val highConfidenceCount: Int get() = results.count { it.isHighConfidence }
    val needsConfirmationCount: Int get() = results.count { it.requiresUserConfirmation }
}

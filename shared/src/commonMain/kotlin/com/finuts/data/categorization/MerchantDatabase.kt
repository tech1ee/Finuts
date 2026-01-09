package com.finuts.data.categorization

import com.finuts.data.categorization.patterns.entertainmentPatterns
import com.finuts.data.categorization.patterns.foodDeliveryPatterns
import com.finuts.data.categorization.patterns.groceryPatterns
import com.finuts.data.categorization.patterns.healthcarePatterns
import com.finuts.data.categorization.patterns.shoppingPatterns
import com.finuts.data.categorization.patterns.transferPatterns
import com.finuts.data.categorization.patterns.transportPatterns
import com.finuts.data.categorization.patterns.utilitiesPatterns
import com.finuts.domain.entity.CategorizationResult
import com.finuts.domain.entity.CategorizationSource

/**
 * Database of merchant patterns for Tier 1 categorization.
 * Contains 100+ patterns for Kazakhstan merchants and popular services.
 */
class MerchantDatabase {

    private val patterns: List<MerchantPattern> by lazy {
        groceryPatterns +
            foodDeliveryPatterns +
            transportPatterns +
            utilitiesPatterns +
            entertainmentPatterns +
            shoppingPatterns +
            healthcarePatterns +
            transferPatterns
    }

    /**
     * Find matching merchant pattern for the given transaction description.
     * @param description Transaction description/merchant name
     * @return CategorizationResult if match found, null otherwise
     */
    fun findMatch(description: String): CategorizationResult? {
        val trimmed = description.trim()
        if (trimmed.isBlank()) return null

        for (pattern in patterns) {
            if (pattern.pattern.containsMatchIn(trimmed)) {
                return CategorizationResult(
                    transactionId = "", // Caller sets this
                    categoryId = pattern.categoryId,
                    confidence = pattern.confidence,
                    source = CategorizationSource.MERCHANT_DATABASE
                )
            }
        }
        return null
    }

    /**
     * Find match and set the transaction ID.
     */
    fun findMatchForTransaction(
        transactionId: String,
        description: String
    ): CategorizationResult? {
        return findMatch(description)?.copy(transactionId = transactionId)
    }

    /**
     * Get all patterns in the database.
     */
    fun getAllPatterns(): List<MerchantPattern> = patterns

    /**
     * Get pattern count by category.
     */
    fun getPatternCountByCategory(): Map<String, Int> {
        return patterns.groupBy { it.categoryId }
            .mapValues { it.value.size }
    }
}

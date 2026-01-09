package com.finuts.data.categorization

import com.finuts.domain.entity.CategorizationResult
import com.finuts.domain.entity.CategorizationSource

/**
 * Tier 1 categorizer using rules, patterns, and merchant database.
 * This is the first and cheapest categorization layer (free).
 */
class RuleBasedCategorizer(
    private val merchantDatabase: MerchantDatabase,
    private val userHistory: Map<String, String> = emptyMap()
) {
    companion object {
        private const val USER_HISTORY_CONFIDENCE = 0.92f
        private const val RULE_BASED_CONFIDENCE = 0.88f
    }

    private val rulePatterns = listOf(
        // ATM and cash
        RulePattern("ATM|БАНКОМАТ", "transfer", RULE_BASED_CONFIDENCE),
        RulePattern("CASH.*WITHDRAW|СНЯТИЕ.*НАЛИЧ", "transfer", RULE_BASED_CONFIDENCE),

        // Salary and income
        RulePattern("ЗАРПЛАТА|SALARY|ЗАРАБОТН", "salary", 0.95f),
        RulePattern("ПЕНСИЯ|PENSION", "salary", 0.95f),
        RulePattern("СТИПЕНДИ|SCHOLARSHIP", "salary", 0.90f),
        RulePattern("ДИВИДЕНД|DIVIDEND", "salary", 0.90f),

        // Interest and refunds
        RulePattern("ПРОЦЕНТ|INTEREST", "other", 0.85f),
        RulePattern("ВОЗВРАТ|REFUND", "other", 0.85f),
        RulePattern("КЭШБЭК|CASHBACK", "other", 0.90f)
    )

    /**
     * Categorize a transaction using Tier 1 rules.
     * Priority: Merchant Database > User History > Regex Rules
     *
     * @param transactionId The ID of the transaction
     * @param description Transaction description/merchant name
     * @return CategorizationResult if matched, null otherwise
     */
    fun categorize(transactionId: String, description: String): CategorizationResult? {
        val trimmed = description.trim()
        if (trimmed.isBlank()) return null

        // 1. Check merchant database first (highest accuracy)
        merchantDatabase.findMatchForTransaction(transactionId, trimmed)?.let {
            return it
        }

        // 2. Check user history
        findInUserHistory(transactionId, trimmed)?.let {
            return it
        }

        // 3. Apply regex rules
        return applyRulePatterns(transactionId, trimmed)
    }

    private fun findInUserHistory(
        transactionId: String,
        description: String
    ): CategorizationResult? {
        val upperDesc = description.uppercase()
        for ((pattern, categoryId) in userHistory) {
            if (upperDesc.contains(pattern.uppercase())) {
                return CategorizationResult(
                    transactionId = transactionId,
                    categoryId = categoryId,
                    confidence = USER_HISTORY_CONFIDENCE,
                    source = CategorizationSource.USER_HISTORY
                )
            }
        }
        return null
    }

    private fun applyRulePatterns(
        transactionId: String,
        description: String
    ): CategorizationResult? {
        for (rule in rulePatterns) {
            if (rule.pattern.containsMatchIn(description)) {
                return CategorizationResult(
                    transactionId = transactionId,
                    categoryId = rule.categoryId,
                    confidence = rule.confidence,
                    source = CategorizationSource.RULE_BASED
                )
            }
        }
        return null
    }

    private data class RulePattern(
        val pattern: Regex,
        val categoryId: String,
        val confidence: Float
    ) {
        constructor(
            patternString: String,
            categoryId: String,
            confidence: Float
        ) : this(
            pattern = Regex(patternString, RegexOption.IGNORE_CASE),
            categoryId = categoryId,
            confidence = confidence
        )
    }
}

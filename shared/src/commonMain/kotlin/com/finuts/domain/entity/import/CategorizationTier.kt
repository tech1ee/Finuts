package com.finuts.domain.entity.import

import kotlinx.serialization.Serializable

/**
 * Tier of the AI categorization pipeline.
 * Used to track which tier is currently processing transactions.
 */
@Serializable
enum class CategorizationTier {
    /**
     * Tier 1: Rule-based categorization using merchant database and patterns.
     * Fastest, handles ~80% of transactions.
     */
    RULE_BASED,

    /**
     * Tier 2: LLM-based categorization using GPT-4o-mini or Claude Haiku.
     * Handles complex cases, ~15% of transactions.
     */
    LLM_FAST,

    /**
     * Tier 3: Advanced LLM categorization using GPT-4o or Claude Sonnet.
     * Handles edge cases, ~5% of transactions.
     */
    LLM_ADVANCED
}

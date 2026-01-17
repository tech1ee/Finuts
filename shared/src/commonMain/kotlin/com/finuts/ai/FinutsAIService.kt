package com.finuts.ai

import com.finuts.ai.model.AIResult
import com.finuts.ai.model.Anomaly
import com.finuts.ai.model.BudgetSuggestion
import com.finuts.ai.model.ChatResponse
import com.finuts.ai.model.DocumentParseResult
import com.finuts.ai.model.Period
import com.finuts.ai.model.PredictionType
import com.finuts.ai.model.Predictions
import com.finuts.ai.model.RecurringPattern
import com.finuts.ai.model.SpendingInsights
import com.finuts.domain.entity.CategorizationResult
import com.finuts.domain.entity.CategoryCorrection
import com.finuts.domain.entity.Transaction

/**
 * Unified AI Service - single entry point for all AI features.
 *
 * This interface provides a consistent API for:
 * - Transaction categorization (Tier 0-3 cascade)
 * - Document parsing (PDF/image to transactions)
 * - Financial insights and analytics
 * - AI chat advisor (future)
 * - Predictions and recommendations
 *
 * All methods handle privacy (PII anonymization) and cost optimization internally.
 */
interface FinutsAIService {

    // === CATEGORIZATION ===

    /**
     * Categorize a single transaction using the tiered cascade.
     * Tries local methods first (Tier 0-1), then cloud AI if needed.
     */
    suspend fun categorize(transaction: Transaction): AIResult<CategorizationResult>

    /**
     * Categorize multiple transactions in batch.
     * More efficient for bulk operations.
     */
    suspend fun categorizeBatch(
        transactions: List<Transaction>
    ): AIResult<List<CategorizationResult>>

    /**
     * Learn from user correction to improve future categorization.
     */
    suspend fun learnFromCorrection(correction: CategoryCorrection): AIResult<Unit>

    // === DOCUMENT PARSING ===

    /**
     * Parse a document (PDF, image) into transactions.
     * Uses OCR + LLM with PII anonymization.
     */
    suspend fun parseDocument(
        bytes: ByteArray,
        filename: String
    ): AIResult<DocumentParseResult>

    /**
     * Parse an image directly (screenshot, photo of receipt).
     */
    suspend fun parseImage(bytes: ByteArray): AIResult<DocumentParseResult>

    // === INSIGHTS & ANALYTICS ===

    /**
     * Generate spending insights for a time period.
     */
    suspend fun getSpendingInsights(period: Period): AIResult<SpendingInsights>

    /**
     * Detect anomalies in recent transactions.
     */
    suspend fun getAnomalies(period: Period): AIResult<List<Anomaly>>

    /**
     * Get predictions (spending forecast, recurring payments).
     */
    suspend fun getPredictions(type: PredictionType): AIResult<Predictions>

    // === CHAT ADVISOR (Future) ===

    /**
     * Send a message to the AI financial advisor.
     * @param message User's question or request
     * @param conversationId Optional ID to continue a conversation
     */
    suspend fun chat(
        message: String,
        conversationId: String? = null
    ): AIResult<ChatResponse>

    // === SMART FEATURES ===

    /**
     * Get AI suggestion for budget amount in a category.
     */
    suspend fun suggestBudget(categoryId: String): AIResult<BudgetSuggestion>

    /**
     * Detect recurring payment patterns.
     */
    suspend fun detectRecurring(): AIResult<List<RecurringPattern>>
}

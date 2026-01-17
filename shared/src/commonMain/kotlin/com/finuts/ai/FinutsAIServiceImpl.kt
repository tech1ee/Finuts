package com.finuts.ai

import co.touchlab.kermit.Logger
import com.finuts.ai.context.UserContextManager
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
import com.finuts.ai.orchestration.AIOrchestrator
import com.finuts.ai.orchestration.AITask
import com.finuts.ai.providers.ProviderPreference
import com.finuts.data.categorization.TransactionCategorizer
import com.finuts.domain.entity.CategorizationResult
import com.finuts.domain.entity.CategorizationSource
import com.finuts.domain.entity.CategoryCorrection
import com.finuts.domain.entity.Transaction
import com.finuts.domain.usecase.LearnFromCorrectionUseCase

/**
 * Implementation of FinutsAIService.
 *
 * Coordinates between:
 * - TransactionCategorizer (existing tiered categorization)
 * - AIOrchestrator (cloud LLM execution)
 * - UserContextManager (personalization)
 */
class FinutsAIServiceImpl(
    private val orchestrator: AIOrchestrator,
    private val categorizer: TransactionCategorizer,
    private val contextManager: UserContextManager,
    private val learnFromCorrection: LearnFromCorrectionUseCase
) : FinutsAIService {
    private val log = Logger.withTag("FinutsAIService")

    // === CATEGORIZATION ===

    override suspend fun categorize(transaction: Transaction): AIResult<CategorizationResult> {
        log.d { "categorize: txId=${transaction.id}, merchant=${transaction.merchant}" }
        return try {
            val description = transaction.description ?: transaction.merchant ?: ""
            if (description.isBlank()) {
                log.w { "categorize: SKIP - no description for txId=${transaction.id}" }
                return AIResult.Error("No description or merchant for categorization")
            }
            val result = categorizer.categorize(transaction.id, description)
            if (result != null) {
                log.i {
                    "categorize: SUCCESS txId=${transaction.id}, " +
                        "category=${result.categoryId}, source=${result.source}"
                }
                AIResult.Success(result)
            } else {
                log.d { "categorize: NO_MATCH txId=${transaction.id}" }
                AIResult.Error("No categorization match found")
            }
        } catch (e: Exception) {
            log.e(e) { "categorize: FAILED txId=${transaction.id} - ${e.message}" }
            AIResult.Error("Categorization failed: ${e.message}", e)
        }
    }

    override suspend fun categorizeBatch(
        transactions: List<Transaction>
    ): AIResult<List<CategorizationResult>> {
        log.d { "categorizeBatch: ${transactions.size} transactions" }
        return try {
            val results = transactions.mapNotNull { tx ->
                val description = tx.description ?: tx.merchant ?: ""
                if (description.isNotBlank()) {
                    categorizer.categorize(tx.id, description)
                } else null
            }
            log.i { "categorizeBatch: ${results.size}/${transactions.size} categorized" }
            AIResult.Success(results)
        } catch (e: Exception) {
            log.e(e) { "categorizeBatch: FAILED - ${e.message}" }
            AIResult.Error("Batch categorization failed: ${e.message}", e)
        }
    }

    override suspend fun learnFromCorrection(correction: CategoryCorrection): AIResult<Unit> {
        log.d {
            "learnFromCorrection: txId=${correction.transactionId}, " +
                "from=${correction.originalCategoryId} to=${correction.correctedCategoryId}"
        }
        return try {
            learnFromCorrection.execute(
                transactionId = correction.transactionId,
                originalCategoryId = correction.originalCategoryId,
                correctedCategoryId = correction.correctedCategoryId,
                merchantName = correction.merchantName
            )
            log.i { "learnFromCorrection: SUCCESS merchant=${correction.merchantName}" }
            AIResult.Success(Unit)
        } catch (e: Exception) {
            log.e(e) { "learnFromCorrection: FAILED - ${e.message}" }
            AIResult.Error("Learning failed: ${e.message}", e)
        }
    }

    // === DOCUMENT PARSING ===

    override suspend fun parseDocument(
        bytes: ByteArray,
        filename: String
    ): AIResult<DocumentParseResult> {
        // TODO: Integrate with PdfParser and LlmDocumentParser from Phase 7.2
        return AIResult.Error("Document parsing not yet implemented")
    }

    override suspend fun parseImage(bytes: ByteArray): AIResult<DocumentParseResult> {
        // TODO: Integrate with OCR service
        return AIResult.Error("Image parsing not yet implemented")
    }

    // === INSIGHTS & ANALYTICS ===

    override suspend fun getSpendingInsights(period: Period): AIResult<SpendingInsights> {
        log.d { "getSpendingInsights: ${period.start} to ${period.end}" }
        return try {
            val context = contextManager.getContextForPrompt()
            log.d { "getSpendingInsights: contextLen=${context.length}" }

            val task = AITask(
                prompt = buildInsightsPrompt(period, context),
                preference = ProviderPreference.BEST_QUALITY,
                maxTokens = 2048,
                systemPrompt = INSIGHTS_SYSTEM_PROMPT
            )

            val response = orchestrator.execute(task)
            when (response) {
                is AIResult.Success -> {
                    log.i { "getSpendingInsights: SUCCESS tokens=${response.tokensUsed}" }
                    val insights = parseInsightsResponse(response.data.content, period)
                    AIResult.Success(insights, response.tokensUsed)
                }
                is AIResult.Error -> {
                    log.w { "getSpendingInsights: ERROR - ${response.message}" }
                    response
                }
                is AIResult.CostLimitExceeded -> {
                    log.w { "getSpendingInsights: COST_LIMIT" }
                    response
                }
                is AIResult.ProviderUnavailable -> {
                    log.w { "getSpendingInsights: PROVIDER_UNAVAILABLE" }
                    response
                }
            }
        } catch (e: Exception) {
            log.e(e) { "getSpendingInsights: FAILED - ${e.message}" }
            AIResult.Error("Failed to generate insights: ${e.message}", e)
        }
    }

    override suspend fun getAnomalies(period: Period): AIResult<List<Anomaly>> {
        // TODO: Implement anomaly detection
        return AIResult.Success(emptyList())
    }

    override suspend fun getPredictions(type: PredictionType): AIResult<Predictions> {
        // TODO: Implement predictions
        return AIResult.Error("Predictions not yet implemented")
    }

    // === CHAT ADVISOR ===

    override suspend fun chat(
        message: String,
        conversationId: String?
    ): AIResult<ChatResponse> {
        log.d { "chat: messageLen=${message.length}, conversationId=$conversationId" }
        return try {
            val context = contextManager.getContextForPrompt()

            val task = AITask(
                prompt = message,
                preference = ProviderPreference.BEST_QUALITY,
                maxTokens = 1024,
                systemPrompt = buildChatSystemPrompt(context)
            )

            val response = orchestrator.execute(task)
            when (response) {
                is AIResult.Success -> {
                    log.i { "chat: SUCCESS tokens=${response.tokensUsed}, responseLen=${response.data.content.length}" }
                    AIResult.Success(
                        ChatResponse(
                            message = response.data.content,
                            suggestions = emptyList(),
                            actions = emptyList()
                        ),
                        response.tokensUsed
                    )
                }
                is AIResult.Error -> {
                    log.w { "chat: ERROR - ${response.message}" }
                    response
                }
                is AIResult.CostLimitExceeded -> {
                    log.w { "chat: COST_LIMIT" }
                    AIResult.Error("Daily budget exceeded")
                }
                is AIResult.ProviderUnavailable -> {
                    log.w { "chat: PROVIDER_UNAVAILABLE" }
                    AIResult.Error("AI service unavailable")
                }
            }
        } catch (e: Exception) {
            log.e(e) { "chat: FAILED - ${e.message}" }
            AIResult.Error("Chat failed: ${e.message}", e)
        }
    }

    // === SMART FEATURES ===

    override suspend fun suggestBudget(categoryId: String): AIResult<BudgetSuggestion> {
        // TODO: Implement budget suggestions
        return AIResult.Error("Budget suggestions not yet implemented")
    }

    override suspend fun detectRecurring(): AIResult<List<RecurringPattern>> {
        // TODO: Implement recurring detection
        return AIResult.Success(emptyList())
    }

    // === PRIVATE HELPERS ===

    private fun buildInsightsPrompt(period: Period, context: String): String {
        return """
            |Analyze the user's spending for the period ${period.start} to ${period.end}.
            |
            |$context
            |
            |Provide insights in JSON format with fields:
            |{
            |  "totalSpent": number (in cents),
            |  "topCategories": [{"categoryId": "...", "amount": number, "percentage": number}],
            |  "comparisonToPrevious": number (percentage change),
            |  "insights": [{"type": "...", "title": "...", "description": "..."}],
            |  "recommendations": ["..."]
            |}
        """.trimMargin()
    }

    private fun buildChatSystemPrompt(context: String): String {
        return """
            |You are a helpful financial advisor for the Finuts app.
            |
            |$context
            |
            |Guidelines:
            |- Be concise and practical (max 2-3 paragraphs)
            |- Reference user's actual spending data when relevant
            |- Suggest actionable steps
            |- Never give specific investment advice
            |- Respond in the user's language (Russian/Kazakh/English)
        """.trimMargin()
    }

    private fun parseInsightsResponse(content: String, period: Period): SpendingInsights {
        // TODO: Parse JSON response
        return SpendingInsights(
            period = period,
            totalSpent = 0,
            topCategories = emptyList(),
            comparisonToPrevious = 0f,
            insights = emptyList(),
            recommendations = emptyList()
        )
    }

    companion object {
        private const val INSIGHTS_SYSTEM_PROMPT = """
            You are a financial analyst. Analyze spending patterns and provide insights.
            Always respond with valid JSON matching the requested schema.
            Be specific and data-driven in your analysis.
        """
    }
}

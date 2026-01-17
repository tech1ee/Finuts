package com.finuts.data.categorization

import co.touchlab.kermit.Logger
import com.finuts.ai.cost.AICostTracker
import com.finuts.ai.privacy.PIIAnonymizer
import com.finuts.ai.prompts.BatchCategoryItem
import com.finuts.ai.prompts.CategorizationPrompt
import com.finuts.ai.prompts.IndexedDescription
import com.finuts.ai.prompts.LLMCategoryResponse
import com.finuts.ai.providers.CompletionRequest
import com.finuts.ai.providers.LLMProvider
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategorizationResult
import com.finuts.domain.entity.CategorizationSource
import com.finuts.domain.entity.CategoryType
import com.finuts.domain.registry.IconRegistryProvider
import com.finuts.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * AI-powered categorizer for Tier 2 (cloud LLM).
 *
 * Called when Tier 0 (user learned) and Tier 1 (rules) fail.
 * Supports both OpenAI and Anthropic providers via LLMProvider interface.
 *
 * Privacy-first:
 * - All descriptions are anonymized before sending to LLM
 * - No PII ever leaves the device
 * - Cost is tracked and limited ($0.10/day, $2/month)
 */
open class AICategorizer(
    private val provider: LLMProvider?,
    private val categoryRepository: CategoryRepository,
    private val anonymizer: PIIAnonymizer,
    private val costTracker: AICostTracker,
    private val iconRegistry: IconRegistryProvider
) {
    private val log = Logger.withTag("AICategorizer")
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val MAX_BATCH_SIZE = 10
    }

    /**
     * Categorize a single transaction using LLM.
     *
     * @param transactionId Transaction ID
     * @param description Original (non-anonymized) transaction description
     * @return CategorizationResult or null if failed/unavailable
     */
    suspend fun categorizeTier2(
        transactionId: String,
        description: String
    ): CategorizationResult? {
        if (provider == null) {
            log.d { "categorizeTier2: No LLM provider available" }
            return null
        }

        // Check cost budget
        if (!costTracker.canExecute(estimateCost())) {
            log.w { "categorizeTier2: Cost budget exceeded" }
            return null
        }

        try {
            // Anonymize description before sending
            val anonymizedResult = anonymizer.anonymize(description)
            val anonymized = anonymizedResult.anonymizedText
            log.d { "categorizeTier2: anonymized='$anonymized'" }

            // Get existing categories
            val existingCategories = categoryRepository.getAllCategories()
                .first()
                .map { it.id }

            // Build prompt
            val prompt = CategorizationPrompt.buildCategorizePrompt(
                description = anonymized,
                existingCategories = existingCategories
            )

            // Call LLM
            val response = provider.complete(
                CompletionRequest(
                    prompt = prompt,
                    maxTokens = 256,
                    temperature = 0.1f
                )
            )

            // Track cost
            costTracker.record(
                inputTokens = response.inputTokens,
                outputTokens = response.outputTokens,
                model = response.model
            )

            // Parse response
            val result = parseResponse(response.content)
            if (result == null) {
                log.w { "categorizeTier2: Failed to parse LLM response" }
                return null
            }

            // If new category suggested, create it
            if (result.isNew && result.newCategoryMetadata != null) {
                createNewCategory(result.categoryId, result.newCategoryMetadata)
            }

            return CategorizationResult(
                transactionId = transactionId,
                categoryId = result.categoryId,
                confidence = result.confidence,
                source = CategorizationSource.LLM_TIER2
            )
        } catch (e: Exception) {
            log.e(e) { "categorizeTier2: failed - ${e.message}" }
            return null
        }
    }

    /**
     * Batch categorize multiple transactions (more efficient for import).
     *
     * @param transactions List of transactions to categorize
     * @param categories Available category IDs
     * @return List of CategorizationResults
     */
    open suspend fun categorizeTier2(
        transactions: List<TransactionForCategorization>,
        categories: List<String>
    ): List<CategorizationResult> {
        if (provider == null || transactions.isEmpty()) {
            return emptyList()
        }

        val batchCost = estimateCost() * transactions.size
        if (!costTracker.canExecute(batchCost)) {
            log.w { "categorizeBatch: Cost budget exceeded for ${transactions.size}" }
            return emptyList()
        }

        val batches = transactions.chunked(MAX_BATCH_SIZE)
        val results = mutableListOf<CategorizationResult>()

        for (batch in batches) {
            try {
                val indexed = batch.mapIndexed { index, tx ->
                    IndexedDescription(index, anonymizer.anonymize(tx.description).anonymizedText)
                }

                val prompt = CategorizationPrompt.buildBatchCategorizePrompt(
                    descriptions = indexed,
                    existingCategories = categories
                )

                val response = provider.complete(
                    CompletionRequest(
                        prompt = prompt,
                        maxTokens = 1024,
                        temperature = 0.1f
                    )
                )

                costTracker.record(
                    inputTokens = response.inputTokens,
                    outputTokens = response.outputTokens,
                    model = response.model
                )

                val batchResults = parseBatchResponse(response.content)
                batchResults.forEach { item ->
                    val tx = batch.getOrNull(item.index)
                    if (tx != null) {
                        results.add(
                            CategorizationResult(
                                transactionId = tx.id,
                                categoryId = item.categoryId,
                                confidence = item.confidence,
                                source = CategorizationSource.LLM_TIER2
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                log.e(e) { "categorizeBatch: Failed batch - ${e.message}" }
            }
        }

        return results
    }

    /**
     * Check if AI categorization is available.
     */
    suspend fun isAvailable(): Boolean {
        return provider?.isAvailable() == true && costTracker.canExecute(estimateCost())
    }

    private fun parseResponse(content: String): LLMCategoryResponse? {
        return try {
            val jsonContent = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            json.decodeFromString<LLMCategoryResponse>(jsonContent)
        } catch (e: Exception) {
            log.e(e) { "parseResponse: Failed to parse - $content" }
            null
        }
    }

    private fun parseBatchResponse(content: String): List<BatchCategoryItem> {
        return try {
            val jsonContent = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            json.decodeFromString<List<BatchCategoryItem>>(jsonContent)
        } catch (e: Exception) {
            log.e(e) { "parseBatchResponse: Failed to parse - $content" }
            emptyList()
        }
    }

    private suspend fun createNewCategory(
        categoryId: String,
        metadata: com.finuts.ai.prompts.NewCategoryMetadata
    ) {
        try {
            val existing = categoryRepository.getCategoryById(categoryId).first()
            if (existing != null) return

            val icon = iconRegistry.findBestMatch(metadata.iconHint)

            val category = Category(
                id = categoryId,
                name = metadata.name,
                icon = icon,
                color = metadata.color,
                type = CategoryType.EXPENSE,
                parentId = null,
                isDefault = false,
                sortOrder = 999
            )

            categoryRepository.createCategory(category)
            log.i { "createNewCategory: Created '$categoryId' with icon='$icon'" }
        } catch (e: Exception) {
            log.e(e) { "createNewCategory: Failed for '$categoryId'" }
        }
    }

    private fun estimateCost(): Float {
        val inputCost = 200 * 0.001f / 1000
        val outputCost = 50 * 0.005f / 1000
        return inputCost + outputCost
    }
}

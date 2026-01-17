package com.finuts.domain.usecase

import co.touchlab.kermit.Logger
import com.finuts.ai.providers.CompletionRequest
import com.finuts.ai.providers.LLMProvider
import com.finuts.data.categorization.AICategorizer
import com.finuts.data.categorization.RuleBasedCategorizer
import com.finuts.data.categorization.TransactionForCategorization
import com.finuts.domain.entity.CategorizationResult
import com.finuts.domain.entity.CategorizationSource
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Use case for categorizing pending transactions using multi-tier approach.
 *
 * Tier 0: User Learned (from corrections) - FREE, INSTANT
 * Tier 1: Rule-based (MerchantDatabase + regex) - FREE, INSTANT
 * Tier 1.5: On-Device LLM (GGUF model) - FREE, <100ms
 * Tier 2: Cloud LLM batch (GPT-4o-mini/Claude Haiku) - LOW COST
 * Tier 3: Premium LLM (GPT-4o/Claude Sonnet) - HIGHER COST
 */
class CategorizePendingTransactionsUseCase(
    private val ruleBasedCategorizer: RuleBasedCategorizer,
    private val aiCategorizer: AICategorizer?,
    private val onDeviceLLMProvider: LLMProvider? = null
) {
    private val log = Logger.withTag("CategorizePending")

    companion object {
        private val DEFAULT_CATEGORIES = listOf(
            "groceries", "food_delivery", "restaurants", "transport",
            "utilities", "entertainment", "shopping", "healthcare",
            "education", "travel", "transfer", "salary", "other"
        )

        /** Batch size for on-device LLM processing. 5 is optimal for SmolLM2-135M. */
        private const val BATCH_SIZE = 5

        /** System prompt for batch categorization */
        private const val BATCH_SYSTEM_PROMPT =
            "You are a financial transaction categorizer. Categorize all transactions and return ONLY valid JSON array."
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

        // Tier 1.5: On-Device LLM (if available and needed)
        val tier1_5Count: Int
        if (remaining.isNotEmpty() && onDeviceLLMProvider != null && onDeviceLLMProvider.isAvailable()) {
            log.i { "Tier1.5: Processing ${remaining.size} remaining transactions with on-device LLM" }

            val tier1_5Input = remaining.map { tx ->
                TransactionForCategorization(
                    id = tx.id,
                    description = tx.description ?: tx.merchant ?: "",
                    amount = tx.amount
                )
            }

            val tier1_5Results = categorizeWithOnDevice(tier1_5Input, categories)
            log.d { "Tier1.5: Raw results count=${tier1_5Results.size}" }

            val highConfTier1_5 = tier1_5Results.filter { it.confidence >= 0.70f }
            tier1_5Count = highConfTier1_5.size
            log.i { "Tier1.5: High confidence results=$tier1_5Count (threshold 0.70)" }

            results.addAll(highConfTier1_5)

            val tier1_5Ids = highConfTier1_5.map { it.transactionId }.toSet()
            remaining = remaining.filter { it.id !in tier1_5Ids }
        } else {
            tier1_5Count = 0
            if (remaining.isNotEmpty()) {
                log.d { "Tier1.5: Skipped - provider available=${onDeviceLLMProvider != null}" }
            }
        }

        // Tier 2: Cloud LLM batch (if available and needed)
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

        // Note: Tier 3 (premium LLM) is handled by Tier 2 with retry if needed
        // For now, remaining transactions stay uncategorized (assigned "other" later)

        val uncategorizedIds = remaining.map { it.id }
        return CategorizationBatchResult(results, uncategorizedIds)
    }

    /**
     * Categorize transactions using on-device LLM (Tier 1.5).
     * Uses batch processing for efficiency (5 transactions per LLM call).
     */
    private suspend fun categorizeWithOnDevice(
        transactions: List<TransactionForCategorization>,
        categories: List<String>
    ): List<CategorizationResult> {
        val provider = onDeviceLLMProvider ?: return emptyList()

        val batchSize = BATCH_SIZE
        val batches = transactions.chunked(batchSize)
        log.i { "Tier1.5: Processing ${transactions.size} transactions in ${batches.size} batches (size=$batchSize)" }

        return batches.flatMapIndexed { batchIndex, batch ->
            try {
                log.d { "Tier1.5: Processing batch ${batchIndex + 1}/${batches.size} with ${batch.size} transactions" }
                val prompt = buildBatchPrompt(batch, categories)
                val response = provider.complete(
                    CompletionRequest(
                        prompt = prompt,
                        maxTokens = 200 + batch.size * 50, // Scale tokens with batch size
                        temperature = 0.1f,
                        systemPrompt = BATCH_SYSTEM_PROMPT
                    )
                )

                val results = parseBatchResponse(batch, response.content)
                log.d { "Tier1.5: Batch ${batchIndex + 1} parsed ${results.size}/${batch.size} transactions" }
                results
            } catch (e: Exception) {
                log.e(e) { "Tier1.5: Batch ${batchIndex + 1} failed: ${e.message}" }
                // Fallback to single-transaction processing for failed batch
                categorizeBatchFallback(batch, categories, provider)
            }
        }
    }

    /**
     * Fallback: process transactions one-by-one if batch fails.
     */
    private suspend fun categorizeBatchFallback(
        transactions: List<TransactionForCategorization>,
        categories: List<String>,
        provider: LLMProvider
    ): List<CategorizationResult> {
        log.d { "Tier1.5: Fallback to single processing for ${transactions.size} transactions" }
        return transactions.mapNotNull { tx ->
            try {
                val prompt = buildCategorizationPrompt(tx.description, categories)
                val response = provider.complete(
                    CompletionRequest(
                        prompt = prompt,
                        maxTokens = 50, // Reduced - we only need a category name
                        temperature = 0.1f
                        // System prompt is configured in Swift - no need to override
                    )
                )
                parseCategorizationResponse(tx.id, response.content)
            } catch (e: Exception) {
                log.e(e) { "Tier1.5 fallback: Exception for txId=${tx.id}: ${e.message}" }
                null
            }
        }
    }

    /**
     * Build categorization prompt for single transaction.
     * SIMPLIFIED for SmolLM2-135M: minimal prompt for better completion quality.
     */
    private fun buildCategorizationPrompt(description: String, categories: List<String>): String {
        // SmolLM2-135M works better with simple completion-style prompts
        val categoryOptions = categories.take(8).joinToString(", ")
        return """Transaction: "$description"
Categories: $categoryOptions
Category:"""
    }

    /**
     * Build batch categorization prompt for multiple transactions.
     * SIMPLIFIED for SmolLM2-135M: simple numbered list format.
     */
    private fun buildBatchPrompt(
        transactions: List<TransactionForCategorization>,
        categories: List<String>
    ): String {
        val txList = transactions.mapIndexed { index, tx ->
            "${index + 1}. ${tx.description}"
        }.joinToString("\n")

        val categoryOptions = categories.take(8).joinToString(", ")

        return """Categorize each transaction (${categoryOptions}):

$txList

Categories (format: 1. category, 2. category, ...):"""
    }

    /**
     * Format amount for display in prompt.
     * Amount is in cents (Long), convert to dollars with 2 decimal places.
     */
    private fun formatAmount(amount: Long): String {
        val amountInDollars = amount / 100.0
        val formatted = amountInDollars.toString().let { str ->
            val parts = str.split(".")
            val intPart = parts[0]
            val decPart = parts.getOrNull(1)?.take(2)?.padEnd(2, '0') ?: "00"
            "$intPart.$decPart"
        }
        return if (amountInDollars >= 0) "+$formatted" else formatted
    }

    /**
     * Parse batch response from LLM.
     * Expected format: "1. groceries, 2. food_delivery, 3. shopping" or similar.
     * Also tries JSON format as fallback.
     */
    private fun parseBatchResponse(
        transactions: List<TransactionForCategorization>,
        response: String
    ): List<CategorizationResult> {
        log.d { "Tier1.5: Parsing batch response: ${response.take(200)}" }

        // Try JSON parsing first (in case model returns JSON)
        val jsonResults = tryParseJsonBatch(transactions, response)
        if (jsonResults.isNotEmpty()) {
            return jsonResults
        }

        // Parse simple numbered format: "1. groceries, 2. food_delivery" or "1. groceries\n2. food_delivery"
        return parseSimpleBatchFormat(transactions, response)
    }

    /**
     * Try to parse JSON batch response (fallback for models that return JSON).
     */
    private fun tryParseJsonBatch(
        transactions: List<TransactionForCategorization>,
        response: String
    ): List<CategorizationResult> {
        return try {
            val jsonStart = response.indexOf('[')
            val jsonEnd = response.lastIndexOf(']')
            if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
                return emptyList()
            }

            val jsonArray = response.substring(jsonStart, jsonEnd + 1)
            val responses = Json.decodeFromString<List<LLMBatchCategoryResponse>>(jsonArray)

            responses.mapNotNull { resp ->
                val txIndex = resp.id - 1
                if (txIndex < 0 || txIndex >= transactions.size) return@mapNotNull null

                val tx = transactions[txIndex]
                CategorizationResult(
                    transactionId = tx.id,
                    categoryId = resp.categoryId,
                    confidence = resp.confidence,
                    source = CategorizationSource.ON_DEVICE_ML
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parse simple numbered format like "1. groceries, 2. food_delivery" or "1. groceries\n2. food".
     */
    private fun parseSimpleBatchFormat(
        transactions: List<TransactionForCategorization>,
        response: String
    ): List<CategorizationResult> {
        val results = mutableListOf<CategorizationResult>()

        // Regex to match "1. category" or "1: category" patterns
        val pattern = Regex("""(\d+)[.\s:]+\s*(\w+)""")
        val matches = pattern.findAll(response)

        for (match in matches) {
            val index = match.groupValues[1].toIntOrNull()?.minus(1) ?: continue
            val category = match.groupValues[2].lowercase()

            if (index < 0 || index >= transactions.size) continue
            if (category !in DEFAULT_CATEGORIES) continue

            val tx = transactions[index]
            results.add(
                CategorizationResult(
                    transactionId = tx.id,
                    categoryId = category,
                    confidence = 0.80f, // Default confidence for simple text parsing
                    source = CategorizationSource.ON_DEVICE_ML
                )
            )
        }

        log.d { "Tier1.5: Parsed ${results.size}/${transactions.size} from simple format" }
        return results
    }

    /**
     * Parse LLM categorization response.
     * Handles both JSON format and simple text (just category name).
     */
    private fun parseCategorizationResponse(
        transactionId: String,
        response: String
    ): CategorizationResult? {
        val trimmed = response.trim().lowercase()

        // Try JSON parsing first
        try {
            val jsonResponse = Json.decodeFromString<LLMCategoryResponse>(response)
            return CategorizationResult(
                transactionId = transactionId,
                categoryId = jsonResponse.categoryId,
                confidence = jsonResponse.confidence,
                source = CategorizationSource.ON_DEVICE_ML
            )
        } catch (_: Exception) {
            // JSON parsing failed, try simple text
        }

        // Simple text parsing: find first matching category in response
        for (category in DEFAULT_CATEGORIES) {
            if (trimmed.contains(category) || trimmed.startsWith(category)) {
                log.d { "Tier1.5: Parsed category '$category' from simple response" }
                return CategorizationResult(
                    transactionId = transactionId,
                    categoryId = category,
                    confidence = 0.80f, // Default confidence for text parsing
                    source = CategorizationSource.ON_DEVICE_ML
                )
            }
        }

        log.w { "Tier1.5: Could not parse category from response: ${response.take(100)}" }
        return null
    }
}

/**
 * LLM response for single transaction categorization.
 */
@Serializable
private data class LLMCategoryResponse(
    val categoryId: String,
    val confidence: Float
)

/**
 * LLM response for batch categorization.
 * Contains 1-based id to map back to original transaction.
 */
@Serializable
private data class LLMBatchCategoryResponse(
    val id: Int,
    val categoryId: String,
    val confidence: Float
)

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

    val highConfidenceCount: Int get() = results.count { it.isHighConfidence }
    val needsConfirmationCount: Int get() = results.count { it.requiresUserConfirmation }
}

package com.finuts.data.categorization

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.finuts.domain.entity.CategorizationResult
import com.finuts.domain.entity.CategorizationSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * AI-based categorizer for Tier 2 and Tier 3 categorization.
 * Uses OpenAI GPT models via openai-kotlin library.
 */
class AICategorizer(
    private val openAI: OpenAI
) {
    companion object {
        private const val TIER2_MODEL = "gpt-4o-mini"
        private const val TIER3_MODEL = "gpt-4o"
        private const val MAX_BATCH_SIZE = 10

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Build prompt for batch transaction categorization.
         */
        fun buildPrompt(
            transactions: List<TransactionForCategorization>,
            categories: List<String>
        ): String {
            val txList = transactions.joinToString("\n") { tx ->
                "- ID: ${tx.id}, Description: \"${tx.description}\", Amount: ${tx.formattedAmount}"
            }

            return """
                |You are a financial transaction categorizer.
                |Categorize each transaction into one of these categories: ${categories.joinToString(", ")}
                |
                |Transactions to categorize:
                |$txList
                |
                |Respond with a JSON array of objects with fields:
                |- transactionId: the transaction ID
                |- categoryId: the category (from the list above)
                |- confidence: your confidence (0.0 to 1.0)
                |
                |Example response:
                |[{"transactionId": "tx-1", "categoryId": "groceries", "confidence": 0.85}]
                |
                |Respond ONLY with the JSON array, no other text.
            """.trimMargin()
        }

        /**
         * Parse LLM response into categorization results.
         */
        fun parseResponse(
            response: String,
            tier: CategorizationSource
        ): List<CategorizationResult> {
            return try {
                val jsonArray = json.parseToJsonElement(response).jsonArray
                jsonArray.map { element ->
                    val obj = element.jsonObject
                    CategorizationResult(
                        transactionId = obj["transactionId"]?.jsonPrimitive?.content ?: "",
                        categoryId = obj["categoryId"]?.jsonPrimitive?.content ?: "other",
                        confidence = obj["confidence"]?.jsonPrimitive?.content?.toFloatOrNull()
                            ?: 0.5f,
                        source = tier
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Categorize transactions using Tier 2 (GPT-4o-mini).
     * More cost-effective, suitable for most transactions.
     */
    suspend fun categorizeTier2(
        transactions: List<TransactionForCategorization>,
        categories: List<String>
    ): List<CategorizationResult> {
        return categorize(transactions, categories, TIER2_MODEL, CategorizationSource.LLM_TIER2)
    }

    /**
     * Categorize transactions using Tier 3 (GPT-4o).
     * More expensive but higher accuracy for complex cases.
     */
    suspend fun categorizeTier3(
        transactions: List<TransactionForCategorization>,
        categories: List<String>
    ): List<CategorizationResult> {
        return categorize(transactions, categories, TIER3_MODEL, CategorizationSource.LLM_TIER3)
    }

    private suspend fun categorize(
        transactions: List<TransactionForCategorization>,
        categories: List<String>,
        model: String,
        source: CategorizationSource
    ): List<CategorizationResult> {
        if (transactions.isEmpty()) return emptyList()

        val batches = transactions.chunked(MAX_BATCH_SIZE)
        val results = mutableListOf<CategorizationResult>()

        for (batch in batches) {
            val prompt = buildPrompt(batch, categories)
            val response = callOpenAI(prompt, model)
            results.addAll(parseResponse(response, source))
        }

        return results
    }

    private suspend fun callOpenAI(prompt: String, model: String): String {
        val request = ChatCompletionRequest(
            model = ModelId(model),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.User,
                    content = prompt
                )
            ),
            temperature = 0.1
        )

        val completion = openAI.chatCompletion(request)
        return completion.choices.firstOrNull()?.message?.content ?: "[]"
    }
}

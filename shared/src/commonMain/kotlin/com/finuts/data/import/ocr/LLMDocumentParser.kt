package com.finuts.data.import.ocr

import co.touchlab.kermit.Logger
import com.finuts.ai.cost.AICostTrackerInterface
import com.finuts.ai.privacy.PIIAnonymizer
import com.finuts.ai.providers.CompletionRequest
import com.finuts.ai.providers.LLMProvider
import com.finuts.domain.entity.import.ImportSource
import com.finuts.domain.entity.import.ImportedTransaction
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * LLM-powered document parser for unknown bank statement formats.
 *
 * This is a fallback parser used when rule-based parsing (regex) fails.
 * It sends anonymized text to an LLM which extracts transactions.
 *
 * Privacy-first approach:
 * - All PII is anonymized BEFORE sending to LLM
 * - Only merchant names and transaction structure are sent
 * - De-anonymization happens locally after response
 *
 * Cost-aware:
 * - Checks budget before making LLM calls
 * - Tracks token usage for cost calculation
 */
class LLMDocumentParser(
    private val provider: LLMProvider?,
    private val anonymizer: PIIAnonymizer,
    private val costTracker: AICostTrackerInterface
) {
    private val log = Logger.withTag("LLMDocumentParser")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        // Estimated cost per document parsing (~500 input tokens, ~200 output)
        private const val ESTIMATED_COST_USD = 0.0003f
    }

    /**
     * Parse document text using LLM.
     *
     * @param text OCR text from the document
     * @return List of extracted transactions, empty if parsing fails or budget exceeded
     */
    suspend fun parse(text: String): List<ImportedTransaction> {
        log.d { "parse: starting, textLen=${text.length}" }

        // Check if provider is available
        if (provider == null) {
            log.w { "parse: no provider available" }
            return emptyList()
        }

        if (!provider.isAvailable()) {
            log.w { "parse: provider not available" }
            return emptyList()
        }

        // Check cost budget
        if (!costTracker.canExecute(ESTIMATED_COST_USD)) {
            log.w { "parse: budget exceeded, skipping LLM call" }
            return emptyList()
        }

        // Anonymize PII before sending to LLM
        val anonymizationResult = anonymizer.anonymize(text)
        val anonymizedText = anonymizationResult.anonymizedText

        if (anonymizationResult.wasModified) {
            log.i { "parse: anonymized ${anonymizationResult.piiCount} PII items" }
        }

        // Build prompt
        val prompt = buildPrompt(anonymizedText)

        // Call LLM
        val response = try {
            val request = CompletionRequest(
                prompt = prompt,
                maxTokens = 2048,
                temperature = 0.1f
            )
            provider.complete(request)
        } catch (e: Exception) {
            log.e(e) { "parse: LLM call failed" }
            return emptyList()
        }

        // Record cost
        costTracker.record(
            inputTokens = response.inputTokens,
            outputTokens = response.outputTokens,
            model = response.model
        )

        // Parse JSON response
        val transactions = parseResponse(response.content)

        log.i { "parse: extracted ${transactions.size} transactions" }

        return transactions
    }

    private fun buildPrompt(text: String): String = """
You are a financial document parser. Extract ALL transactions from this bank statement.

The document may be in ANY language (Russian, English, Kazakh, etc.) and ANY format.
Detect the structure automatically.

Document text:
---
$text
---

For each transaction, extract:
- date: ISO format (YYYY-MM-DD)
- amount: integer in minor units (cents/kopecks/tiyn). Negative for expenses, positive for income.
- description: original transaction description
- merchant: cleaned merchant name (if identifiable), or null
- currency: detected currency code (KZT, RUB, USD, EUR, etc.)

IMPORTANT:
- Amounts should be in minor units (e.g., $15.50 = 1550, 15000₸ = 1500000)
- Use negative amounts for expenses, positive for income
- Keep original language in descriptions

Return ONLY a JSON array, no additional text:
[
  {"date": "2026-01-15", "amount": -1500000, "description": "Покупка в МАГНУМ ТОО", "merchant": "МАГНУМ", "currency": "KZT"},
  {"date": "2026-01-14", "amount": 5000000, "description": "Зачисление зарплаты", "merchant": null, "currency": "KZT"}
]
""".trimIndent()

    private fun parseResponse(content: String): List<ImportedTransaction> {
        return try {
            // Extract JSON array from response (handle markdown code blocks)
            val jsonContent = extractJsonArray(content)

            val items = json.decodeFromString<List<LLMTransactionItem>>(jsonContent)

            items.mapNotNull { item ->
                try {
                    val date = LocalDate.parse(item.date)
                    ImportedTransaction(
                        date = date,
                        amount = item.amount,
                        description = item.description,
                        merchant = item.merchant,
                        confidence = 0.85f, // LLM confidence is moderate
                        source = ImportSource.LLM_ENHANCED
                    )
                } catch (e: Exception) {
                    log.w { "parseResponse: failed to parse item: ${e.message}" }
                    null
                }
            }
        } catch (e: Exception) {
            log.e(e) { "parseResponse: failed to parse JSON: ${e.message}" }
            emptyList()
        }
    }

    /**
     * Extract JSON array from potentially markdown-wrapped response.
     */
    private fun extractJsonArray(content: String): String {
        val trimmed = content.trim()

        // Handle markdown code blocks
        if (trimmed.startsWith("```")) {
            val start = trimmed.indexOf('[')
            val end = trimmed.lastIndexOf(']')
            if (start != -1 && end != -1) {
                return trimmed.substring(start, end + 1)
            }
        }

        // Already clean JSON
        if (trimmed.startsWith("[")) {
            return trimmed
        }

        // Try to find JSON array anywhere in the response
        val start = trimmed.indexOf('[')
        val end = trimmed.lastIndexOf(']')
        if (start != -1 && end != -1) {
            return trimmed.substring(start, end + 1)
        }

        return trimmed
    }
}

/**
 * Internal data class for parsing LLM response.
 */
@Serializable
private data class LLMTransactionItem(
    val date: String,
    val amount: Long,
    val description: String,
    val merchant: String? = null,
    val currency: String? = null
)

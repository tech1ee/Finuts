package com.finuts.data.categorization

import co.touchlab.kermit.Logger
import com.finuts.ai.cost.AICostTrackerInterface
import com.finuts.ai.providers.CompletionRequest
import com.finuts.ai.providers.LLMProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Tier 1.5: LLM-powered merchant enrichment.
 *
 * Enriches raw merchant names with clean brand names, types, and MCC codes.
 * Called after Tier 1 (rules) but before Tier 2 (full categorization).
 *
 * Examples:
 * - "SBUX #1234 COFFEE" → Starbucks (COFFEE_SHOP, MCC 5814)
 * - "МАГНУМ КЭШ АСТАНА" → Magnum Cash & Carry (GROCERY)
 * - "WOLT*RESTO DELIVERY" → Wolt (FOOD_DELIVERY)
 *
 * Cost: ~$0.0004 per call (200 input + 50 output tokens)
 * Only called for unknown merchants not in MerchantDatabase.
 */
class LLMMerchantEnricher(
    private val provider: LLMProvider?,
    private val costTracker: AICostTrackerInterface
) {
    private val log = Logger.withTag("LLMMerchantEnricher")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Enrich merchant name using LLM.
     *
     * @param rawDescription Original transaction description
     * @param normalizedName Normalized merchant name from MerchantNormalizer
     * @return MerchantEnrichment if successful and confidence >= 0.7, null otherwise
     */
    suspend fun enrich(
        rawDescription: String,
        normalizedName: String
    ): MerchantEnrichment? {
        // 1. Check provider availability
        if (provider == null) {
            log.d { "enrich: provider is null, skipping" }
            return null
        }

        if (!provider.isAvailable()) {
            log.d { "enrich: provider unavailable" }
            return null
        }

        // 2. Check cost budget (estimated ~$0.0004 per call)
        val estimatedCost = ESTIMATED_COST_PER_CALL
        if (!costTracker.canExecute(estimatedCost)) {
            log.w { "enrich: cost budget exceeded" }
            return null
        }

        // 3. Build prompt
        val prompt = buildEnrichmentPrompt(rawDescription, normalizedName)

        // 4. Call LLM
        return try {
            val response = provider.complete(
                CompletionRequest(
                    prompt = prompt,
                    maxTokens = 100,
                    temperature = 0.1f
                )
            )

            // 5. Record cost
            costTracker.record(
                inputTokens = response.inputTokens,
                outputTokens = response.outputTokens,
                model = response.model
            )

            // 6. Parse response
            val enrichment = parseEnrichmentResponse(response.content)

            // 7. Filter by confidence threshold
            if (enrichment != null && enrichment.confidence >= CONFIDENCE_THRESHOLD) {
                log.i {
                    "enrich: SUCCESS - '$normalizedName' → '${enrichment.cleanMerchantName}' " +
                        "(${enrichment.merchantType}, conf=${enrichment.confidence})"
                }
                enrichment
            } else {
                log.d {
                    "enrich: low confidence - '$normalizedName' " +
                        "(conf=${enrichment?.confidence ?: 0f})"
                }
                null
            }
        } catch (e: Exception) {
            log.e(e) { "enrich: exception during LLM call" }
            null
        }
    }

    private fun buildEnrichmentPrompt(
        rawDescription: String,
        normalizedName: String
    ): String = """
You are a financial transaction merchant enrichment specialist.

Task: Extract clean merchant name from transaction description.

Input: "$rawDescription"
Normalized by rules: "$normalizedName"

Extract:
1. cleanMerchantName - The brand name (e.g., "Starbucks" not "SBUX")
2. brandName - Full brand name if different (optional)
3. merchantType - One of: COFFEE_SHOP, GROCERY, RESTAURANT, BANK, GAS_STATION, RETAIL, TRANSPORT, FOOD_DELIVERY, ENTERTAINMENT, PHARMACY, UTILITIES, SUBSCRIPTION, OTHER
4. mccCode - 4-digit Visa MCC if known (optional)
5. confidence - 0.0 to 1.0

Examples:
- "SBUX #1234" → {"cleanMerchantName": "Starbucks", "merchantType": "COFFEE_SHOP", "mccCode": "5814", "confidence": 0.95}
- "МАГНУМ ТОО" → {"cleanMerchantName": "Magnum Cash & Carry", "brandName": "Magnum", "merchantType": "GROCERY", "confidence": 0.92}

Return JSON only:
{"cleanMerchantName": "...", "merchantType": "...", "confidence": 0.9}
""".trimIndent()

    private fun parseEnrichmentResponse(content: String): MerchantEnrichment? {
        if (content.isBlank()) return null

        return try {
            // Extract JSON from response (may have surrounding text)
            val jsonStart = content.indexOf('{')
            val jsonEnd = content.lastIndexOf('}')

            if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
                log.w { "parseEnrichmentResponse: no valid JSON found" }
                return null
            }

            val jsonString = content.substring(jsonStart, jsonEnd + 1)
            json.decodeFromString<MerchantEnrichment>(jsonString)
        } catch (e: Exception) {
            log.e(e) { "parseEnrichmentResponse: JSON parsing failed" }
            null
        }
    }

    companion object {
        const val CONFIDENCE_THRESHOLD = 0.70f
        const val ESTIMATED_COST_PER_CALL = 0.0004f // ~200 input + 50 output tokens
    }
}

/**
 * Result of LLM merchant enrichment.
 *
 * Contains clean merchant name, brand, type, and optional MCC code.
 */
@Serializable
data class MerchantEnrichment(
    /** Clean merchant name (e.g., "Starbucks" not "SBUX") */
    val cleanMerchantName: String,

    /** Full brand name if different (e.g., "Starbucks Coffee Company") */
    val brandName: String? = null,

    /** Merchant type for categorization hints */
    val merchantType: String? = null,

    /** 4-digit Visa MCC code if known */
    val mccCode: String? = null,

    /** Confidence score 0.0-1.0 */
    val confidence: Float
)

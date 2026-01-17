package com.finuts.ai.prompts

import kotlinx.serialization.Serializable

/**
 * Prompts for AI-powered transaction categorization (Tier 2).
 *
 * Used when local rules (Tier 0-1) fail to categorize a transaction.
 * Input is always anonymized via PIIAnonymizer before sending.
 *
 * Features:
 * - Few-shot examples for improved accuracy
 * - User context integration (currency, recent similar transactions)
 * - Multi-language support (EN, RU, KK markets)
 * - Category descriptions for better understanding
 */
object CategorizationPrompt {

    // Few-shot examples for English markets
    private val ENGLISH_EXAMPLES = listOf(
        "STARBUCKS #1234" to "coffee_shops",
        "AMAZON.COM PURCHASE" to "shopping",
        "UBER TRIP" to "transport",
        "NETFLIX SUBSCRIPTION" to "subscriptions",
        "WHOLE FOODS MARKET" to "groceries"
    )

    // Few-shot examples for Russian/Kazakh markets
    private val RUSSIAN_EXAMPLES = listOf(
        "МАГНУМ ТОО" to "groceries",
        "GLOVO ДОСТАВКА" to "food_delivery",
        "KASPI GOLD ПЕРЕВОД" to "transfer",
        "ЯНДЕКС ТАКСИ" to "transport",
        "WOLT ORDER" to "food_delivery"
    )

    // Category descriptions for better understanding
    private val CATEGORY_DESCRIPTIONS = mapOf(
        "groceries" to "supermarket, food shopping, продукты, магазин",
        "transport" to "taxi, bus, metro, car expenses, такси, транспорт",
        "dining" to "restaurants, cafes, рестораны, кафе",
        "food_delivery" to "Glovo, Wolt, Yandex Eats, delivery services",
        "shopping" to "online shopping, retail stores, покупки",
        "subscriptions" to "Netflix, Spotify, monthly services",
        "transfer" to "money transfers, bank transfers, переводы",
        "utilities" to "electricity, water, internet bills, коммуналка",
        "coffee_shops" to "Starbucks, coffee houses, кофейни"
    )

    /**
     * Build categorization prompt for a single transaction with few-shot examples.
     *
     * @param description Anonymized transaction description
     * @param existingCategories List of available category IDs
     * @param language User's preferred language (en, ru, kk)
     */
    fun buildCategorizePrompt(
        description: String,
        existingCategories: List<String>,
        language: String = "en"
    ): String {
        val examples = getExamplesForLanguage(language)
        val formattedExamples = examples.mapIndexed { idx, (desc, cat) ->
            "${idx + 1}. \"$desc\" → $cat"
        }.joinToString("\n")

        val categoryHints = buildCategoryHints(existingCategories)

        return """
You are a financial transaction categorizer for a personal finance app.

## Examples (learn from these):
$formattedExamples

## Task
Categorize this transaction: "$description"

## Available categories:
$categoryHints

## Rules:
1. Use an existing category if it fits (most common case)
2. If no existing category fits well, suggest a new category ID (lowercase, snake_case)
3. Return confidence 0.0-1.0 based on how certain you are
4. For new categories, suggest a display name, icon hint, and color

## Response format (JSON only):

For existing category:
{"categoryId": "groceries", "confidence": 0.92, "isNew": false}

For new category:
{"categoryId": "pet_supplies", "confidence": 0.85, "isNew": true, "newCategoryMetadata": {"name": "Pet Supplies", "iconHint": "paw", "color": "#8D6E63"}}
""".trimIndent()
    }

    /**
     * Build categorization prompt with user context for personalized categorization.
     *
     * @param description Anonymized transaction description
     * @param existingCategories List of available category IDs
     * @param context User's categorization context (nullable)
     * @param language User's preferred language
     */
    fun buildCategorizePromptWithContext(
        description: String,
        existingCategories: List<String>,
        context: UserCategorizationContext?,
        language: String = "en"
    ): String {
        val examples = getExamplesForLanguage(language)
        val formattedExamples = examples.mapIndexed { idx, (desc, cat) ->
            "${idx + 1}. \"$desc\" → $cat"
        }.joinToString("\n")

        val categoryHints = buildCategoryHints(existingCategories)
        val contextSection = buildContextSection(context)

        return """
You are a financial transaction categorizer for a personal finance app.

$contextSection
## Examples (learn from these):
$formattedExamples

## Task
Categorize this transaction: "$description"

## Available categories:
$categoryHints

## Rules:
1. Use an existing category if it fits (most common case)
2. Consider user's recent similar transactions for context
3. Return confidence 0.0-1.0 based on how certain you are
4. For new categories, suggest a display name, icon hint, and color

## Response format (JSON only):
{"categoryId": "groceries", "confidence": 0.92, "isNew": false}
""".trimIndent()
    }

    private fun getExamplesForLanguage(language: String): List<Pair<String, String>> {
        return when (language.lowercase()) {
            "ru", "kk" -> RUSSIAN_EXAMPLES
            else -> ENGLISH_EXAMPLES
        }
    }

    private fun buildCategoryHints(categories: List<String>): String {
        if (categories.isEmpty()) {
            return "No existing categories. Please suggest a new category that fits best."
        }
        return categories.joinToString("\n") { cat ->
            val hint = CATEGORY_DESCRIPTIONS[cat]
            if (hint != null) "- $cat ($hint)" else "- $cat"
        }
    }

    private fun buildContextSection(context: UserCategorizationContext?): String {
        if (context == null) return ""

        val parts = mutableListOf<String>()

        context.currency?.let { currency ->
            parts.add("Currency: $currency")
        }

        context.recentSimilarTransaction?.let { recent ->
            parts.add("Recent similar transaction: \"${recent.description}\" → ${recent.categoryId} (confidence: ${recent.confidence})")
        }

        if (context.topCategories.isNotEmpty()) {
            parts.add("User's top categories: ${context.topCategories.joinToString(", ")}")
        }

        return if (parts.isNotEmpty()) {
            "## User Context\n${parts.joinToString("\n")}\n\n"
        } else ""
    }

    /**
     * Build batch categorization prompt for multiple transactions.
     * More efficient than single calls for bulk import.
     *
     * @param descriptions List of anonymized descriptions with indices
     * @param existingCategories Available category IDs
     * @param language User's preferred language
     */
    fun buildBatchCategorizePrompt(
        descriptions: List<IndexedDescription>,
        existingCategories: List<String>,
        language: String = "en"
    ): String {
        val examples = getExamplesForLanguage(language)
        val formattedExamples = examples.take(3).mapIndexed { idx, (desc, cat) ->
            "${idx}. \"$desc\" → $cat"
        }.joinToString("\n")

        val categoryHints = buildCategoryHints(existingCategories)
        val transactionList = descriptions.joinToString("\n") { "${it.index}: \"${it.description}\"" }

        return """
You are a financial transaction categorizer. Categorize ALL transactions below.

## Example categorizations:
$formattedExamples

## Transactions to categorize:
$transactionList

## Available categories:
$categoryHints

## Response format
Return a JSON array with: index, categoryId, confidence (0.0-1.0).

Example output:
[{"index": 0, "categoryId": "groceries", "confidence": 0.95}, {"index": 1, "categoryId": "transport", "confidence": 0.88}]

Return ONLY the JSON array, no additional text.
""".trimIndent()
    }

    /**
     * Build metadata suggestion prompt for unknown category.
     *
     * @param categoryId The category ID to suggest metadata for
     */
    fun buildMetadataSuggestionPrompt(
        categoryId: String
    ): String = """
Suggest display metadata for the category "$categoryId" in a personal finance app.

Respond ONLY with JSON:
{
  "name": "Human Readable Name",
  "nameRu": "Название на русском",
  "iconHint": "icon_keyword",
  "color": "#HEXCOLOR"
}

Icon hints should be simple keywords like: food, car, home, health, shopping, gift, etc.
Colors should be hex format suitable for UI.
""".trimIndent()

    /**
     * JSON response schema for structured output.
     */
    val CATEGORIZATION_SCHEMA = """
{
  "type": "object",
  "properties": {
    "categoryId": {"type": "string"},
    "confidence": {"type": "number", "minimum": 0, "maximum": 1},
    "isNew": {"type": "boolean"},
    "newCategoryMetadata": {
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "iconHint": {"type": "string"},
        "color": {"type": "string"}
      }
    }
  },
  "required": ["categoryId", "confidence", "isNew"]
}
""".trimIndent()
}

/**
 * Indexed description for batch processing.
 */
data class IndexedDescription(
    val index: Int,
    val description: String
)

/**
 * Response from categorization LLM.
 */
@Serializable
data class LLMCategoryResponse(
    val categoryId: String,
    val confidence: Float,
    val isNew: Boolean = false,
    val newCategoryMetadata: NewCategoryMetadata? = null
)

/**
 * Metadata for new category suggested by LLM.
 */
@Serializable
data class NewCategoryMetadata(
    val name: String,
    val iconHint: String,
    val color: String
)

/**
 * Batch categorization response item.
 */
@Serializable
data class BatchCategoryItem(
    val index: Int,
    val categoryId: String,
    val confidence: Float
)

/**
 * User context for personalized categorization.
 * Used to improve accuracy based on user's history and preferences.
 */
data class UserCategorizationContext(
    val currency: String? = null,
    val recentSimilarTransaction: RecentTransaction? = null,
    val topCategories: List<String> = emptyList()
)

/**
 * Recent transaction for context reference.
 * Helps LLM understand user's categorization patterns.
 */
data class RecentTransaction(
    val description: String,
    val categoryId: String,
    val confidence: Float
)

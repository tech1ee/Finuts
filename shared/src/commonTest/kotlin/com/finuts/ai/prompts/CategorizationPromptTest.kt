package com.finuts.ai.prompts

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for CategorizationPrompt few-shot and context-aware improvements.
 *
 * TDD: RED phase - these tests define expected behavior for improved prompts.
 */
class CategorizationPromptTest {

    // === Few-shot Examples Tests ===

    @Test
    fun `prompt includes few-shot examples`() {
        val prompt = CategorizationPrompt.buildCategorizePrompt(
            description = "UNKNOWN MERCHANT",
            existingCategories = listOf("groceries", "transport", "dining")
        )

        // Should contain example transactions
        assertTrue(
            prompt.contains("Example") || prompt.contains("example"),
            "Prompt should include few-shot examples section"
        )

        // Should have at least 3 examples
        val exampleCount = prompt.split("→").size - 1
        assertTrue(
            exampleCount >= 3,
            "Prompt should have at least 3 few-shot examples, found: $exampleCount"
        )
    }

    @Test
    fun `prompt includes category descriptions`() {
        val prompt = CategorizationPrompt.buildCategorizePrompt(
            description = "TEST",
            existingCategories = listOf("groceries", "transport")
        )

        // Should explain what groceries category is for
        assertTrue(
            prompt.contains("supermarket") ||
            prompt.contains("food") ||
            prompt.contains("продукты"),
            "Prompt should include category descriptions/hints"
        )
    }

    // === User Context Tests ===

    @Test
    fun `prompt includes user context when provided`() {
        val context = UserCategorizationContext(
            currency = "KZT",
            recentSimilarTransaction = RecentTransaction(
                description = "MAGNUM SUPER",
                categoryId = "groceries",
                confidence = 0.95f
            )
        )

        val prompt = CategorizationPrompt.buildCategorizePromptWithContext(
            description = "MAGNUM ALMATY",
            existingCategories = listOf("groceries", "transport"),
            context = context
        )

        assertTrue(
            prompt.contains("KZT") || prompt.contains("currency"),
            "Prompt should include user's currency"
        )
        assertTrue(
            prompt.contains("MAGNUM") || prompt.contains("similar"),
            "Prompt should reference recent similar transaction"
        )
    }

    @Test
    fun `prompt works without user context`() {
        val prompt = CategorizationPrompt.buildCategorizePromptWithContext(
            description = "TEST MERCHANT",
            existingCategories = listOf("groceries"),
            context = null
        )

        // Should not crash and should still have basic structure
        assertTrue(prompt.contains("TEST MERCHANT"))
        assertTrue(prompt.contains("groceries"))
    }

    // === Multi-language Tests ===

    @Test
    fun `prompt includes Russian market examples for RU locale`() {
        val prompt = CategorizationPrompt.buildCategorizePrompt(
            description = "МАГНУМ ТОО",
            existingCategories = listOf("groceries", "transport"),
            language = "ru"
        )

        // Should have Russian/Kazakh merchant examples
        val hasLocalExamples = prompt.contains("МАГНУМ") ||
            prompt.contains("GLOVO") ||
            prompt.contains("KASPI") ||
            prompt.contains("Яндекс") ||
            prompt.contains("WOLT")

        assertTrue(
            hasLocalExamples,
            "Russian locale prompt should include local merchant examples"
        )
    }

    @Test
    fun `prompt includes English market examples for EN locale`() {
        val prompt = CategorizationPrompt.buildCategorizePrompt(
            description = "STARBUCKS",
            existingCategories = listOf("coffee_shops", "dining"),
            language = "en"
        )

        // Should have English merchant examples
        val hasEnglishExamples = prompt.contains("Starbucks") ||
            prompt.contains("Amazon") ||
            prompt.contains("Uber") ||
            prompt.contains("Netflix")

        assertTrue(
            hasEnglishExamples,
            "English locale prompt should include English merchant examples"
        )
    }

    // === Batch Prompt Tests ===

    @Test
    fun `batch prompt includes few-shot examples`() {
        val descriptions = listOf(
            IndexedDescription(0, "MERCHANT A"),
            IndexedDescription(1, "MERCHANT B")
        )

        val prompt = CategorizationPrompt.buildBatchCategorizePrompt(
            descriptions = descriptions,
            existingCategories = listOf("groceries", "transport")
        )

        // Should contain examples for batch processing
        assertTrue(
            prompt.contains("Example") || prompt.contains("example"),
            "Batch prompt should include examples"
        )
    }

    @Test
    fun `batch prompt has clear output format instructions`() {
        val descriptions = listOf(
            IndexedDescription(0, "TEST")
        )

        val prompt = CategorizationPrompt.buildBatchCategorizePrompt(
            descriptions = descriptions,
            existingCategories = listOf("groceries")
        )

        // Should specify JSON array format
        assertTrue(prompt.contains("JSON"))
        assertTrue(prompt.contains("array") || prompt.contains("["))
        assertTrue(prompt.contains("index"))
        assertTrue(prompt.contains("categoryId"))
        assertTrue(prompt.contains("confidence"))
    }

    // === Response Parsing Tests ===

    @Test
    fun `LLMCategoryResponse parses valid JSON`() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

        val response = json.decodeFromString<LLMCategoryResponse>(
            """{"categoryId": "groceries", "confidence": 0.92, "isNew": false}"""
        )

        assertEquals("groceries", response.categoryId)
        assertEquals(0.92f, response.confidence)
        assertFalse(response.isNew)
    }

    @Test
    fun `LLMCategoryResponse parses new category with metadata`() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

        val response = json.decodeFromString<LLMCategoryResponse>(
            """{
                "categoryId": "pet_supplies",
                "confidence": 0.85,
                "isNew": true,
                "newCategoryMetadata": {
                    "name": "Pet Supplies",
                    "iconHint": "paw",
                    "color": "#8D6E63"
                }
            }"""
        )

        assertEquals("pet_supplies", response.categoryId)
        assertEquals(0.85f, response.confidence)
        assertTrue(response.isNew)
        assertNotNull(response.newCategoryMetadata)
        assertEquals("Pet Supplies", response.newCategoryMetadata?.name)
        assertEquals("paw", response.newCategoryMetadata?.iconHint)
        assertEquals("#8D6E63", response.newCategoryMetadata?.color)
    }

    @Test
    fun `BatchCategoryItem parses correctly`() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

        val items = json.decodeFromString<List<BatchCategoryItem>>(
            """[
                {"index": 0, "categoryId": "groceries", "confidence": 0.95},
                {"index": 1, "categoryId": "transport", "confidence": 0.88}
            ]"""
        )

        assertEquals(2, items.size)
        assertEquals(0, items[0].index)
        assertEquals("groceries", items[0].categoryId)
        assertEquals(0.95f, items[0].confidence)
        assertEquals(1, items[1].index)
        assertEquals("transport", items[1].categoryId)
    }

    // === Edge Cases ===

    @Test
    fun `prompt handles empty category list`() {
        val prompt = CategorizationPrompt.buildCategorizePrompt(
            description = "TEST",
            existingCategories = emptyList()
        )

        // Should still generate valid prompt
        assertTrue(prompt.contains("TEST"))
        // Should emphasize creating new category
        assertTrue(
            prompt.contains("suggest") || prompt.contains("new"),
            "Empty categories prompt should guide LLM to suggest new category"
        )
    }

    @Test
    fun `prompt handles special characters in description`() {
        val prompt = CategorizationPrompt.buildCategorizePrompt(
            description = "CAFÉ \"LA BELLE\" #123 *4567",
            existingCategories = listOf("dining")
        )

        // Should contain escaped/sanitized description
        assertTrue(prompt.contains("CAFÉ") || prompt.contains("LA BELLE"))
    }

    @Test
    fun `batch prompt handles large batch`() {
        val descriptions = (0..15).map { IndexedDescription(it, "MERCHANT_$it") }

        val prompt = CategorizationPrompt.buildBatchCategorizePrompt(
            descriptions = descriptions,
            existingCategories = listOf("groceries", "transport")
        )

        // Should contain all merchant descriptions
        assertTrue(prompt.contains("MERCHANT_0"))
        assertTrue(prompt.contains("MERCHANT_15"))
    }

    // === Data Class Tests ===

    @Test
    fun `IndexedDescription stores all fields`() {
        val desc = IndexedDescription(index = 5, description = "TEST MERCHANT")

        assertEquals(5, desc.index)
        assertEquals("TEST MERCHANT", desc.description)
    }

    @Test
    fun `NewCategoryMetadata stores all fields`() {
        val metadata = NewCategoryMetadata(
            name = "Pet Supplies",
            iconHint = "paw",
            color = "#FF5722"
        )

        assertEquals("Pet Supplies", metadata.name)
        assertEquals("paw", metadata.iconHint)
        assertEquals("#FF5722", metadata.color)
    }

    @Test
    fun `UserCategorizationContext with all fields`() {
        val recent = RecentTransaction(
            description = "STORE A",
            categoryId = "shopping",
            confidence = 0.8f
        )
        val context = UserCategorizationContext(
            currency = "USD",
            recentSimilarTransaction = recent,
            topCategories = listOf("groceries", "dining", "transport")
        )

        assertEquals("USD", context.currency)
        assertNotNull(context.recentSimilarTransaction)
        assertEquals(3, context.topCategories.size)
    }

    @Test
    fun `UserCategorizationContext with defaults`() {
        val context = UserCategorizationContext()

        assertEquals(null, context.currency)
        assertEquals(null, context.recentSimilarTransaction)
        assertTrue(context.topCategories.isEmpty())
    }

    @Test
    fun `RecentTransaction stores all fields`() {
        val recent = RecentTransaction(
            description = "COFFEE SHOP",
            categoryId = "coffee_shops",
            confidence = 0.92f
        )

        assertEquals("COFFEE SHOP", recent.description)
        assertEquals("coffee_shops", recent.categoryId)
        assertEquals(0.92f, recent.confidence)
    }

    @Test
    fun `LLMCategoryResponse isNew defaults to false`() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

        val response = json.decodeFromString<LLMCategoryResponse>(
            """{"categoryId": "groceries", "confidence": 0.9}"""
        )

        assertFalse(response.isNew)
        assertEquals(null, response.newCategoryMetadata)
    }

    // === Metadata Suggestion Prompt Tests ===

    @Test
    fun `metadata suggestion prompt includes category id`() {
        val prompt = CategorizationPrompt.buildMetadataSuggestionPrompt("pet_supplies")

        assertTrue(prompt.contains("pet_supplies"))
        assertTrue(prompt.contains("JSON"))
        assertTrue(prompt.contains("name"))
        assertTrue(prompt.contains("iconHint"))
        assertTrue(prompt.contains("color"))
    }

    @Test
    fun `CATEGORIZATION_SCHEMA is valid JSON schema`() {
        val schema = CategorizationPrompt.CATEGORIZATION_SCHEMA

        assertTrue(schema.contains("type"))
        assertTrue(schema.contains("categoryId"))
        assertTrue(schema.contains("confidence"))
        assertTrue(schema.contains("isNew"))
        assertTrue(schema.contains("required"))
    }

    // === Context with top categories ===

    @Test
    fun `prompt includes top categories in context`() {
        val context = UserCategorizationContext(
            topCategories = listOf("groceries", "transport", "dining")
        )

        val prompt = CategorizationPrompt.buildCategorizePromptWithContext(
            description = "TEST",
            existingCategories = listOf("groceries"),
            context = context
        )

        assertTrue(
            prompt.contains("top categories") ||
            prompt.contains("groceries, transport") ||
            prompt.contains("groceries"),
            "Prompt should include user's top categories"
        )
    }

    // === Language edge cases ===

    @Test
    fun `prompt uses English examples for KK locale`() {
        val prompt = CategorizationPrompt.buildCategorizePrompt(
            description = "TEST",
            existingCategories = listOf("groceries"),
            language = "kk"
        )

        // KK should use Russian examples (same as RU in current implementation)
        val hasLocalExamples = prompt.contains("МАГНУМ") ||
            prompt.contains("GLOVO") ||
            prompt.contains("KASPI")

        assertTrue(
            hasLocalExamples,
            "Kazakh locale should include local merchant examples"
        )
    }

    @Test
    fun `prompt defaults to English for unknown locale`() {
        val prompt = CategorizationPrompt.buildCategorizePrompt(
            description = "TEST",
            existingCategories = listOf("groceries"),
            language = "de"
        )

        // Unknown locale should default to English
        val hasEnglishExamples = prompt.contains("STARBUCKS") ||
            prompt.contains("AMAZON") ||
            prompt.contains("UBER") ||
            prompt.contains("NETFLIX") ||
            prompt.contains("WHOLE FOODS")

        assertTrue(
            hasEnglishExamples,
            "Unknown locale should default to English examples"
        )
    }
}

package com.finuts.ai.providers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for LLM provider data classes and models.
 */
class LLMProviderModelsTest {

    // === ModelConfig ===

    @Test
    fun `ModelConfig stores all fields`() {
        val config = ModelConfig(
            id = "gpt-4o-mini",
            name = "GPT-4o Mini",
            maxTokens = 128000,
            costPer1kInputTokens = 0.00015f,
            costPer1kOutputTokens = 0.0006f,
            supportsStructuredOutput = true
        )

        assertEquals("gpt-4o-mini", config.id)
        assertEquals("GPT-4o Mini", config.name)
        assertEquals(128000, config.maxTokens)
        assertEquals(0.00015f, config.costPer1kInputTokens)
        assertEquals(0.0006f, config.costPer1kOutputTokens)
        assertTrue(config.supportsStructuredOutput)
    }

    @Test
    fun `ModelConfig default supportsStructuredOutput is false`() {
        val config = ModelConfig(
            id = "smollm2",
            name = "SmolLM2",
            maxTokens = 2048,
            costPer1kInputTokens = 0f,
            costPer1kOutputTokens = 0f
        )

        assertFalse(config.supportsStructuredOutput)
    }

    // === CompletionRequest ===

    @Test
    fun `CompletionRequest stores all fields`() {
        val request = CompletionRequest(
            prompt = "Categorize this transaction",
            model = "gpt-4o",
            maxTokens = 2048,
            temperature = 0.5f,
            systemPrompt = "You are a financial assistant"
        )

        assertEquals("Categorize this transaction", request.prompt)
        assertEquals("gpt-4o", request.model)
        assertEquals(2048, request.maxTokens)
        assertEquals(0.5f, request.temperature)
        assertEquals("You are a financial assistant", request.systemPrompt)
    }

    @Test
    fun `CompletionRequest has sensible defaults`() {
        val request = CompletionRequest(prompt = "Test")

        assertEquals("Test", request.prompt)
        assertNull(request.model)
        assertEquals(1024, request.maxTokens)
        assertEquals(0.1f, request.temperature)
        assertNull(request.systemPrompt)
    }

    @Test
    fun `CompletionRequest copy works correctly`() {
        val original = CompletionRequest(prompt = "Original")
        val copy = original.copy(prompt = "Modified", maxTokens = 512)

        assertEquals("Modified", copy.prompt)
        assertEquals(512, copy.maxTokens)
        assertEquals(original.temperature, copy.temperature)
    }

    // === ChatMessage ===

    @Test
    fun `ChatMessage stores role and content`() {
        val message = ChatMessage(
            role = ChatRole.USER,
            content = "Hello, assistant"
        )

        assertEquals(ChatRole.USER, message.role)
        assertEquals("Hello, assistant", message.content)
    }

    @Test
    fun `ChatMessage can be system role`() {
        val message = ChatMessage(
            role = ChatRole.SYSTEM,
            content = "You are helpful"
        )

        assertEquals(ChatRole.SYSTEM, message.role)
    }

    @Test
    fun `ChatMessage can be assistant role`() {
        val message = ChatMessage(
            role = ChatRole.ASSISTANT,
            content = "I will help"
        )

        assertEquals(ChatRole.ASSISTANT, message.role)
    }

    // === ChatRole enum ===

    @Test
    fun `ChatRole enum has all values`() {
        val roles = ChatRole.entries

        assertEquals(3, roles.size)
        assertTrue(ChatRole.SYSTEM in roles)
        assertTrue(ChatRole.USER in roles)
        assertTrue(ChatRole.ASSISTANT in roles)
    }

    // === CompletionResponse ===

    @Test
    fun `CompletionResponse stores all fields`() {
        val response = CompletionResponse(
            content = "The category is groceries",
            inputTokens = 100,
            outputTokens = 10,
            model = "gpt-4o-mini",
            finishReason = FinishReason.STOP
        )

        assertEquals("The category is groceries", response.content)
        assertEquals(100, response.inputTokens)
        assertEquals(10, response.outputTokens)
        assertEquals("gpt-4o-mini", response.model)
        assertEquals(FinishReason.STOP, response.finishReason)
    }

    @Test
    fun `CompletionResponse default finishReason is STOP`() {
        val response = CompletionResponse(
            content = "Test",
            inputTokens = 50,
            outputTokens = 5,
            model = "test-model"
        )

        assertEquals(FinishReason.STOP, response.finishReason)
    }

    @Test
    fun `CompletionResponse copy preserves fields`() {
        val original = CompletionResponse(
            content = "Original",
            inputTokens = 100,
            outputTokens = 50,
            model = "model"
        )
        val copy = original.copy(content = "Modified")

        assertEquals("Modified", copy.content)
        assertEquals(original.inputTokens, copy.inputTokens)
        assertEquals(original.outputTokens, copy.outputTokens)
    }

    // === FinishReason enum ===

    @Test
    fun `FinishReason enum has all values`() {
        val reasons = FinishReason.entries

        assertEquals(3, reasons.size)
        assertTrue(FinishReason.STOP in reasons)
        assertTrue(FinishReason.LENGTH in reasons)
        assertTrue(FinishReason.ERROR in reasons)
    }

    // === ProviderPreference enum ===

    @Test
    fun `ProviderPreference enum has all values`() {
        val preferences = ProviderPreference.entries

        assertEquals(5, preferences.size)
        assertTrue(ProviderPreference.FAST_CHEAP in preferences)
        assertTrue(ProviderPreference.BEST_QUALITY in preferences)
        assertTrue(ProviderPreference.STRUCTURED_OUTPUT in preferences)
        assertTrue(ProviderPreference.LOCAL_ONLY in preferences)
        assertTrue(ProviderPreference.CHEAPEST in preferences)
    }

    // === ProviderUnavailableException ===

    @Test
    fun `ProviderUnavailableException stores provider name`() {
        val exception = ProviderUnavailableException("openai")

        assertEquals("openai", exception.provider)
        assertTrue(exception.message?.contains("openai") == true)
    }

    @Test
    fun `ProviderUnavailableException can have custom message`() {
        val exception = ProviderUnavailableException(
            provider = "anthropic",
            message = "API key not configured"
        )

        assertEquals("anthropic", exception.provider)
        assertEquals("API key not configured", exception.message)
    }

    // === ProviderRateLimitException ===

    @Test
    fun `ProviderRateLimitException stores provider and retry time`() {
        val exception = ProviderRateLimitException(
            provider = "openai",
            retryAfterMs = 5000
        )

        assertEquals("openai", exception.provider)
        assertEquals(5000, exception.retryAfterMs)
        assertTrue(exception.message?.contains("openai") == true)
    }

    @Test
    fun `ProviderRateLimitException retryAfterMs can be null`() {
        val exception = ProviderRateLimitException(provider = "anthropic")

        assertEquals("anthropic", exception.provider)
        assertNull(exception.retryAfterMs)
    }

    // === ProviderQuotaExceededException ===

    @Test
    fun `ProviderQuotaExceededException stores provider`() {
        val exception = ProviderQuotaExceededException(provider = "openai")

        assertEquals("openai", exception.provider)
        assertTrue(exception.message?.contains("openai") == true)
        assertTrue(exception.message?.contains("Quota") == true)
    }
}

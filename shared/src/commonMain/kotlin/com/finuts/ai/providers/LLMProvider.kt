package com.finuts.ai.providers

import kotlinx.serialization.Serializable

/**
 * Abstract interface for LLM providers (OpenAI, Anthropic, On-device).
 *
 * Provides a unified API regardless of the underlying provider,
 * allowing easy switching and fallback between providers.
 */
interface LLMProvider {
    val name: String
    val availableModels: List<ModelConfig>

    /**
     * Check if the provider is available and configured.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Send a completion request.
     */
    suspend fun complete(request: CompletionRequest): CompletionResponse

    /**
     * Send a chat request with conversation history.
     */
    suspend fun chat(messages: List<ChatMessage>): CompletionResponse

    /**
     * Request structured JSON output.
     */
    suspend fun structuredOutput(
        prompt: String,
        schema: String // JSON Schema
    ): CompletionResponse
}

/**
 * Configuration for an LLM model.
 */
@Serializable
data class ModelConfig(
    val id: String,
    val name: String,
    val maxTokens: Int,
    val costPer1kInputTokens: Float,
    val costPer1kOutputTokens: Float,
    val supportsStructuredOutput: Boolean = false
)

/**
 * Request for LLM completion.
 */
data class CompletionRequest(
    val prompt: String,
    val model: String? = null, // null = use default
    val maxTokens: Int = 1024,
    val temperature: Float = 0.1f,
    val systemPrompt: String? = null
)

/**
 * Chat message for conversation.
 */
data class ChatMessage(
    val role: ChatRole,
    val content: String
)

enum class ChatRole { SYSTEM, USER, ASSISTANT }

/**
 * Response from LLM.
 */
data class CompletionResponse(
    val content: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val model: String,
    val finishReason: FinishReason = FinishReason.STOP
)

enum class FinishReason { STOP, LENGTH, ERROR }

/**
 * Provider preference for routing.
 */
enum class ProviderPreference {
    /** Fast and cheap - use for simple tasks */
    FAST_CHEAP,

    /** Best quality - use for complex analysis */
    BEST_QUALITY,

    /** Structured output required - use for document parsing */
    STRUCTURED_OUTPUT,

    /** Must be local only - for privacy-critical operations */
    LOCAL_ONLY,

    /** Cheapest available option */
    CHEAPEST
}

/**
 * Exceptions for provider errors.
 */
class ProviderUnavailableException(
    val provider: String,
    message: String = "Provider $provider is not available"
) : Exception(message)

class ProviderRateLimitException(
    val provider: String,
    val retryAfterMs: Long? = null
) : Exception("Rate limited by $provider")

class ProviderQuotaExceededException(
    val provider: String
) : Exception("Quota exceeded for $provider")

package com.finuts.ai.providers

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * LLM Provider implementation for Anthropic Claude API.
 *
 * Uses Ktor HTTP client for direct API calls (more reliable than SDK).
 *
 * Supports:
 * - Claude 3.5 Haiku (fast, cheap) - default for categorization
 * - Claude 3.5 Sonnet (balanced) - for complex analysis
 *
 * Privacy-first:
 * - All data is anonymized by PIIAnonymizer before sending
 * - No PII ever leaves the device
 */
class AnthropicProvider(
    private val apiKey: String
) : LLMProvider {

    private val log = Logger.withTag("AnthropicProvider")
    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    override val name: String = "anthropic-haiku"

    override val availableModels: List<ModelConfig> = listOf(
        ModelConfig(
            id = "claude-3-5-haiku-20241022",
            name = "Claude 3.5 Haiku",
            maxTokens = 8192,
            costPer1kInputTokens = 0.001f,
            costPer1kOutputTokens = 0.005f,
            supportsStructuredOutput = true
        ),
        ModelConfig(
            id = "claude-3-5-sonnet-20241022",
            name = "Claude 3.5 Sonnet",
            maxTokens = 8192,
            costPer1kInputTokens = 0.003f,
            costPer1kOutputTokens = 0.015f,
            supportsStructuredOutput = true
        )
    )

    companion object {
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val API_VERSION = "2023-06-01"
        private const val DEFAULT_MODEL = "claude-3-5-haiku-20241022"
    }

    override suspend fun isAvailable(): Boolean {
        return apiKey.isNotBlank()
    }

    override suspend fun complete(request: CompletionRequest): CompletionResponse {
        log.d { "complete: prompt length=${request.prompt.length}" }

        val model = request.model ?: DEFAULT_MODEL
        val apiRequest = AnthropicRequest(
            model = model,
            maxTokens = request.maxTokens,
            system = request.systemPrompt,
            messages = listOf(
                AnthropicMessage(role = "user", content = request.prompt)
            )
        )

        return try {
            val response = httpClient.post(API_URL) {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                    append("x-api-key", apiKey)
                    append("anthropic-version", API_VERSION)
                }
                setBody(json.encodeToString(AnthropicRequest.serializer(), apiRequest))
            }

            val responseBody = response.bodyAsText()
            val apiResponse = json.decodeFromString<AnthropicResponse>(responseBody)

            val textContent = apiResponse.content
                .filter { it.type == "text" }
                .joinToString("") { it.text ?: "" }

            log.d { "complete: response length=${textContent.length}" }

            CompletionResponse(
                content = textContent,
                inputTokens = apiResponse.usage.inputTokens,
                outputTokens = apiResponse.usage.outputTokens,
                model = apiResponse.model,
                finishReason = when (apiResponse.stopReason) {
                    "end_turn" -> FinishReason.STOP
                    "max_tokens" -> FinishReason.LENGTH
                    else -> FinishReason.STOP
                }
            )
        } catch (e: Exception) {
            log.e(e) { "complete: failed - ${e.message}" }
            throw e
        }
    }

    override suspend fun chat(messages: List<ChatMessage>): CompletionResponse {
        log.d { "chat: ${messages.size} messages" }

        val systemMessage = messages.find { it.role == ChatRole.SYSTEM }?.content
        val conversationMessages = messages
            .filter { it.role != ChatRole.SYSTEM }
            .map { msg ->
                AnthropicMessage(
                    role = when (msg.role) {
                        ChatRole.USER -> "user"
                        ChatRole.ASSISTANT -> "assistant"
                        ChatRole.SYSTEM -> "user"
                    },
                    content = msg.content
                )
            }

        val apiRequest = AnthropicRequest(
            model = DEFAULT_MODEL,
            maxTokens = 1024,
            system = systemMessage,
            messages = conversationMessages
        )

        return try {
            val response = httpClient.post(API_URL) {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                    append("x-api-key", apiKey)
                    append("anthropic-version", API_VERSION)
                }
                setBody(json.encodeToString(AnthropicRequest.serializer(), apiRequest))
            }

            val responseBody = response.bodyAsText()
            val apiResponse = json.decodeFromString<AnthropicResponse>(responseBody)

            val textContent = apiResponse.content
                .filter { it.type == "text" }
                .joinToString("") { it.text ?: "" }

            CompletionResponse(
                content = textContent,
                inputTokens = apiResponse.usage.inputTokens,
                outputTokens = apiResponse.usage.outputTokens,
                model = apiResponse.model,
                finishReason = FinishReason.STOP
            )
        } catch (e: Exception) {
            log.e(e) { "chat: failed - ${e.message}" }
            throw e
        }
    }

    override suspend fun structuredOutput(prompt: String, schema: String): CompletionResponse {
        log.d { "structuredOutput: prompt length=${prompt.length}" }

        val structuredPrompt = """
            |$prompt
            |
            |IMPORTANT: Respond ONLY with valid JSON matching this schema:
            |$schema
            |
            |Do not include any text before or after the JSON.
        """.trimMargin()

        return complete(
            CompletionRequest(
                prompt = structuredPrompt,
                model = DEFAULT_MODEL,
                maxTokens = 1024,
                temperature = 0.0f
            )
        )
    }
}

@Serializable
private data class AnthropicRequest(
    val model: String,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val system: String? = null,
    val messages: List<AnthropicMessage>
)

@Serializable
private data class AnthropicMessage(
    val role: String,
    val content: String
)

@Serializable
private data class AnthropicResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    @SerialName("stop_reason")
    val stopReason: String? = null,
    val usage: Usage
)

@Serializable
private data class ContentBlock(
    val type: String,
    val text: String? = null
)

@Serializable
private data class Usage(
    @SerialName("input_tokens")
    val inputTokens: Int,
    @SerialName("output_tokens")
    val outputTokens: Int
)

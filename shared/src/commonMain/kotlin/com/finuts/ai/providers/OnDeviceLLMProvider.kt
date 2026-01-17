package com.finuts.ai.providers

import co.touchlab.kermit.Logger
import com.finuts.ai.inference.InferenceEngine
import com.finuts.domain.model.ModelStatus
import com.finuts.domain.repository.ModelRepository
import kotlinx.coroutines.flow.first
import com.finuts.ai.providers.ModelConfig as ProviderModelConfig

/**
 * On-device LLM provider using GGUF models.
 *
 * Implements the LLMProvider interface for local inference,
 * enabling privacy-first AI without sending data to cloud APIs.
 *
 * Tier 1.5 in the categorization pipeline:
 * - Called after rule-based categorization fails
 * - Before cloud LLM (if available)
 * - Cost: $0 (runs locally)
 *
 * @property modelRepository Repository for model management
 * @property engine Platform-specific inference engine
 */
class OnDeviceLLMProvider(
    private val modelRepository: ModelRepository,
    private val engine: InferenceEngine
) : LLMProvider {

    private val log = Logger.withTag("OnDeviceLLMProvider")

    override val name: String = "on-device"

    override val availableModels: List<ProviderModelConfig> = emptyList()

    /**
     * Check if on-device inference is available.
     *
     * Returns true only if:
     * 1. A model is installed and selected
     * 2. Model status is READY
     * 3. Engine can load the model
     */
    override suspend fun isAvailable(): Boolean {
        val model = modelRepository.currentModel.first()
        if (model == null) {
            log.d { "isAvailable: no model selected" }
            return false
        }

        if (model.status != ModelStatus.READY) {
            log.d { "isAvailable: model status is ${model.status}" }
            return false
        }

        if (!engine.isModelLoaded()) {
            log.d { "isAvailable: loading model from ${model.filePath}" }
            val loaded = engine.loadModel(model.filePath)
            if (!loaded) {
                log.w { "isAvailable: failed to load model" }
                return false
            }
        }

        return true
    }

    /**
     * Generate completion using on-device model.
     *
     * @throws ProviderUnavailableException if model not available
     */
    override suspend fun complete(request: CompletionRequest): CompletionResponse {
        ensureModelLoaded()

        val prompt = buildPrompt(request)
        log.d { "complete: prompt length=${prompt.length}, maxTokens=${request.maxTokens}" }

        return try {
            val result = engine.complete(
                prompt = prompt,
                maxTokens = request.maxTokens,
                temperature = request.temperature
            )

            log.d {
                "complete: generated ${result.outputTokens} tokens " +
                    "in ${result.durationMs}ms (${result.tokensPerSecond} tok/s)"
            }

            CompletionResponse(
                content = result.text.trim(),
                inputTokens = result.inputTokens,
                outputTokens = result.outputTokens,
                model = name,
                finishReason = FinishReason.STOP
            )
        } catch (e: Exception) {
            log.e(e) { "complete: inference failed" }
            throw ProviderUnavailableException(name, "Inference failed: ${e.message}")
        }
    }

    /**
     * Chat completion with message history.
     *
     * Converts messages to a prompt format suitable for the model.
     */
    override suspend fun chat(messages: List<ChatMessage>): CompletionResponse {
        val prompt = buildChatPrompt(messages)
        return complete(CompletionRequest(prompt = prompt))
    }

    /**
     * Generate structured output (JSON).
     *
     * Includes the schema in the prompt to guide generation.
     */
    override suspend fun structuredOutput(
        prompt: String,
        schema: String
    ): CompletionResponse {
        val enhancedPrompt = """$prompt

Return ONLY valid JSON matching this schema:
$schema"""
        return complete(CompletionRequest(prompt = enhancedPrompt))
    }

    /**
     * Build prompt for completion request.
     *
     * NOTE: Do NOT wrap with ChatML here! LLM.swift applies the template automatically.
     * The system prompt is configured in Swift's detectTemplate() function.
     * We only send the raw user content here.
     */
    private fun buildPrompt(request: CompletionRequest): String {
        // Just return the user prompt - Swift side handles ChatML formatting
        return request.prompt
    }

    /**
     * Build prompt from chat messages.
     *
     * NOTE: LLM.swift handles template formatting, so we just concatenate
     * the user/assistant messages into a conversation format.
     * System prompt is set at model initialization time in Swift.
     */
    private fun buildChatPrompt(messages: List<ChatMessage>): String {
        // Filter out system messages (handled by Swift template)
        // Just return the last user message for completion
        val userMessages = messages.filter { it.role == ChatRole.USER }
        return userMessages.lastOrNull()?.content ?: ""
    }

    /**
     * Ensure model is loaded before inference.
     */
    private suspend fun ensureModelLoaded() {
        if (!isAvailable()) {
            throw ProviderUnavailableException(
                name,
                "No on-device model available"
            )
        }
    }

    companion object {
        // Note: System prompt is now set in Swift's SwiftLLMBridge.detectTemplate()
        // This allows proper ChatML template formatting without double-wrapping
    }
}

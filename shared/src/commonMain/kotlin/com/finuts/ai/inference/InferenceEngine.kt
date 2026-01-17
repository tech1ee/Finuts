package com.finuts.ai.inference

/**
 * Interface for on-device LLM inference engine.
 *
 * Platform-specific implementations use:
 * - Android: llama.cpp via kotlinllamacpp
 * - iOS: llama.cpp via LLM.swift bridge
 *
 * This interface abstracts the inference runtime,
 * allowing the OnDeviceLLMProvider to work with any GGUF model.
 */
interface InferenceEngine {
    /**
     * Load a model from the given file path.
     * @param modelPath Absolute path to the GGUF model file
     * @return true if model loaded successfully
     */
    suspend fun loadModel(modelPath: String): Boolean

    /**
     * Check if a model is currently loaded and ready.
     */
    fun isModelLoaded(): Boolean

    /**
     * Unload the current model to free memory.
     */
    suspend fun unloadModel()

    /**
     * Generate completion for the given prompt.
     *
     * @param prompt The input prompt (already formatted for the model)
     * @param maxTokens Maximum tokens to generate
     * @param temperature Sampling temperature (0.0-1.0)
     * @param stopSequences Sequences that stop generation
     * @return InferenceResult with generated text and metrics
     */
    suspend fun complete(
        prompt: String,
        maxTokens: Int = 256,
        temperature: Float = 0.1f,
        stopSequences: List<String> = emptyList()
    ): InferenceResult

    /**
     * Count tokens in text (for estimation).
     * Uses model's tokenizer if loaded, otherwise rough estimate.
     */
    fun tokenCount(text: String): Int
}

/**
 * Result of an inference operation.
 *
 * @property text Generated text
 * @property inputTokens Number of input tokens processed
 * @property outputTokens Number of output tokens generated
 * @property durationMs Time taken for inference in milliseconds
 * @property tokensPerSecond Generation speed
 */
data class InferenceResult(
    val text: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val durationMs: Long,
    val tokensPerSecond: Float
)

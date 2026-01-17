package com.finuts.ai.inference

/**
 * Bridge interface for Swift LLM implementation.
 *
 * Swift code implements this interface and registers it with [LLMBridgeRegistry].
 * Kotlin [GGUFInferenceEngine] uses the registered bridge for inference.
 *
 * This pattern allows Swift to use native LLM libraries (LLM.swift, llama.cpp)
 * while exposing functionality to Kotlin through a clean interface.
 */
interface LLMBridge {
    /**
     * Load GGUF model from file path.
     * @param modelPath Absolute path to .gguf file
     * @return true if loaded successfully
     */
    fun loadModel(modelPath: String): Boolean

    /**
     * Check if model is currently loaded.
     */
    fun isModelLoaded(): Boolean

    /**
     * Unload current model and free memory.
     */
    fun unloadModel()

    /**
     * Generate completion for prompt.
     *
     * @param prompt Input text
     * @param maxTokens Maximum tokens to generate
     * @param temperature Sampling temperature (0.0-1.0)
     * @return Generated text or null if failed
     */
    fun complete(
        prompt: String,
        maxTokens: Int,
        temperature: Float
    ): LLMBridgeResult?

    /**
     * Estimate token count for text.
     */
    fun tokenCount(text: String): Int
}

/**
 * Result from LLM completion.
 */
data class LLMBridgeResult(
    val text: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val durationMs: Long
)

/**
 * Registry for Swift LLM bridge.
 *
 * Swift code calls [register] at app startup to provide LLM implementation.
 * Kotlin [GGUFInferenceEngine] retrieves the bridge via [getBridge].
 *
 * Thread-safe: Uses @Volatile for proper visibility across threads.
 *
 * Usage from Swift:
 * ```swift
 * import shared
 *
 * class SwiftLLMBridge: LLMBridge {
 *     private var llm: LLM?
 *
 *     func loadModel(modelPath: String) -> Bool {
 *         llm = try? LLM(from: URL(fileURLWithPath: modelPath))
 *         return llm != nil
 *     }
 *     // ... implement other methods
 * }
 *
 * // In AppDelegate or IosEntry:
 * LLMBridgeRegistry.shared.register(bridge: SwiftLLMBridge())
 * ```
 */
object LLMBridgeRegistry {
    private val logger = co.touchlab.kermit.Logger.withTag("LLMBridgeRegistry")

    // FIX BUG #4: Use @Volatile for thread-safe access across threads
    // Note: In Kotlin/Native, volatile is the default behavior for object references
    // due to the new memory model (since Kotlin 1.7.20)
    @kotlin.concurrent.Volatile
    private var bridge: LLMBridge? = null

    /**
     * Register Swift LLM bridge implementation.
     * Call this from Swift at app startup before any inference calls.
     */
    fun register(bridge: LLMBridge) {
        logger.i { "Registering LLMBridge: ${bridge::class.simpleName}" }
        this.bridge = bridge
        logger.i { "LLMBridge registered successfully" }
    }

    /**
     * Get registered bridge, or null if not registered.
     * Thread-safe read operation.
     */
    fun getBridge(): LLMBridge? {
        val b = bridge
        if (b == null) {
            logger.w { "getBridge called but no bridge registered" }
        }
        return b
    }

    /**
     * Check if bridge is registered.
     * Thread-safe read operation.
     */
    fun isRegistered(): Boolean = bridge != null
}

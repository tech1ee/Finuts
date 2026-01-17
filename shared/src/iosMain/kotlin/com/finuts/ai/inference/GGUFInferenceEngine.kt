package com.finuts.ai.inference

import co.touchlab.kermit.Logger

/**
 * iOS implementation of InferenceEngine.
 *
 * This implementation uses [LLMBridgeRegistry] to access Swift LLM code.
 * Swift must register an [LLMBridge] implementation at app startup.
 *
 * ## Setup Required (in iosApp):
 *
 * 1. Add LLM.swift package dependency in Xcode:
 *    File → Add Package Dependencies → https://github.com/eastriverlee/LLM.swift
 *
 * 2. Create Swift bridge in iosApp/iosApp/LLMBridge/SwiftLLMBridge.swift:
 *    ```swift
 *    import LLM
 *    import shared
 *
 *    class SwiftLLMBridge: LLMBridge {
 *        private var llm: LLM?
 *
 *        func loadModel(modelPath: String) -> KotlinBoolean {
 *            do {
 *                llm = try LLM(from: URL(fileURLWithPath: modelPath), template: .gemma)
 *                return KotlinBoolean(bool: llm?.isAvailable ?? false)
 *            } catch {
 *                return KotlinBoolean(bool: false)
 *            }
 *        }
 *
 *        func isModelLoaded() -> KotlinBoolean {
 *            return KotlinBoolean(bool: llm?.isAvailable ?? false)
 *        }
 *
 *        func unloadModel() {
 *            llm = nil
 *        }
 *
 *        func complete(prompt: String, maxTokens: Int32, temperature: Float) -> LLMBridgeResult? {
 *            guard let llm = llm else { return nil }
 *            let start = Date()
 *            let response = llm.getCompletion(from: prompt)
 *            let duration = Int64(Date().timeIntervalSince(start) * 1000)
 *            return LLMBridgeResult(
 *                text: response,
 *                inputTokens: Int32(prompt.count / 4),
 *                outputTokens: Int32(response.count / 4),
 *                durationMs: duration
 *            )
 *        }
 *
 *        func tokenCount(text: String) -> Int32 {
 *            let hasNonLatin = text.unicodeScalars.contains { $0.value > 127 }
 *            let charsPerToken: Float = hasNonLatin ? 2.5 : 4.0
 *            return max(1, Int32(Float(text.count) / charsPerToken))
 *        }
 *    }
 *    ```
 *
 * 3. Register bridge in iosApp/iosApp/ContentView.swift (before ComposeView):
 *    ```swift
 *    init() {
 *        LLMBridgeRegistry.shared.register(bridge: SwiftLLMBridge())
 *    }
 *    ```
 */
class GGUFInferenceEngine : InferenceEngine {

    private val logger = Logger.withTag("GGUFInferenceEngine")

    override suspend fun loadModel(modelPath: String): Boolean {
        val bridge = LLMBridgeRegistry.getBridge()
        if (bridge == null) {
            logger.e { "LLMBridge not registered. See GGUFInferenceEngine docs for setup." }
            throw IllegalStateException(
                "LLMBridge not registered. " +
                "Swift must call LLMBridgeRegistry.shared.register() at app startup. " +
                "See GGUFInferenceEngine documentation for setup instructions."
            )
        }

        logger.d { "Loading model from: $modelPath" }
        return bridge.loadModel(modelPath).also { success ->
            if (success) {
                logger.i { "Model loaded successfully: $modelPath" }
            } else {
                logger.w { "Failed to load model: $modelPath" }
            }
        }
    }

    override fun isModelLoaded(): Boolean {
        return LLMBridgeRegistry.getBridge()?.isModelLoaded() ?: false
    }

    override suspend fun unloadModel() {
        logger.d { "Unloading model..." }
        LLMBridgeRegistry.getBridge()?.unloadModel()
        logger.d { "Model unloaded" }
    }

    override suspend fun complete(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        stopSequences: List<String>
    ): InferenceResult {
        val bridge = LLMBridgeRegistry.getBridge()
            ?: throw IllegalStateException(
                "LLMBridge not registered. " +
                "Swift must call LLMBridgeRegistry.shared.register() at app startup."
            )

        if (!bridge.isModelLoaded()) {
            throw IllegalStateException("Model not loaded. Call loadModel() first.")
        }

        logger.d { "Starting completion. Prompt length: ${prompt.length}, maxTokens: $maxTokens" }

        val result = bridge.complete(prompt, maxTokens, temperature)
            ?: throw IllegalStateException("Inference failed - bridge returned null")

        var text = result.text

        // Apply stop sequences
        for (stop in stopSequences) {
            val idx = text.indexOf(stop)
            if (idx >= 0) {
                text = text.substring(0, idx)
                logger.d { "Stop sequence detected: $stop" }
                break
            }
        }

        val tokensPerSecond = if (result.durationMs > 0) {
            result.outputTokens * 1000f / result.durationMs
        } else {
            0f
        }

        logger.i {
            "Completion done. Output tokens: ${result.outputTokens}, " +
            "Duration: ${result.durationMs}ms, Speed: $tokensPerSecond tok/s"
        }

        return InferenceResult(
            text = text.trim(),
            inputTokens = result.inputTokens,
            outputTokens = result.outputTokens,
            durationMs = result.durationMs,
            tokensPerSecond = tokensPerSecond
        )
    }

    override fun tokenCount(text: String): Int {
        return LLMBridgeRegistry.getBridge()?.tokenCount(text)
            ?: estimateTokenCount(text)
    }

    private fun estimateTokenCount(text: String): Int {
        val hasNonLatin = text.any { it.code > 127 }
        val charsPerToken = if (hasNonLatin) 2.5f else 4f
        return (text.length / charsPerToken).toInt().coerceAtLeast(1)
    }
}

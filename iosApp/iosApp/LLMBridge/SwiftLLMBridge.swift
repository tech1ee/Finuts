import Foundation
import os.log
import LLM
import composeApp

/// Swift implementation of LLMBridge for on-device LLM inference.
///
/// Uses LLM.swift library for GGUF model inference on Apple Silicon.
/// Register this bridge at app startup before using GGUFInferenceEngine.
class SwiftLLMBridge: LLMBridge {

    private static let logger = Logger(
        subsystem: "com.finuts.app",
        category: "LLMBridge"
    )

    private var llm: LLM?
    private var currentModelPath: String?
    private let queue = DispatchQueue(label: "com.finuts.llmbridge", qos: .userInitiated)

    // FIX BUG #6: DispatchSemaphore for serializing LLM calls
    // LLM.swift has internal lock that returns "LLM is being used" for concurrent calls
    // We use semaphore with value 1 to ensure only ONE inference runs at a time
    private let inferenceSemaphore = DispatchSemaphore(value: 1)

    // FIX: Class wrapper for result to ensure reference semantics in Task closure
    private class ResultBox {
        var response: String = ""
        var completed: Bool = false
    }

    init() {
        Self.logger.info("SwiftLLMBridge initialized")
    }

    // MARK: - LLMBridge Protocol

    func loadModel(modelPath: String) -> Bool {
        Self.logger.info("loadModel called with path: \(modelPath)")

        let url = URL(fileURLWithPath: modelPath)

        // Check file exists
        guard FileManager.default.fileExists(atPath: modelPath) else {
            Self.logger.error("Model file not found: \(modelPath)")
            return false
        }

        // Get file size for logging
        if let attrs = try? FileManager.default.attributesOfItem(atPath: modelPath),
           let fileSize = attrs[.size] as? Int64 {
            Self.logger.info("Model file size: \(fileSize) bytes (\(fileSize / 1_000_000) MB)")
        }

        // FIX BUG #5: Auto-detect template from model filename
        // FIX BUG #7: Include system prompt for financial categorization
        let filename = url.lastPathComponent.lowercased()
        let systemPrompt = """
            You are a financial transaction categorizer. Your task is to categorize bank transactions.
            Always respond with valid JSON only. No explanations, no additional text.
            """
        let template = detectTemplate(from: filename, systemPrompt: systemPrompt)
        Self.logger.info("Auto-detected template for model: \(filename)")

        Self.logger.info("Attempting to load GGUF model...")
        let startTime = Date()

        // LLM initializer returns optional
        if let loadedLLM = LLM(from: url, template: template) {
            llm = loadedLLM
            currentModelPath = modelPath
            let loadTime = Date().timeIntervalSince(startTime)
            Self.logger.info("Model loaded successfully in \(String(format: "%.2f", loadTime))s")
            return true
        } else {
            let loadTime = Date().timeIntervalSince(startTime)
            Self.logger.error("Failed to load model after \(String(format: "%.2f", loadTime))s")
            llm = nil
            currentModelPath = nil
            return false
        }
    }

    /// Detect LLM template from model filename.
    /// Different models require different prompt templates for proper formatting.
    /// The system prompt is included in the template for chat-style models.
    private func detectTemplate(from filename: String, systemPrompt: String) -> Template {
        // SmolLM models use ChatML-style template
        // Note: "compact" is our internal name for SmolLM2-135M-Instruct
        if filename.contains("smollm") || filename.contains("smol-lm") ||
           filename.contains("smol_lm") || filename.contains("compact") {
            Self.logger.info("SmolLM model detected - using chatML template with system prompt")
            return .chatML(systemPrompt)
        }

        // Gemma models - use chatML with system prompt (Gemma 2+ supports it)
        if filename.contains("gemma") {
            Self.logger.info("Gemma model detected - using chatML template with system prompt")
            return .chatML(systemPrompt)
        }

        // Llama models - use chatML for Llama 2+ instruction-tuned
        if filename.contains("llama") {
            Self.logger.info("Llama model detected - using chatML template with system prompt")
            return .chatML(systemPrompt)
        }

        // Mistral models - use chatML for instruction-tuned variants
        if filename.contains("mistral") {
            Self.logger.info("Mistral model detected - using chatML template with system prompt")
            return .chatML(systemPrompt)
        }

        // Phi models use ChatML (Phi-3 instruction format)
        if filename.contains("phi") {
            Self.logger.info("Phi model detected - using chatML template with system prompt")
            return .chatML(systemPrompt)
        }

        // Qwen models use ChatML
        if filename.contains("qwen") {
            Self.logger.info("Qwen model detected - using chatML template with system prompt")
            return .chatML(systemPrompt)
        }

        // Default to chatML for unknown models (most widely compatible)
        Self.logger.warning("Unknown model type, defaulting to chatML template with system prompt")
        return .chatML(systemPrompt)
    }

    func isModelLoaded() -> Bool {
        let loaded = llm != nil
        Self.logger.debug("isModelLoaded: \(loaded)")
        return loaded
    }

    func unloadModel() {
        Self.logger.info("Unloading model...")
        llm = nil
        currentModelPath = nil
        Self.logger.info("Model unloaded successfully")
    }

    func complete(
        prompt: String,
        maxTokens: Int32,
        temperature: Float
    ) -> LLMBridgeResult? {
        return completeWithRetry(prompt: prompt, maxTokens: maxTokens, temperature: temperature, retryCount: 0)
    }

    /// Internal completion with retry counter to prevent infinite loops.
    /// Max 2 retries (3 total attempts).
    private func completeWithRetry(
        prompt: String,
        maxTokens: Int32,
        temperature: Float,
        retryCount: Int
    ) -> LLMBridgeResult? {
        let maxRetries = 2

        // DEBUG: print() always outputs, unlike os_log which can be filtered
        print("[SwiftLLMBridge] complete called - prompt length: \(prompt.count), retry: \(retryCount)/\(maxRetries)")
        Self.logger.info("complete called - prompt length: \(prompt.count), maxTokens: \(maxTokens), temp: \(temperature), retry: \(retryCount)/\(maxRetries)")

        guard let llm = llm else {
            print("[SwiftLLMBridge] ERROR: No model loaded")
            Self.logger.error("complete failed: No model loaded")
            return nil
        }

        let promptPreview = String(prompt.prefix(100))
        Self.logger.debug("Prompt preview: \(promptPreview)...")

        // FIX BUG #6: Use semaphore to serialize LLM calls COMPLETELY
        // We wait BEFORE starting inference to ensure only ONE inference at a time
        print("[SwiftLLMBridge] Waiting for inference semaphore...")
        Self.logger.info("Waiting for inference semaphore...")
        let waitResult = inferenceSemaphore.wait(timeout: .now() + 120)  // 2 min timeout

        if waitResult == .timedOut {
            print("[SwiftLLMBridge] ERROR: Semaphore wait timed out (previous inference stuck)")
            Self.logger.error("Semaphore wait timed out - previous inference may be stuck")
            return nil
        }

        print("[SwiftLLMBridge] Semaphore acquired, starting inference...")
        Self.logger.info("Semaphore acquired, starting inference...")

        // FIX: Use class wrapper for guaranteed reference semantics
        let resultBox = ResultBox()

        let group = DispatchGroup()
        group.enter()

        let startTime = Date()

        // Run inference on global queue to avoid blocking issues
        // Capture llm and resultBox (both are reference types)
        DispatchQueue.global(qos: .userInitiated).async { [llm, resultBox] in
            print("[SwiftLLMBridge] Inside DispatchQueue.global, creating Task...")

            // Create a new Task on this thread's context
            Task {
                print("[SwiftLLMBridge] Inside Task, calling llm.getCompletion...")
                let output = await llm.getCompletion(from: prompt)
                print("[SwiftLLMBridge] llm.getCompletion returned: '\(String(output.prefix(100)))'")

                resultBox.response = output
                resultBox.completed = true
                group.leave()
            }
        }

        // Wait for completion with timeout (120 seconds for real inference on simulator)
        print("[SwiftLLMBridge] Waiting for inference completion (timeout: 120s)...")
        let timeoutResult = group.wait(timeout: .now() + 120)

        let durationMs = Int64(Date().timeIntervalSince(startTime) * 1000)
        let response = resultBox.response

        // CRITICAL: Release semaphore AFTER inference completes
        print("[SwiftLLMBridge] Releasing semaphore after inference")
        inferenceSemaphore.signal()

        print("[SwiftLLMBridge] Wait finished. Timeout: \(timeoutResult == .timedOut), Duration: \(durationMs)ms, Response length: \(response.count)")

        if timeoutResult == .timedOut {
            print("[SwiftLLMBridge] ERROR: Inference timed out after 120 seconds")
            Self.logger.error("Inference timed out after 120 seconds")
            return LLMBridgeResult(
                text: "",
                inputTokens: estimateTokens(text: prompt),
                outputTokens: 1,
                durationMs: durationMs
            )
        }

        // FIX BUG #6: Check for "LLM is being used" response
        // This should NOT happen now with semaphore, but keep as safety check
        if response == "LLM is being used" {
            print("[SwiftLLMBridge] WARNING: LLM returned busy signal despite semaphore! retry \(retryCount + 1)/\(maxRetries)")
            Self.logger.warning("LLM returned busy signal despite semaphore, retry \(retryCount + 1)/\(maxRetries)")
            if retryCount < maxRetries {
                Thread.sleep(forTimeInterval: 2.0)  // Longer delay between retries
                return completeWithRetry(prompt: prompt, maxTokens: maxTokens, temperature: temperature, retryCount: retryCount + 1)
            } else {
                print("[SwiftLLMBridge] ERROR: LLM still busy after \(maxRetries) retries, giving up")
                Self.logger.error("LLM still busy after \(maxRetries) retries, giving up")
                return nil
            }
        }

        // Estimate tokens
        let inputTokens = estimateTokens(text: prompt)
        let outputTokens = estimateTokens(text: response)
        let tokensPerSecond = durationMs > 0 ? Float(outputTokens) * 1000.0 / Float(durationMs) : 0

        print("[SwiftLLMBridge] Inference complete: \(outputTokens) tokens in \(durationMs)ms (\(String(format: "%.1f", tokensPerSecond)) tok/s)")
        Self.logger.info("""
            Inference complete:
            - Duration: \(durationMs)ms
            - Input tokens: \(inputTokens)
            - Output tokens: \(outputTokens)
            - Speed: \(String(format: "%.1f", tokensPerSecond)) tok/s
            """)

        let responsePreview = String(response.prefix(200))
        print("[SwiftLLMBridge] Response preview: \(responsePreview)")
        Self.logger.debug("Response preview: \(responsePreview)...")

        return LLMBridgeResult(
            text: response,
            inputTokens: inputTokens,
            outputTokens: outputTokens,
            durationMs: durationMs
        )
    }

    func tokenCount(text: String) -> Int32 {
        let count = estimateTokens(text: text)
        Self.logger.debug("tokenCount for \(text.count) chars: \(count) tokens")
        return count
    }

    // MARK: - Private

    private func estimateTokens(text: String) -> Int32 {
        // Rough estimate: ~4 chars per token for English, ~2.5 for non-Latin
        let hasNonLatin = text.unicodeScalars.contains { $0.value > 127 }
        let charsPerToken: Float = hasNonLatin ? 2.5 : 4.0
        return max(1, Int32(Float(text.count) / charsPerToken))
    }
}

package com.finuts.ai.inference

import android.content.Context
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.nehuatl.llamacpp.LlamaHelper
import kotlin.system.measureTimeMillis

/**
 * Android implementation of InferenceEngine using kotlinllamacpp (llama.cpp binding).
 *
 * This class wraps LlamaHelper to provide on-device LLM inference for GGUF models.
 * Optimized for Arm architecture with automatic feature detection (i8mm, dotprod).
 *
 * @param context Android context for ContentResolver access
 */
class GGUFInferenceEngine(
    private val context: Context
) : InferenceEngine {

    private val logger = Logger.withTag("GGUFInferenceEngine")

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _eventFlow = MutableSharedFlow<LlamaHelper.LLMEvent>(
        extraBufferCapacity = 256
    )
    val eventFlow = _eventFlow.asSharedFlow()

    private var llamaHelper: LlamaHelper? = null
    private var isLoaded = false
    private var currentModelPath: String? = null
    private var contextId: Long = -1L

    override suspend fun loadModel(modelPath: String): Boolean {
        return try {
            logger.d { "Loading model from: $modelPath" }

            // Release previous model if any
            if (isLoaded) {
                unloadModel()
            }

            llamaHelper = LlamaHelper(
                context.contentResolver,
                scope,
                _eventFlow
            )

            var loadSuccess = false
            llamaHelper?.load(modelPath, contextLength = 2048) { id ->
                contextId = id
                loadSuccess = id >= 0L
                logger.d { "Model loaded with contextId: $id" }
            }

            // Give time for the callback to execute
            kotlinx.coroutines.delay(200)

            isLoaded = loadSuccess
            if (loadSuccess) {
                currentModelPath = modelPath
                logger.i { "Model loaded successfully: $modelPath" }
            } else {
                logger.w { "Failed to load model: $modelPath" }
            }

            loadSuccess
        } catch (e: Exception) {
            logger.e(e) { "Exception loading model: ${e.message}" }
            isLoaded = false
            false
        }
    }

    override fun isModelLoaded(): Boolean = isLoaded

    override suspend fun unloadModel() {
        logger.d { "Unloading model..." }
        try {
            llamaHelper?.abort()
            llamaHelper?.release()
        } catch (e: Exception) {
            logger.w(e) { "Error releasing model: ${e.message}" }
        }
        llamaHelper = null
        isLoaded = false
        currentModelPath = null
        contextId = -1L
        logger.d { "Model unloaded" }
    }

    override suspend fun complete(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        stopSequences: List<String>
    ): InferenceResult {
        val helper = llamaHelper
            ?: throw IllegalStateException("Model not loaded. Call loadModel() first.")

        if (!isLoaded) {
            throw IllegalStateException("Model is not in loaded state")
        }

        logger.d { "Starting completion. Prompt length: ${prompt.length}, maxTokens: $maxTokens" }

        val outputBuffer = StringBuilder()
        var outputTokenCount = 0
        var finished = false

        val duration = measureTimeMillis {
            // Start prediction
            helper.predict(prompt)

            // Collect tokens with timeout (30 seconds max)
            withTimeoutOrNull(30_000L) {
                _eventFlow.collect { event ->
                    when (event) {
                        is LlamaHelper.LLMEvent.Started -> {
                            logger.v { "Inference started" }
                        }
                        is LlamaHelper.LLMEvent.Ongoing -> {
                            outputBuffer.append(event.word)
                            outputTokenCount++

                            // Check for stop sequences
                            val currentText = outputBuffer.toString()
                            for (stop in stopSequences) {
                                if (currentText.endsWith(stop)) {
                                    logger.d { "Stop sequence detected: $stop" }
                                    helper.stopPrediction()
                                    finished = true
                                    return@collect
                                }
                            }

                            // Check max tokens
                            if (outputTokenCount >= maxTokens) {
                                logger.d { "Max tokens reached: $maxTokens" }
                                helper.stopPrediction()
                                finished = true
                                return@collect
                            }
                        }
                        is LlamaHelper.LLMEvent.Done -> {
                            logger.v { "Inference completed normally" }
                            finished = true
                            return@collect
                        }
                        is LlamaHelper.LLMEvent.Error -> {
                            logger.w { "Inference error: ${event.message}" }
                            finished = true
                            return@collect
                        }
                        else -> {
                            // Handle any other event types (e.g., Loaded)
                            logger.v { "Other event: $event" }
                        }
                    }
                }
            }
        }

        var text = outputBuffer.toString()

        // Trim stop sequences from output
        for (stop in stopSequences) {
            val idx = text.lastIndexOf(stop)
            if (idx >= 0) {
                text = text.substring(0, idx)
                break
            }
        }

        val inputTokens = tokenCount(prompt)

        logger.i {
            "Completion done. Output tokens: $outputTokenCount, " +
                "Duration: ${duration}ms, Speed: ${outputTokenCount * 1000f / duration} tok/s"
        }

        return InferenceResult(
            text = text.trim(),
            inputTokens = inputTokens,
            outputTokens = outputTokenCount,
            durationMs = duration,
            tokensPerSecond = if (duration > 0) {
                (outputTokenCount * 1000f / duration)
            } else 0f
        )
    }

    override fun tokenCount(text: String): Int {
        // Rough estimate: ~4 chars per token for English
        // Adjust for non-Latin scripts (Russian, Kazakh) which use ~2.5 chars per token
        val hasNonLatin = text.any { it.code > 127 }
        val charsPerToken = if (hasNonLatin) 2.5f else 4f
        return (text.length / charsPerToken).toInt().coerceAtLeast(1)
    }
}

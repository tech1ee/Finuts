package com.finuts.ai.orchestration

import co.touchlab.kermit.Logger
import com.finuts.ai.cost.AICostTracker
import com.finuts.ai.model.AIResult
import com.finuts.ai.privacy.PIIAnonymizer
import com.finuts.ai.providers.CompletionRequest
import com.finuts.ai.providers.CompletionResponse
import com.finuts.ai.providers.LLMProviderFactory
import com.finuts.ai.providers.ProviderPreference
import com.finuts.ai.providers.ProviderQuotaExceededException
import com.finuts.ai.providers.ProviderRateLimitException
import com.finuts.ai.providers.ProviderUnavailableException
import kotlinx.coroutines.delay

/**
 * AI Orchestrator - central routing and execution layer.
 *
 * Responsibilities:
 * - Route tasks to optimal provider based on requirements
 * - Apply PII anonymization before cloud requests
 * - Track and enforce cost limits
 * - Handle errors with fallback strategies
 * - Manage retries and rate limiting
 */
class AIOrchestrator(
    private val providerFactory: LLMProviderFactory,
    private val costTracker: AICostTracker,
    private val anonymizer: PIIAnonymizer
) {
    private val log = Logger.withTag("AIOrchestrator")
    /**
     * Execute an AI task with automatic provider selection and error handling.
     */
    suspend fun execute(
        task: AITask,
        options: ExecutionOptions = ExecutionOptions.DEFAULT
    ): AIResult<CompletionResponse> {
        log.d { "execute: preference=${task.preference}, model=${task.model}, promptLen=${task.prompt.length}" }

        // 1. Check cost budget
        if (!costTracker.canExecute(task.estimatedCost)) {
            log.w { "execute: COST_LIMIT - estimated=${task.estimatedCost}, budget exceeded" }
            return AIResult.CostLimitExceeded
        }
        log.d { "execute: cost check passed, estimated=${task.estimatedCost}" }

        // 2. Anonymize if task requires cloud
        val processedTask = if (task.requiresAnonymization) {
            val result = anonymizeTask(task)
            log.d { "execute: anonymized, piiCount=${result.anonymizationMapping?.size ?: 0}" }
            result
        } else {
            log.d { "execute: no anonymization required" }
            task
        }

        // 3. Get providers with fallback
        val providers = providerFactory.getProvidersWithFallback(task.preference)
        if (providers.isEmpty()) {
            log.e { "execute: NO_PROVIDERS for preference=${task.preference}" }
            return AIResult.ProviderUnavailable
        }
        log.i { "execute: providers=${providers.map { it.name }}" }

        // 4. Try each provider with retry logic
        var lastError: Throwable? = null
        for ((providerIndex, provider) in providers.withIndex()) {
            log.d { "execute: trying provider=${provider.name} (${providerIndex + 1}/${providers.size})" }

            for (attempt in 1..options.maxRetries) {
                log.d { "execute: attempt $attempt/${options.maxRetries} with ${provider.name}" }
                try {
                    val request = CompletionRequest(
                        prompt = processedTask.prompt,
                        model = processedTask.model,
                        maxTokens = processedTask.maxTokens,
                        temperature = processedTask.temperature,
                        systemPrompt = processedTask.systemPrompt
                    )

                    val startTime = kotlin.time.TimeSource.Monotonic.markNow()
                    val response = provider.complete(request)
                    val latencyMs = startTime.elapsedNow().inWholeMilliseconds

                    log.i {
                        "execute: SUCCESS provider=${provider.name}, " +
                            "model=${response.model}, " +
                            "tokens=${response.inputTokens}+${response.outputTokens}, " +
                            "latency=${latencyMs}ms"
                    }

                    // Record cost
                    costTracker.record(
                        inputTokens = response.inputTokens,
                        outputTokens = response.outputTokens,
                        model = response.model
                    )

                    // De-anonymize if needed (use processedTask which contains the mapping)
                    val finalResponse = if (task.requiresAnonymization && processedTask.anonymizationMapping != null) {
                        log.d { "execute: de-anonymizing response" }
                        response.copy(
                            content = anonymizer.deanonymize(response.content, processedTask.anonymizationMapping!!)
                        )
                    } else {
                        response
                    }

                    return AIResult.Success(
                        data = finalResponse,
                        tokensUsed = response.inputTokens + response.outputTokens
                    )
                } catch (e: ProviderRateLimitException) {
                    // Wait and retry
                    val waitTime = e.retryAfterMs ?: (1000L * attempt)
                    log.w { "execute: RATE_LIMIT provider=${provider.name}, waiting ${waitTime}ms" }
                    delay(waitTime)
                    lastError = e
                } catch (e: ProviderQuotaExceededException) {
                    // Try next provider
                    log.w { "execute: QUOTA_EXCEEDED provider=${provider.name}, trying next" }
                    lastError = e
                    break
                } catch (e: ProviderUnavailableException) {
                    // Try next provider
                    log.w { "execute: UNAVAILABLE provider=${provider.name}, trying next" }
                    lastError = e
                    break
                } catch (e: Exception) {
                    log.e(e) { "execute: ERROR provider=${provider.name}, attempt=$attempt - ${e.message}" }
                    lastError = e
                    if (attempt == options.maxRetries) break
                    delay(1000L * attempt)
                }
            }
        }

        log.e { "execute: ALL_PROVIDERS_FAILED, lastError=${lastError?.message}" }
        return AIResult.Error(
            message = lastError?.message ?: "All providers failed",
            cause = lastError
        )
    }

    /**
     * Execute with structured JSON output.
     */
    suspend fun executeStructured(
        task: AITask,
        jsonSchema: String,
        options: ExecutionOptions = ExecutionOptions.DEFAULT
    ): AIResult<CompletionResponse> {
        log.d { "executeStructured: schemaLen=${jsonSchema.length}" }
        // Force structured output provider
        val structuredTask = task.copy(preference = ProviderPreference.STRUCTURED_OUTPUT)
        return execute(structuredTask, options)
    }

    private fun anonymizeTask(task: AITask): AITask {
        val result = anonymizer.anonymize(task.prompt)
        log.d {
            "anonymizeTask: wasModified=${result.wasModified}, " +
                "piiCount=${result.piiCount}, " +
                "types=${result.detectedPII.map { it.type }.distinct()}"
        }
        return task.copy(
            prompt = result.anonymizedText,
            anonymizationMapping = result.mapping
        )
    }
}

/**
 * Task definition for AI execution.
 */
data class AITask(
    val prompt: String,
    val preference: ProviderPreference = ProviderPreference.FAST_CHEAP,
    val model: String? = null,
    val maxTokens: Int = 1024,
    val temperature: Float = 0.1f,
    val systemPrompt: String? = null,
    val requiresAnonymization: Boolean = true,
    val estimatedCost: Float = 0.001f,
    val anonymizationMapping: Map<String, String>? = null
)

/**
 * Options for task execution.
 */
data class ExecutionOptions(
    val maxRetries: Int = 3,
    val timeoutMs: Long = 30_000
) {
    companion object {
        val DEFAULT = ExecutionOptions()
        val NO_RETRY = ExecutionOptions(maxRetries = 1)
        val LONG_TIMEOUT = ExecutionOptions(timeoutMs = 60_000)
    }
}

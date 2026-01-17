package com.finuts.ai.orchestration

import com.finuts.ai.cost.AICostTracker
import com.finuts.ai.model.AIResult
import com.finuts.ai.privacy.AnonymizationResult
import com.finuts.ai.privacy.PIIAnonymizer
import com.finuts.ai.privacy.DetectedPII
import com.finuts.ai.providers.ChatMessage
import com.finuts.ai.providers.CompletionRequest
import com.finuts.ai.providers.CompletionResponse
import com.finuts.ai.providers.LLMProvider
import com.finuts.ai.providers.LLMProviderFactory
import com.finuts.ai.providers.ModelConfig
import com.finuts.ai.providers.ProviderPreference
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for AIOrchestrator.
 */
class AIOrchestratorTest {

    // === Cost Limit Tests ===

    @Test
    fun `execute returns CostLimitExceeded when budget exceeded`() = runTest {
        val costTracker = AICostTracker(dailyBudget = 0f, monthlyBudget = 0f)
        val factory = LLMProviderFactory(null, null, null)
        val anonymizer = FakeAnonymizer()

        val orchestrator = AIOrchestrator(factory, costTracker, anonymizer)

        val result = orchestrator.execute(AITask(prompt = "Test", estimatedCost = 0.01f))

        assertIs<AIResult.CostLimitExceeded>(result)
    }

    // === Provider Availability Tests ===

    @Test
    fun `execute returns ProviderUnavailable when no providers`() = runTest {
        val costTracker = AICostTracker()
        val factory = LLMProviderFactory(null, null, null)
        val anonymizer = FakeAnonymizer()

        val orchestrator = AIOrchestrator(factory, costTracker, anonymizer)

        val result = orchestrator.execute(AITask(prompt = "Test"))

        assertIs<AIResult.ProviderUnavailable>(result)
    }

    // === Successful Execution Tests ===

    @Test
    fun `execute returns Success with valid response`() = runTest {
        val provider = FakeProvider(
            providerName = "gpt-4o-mini",
            response = CompletionResponse(
                content = "groceries",
                inputTokens = 50,
                outputTokens = 10,
                model = "gpt-4o-mini"
            )
        )
        val costTracker = AICostTracker()
        val factory = LLMProviderFactory(provider, null, null)
        val anonymizer = FakeAnonymizer()

        val orchestrator = AIOrchestrator(factory, costTracker, anonymizer)

        val result = orchestrator.execute(
            AITask(
                prompt = "Categorize: MAGNUM",
                requiresAnonymization = false
            )
        )

        assertIs<AIResult.Success<CompletionResponse>>(result)
        assertEquals("groceries", result.data.content)
        assertEquals(60, result.tokensUsed)
    }

    @Test
    fun `execute uses anthropic provider when available`() = runTest {
        val anthropicProvider = FakeProvider(
            providerName = "claude-sonnet",
            response = CompletionResponse(
                content = "result",
                inputTokens = 100,
                outputTokens = 20,
                model = "claude-sonnet"
            )
        )
        val costTracker = AICostTracker()
        val factory = LLMProviderFactory(null, anthropicProvider, null)
        val anonymizer = FakeAnonymizer()

        val orchestrator = AIOrchestrator(factory, costTracker, anonymizer)

        val result = orchestrator.execute(
            AITask(
                prompt = "Test",
                preference = ProviderPreference.BEST_QUALITY,
                requiresAnonymization = false
            )
        )

        assertIs<AIResult.Success<CompletionResponse>>(result)
        assertEquals("claude-sonnet", result.data.model)
    }

    // === Anonymization Tests ===

    @Test
    fun `execute anonymizes prompt when requiresAnonymization is true`() = runTest {
        val provider = FakeProvider(providerName = "gpt-4o-mini")
        val costTracker = AICostTracker()
        val factory = LLMProviderFactory(provider, null, null)
        val anonymizer = FakeAnonymizer()

        val orchestrator = AIOrchestrator(factory, costTracker, anonymizer)

        orchestrator.execute(
            AITask(
                prompt = "Categorize for John Smith",
                requiresAnonymization = true
            )
        )

        assertTrue(anonymizer.anonymizeCalled)
    }

    @Test
    fun `execute skips anonymization when requiresAnonymization is false`() = runTest {
        val provider = FakeProvider(providerName = "gpt-4o-mini")
        val costTracker = AICostTracker()
        val factory = LLMProviderFactory(provider, null, null)
        val anonymizer = FakeAnonymizer()

        val orchestrator = AIOrchestrator(factory, costTracker, anonymizer)

        orchestrator.execute(
            AITask(
                prompt = "Test",
                requiresAnonymization = false
            )
        )

        kotlin.test.assertFalse(anonymizer.anonymizeCalled)
    }

    // === Error Handling Tests ===

    @Test
    fun `execute returns Error when provider fails`() = runTest {
        val provider = FakeProvider(providerName = "gpt-4o-mini", shouldThrow = true)
        val costTracker = AICostTracker()
        val factory = LLMProviderFactory(provider, null, null)
        val anonymizer = FakeAnonymizer()

        val orchestrator = AIOrchestrator(factory, costTracker, anonymizer)

        val result = orchestrator.execute(
            AITask(
                prompt = "Test",
                requiresAnonymization = false
            ),
            ExecutionOptions.NO_RETRY
        )

        assertIs<AIResult.Error>(result)
    }

    // === ExecutionOptions Tests ===

    @Test
    fun `ExecutionOptions NO_RETRY has single attempt`() {
        val options = ExecutionOptions.NO_RETRY

        assertEquals(1, options.maxRetries)
    }

    @Test
    fun `ExecutionOptions DEFAULT has standard settings`() {
        val options = ExecutionOptions.DEFAULT

        assertEquals(3, options.maxRetries)
        assertEquals(30_000L, options.timeoutMs)
    }

    @Test
    fun `ExecutionOptions LONG_TIMEOUT has extended timeout`() {
        val options = ExecutionOptions.LONG_TIMEOUT

        assertEquals(60_000L, options.timeoutMs)
    }

    @Test
    fun `custom ExecutionOptions can be created`() {
        val options = ExecutionOptions(
            maxRetries = 5,
            timeoutMs = 45_000
        )

        assertEquals(5, options.maxRetries)
        assertEquals(45_000L, options.timeoutMs)
    }

    // === AITask Tests ===

    @Test
    fun `AITask default values are set correctly`() {
        val task = AITask(prompt = "Test prompt")

        assertEquals("Test prompt", task.prompt)
        assertEquals(ProviderPreference.FAST_CHEAP, task.preference)
        assertEquals(1024, task.maxTokens)
        assertEquals(true, task.requiresAnonymization)
        assertEquals(0.1f, task.temperature)
    }

    @Test
    fun `AITask custom values override defaults`() {
        val task = AITask(
            prompt = "Custom prompt",
            preference = ProviderPreference.BEST_QUALITY,
            maxTokens = 2048,
            requiresAnonymization = false,
            temperature = 0.7f,
            estimatedCost = 0.05f
        )

        assertEquals("Custom prompt", task.prompt)
        assertEquals(ProviderPreference.BEST_QUALITY, task.preference)
        assertEquals(2048, task.maxTokens)
        assertEquals(false, task.requiresAnonymization)
        assertEquals(0.7f, task.temperature)
        assertEquals(0.05f, task.estimatedCost)
    }

    @Test
    fun `AITask systemPrompt can be set`() {
        val task = AITask(
            prompt = "User message",
            systemPrompt = "You are a helpful assistant"
        )

        assertEquals("You are a helpful assistant", task.systemPrompt)
    }

    // === Structured Output Tests ===

    @Test
    fun `executeStructured returns Success with valid response`() = runTest {
        val provider = FakeProvider(
            providerName = "gpt-4o-mini",
            response = CompletionResponse(
                content = """{"categoryId":"groceries","confidence":0.95}""",
                inputTokens = 100,
                outputTokens = 50,
                model = "gpt-4o-mini"
            )
        )
        val costTracker = AICostTracker()
        val factory = LLMProviderFactory(provider, null, null)
        val anonymizer = FakeAnonymizer()

        val orchestrator = AIOrchestrator(factory, costTracker, anonymizer)

        val result = orchestrator.executeStructured(
            task = AITask(
                prompt = "Categorize: MAGNUM",
                requiresAnonymization = false
            ),
            jsonSchema = """{"type":"object","properties":{"categoryId":{"type":"string"}}}"""
        )

        assertIs<AIResult.Success<CompletionResponse>>(result)
        assertTrue(result.data.content.contains("groceries"))
    }

    @Test
    fun `executeStructured forces STRUCTURED_OUTPUT preference`() = runTest {
        val provider = FakeProvider(providerName = "gpt-4o-mini")
        val costTracker = AICostTracker()
        val factory = LLMProviderFactory(provider, null, null)
        val anonymizer = FakeAnonymizer()

        val orchestrator = AIOrchestrator(factory, costTracker, anonymizer)

        // Even with FAST_CHEAP preference, executeStructured should work
        val result = orchestrator.executeStructured(
            task = AITask(
                prompt = "Test",
                preference = ProviderPreference.FAST_CHEAP,
                requiresAnonymization = false
            ),
            jsonSchema = "{}"
        )

        assertIs<AIResult.Success<CompletionResponse>>(result)
    }

    @Test
    fun `executeStructured returns CostLimitExceeded when budget exceeded`() = runTest {
        val costTracker = AICostTracker(dailyBudget = 0f, monthlyBudget = 0f)
        val factory = LLMProviderFactory(null, null, null)
        val anonymizer = FakeAnonymizer()

        val orchestrator = AIOrchestrator(factory, costTracker, anonymizer)

        val result = orchestrator.executeStructured(
            task = AITask(prompt = "Test", estimatedCost = 0.01f),
            jsonSchema = "{}"
        )

        assertIs<AIResult.CostLimitExceeded>(result)
    }

    @Test
    fun `executeStructured uses custom ExecutionOptions`() = runTest {
        val provider = FakeProvider(providerName = "gpt-4o-mini", shouldThrow = true)
        val costTracker = AICostTracker()
        val factory = LLMProviderFactory(provider, null, null)
        val anonymizer = FakeAnonymizer()

        val orchestrator = AIOrchestrator(factory, costTracker, anonymizer)

        val result = orchestrator.executeStructured(
            task = AITask(prompt = "Test", requiresAnonymization = false),
            jsonSchema = "{}",
            options = ExecutionOptions.NO_RETRY
        )

        assertIs<AIResult.Error>(result)
    }

    // === De-anonymization Tests ===

    @Test
    fun `execute deanonymizes response when anonymization was applied`() = runTest {
        val provider = FakeProvider(
            providerName = "gpt-4o-mini",
            response = CompletionResponse(
                content = "Result for [PII_1]",
                inputTokens = 50,
                outputTokens = 20,
                model = "gpt-4o-mini"
            )
        )
        val costTracker = AICostTracker()
        val factory = LLMProviderFactory(provider, null, null)
        val anonymizer = FakeDeanonymizingAnonymizer()

        val orchestrator = AIOrchestrator(factory, costTracker, anonymizer)

        val result = orchestrator.execute(
            AITask(
                prompt = "Categorize for John Smith",
                requiresAnonymization = true
            )
        )

        assertIs<AIResult.Success<CompletionResponse>>(result)
        // Deanonymize should have been called
        assertTrue(anonymizer.deanonymizeCalled)
    }

    // === AITask Copy Tests ===

    @Test
    fun `AITask copy preserves all fields`() {
        val original = AITask(
            prompt = "Original",
            preference = ProviderPreference.BEST_QUALITY,
            model = "gpt-4o",
            maxTokens = 2048,
            temperature = 0.5f,
            systemPrompt = "System",
            requiresAnonymization = false,
            estimatedCost = 0.1f,
            anonymizationMapping = mapOf("key" to "value")
        )

        val copy = original.copy(prompt = "Modified")

        assertEquals("Modified", copy.prompt)
        assertEquals(ProviderPreference.BEST_QUALITY, copy.preference)
        assertEquals("gpt-4o", copy.model)
        assertEquals(2048, copy.maxTokens)
    }

    @Test
    fun `AITask anonymizationMapping can be set`() {
        val mapping = mapOf("[PII_1]" to "John Smith")
        val task = AITask(
            prompt = "Test [PII_1]",
            anonymizationMapping = mapping
        )

        assertEquals(mapping, task.anonymizationMapping)
        assertEquals("John Smith", task.anonymizationMapping?.get("[PII_1]"))
    }

    // === Fakes ===

    private class FakeDeanonymizingAnonymizer : PIIAnonymizer {
        var anonymizeCalled = false
        var deanonymizeCalled = false

        override fun anonymize(text: String): AnonymizationResult {
            anonymizeCalled = true
            return AnonymizationResult(
                anonymizedText = text.replace("John Smith", "[PII_1]"),
                mapping = mapOf("[PII_1]" to "John Smith"),
                detectedPII = emptyList(),
                wasModified = true
            )
        }

        override fun deanonymize(text: String, mapping: Map<String, String>): String {
            deanonymizeCalled = true
            var result = text
            mapping.forEach { (placeholder, original) ->
                result = result.replace(placeholder, original)
            }
            return result
        }

        override fun detectPII(text: String): List<DetectedPII> = emptyList()
    }

    private class FakeProvider(
        private val providerName: String = "fake-provider",
        private val response: CompletionResponse = CompletionResponse(
            content = "test",
            inputTokens = 10,
            outputTokens = 5,
            model = "fake"
        ),
        private val shouldThrow: Boolean = false
    ) : LLMProvider {
        override val name: String = providerName
        override val availableModels: List<ModelConfig> = emptyList()

        override suspend fun isAvailable(): Boolean = true

        override suspend fun complete(request: CompletionRequest): CompletionResponse {
            if (shouldThrow) {
                throw RuntimeException("Provider error")
            }
            return response
        }

        override suspend fun chat(messages: List<ChatMessage>): CompletionResponse =
            complete(CompletionRequest(""))

        override suspend fun structuredOutput(prompt: String, schema: String): CompletionResponse =
            complete(CompletionRequest(prompt))
    }

    private class FakeAnonymizer : PIIAnonymizer {
        var anonymizeCalled = false

        override fun anonymize(text: String): AnonymizationResult {
            anonymizeCalled = true
            return AnonymizationResult(
                anonymizedText = text,
                mapping = emptyMap(),
                detectedPII = emptyList(),
                wasModified = false
            )
        }

        override fun deanonymize(text: String, mapping: Map<String, String>): String = text
        override fun detectPII(text: String): List<DetectedPII> = emptyList()
    }
}

package com.finuts.ai.orchestration

import com.finuts.ai.providers.ProviderPreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for AITask and ExecutionOptions data classes.
 */
class AITaskTest {

    // === AITask ===

    @Test
    fun `AITask stores all fields`() {
        val task = AITask(
            prompt = "Categorize this",
            preference = ProviderPreference.BEST_QUALITY,
            model = "gpt-4o",
            maxTokens = 2048,
            temperature = 0.5f,
            systemPrompt = "You are an assistant",
            requiresAnonymization = false,
            estimatedCost = 0.005f,
            anonymizationMapping = mapOf("[NAME_1]" to "John")
        )

        assertEquals("Categorize this", task.prompt)
        assertEquals(ProviderPreference.BEST_QUALITY, task.preference)
        assertEquals("gpt-4o", task.model)
        assertEquals(2048, task.maxTokens)
        assertEquals(0.5f, task.temperature)
        assertEquals("You are an assistant", task.systemPrompt)
        assertEquals(false, task.requiresAnonymization)
        assertEquals(0.005f, task.estimatedCost)
        assertNotNull(task.anonymizationMapping)
        assertEquals("John", task.anonymizationMapping!!["[NAME_1]"])
    }

    @Test
    fun `AITask has sensible defaults`() {
        val task = AITask(prompt = "Test prompt")

        assertEquals("Test prompt", task.prompt)
        assertEquals(ProviderPreference.FAST_CHEAP, task.preference)
        assertNull(task.model)
        assertEquals(1024, task.maxTokens)
        assertEquals(0.1f, task.temperature)
        assertNull(task.systemPrompt)
        assertTrue(task.requiresAnonymization)
        assertEquals(0.001f, task.estimatedCost)
        assertNull(task.anonymizationMapping)
    }

    @Test
    fun `AITask copy creates modified copy`() {
        val original = AITask(prompt = "Original")
        val copy = original.copy(
            prompt = "Modified",
            maxTokens = 512
        )

        assertEquals("Modified", copy.prompt)
        assertEquals(512, copy.maxTokens)
        // Other fields should remain the same
        assertEquals(original.preference, copy.preference)
        assertEquals(original.temperature, copy.temperature)
    }

    @Test
    fun `AITask copy preserves anonymization mapping`() {
        val original = AITask(
            prompt = "Original",
            anonymizationMapping = mapOf("[A]" to "B")
        )
        val copy = original.copy(prompt = "New prompt")

        assertNotNull(copy.anonymizationMapping)
        assertEquals("B", copy.anonymizationMapping!!["[A]"])
    }

    // === ExecutionOptions ===

    @Test
    fun `ExecutionOptions stores fields`() {
        val options = ExecutionOptions(
            maxRetries = 5,
            timeoutMs = 60_000
        )

        assertEquals(5, options.maxRetries)
        assertEquals(60_000, options.timeoutMs)
    }

    @Test
    fun `ExecutionOptions DEFAULT has 3 retries and 30s timeout`() {
        val default = ExecutionOptions.DEFAULT

        assertEquals(3, default.maxRetries)
        assertEquals(30_000, default.timeoutMs)
    }

    @Test
    fun `ExecutionOptions NO_RETRY has 1 retry`() {
        val noRetry = ExecutionOptions.NO_RETRY

        assertEquals(1, noRetry.maxRetries)
    }

    @Test
    fun `ExecutionOptions LONG_TIMEOUT has 60s timeout`() {
        val longTimeout = ExecutionOptions.LONG_TIMEOUT

        assertEquals(60_000, longTimeout.timeoutMs)
    }

    @Test
    fun `ExecutionOptions copy works correctly`() {
        val original = ExecutionOptions.DEFAULT
        val modified = original.copy(maxRetries = 10)

        assertEquals(10, modified.maxRetries)
        assertEquals(original.timeoutMs, modified.timeoutMs)
    }
}

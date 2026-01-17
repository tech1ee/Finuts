package com.finuts.ai.inference

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for InferenceResult data class.
 */
class InferenceResultTest {

    @Test
    fun `InferenceResult stores all fields`() {
        val result = InferenceResult(
            text = "groceries",
            inputTokens = 100,
            outputTokens = 10,
            durationMs = 500,
            tokensPerSecond = 20.0f
        )

        assertEquals("groceries", result.text)
        assertEquals(100, result.inputTokens)
        assertEquals(10, result.outputTokens)
        assertEquals(500, result.durationMs)
        assertEquals(20.0f, result.tokensPerSecond)
    }

    @Test
    fun `InferenceResult equals works correctly`() {
        val result1 = InferenceResult(
            text = "test",
            inputTokens = 50,
            outputTokens = 5,
            durationMs = 100,
            tokensPerSecond = 50.0f
        )
        val result2 = InferenceResult(
            text = "test",
            inputTokens = 50,
            outputTokens = 5,
            durationMs = 100,
            tokensPerSecond = 50.0f
        )

        assertEquals(result1, result2)
    }

    @Test
    fun `InferenceResult copy creates modified copy`() {
        val original = InferenceResult(
            text = "original",
            inputTokens = 100,
            outputTokens = 10,
            durationMs = 500,
            tokensPerSecond = 20.0f
        )
        val copy = original.copy(text = "modified")

        assertEquals("modified", copy.text)
        assertEquals(original.inputTokens, copy.inputTokens)
        assertEquals(original.outputTokens, copy.outputTokens)
    }

    @Test
    fun `InferenceResult hashCode consistent with equals`() {
        val result1 = InferenceResult(
            text = "test",
            inputTokens = 50,
            outputTokens = 5,
            durationMs = 100,
            tokensPerSecond = 50.0f
        )
        val result2 = InferenceResult(
            text = "test",
            inputTokens = 50,
            outputTokens = 5,
            durationMs = 100,
            tokensPerSecond = 50.0f
        )

        assertEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun `InferenceResult toString contains fields`() {
        val result = InferenceResult(
            text = "grocery",
            inputTokens = 75,
            outputTokens = 8,
            durationMs = 250,
            tokensPerSecond = 32.0f
        )
        val string = result.toString()

        // Just verify it doesn't crash and contains some info
        kotlin.test.assertTrue(string.contains("InferenceResult"))
    }

    @Test
    fun `InferenceResult handles empty text`() {
        val result = InferenceResult(
            text = "",
            inputTokens = 10,
            outputTokens = 0,
            durationMs = 50,
            tokensPerSecond = 0.0f
        )

        assertEquals("", result.text)
        assertEquals(0, result.outputTokens)
    }

    @Test
    fun `InferenceResult handles zero duration`() {
        val result = InferenceResult(
            text = "fast",
            inputTokens = 5,
            outputTokens = 1,
            durationMs = 0,
            tokensPerSecond = Float.POSITIVE_INFINITY
        )

        assertEquals(0, result.durationMs)
    }
}

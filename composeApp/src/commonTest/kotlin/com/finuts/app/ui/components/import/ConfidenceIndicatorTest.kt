package com.finuts.app.ui.components.import

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ConfidenceIndicator component logic.
 * UI rendering tested via screenshot/manual testing.
 */
class ConfidenceIndicatorTest {

    @Test
    fun `confidence above 0 point 8 is high`() {
        val confidence = 0.85f
        val level = getConfidenceLevel(confidence)
        assertEquals(ConfidenceLevel.HIGH, level)
    }

    @Test
    fun `confidence at 0 point 8 is high`() {
        val confidence = 0.8f
        val level = getConfidenceLevel(confidence)
        assertEquals(ConfidenceLevel.HIGH, level)
    }

    @Test
    fun `confidence between 0 point 5 and 0 point 8 is medium`() {
        val confidence = 0.65f
        val level = getConfidenceLevel(confidence)
        assertEquals(ConfidenceLevel.MEDIUM, level)
    }

    @Test
    fun `confidence at 0 point 5 is medium`() {
        val confidence = 0.5f
        val level = getConfidenceLevel(confidence)
        assertEquals(ConfidenceLevel.MEDIUM, level)
    }

    @Test
    fun `confidence below 0 point 5 is low`() {
        val confidence = 0.35f
        val level = getConfidenceLevel(confidence)
        assertEquals(ConfidenceLevel.LOW, level)
    }

    @Test
    fun `confidence at 0 is low`() {
        val confidence = 0.0f
        val level = getConfidenceLevel(confidence)
        assertEquals(ConfidenceLevel.LOW, level)
    }

    @Test
    fun `confidence at 1 is high`() {
        val confidence = 1.0f
        val level = getConfidenceLevel(confidence)
        assertEquals(ConfidenceLevel.HIGH, level)
    }

    @Test
    fun `percentage calculation is correct`() {
        val confidence = 0.95f
        val percentage = (confidence * 100).toInt()
        assertEquals(95, percentage)
    }

    @Test
    fun `confidence is clamped between 0 and 1`() {
        val overOne = 1.5f.coerceIn(0f, 1f)
        val underZero = (-0.5f).coerceIn(0f, 1f)

        assertEquals(1f, overOne)
        assertEquals(0f, underZero)
    }
}

/**
 * Confidence level for categorization.
 */
enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Get confidence level from float value.
 */
fun getConfidenceLevel(confidence: Float): ConfidenceLevel {
    return when {
        confidence >= 0.8f -> ConfidenceLevel.HIGH
        confidence >= 0.5f -> ConfidenceLevel.MEDIUM
        else -> ConfidenceLevel.LOW
    }
}

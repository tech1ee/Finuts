package com.finuts.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for CategorizationResult entity.
 * Tests categorization confidence, sources, and validation.
 */
class CategorizationResultTest {

    @Test
    fun `CategorizationResult can be created with all parameters`() {
        val result = CategorizationResult(
            transactionId = "tx-1",
            categoryId = "groceries",
            confidence = 0.95f,
            source = CategorizationSource.RULE_BASED
        )

        assertEquals("tx-1", result.transactionId)
        assertEquals("groceries", result.categoryId)
        assertEquals(0.95f, result.confidence)
        assertEquals(CategorizationSource.RULE_BASED, result.source)
    }

    @Test
    fun `CategorizationSource has all expected values`() {
        val sources = CategorizationSource.entries.map { it.name }
        // Tier 0: User learned mappings
        assertTrue("USER_LEARNED" in sources)
        // Tier 1: Rule-based
        assertTrue("RULE_BASED" in sources)
        assertTrue("MERCHANT_DATABASE" in sources)
        assertTrue("USER_HISTORY" in sources)
        // Tier 1.5: On-device ML
        assertTrue("ON_DEVICE_ML" in sources)
        // Tier 2-3: LLM
        assertTrue("LLM_TIER2" in sources)
        assertTrue("LLM_TIER3" in sources)
        // Manual
        assertTrue("USER" in sources)
        assertEquals(8, CategorizationSource.entries.size)
    }

    @Test
    fun `isHighConfidence returns true when confidence is 0_85 or higher`() {
        val highConfidence = CategorizationResult(
            transactionId = "tx-1",
            categoryId = "groceries",
            confidence = 0.85f,
            source = CategorizationSource.RULE_BASED
        )
        assertTrue(highConfidence.isHighConfidence)

        val higherConfidence = highConfidence.copy(confidence = 0.95f)
        assertTrue(higherConfidence.isHighConfidence)
    }

    @Test
    fun `isHighConfidence returns false when confidence is below 0_85`() {
        val lowConfidence = CategorizationResult(
            transactionId = "tx-1",
            categoryId = "groceries",
            confidence = 0.84f,
            source = CategorizationSource.LLM_TIER2
        )
        assertFalse(lowConfidence.isHighConfidence)
    }

    @Test
    fun `isMediumConfidence returns true when confidence is between 0_70 and 0_85`() {
        val mediumConfidence = CategorizationResult(
            transactionId = "tx-1",
            categoryId = "groceries",
            confidence = 0.75f,
            source = CategorizationSource.LLM_TIER2
        )
        assertTrue(mediumConfidence.isMediumConfidence)
    }

    @Test
    fun `isMediumConfidence returns false when confidence is below 0_70`() {
        val lowConfidence = CategorizationResult(
            transactionId = "tx-1",
            categoryId = "groceries",
            confidence = 0.69f,
            source = CategorizationSource.LLM_TIER2
        )
        assertFalse(lowConfidence.isMediumConfidence)
    }

    @Test
    fun `isLowConfidence returns true when confidence is below 0_70`() {
        val lowConfidence = CategorizationResult(
            transactionId = "tx-1",
            categoryId = "groceries",
            confidence = 0.50f,
            source = CategorizationSource.LLM_TIER3
        )
        assertTrue(lowConfidence.isLowConfidence)
    }

    @Test
    fun `requiresUserConfirmation returns true for low confidence results`() {
        val lowConfidence = CategorizationResult(
            transactionId = "tx-1",
            categoryId = "groceries",
            confidence = 0.60f,
            source = CategorizationSource.LLM_TIER3
        )
        assertTrue(lowConfidence.requiresUserConfirmation)
    }

    @Test
    fun `requiresUserConfirmation returns false for high confidence results`() {
        val highConfidence = CategorizationResult(
            transactionId = "tx-1",
            categoryId = "groceries",
            confidence = 0.90f,
            source = CategorizationSource.RULE_BASED
        )
        assertFalse(highConfidence.requiresUserConfirmation)
    }

    @Test
    fun `isLocalSource returns true for local sources`() {
        val sources = listOf(
            CategorizationSource.USER_LEARNED,
            CategorizationSource.RULE_BASED,
            CategorizationSource.MERCHANT_DATABASE,
            CategorizationSource.USER_HISTORY,
            CategorizationSource.ON_DEVICE_ML
        )
        sources.forEach { source ->
            val result = CategorizationResult(
                transactionId = "tx-1",
                categoryId = "groceries",
                confidence = 0.90f,
                source = source
            )
            assertTrue(result.isLocalSource, "Expected $source to be local source")
        }
    }

    @Test
    fun `isLocalSource returns false for LLM sources`() {
        val result = CategorizationResult(
            transactionId = "tx-1",
            categoryId = "groceries",
            confidence = 0.80f,
            source = CategorizationSource.LLM_TIER2
        )
        assertFalse(result.isLocalSource)
    }

    @Test
    fun `copy works correctly`() {
        val original = CategorizationResult(
            transactionId = "tx-1",
            categoryId = "groceries",
            confidence = 0.85f,
            source = CategorizationSource.RULE_BASED
        )
        val modified = original.copy(
            categoryId = "food",
            confidence = 0.90f
        )

        assertEquals("tx-1", modified.transactionId)
        assertEquals("food", modified.categoryId)
        assertEquals(0.90f, modified.confidence)
        assertEquals(CategorizationSource.RULE_BASED, modified.source)
    }

    @Test
    fun `confidence boundary values are handled correctly`() {
        val exactlyHigh = CategorizationResult(
            transactionId = "tx-1",
            categoryId = "groceries",
            confidence = 0.85f,
            source = CategorizationSource.RULE_BASED
        )
        assertTrue(exactlyHigh.isHighConfidence)
        assertFalse(exactlyHigh.isMediumConfidence)
        assertFalse(exactlyHigh.isLowConfidence)

        val exactlyMedium = exactlyHigh.copy(confidence = 0.70f)
        assertFalse(exactlyMedium.isHighConfidence)
        assertTrue(exactlyMedium.isMediumConfidence)
        assertFalse(exactlyMedium.isLowConfidence)

        val justBelowMedium = exactlyHigh.copy(confidence = 0.699f)
        assertFalse(justBelowMedium.isHighConfidence)
        assertFalse(justBelowMedium.isMediumConfidence)
        assertTrue(justBelowMedium.isLowConfidence)
    }
}

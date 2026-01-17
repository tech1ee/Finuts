package com.finuts.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/**
 * Tests for InstalledModel and ModelStatus.
 */
class InstalledModelTest {

    private val testConfig = ModelConfig(
        id = "test-model",
        displayName = "Test Model",
        description = "Test description",
        sizeBytes = 125_000_000L,
        minRamBytes = 500_000_000L,
        capabilities = setOf(ModelCapability.CATEGORIZATION),
        downloadUrl = "https://example.com/model.bin",
        checksumSha256 = "abc123",
        version = "1.0.0",
        minAppVersion = "1.0.0"
    )

    private val testInstant = Instant.parse("2026-01-12T10:00:00Z")

    @Test
    fun `InstalledModel can be created with required fields`() {
        val model = InstalledModel(
            config = testConfig,
            filePath = "/path/to/model.bin",
            installedAt = testInstant
        )
        assertEquals(testConfig, model.config)
        assertEquals("/path/to/model.bin", model.filePath)
        assertEquals(testInstant, model.installedAt)
    }

    @Test
    fun `InstalledModel has null lastUsedAt by default`() {
        val model = InstalledModel(
            config = testConfig,
            filePath = "/path/to/model.bin",
            installedAt = testInstant
        )
        assertNull(model.lastUsedAt)
    }

    @Test
    fun `InstalledModel is not selected by default`() {
        val model = InstalledModel(
            config = testConfig,
            filePath = "/path/to/model.bin",
            installedAt = testInstant
        )
        assertFalse(model.isSelected)
    }

    @Test
    fun `InstalledModel has READY status by default`() {
        val model = InstalledModel(
            config = testConfig,
            filePath = "/path/to/model.bin",
            installedAt = testInstant
        )
        assertEquals(ModelStatus.READY, model.status)
    }

    @Test
    fun `InstalledModel can be created with all fields`() {
        val lastUsed = testInstant + 1.hours
        val model = InstalledModel(
            config = testConfig,
            filePath = "/path/to/model.bin",
            installedAt = testInstant,
            lastUsedAt = lastUsed,
            isSelected = true,
            status = ModelStatus.UPDATE_AVAILABLE
        )
        assertEquals(testInstant, model.installedAt)
        assertEquals(lastUsed, model.lastUsedAt)
        assertTrue(model.isSelected)
        assertEquals(ModelStatus.UPDATE_AVAILABLE, model.status)
    }

    @Test
    fun `InstalledModel copy works correctly`() {
        val original = InstalledModel(
            config = testConfig,
            filePath = "/path/to/model.bin",
            installedAt = testInstant
        )
        val modified = original.copy(isSelected = true)
        assertFalse(original.isSelected)
        assertTrue(modified.isSelected)
        assertEquals(original.config, modified.config)
    }

    @Test
    fun `ModelStatus has three values`() {
        assertEquals(3, ModelStatus.entries.size)
    }

    @Test
    fun `ModelStatus contains all expected values`() {
        val statuses = ModelStatus.entries.map { it.name }
        assertTrue("READY" in statuses)
        assertTrue("CORRUPTED" in statuses)
        assertTrue("UPDATE_AVAILABLE" in statuses)
    }

    @Test
    fun `InstalledModel can have CORRUPTED status`() {
        val model = InstalledModel(
            config = testConfig,
            filePath = "/path/to/model.bin",
            installedAt = testInstant,
            status = ModelStatus.CORRUPTED
        )
        assertEquals(ModelStatus.CORRUPTED, model.status)
    }

    @Test
    fun `two InstalledModels with same values are equal`() {
        val model1 = InstalledModel(
            config = testConfig,
            filePath = "/path/to/model.bin",
            installedAt = testInstant
        )
        val model2 = InstalledModel(
            config = testConfig,
            filePath = "/path/to/model.bin",
            installedAt = testInstant
        )
        assertEquals(model1, model2)
    }
}

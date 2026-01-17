package com.finuts.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for ModelConfig and ModelCapability.
 */
class ModelConfigTest {

    private fun createTestConfig(
        id: String = "test-model",
        displayName: String = "Test Model",
        sizeBytes: Long = 125_000_000L
    ) = ModelConfig(
        id = id,
        displayName = displayName,
        description = "Test description",
        sizeBytes = sizeBytes,
        minRamBytes = 500_000_000L,
        capabilities = setOf(ModelCapability.CATEGORIZATION),
        downloadUrl = "https://example.com/model.bin",
        checksumSha256 = "abc123",
        version = "1.0.0",
        minAppVersion = "1.0.0"
    )

    @Test
    fun `ModelConfig can be created with all fields`() {
        val config = createTestConfig()
        assertEquals("test-model", config.id)
        assertEquals("Test Model", config.displayName)
        assertEquals(125_000_000L, config.sizeBytes)
    }

    @Test
    fun `ModelConfig copy works correctly`() {
        val original = createTestConfig()
        val modified = original.copy(displayName = "Modified")
        assertEquals("Test Model", original.displayName)
        assertEquals("Modified", modified.displayName)
        assertEquals(original.id, modified.id)
    }

    @Test
    fun `formattedSize returns MB for megabyte sizes`() {
        val config = createTestConfig(sizeBytes = 125_000_000L)
        assertEquals("125 MB", config.formattedSize)
    }

    @Test
    fun `formattedSize returns GB for gigabyte sizes`() {
        val config = createTestConfig(sizeBytes = 1_500_000_000L)
        assertEquals("1.5 GB", config.formattedSize)
    }

    @Test
    fun `formattedSize returns KB for kilobyte sizes`() {
        val config = createTestConfig(sizeBytes = 500_000L)
        assertEquals("500 KB", config.formattedSize)
    }

    @Test
    fun `formattedSize returns B for byte sizes`() {
        val config = createTestConfig(sizeBytes = 500L)
        assertEquals("500 B", config.formattedSize)
    }

    @Test
    fun `ModelCapability has five values`() {
        assertEquals(5, ModelCapability.entries.size)
    }

    @Test
    fun `ModelCapability contains all expected values`() {
        val capabilities = ModelCapability.entries.map { it.name }
        assertTrue("CATEGORIZATION" in capabilities)
        assertTrue("INSIGHTS" in capabilities)
        assertTrue("CHAT" in capabilities)
        assertTrue("OCR_ENHANCEMENT" in capabilities)
        assertTrue("MULTILINGUAL" in capabilities)
    }

    @Test
    fun `ModelConfig capabilities can contain multiple values`() {
        val config = createTestConfig().copy(
            capabilities = setOf(
                ModelCapability.CATEGORIZATION,
                ModelCapability.INSIGHTS,
                ModelCapability.MULTILINGUAL
            )
        )
        assertEquals(3, config.capabilities.size)
        assertTrue(ModelCapability.CATEGORIZATION in config.capabilities)
        assertTrue(ModelCapability.INSIGHTS in config.capabilities)
        assertTrue(ModelCapability.MULTILINGUAL in config.capabilities)
    }

    @Test
    fun `ModelConfig capabilities can be empty`() {
        val config = createTestConfig().copy(capabilities = emptySet())
        assertTrue(config.capabilities.isEmpty())
    }

    @Test
    fun `two ModelConfigs with same values are equal`() {
        val config1 = createTestConfig()
        val config2 = createTestConfig()
        assertEquals(config1, config2)
    }

    @Test
    fun `ModelConfig hashCode is consistent`() {
        val config1 = createTestConfig()
        val config2 = createTestConfig()
        assertEquals(config1.hashCode(), config2.hashCode())
    }
}

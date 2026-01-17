package com.finuts.domain.model

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for DownloadProgress sealed interface and DownloadError.
 */
class DownloadProgressTest {

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

    private val testInstalledModel = InstalledModel(
        config = testConfig,
        filePath = "/path/to/model.bin",
        installedAt = Instant.parse("2026-01-12T10:00:00Z")
    )

    @Test
    fun `Idle is a valid DownloadProgress`() {
        val progress: DownloadProgress = DownloadProgress.Idle
        assertTrue(progress is DownloadProgress.Idle)
    }

    @Test
    fun `Preparing contains modelId`() {
        val progress = DownloadProgress.Preparing("test-model")
        assertEquals("test-model", progress.modelId)
    }

    @Test
    fun `Downloading contains all required fields`() {
        val progress = DownloadProgress.Downloading(
            modelId = "test-model",
            bytesDownloaded = 50_000_000L,
            totalBytes = 100_000_000L,
            speedBytesPerSecond = 1_000_000L
        )
        assertEquals("test-model", progress.modelId)
        assertEquals(50_000_000L, progress.bytesDownloaded)
        assertEquals(100_000_000L, progress.totalBytes)
        assertEquals(1_000_000L, progress.speedBytesPerSecond)
    }

    @Test
    fun `Downloading progress is calculated correctly`() {
        val progress = DownloadProgress.Downloading(
            modelId = "test-model",
            bytesDownloaded = 50_000_000L,
            totalBytes = 100_000_000L,
            speedBytesPerSecond = 1_000_000L
        )
        assertEquals(0.5f, progress.progress)
    }

    @Test
    fun `Downloading progressPercent is calculated correctly`() {
        val progress = DownloadProgress.Downloading(
            modelId = "test-model",
            bytesDownloaded = 75_000_000L,
            totalBytes = 100_000_000L,
            speedBytesPerSecond = 1_000_000L
        )
        assertEquals(75, progress.progressPercent)
    }

    @Test
    fun `Downloading remainingSeconds is calculated correctly`() {
        val progress = DownloadProgress.Downloading(
            modelId = "test-model",
            bytesDownloaded = 50_000_000L,
            totalBytes = 100_000_000L,
            speedBytesPerSecond = 10_000_000L
        )
        // (100M - 50M) / 10M = 5 seconds
        assertEquals(5L, progress.remainingSeconds)
    }

    @Test
    fun `Downloading progress is 0 when totalBytes is 0`() {
        val progress = DownloadProgress.Downloading(
            modelId = "test-model",
            bytesDownloaded = 0L,
            totalBytes = 0L,
            speedBytesPerSecond = 0L
        )
        assertEquals(0f, progress.progress)
    }

    @Test
    fun `Downloading remainingSeconds is 0 when speed is 0`() {
        val progress = DownloadProgress.Downloading(
            modelId = "test-model",
            bytesDownloaded = 50_000_000L,
            totalBytes = 100_000_000L,
            speedBytesPerSecond = 0L
        )
        assertEquals(0L, progress.remainingSeconds)
    }

    @Test
    fun `Verifying contains modelId`() {
        val progress = DownloadProgress.Verifying("test-model")
        assertEquals("test-model", progress.modelId)
    }

    @Test
    fun `Completed contains modelId and installedModel`() {
        val progress = DownloadProgress.Completed(
            modelId = "test-model",
            installedModel = testInstalledModel
        )
        assertEquals("test-model", progress.modelId)
        assertEquals(testInstalledModel, progress.installedModel)
    }

    @Test
    fun `Failed contains modelId and error`() {
        val progress = DownloadProgress.Failed(
            modelId = "test-model",
            error = DownloadError.NETWORK_ERROR
        )
        assertEquals("test-model", progress.modelId)
        assertEquals(DownloadError.NETWORK_ERROR, progress.error)
    }

    @Test
    fun `Cancelled is a valid DownloadProgress`() {
        val progress: DownloadProgress = DownloadProgress.Cancelled
        assertTrue(progress is DownloadProgress.Cancelled)
    }

    @Test
    fun `DownloadError has five values`() {
        assertEquals(5, DownloadError.entries.size)
    }

    @Test
    fun `DownloadError contains all expected values`() {
        val errors = DownloadError.entries.map { it.name }
        assertTrue("NETWORK_ERROR" in errors)
        assertTrue("INSUFFICIENT_STORAGE" in errors)
        assertTrue("CHECKSUM_MISMATCH" in errors)
        assertTrue("UNSUPPORTED_DEVICE" in errors)
        assertTrue("UNKNOWN" in errors)
    }

    @Test
    fun `DownloadProgress can be used in when expression`() {
        val progress: DownloadProgress = DownloadProgress.Downloading(
            modelId = "test",
            bytesDownloaded = 50,
            totalBytes = 100,
            speedBytesPerSecond = 10
        )
        val result = when (progress) {
            is DownloadProgress.Idle -> "idle"
            is DownloadProgress.Preparing -> "preparing"
            is DownloadProgress.Downloading -> "downloading"
            is DownloadProgress.Verifying -> "verifying"
            is DownloadProgress.Completed -> "completed"
            is DownloadProgress.Failed -> "failed"
            is DownloadProgress.Cancelled -> "cancelled"
        }
        assertEquals("downloading", result)
    }
}

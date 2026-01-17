package com.finuts.test.fakes

import com.finuts.data.model.ModelDownloaderOperations

/**
 * Fake implementation of ModelDownloaderOperations for testing.
 * Allows controlling file existence, download behavior, and storage.
 */
class FakeModelDownloader(
    private val modelsDirectory: String = "/test/models",
    private val availableStorage: Long = 10_000_000_000L,
    initialExistingFiles: Set<String> = emptySet()
) : ModelDownloaderOperations {

    private val existingFiles = initialExistingFiles.toMutableSet()
    private val downloadedFiles = mutableMapOf<String, ByteArray>()

    // Tracking for test assertions
    var downloadCalled = false
        private set
    var lastDownloadUrl: String? = null
        private set
    var lastDownloadDestination: String? = null
        private set
    var deleteFileCalled = false
        private set
    var lastDeletedFile: String? = null
        private set

    // Control download behavior
    var shouldDownloadFail = false
    var downloadError: Exception? = null

    override fun getModelsDirectory(): String = modelsDirectory

    override suspend fun getAvailableStorage(): Long = availableStorage

    override suspend fun download(
        url: String,
        destination: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<Unit> {
        downloadCalled = true
        lastDownloadUrl = url
        lastDownloadDestination = destination

        if (shouldDownloadFail) {
            return Result.failure(downloadError ?: Exception("Download failed"))
        }

        // Simulate download progress
        val totalBytes = 100_000L
        for (i in 1..10) {
            onProgress(i * 10_000L, totalBytes)
        }

        // Mark file as existing after download
        existingFiles.add(destination)
        downloadedFiles[destination] = ByteArray(totalBytes.toInt())

        return Result.success(Unit)
    }

    override suspend fun verifyChecksum(filePath: String, expectedSha256: String): Boolean {
        // Skip verification if no checksum provided
        if (expectedSha256.isBlank()) return true
        // In tests, assume checksum is valid if file exists
        return existingFiles.contains(filePath)
    }

    override suspend fun deleteFile(filePath: String): Boolean {
        deleteFileCalled = true
        lastDeletedFile = filePath
        existingFiles.remove(filePath)
        downloadedFiles.remove(filePath)
        return true
    }

    override suspend fun fileExists(filePath: String): Boolean {
        return existingFiles.contains(filePath)
    }

    // Test helper methods
    fun addExistingFile(filePath: String) {
        existingFiles.add(filePath)
    }

    fun removeExistingFile(filePath: String) {
        existingFiles.remove(filePath)
    }

    fun getExistingFiles(): Set<String> = existingFiles.toSet()

    fun reset() {
        downloadCalled = false
        lastDownloadUrl = null
        lastDownloadDestination = null
        deleteFileCalled = false
        lastDeletedFile = null
        shouldDownloadFail = false
        downloadError = null
    }
}

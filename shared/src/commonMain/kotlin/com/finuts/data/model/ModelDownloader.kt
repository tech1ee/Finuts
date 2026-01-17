package com.finuts.data.model

/**
 * Platform-specific model file downloader.
 * Uses expect/actual pattern for Android/iOS implementations.
 * Implements [ModelDownloaderOperations] for testability.
 */
expect class ModelDownloader : ModelDownloaderOperations {
    /**
     * Get the path to the models directory.
     * Creates the directory if it doesn't exist.
     */
    override fun getModelsDirectory(): String

    /**
     * Get available storage space on the device in bytes.
     */
    override suspend fun getAvailableStorage(): Long

    /**
     * Download a file from URL to destination with progress tracking.
     *
     * @param url Source URL to download from
     * @param destination Absolute path to save the file
     * @param onProgress Callback with (bytesDownloaded, totalBytes)
     * @return Result indicating success or failure
     */
    override suspend fun download(
        url: String,
        destination: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<Unit>

    /**
     * Verify file checksum matches expected SHA-256 hash.
     *
     * @param filePath Path to the file to verify
     * @param expectedSha256 Expected SHA-256 hash (lowercase hex string)
     * @return true if checksum matches
     */
    override suspend fun verifyChecksum(filePath: String, expectedSha256: String): Boolean

    /**
     * Delete a file at the given path.
     *
     * @param filePath Path to the file to delete
     * @return true if file was deleted or didn't exist
     */
    override suspend fun deleteFile(filePath: String): Boolean

    /**
     * Check if a file exists at the given path.
     *
     * @param filePath Absolute path to the file
     * @return true if file exists
     */
    override suspend fun fileExists(filePath: String): Boolean
}

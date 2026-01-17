package com.finuts.domain.model

/**
 * Represents the progress state of a model download.
 * Used by UI to show download status and progress.
 */
sealed interface DownloadProgress {
    /** No download in progress */
    data object Idle : DownloadProgress

    /** Preparing download (checking storage, network) */
    data class Preparing(val modelId: String) : DownloadProgress

    /** Actively downloading */
    data class Downloading(
        val modelId: String,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedBytesPerSecond: Long
    ) : DownloadProgress {
        /** Progress as fraction 0.0-1.0 */
        val progress: Float
            get() = if (totalBytes > 0) {
                bytesDownloaded.toFloat() / totalBytes
            } else 0f

        /** Estimated remaining time in seconds */
        val remainingSeconds: Long
            get() = if (speedBytesPerSecond > 0) {
                (totalBytes - bytesDownloaded) / speedBytesPerSecond
            } else 0L

        /** Progress as percentage 0-100 */
        val progressPercent: Int
            get() = (progress * 100).toInt()
    }

    /** Verifying downloaded file (checksum) */
    data class Verifying(val modelId: String) : DownloadProgress

    /** Download completed successfully */
    data class Completed(
        val modelId: String,
        val installedModel: InstalledModel
    ) : DownloadProgress

    /** Download failed */
    data class Failed(
        val modelId: String,
        val error: DownloadError
    ) : DownloadProgress

    /** Download was cancelled by user */
    data object Cancelled : DownloadProgress
}

/**
 * Reasons why a model download can fail.
 */
enum class DownloadError {
    /** Network connection issue */
    NETWORK_ERROR,

    /** Not enough storage space */
    INSUFFICIENT_STORAGE,

    /** Downloaded file doesn't match expected checksum */
    CHECKSUM_MISMATCH,

    /** Device doesn't meet minimum requirements */
    UNSUPPORTED_DEVICE,

    /** Unknown error */
    UNKNOWN
}

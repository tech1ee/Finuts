package com.finuts.domain.model

/**
 * Configuration for an AI model available for download.
 * Model-agnostic design - no specific model names hardcoded.
 *
 * @property id Unique identifier (e.g., "compact", "standard", "pro")
 * @property displayName Localized display name
 * @property description Localized description
 * @property sizeBytes Model file size in bytes
 * @property minRamBytes Minimum RAM required for inference
 * @property capabilities Set of features this model supports
 * @property downloadUrl URL to download the model file
 * @property checksumSha256 SHA-256 checksum for verification
 * @property version Model version string
 * @property minAppVersion Minimum app version required
 */
data class ModelConfig(
    val id: String,
    val displayName: String,
    val description: String,
    val sizeBytes: Long,
    val minRamBytes: Long,
    val capabilities: Set<ModelCapability>,
    val downloadUrl: String,
    val checksumSha256: String,
    val version: String,
    val minAppVersion: String
) {
    /**
     * Human-readable size string (e.g., "125 MB").
     */
    val formattedSize: String
        get() = when {
            sizeBytes >= 1_000_000_000 -> {
                val gb = sizeBytes / 1_000_000_000.0
                "${(gb * 10).toLong() / 10.0} GB"
            }
            sizeBytes >= 1_000_000 -> "${sizeBytes / 1_000_000} MB"
            sizeBytes >= 1_000 -> "${sizeBytes / 1_000} KB"
            else -> "$sizeBytes B"
        }
}

/**
 * Capabilities that an AI model can support.
 * Used to filter and recommend models based on user needs.
 */
enum class ModelCapability {
    /** Transaction categorization */
    CATEGORIZATION,

    /** Financial insights and recommendations */
    INSIGHTS,

    /** Chat/assistant functionality */
    CHAT,

    /** OCR text enhancement */
    OCR_ENHANCEMENT,

    /** Multi-language support (RU, KK, EN) */
    MULTILINGUAL
}

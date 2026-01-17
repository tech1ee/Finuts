package com.finuts.domain.model

import kotlinx.datetime.Instant

/**
 * Represents an AI model installed on the device.
 *
 * @property config The model configuration
 * @property filePath Absolute path to the model file
 * @property installedAt When the model was installed
 * @property lastUsedAt When the model was last used for inference
 * @property isSelected Whether this is the currently active model
 * @property status Current status of the installed model
 */
data class InstalledModel(
    val config: ModelConfig,
    val filePath: String,
    val installedAt: Instant,
    val lastUsedAt: Instant? = null,
    val isSelected: Boolean = false,
    val status: ModelStatus = ModelStatus.READY
)

/**
 * Status of an installed model.
 */
enum class ModelStatus {
    /** Model is ready for inference */
    READY,

    /** Model file is corrupted (checksum mismatch) */
    CORRUPTED,

    /** Newer version available for download */
    UPDATE_AVAILABLE
}

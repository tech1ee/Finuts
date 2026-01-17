package com.finuts.domain.repository

import com.finuts.domain.model.DownloadProgress
import com.finuts.domain.model.InstalledModel
import com.finuts.domain.model.ModelConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for managing AI models (download, storage, selection).
 * Model-agnostic interface - no specific model names.
 */
interface ModelRepository {
    /** List of models available for download */
    val availableModels: List<ModelConfig>

    /** Currently installed models on device */
    val installedModels: StateFlow<List<InstalledModel>>

    /** Currently selected model for inference */
    val currentModel: StateFlow<InstalledModel?>

    /** Current download progress (Idle when not downloading) */
    val downloadProgress: StateFlow<DownloadProgress>

    /**
     * Get the recommended model based on device capabilities.
     * Takes into account RAM, storage, and CPU/GPU support.
     */
    suspend fun getRecommendedModel(): ModelConfig

    /**
     * Check if device meets minimum requirements for a model.
     * @param modelId Model to check compatibility for
     * @return true if device can run this model
     */
    suspend fun isDeviceCompatible(modelId: String): Boolean

    /**
     * Check if there's enough storage space for a model.
     * @param modelId Model to check storage for
     * @return true if enough space available
     */
    suspend fun hasEnoughStorage(modelId: String): Boolean

    /**
     * Download a model to device storage.
     * Progress is emitted to [downloadProgress].
     * @param modelId Model to download
     * @return Result with InstalledModel on success
     */
    suspend fun downloadModel(modelId: String): Result<InstalledModel>

    /**
     * Cancel ongoing download.
     * Sets [downloadProgress] to Cancelled.
     */
    fun cancelDownload()

    /**
     * Delete an installed model from device.
     * @param modelId Model to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteModel(modelId: String): Result<Unit>

    /**
     * Select a model as the current/active model.
     * @param modelId Model to select
     * @return Result indicating success or failure
     */
    suspend fun selectModel(modelId: String): Result<Unit>

    /**
     * Check for available model updates.
     * @return List of models with newer versions available
     */
    suspend fun checkForUpdates(): List<ModelConfig>
}

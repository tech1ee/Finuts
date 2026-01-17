package com.finuts.data.repository

import co.touchlab.kermit.Logger
import com.finuts.data.model.ModelDownloaderOperations
import com.finuts.domain.model.DownloadError
import com.finuts.domain.model.DownloadProgress
import com.finuts.domain.model.InstalledModel
import com.finuts.domain.model.ModelCapability
import com.finuts.domain.model.ModelConfig
import com.finuts.domain.model.ModelStatus
import com.finuts.domain.repository.ModelRepository
import com.finuts.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

/**
 * Implementation of ModelRepository.
 * Manages AI model downloads, storage, and selection.
 */
class ModelRepositoryImpl(
    private val downloader: ModelDownloaderOperations,
    private val preferencesRepository: PreferencesRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : ModelRepository {

    private val logger = Logger.withTag("ModelRepository")

    private val _installedModels = MutableStateFlow<List<InstalledModel>>(emptyList())
    override val installedModels: StateFlow<List<InstalledModel>> = _installedModels.asStateFlow()

    private val _currentModel = MutableStateFlow<InstalledModel?>(null)
    override val currentModel: StateFlow<InstalledModel?> = _currentModel.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress>(DownloadProgress.Idle)
    override val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    private var downloadJob: Job? = null

    override val availableModels: List<ModelConfig> = listOf(
        ModelConfig(
            id = "compact",
            displayName = "SmolLM2 135M",
            description = "Fast categorization, minimal storage",
            sizeBytes = 105_000_000L,
            minRamBytes = 300_000_000L,
            capabilities = setOf(ModelCapability.CATEGORIZATION),
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf",
            checksumSha256 = "",
            version = "1.0.0",
            minAppVersion = "1.0.0"
        ),
        ModelConfig(
            id = "standard",
            displayName = "Gemma 3 270M",
            description = "Balanced performance and accuracy",
            sizeBytes = 292_000_000L,
            minRamBytes = 500_000_000L,
            capabilities = setOf(
                ModelCapability.CATEGORIZATION,
                ModelCapability.INSIGHTS
            ),
            downloadUrl = "https://huggingface.co/ggml-org/gemma-3-270m-it-GGUF/resolve/main/gemma-3-270m-it-Q8_0.gguf",
            checksumSha256 = "",
            version = "1.0.0",
            minAppVersion = "1.0.0"
        ),
        ModelConfig(
            id = "pro",
            displayName = "Gemma 3 1B",
            description = "Best accuracy, financial insights",
            sizeBytes = 806_000_000L,
            minRamBytes = 1_500_000_000L,
            capabilities = setOf(
                ModelCapability.CATEGORIZATION,
                ModelCapability.INSIGHTS,
                ModelCapability.CHAT,
                ModelCapability.MULTILINGUAL
            ),
            downloadUrl = "https://huggingface.co/bartowski/google_gemma-3-1b-it-GGUF/resolve/main/google_gemma-3-1b-it-Q4_K_M.gguf",
            checksumSha256 = "",
            version = "1.0.0",
            minAppVersion = "1.0.0"
        )
    )

    init {
        scope.launch { refreshInstalledModels() }
    }

    override suspend fun getRecommendedModel(): ModelConfig {
        val availableStorage = downloader.getAvailableStorage()
        return availableModels.lastOrNull { config ->
            config.sizeBytes < availableStorage * 0.8
        } ?: availableModels.first()
    }

    override suspend fun isDeviceCompatible(modelId: String): Boolean {
        val config = availableModels.find { it.id == modelId } ?: return false
        // Basic compatibility check - can be extended with actual RAM check
        return true
    }

    override suspend fun hasEnoughStorage(modelId: String): Boolean {
        val config = availableModels.find { it.id == modelId } ?: return false
        val available = downloader.getAvailableStorage()
        val required = config.sizeBytes + STORAGE_BUFFER
        return available >= required
    }

    override suspend fun downloadModel(modelId: String): Result<InstalledModel> =
        withContext(Dispatchers.Default) {
            logger.i { "downloadModel() called with modelId=$modelId" }

            val config = availableModels.find { it.id == modelId }
                ?: run {
                    logger.e { "Unknown model: $modelId" }
                    return@withContext Result.failure(
                        IllegalArgumentException("Unknown model: $modelId")
                    )
                }

            logger.d { "Found config: ${config.displayName}, url=${config.downloadUrl}" }

            // Check if URL is empty (models not yet configured)
            if (config.downloadUrl.isBlank()) {
                logger.w { "Download URL is empty for model $modelId - model not yet available" }
                _downloadProgress.value = DownloadProgress.Failed(
                    modelId, DownloadError.NETWORK_ERROR
                )
                return@withContext Result.failure(
                    Exception("Model download not yet available. Download URL not configured.")
                )
            }

            if (!hasEnoughStorage(modelId)) {
                logger.w { "Insufficient storage for model $modelId" }
                _downloadProgress.value = DownloadProgress.Failed(
                    modelId, DownloadError.INSUFFICIENT_STORAGE
                )
                return@withContext Result.failure(
                    Exception("Insufficient storage")
                )
            }

            _downloadProgress.value = DownloadProgress.Preparing(modelId)
            logger.i { "Starting download: $modelId from ${config.downloadUrl}" }

            val destination = "${downloader.getModelsDirectory()}/${config.id}.gguf"
            var lastSpeed = 0L
            var lastTime = kotlin.time.Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds()).toEpochMilliseconds()
            var lastBytes = 0L

            downloadJob = scope.launch {
                val result = downloader.download(
                    url = config.downloadUrl,
                    destination = destination
                ) { downloaded, total ->
                    val now = kotlin.time.Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds()).toEpochMilliseconds()
                    val elapsed = now - lastTime
                    if (elapsed > 500) {
                        lastSpeed = ((downloaded - lastBytes) * 1000 / elapsed)
                        lastTime = now
                        lastBytes = downloaded
                    }
                    _downloadProgress.value = DownloadProgress.Downloading(
                        modelId = modelId,
                        bytesDownloaded = downloaded,
                        totalBytes = total,
                        speedBytesPerSecond = lastSpeed
                    )
                }

                if (result.isFailure) {
                    _downloadProgress.value = DownloadProgress.Failed(
                        modelId, DownloadError.NETWORK_ERROR
                    )
                    return@launch
                }

                // Verify checksum if provided
                if (config.checksumSha256.isNotEmpty()) {
                    _downloadProgress.value = DownloadProgress.Verifying(modelId)
                    if (!downloader.verifyChecksum(destination, config.checksumSha256)) {
                        downloader.deleteFile(destination)
                        _downloadProgress.value = DownloadProgress.Failed(
                            modelId, DownloadError.CHECKSUM_MISMATCH
                        )
                        return@launch
                    }
                }

                val installed = InstalledModel(
                    config = config,
                    filePath = destination,
                    installedAt = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds()),
                    isSelected = false,
                    status = ModelStatus.READY
                )

                refreshInstalledModels()
                _downloadProgress.value = DownloadProgress.Completed(modelId, installed)
                logger.i { "Download completed: $modelId" }
            }

            downloadJob?.join()

            val progress = _downloadProgress.value
            return@withContext if (progress is DownloadProgress.Completed) {
                Result.success(progress.installedModel)
            } else {
                Result.failure(Exception("Download failed"))
            }
        }

    override fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadProgress.value = DownloadProgress.Cancelled
        logger.i { "Download cancelled" }
    }

    override suspend fun deleteModel(modelId: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            val installed = _installedModels.value.find { it.config.id == modelId }
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Model not installed: $modelId")
                )

            val deleted = downloader.deleteFile(installed.filePath)
            if (!deleted) {
                return@withContext Result.failure(Exception("Failed to delete"))
            }

            if (_currentModel.value?.config?.id == modelId) {
                preferencesRepository.setSelectedModelId(null)
                _currentModel.value = null
            }

            refreshInstalledModels()
            logger.i { "Model deleted: $modelId" }
            Result.success(Unit)
        }

    override suspend fun selectModel(modelId: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            val installed = _installedModels.value.find { it.config.id == modelId }
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Model not installed: $modelId")
                )

            // FIX BUG #3: Persist to preferences first
            preferencesRepository.setSelectedModelId(modelId)

            // Update currentModel immediately (no race condition)
            _currentModel.value = installed.copy(isSelected = true)

            // Atomically update installed models list with new selection state
            _installedModels.update { models ->
                models.map { model ->
                    model.copy(isSelected = model.config.id == modelId)
                }
            }

            logger.i { "Model selected: $modelId" }
            Result.success(Unit)
        }

    override suspend fun checkForUpdates(): List<ModelConfig> {
        // TODO: Check remote config for newer versions
        return emptyList()
    }

    private suspend fun refreshInstalledModels() {
        val modelsDir = downloader.getModelsDirectory()
        val prefs = preferencesRepository.preferences.first()
        val selectedId = prefs.selectedModelId

        val installed = availableModels.mapNotNull { config ->
            val filePath = "$modelsDir/${config.id}.gguf"

            // FIX BUG #2: Actually check if model file exists on disk
            val exists = downloader.fileExists(filePath)

            if (exists) {
                InstalledModel(
                    config = config,
                    filePath = filePath,
                    installedAt = Instant.fromEpochMilliseconds(
                        kotlin.time.Clock.System.now().toEpochMilliseconds()
                    ),
                    isSelected = config.id == selectedId,
                    status = ModelStatus.READY
                )
            } else null
        }

        _installedModels.value = installed
        _currentModel.value = installed.find { it.isSelected }

        logger.d {
            "refreshInstalledModels: found ${installed.size} installed, " +
                "selected=${_currentModel.value?.config?.id}"
        }
    }

    companion object {
        private const val STORAGE_BUFFER = 50_000_000L // 50MB buffer
    }
}

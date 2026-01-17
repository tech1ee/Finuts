package com.finuts.app.feature.settings.ai

import androidx.lifecycle.viewModelScope
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.model.DownloadProgress
import com.finuts.domain.model.InstalledModel
import com.finuts.domain.model.ModelConfig
import com.finuts.domain.model.UserPreferences
import com.finuts.domain.repository.ModelRepository
import com.finuts.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for AI Features settings screen.
 * Manages AI model download, selection, and preferences.
 */
class AIFeaturesViewModel(
    private val modelRepository: ModelRepository,
    private val preferencesRepository: PreferencesRepository
) : BaseViewModel() {

    /** Available models for download */
    val availableModels: List<ModelConfig> = modelRepository.availableModels

    /** Currently installed models */
    val installedModels: StateFlow<List<InstalledModel>> = modelRepository.installedModels

    /** Currently selected/active model */
    val currentModel: StateFlow<InstalledModel?> = modelRepository.currentModel

    /** Download progress state */
    val downloadProgress: StateFlow<DownloadProgress> = modelRepository.downloadProgress

    /** User preferences for AI features */
    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .catch { emit(UserPreferences()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    /**
     * Start downloading a model.
     */
    fun downloadModel(modelId: String) {
        launchSafe {
            modelRepository.downloadModel(modelId)
        }
    }

    /**
     * Cancel the current download.
     */
    fun cancelDownload() {
        modelRepository.cancelDownload()
    }

    /**
     * Delete an installed model.
     */
    fun deleteModel(modelId: String) {
        launchSafe {
            modelRepository.deleteModel(modelId)
        }
    }

    /**
     * Select a model as the active model.
     */
    fun selectModel(modelId: String) {
        launchSafe {
            modelRepository.selectModel(modelId)
            preferencesRepository.setSelectedModelId(modelId)
        }
    }

    /**
     * Toggle AI categorization feature.
     */
    fun setAICategorizationEnabled(enabled: Boolean) {
        launchSafe {
            preferencesRepository.setAICategorizationEnabled(enabled)
        }
    }
}

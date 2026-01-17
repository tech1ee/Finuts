package com.finuts.domain.repository

import com.finuts.domain.model.AppLanguage
import com.finuts.domain.model.AppTheme
import com.finuts.domain.model.UserGoal
import com.finuts.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repository for user preferences stored in DataStore.
 */
interface PreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setTheme(theme: AppTheme)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setDefaultCurrency(currency: String)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setBiometricEnabled(enabled: Boolean)

    // Onboarding-related methods
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setUserGoal(goal: UserGoal)

    // AI-related methods
    suspend fun setAICategorizationEnabled(enabled: Boolean)
    suspend fun setSelectedModelId(modelId: String?)
    suspend fun setAIModelDownloadedInOnboarding(downloaded: Boolean)

    suspend fun clearAll()
}

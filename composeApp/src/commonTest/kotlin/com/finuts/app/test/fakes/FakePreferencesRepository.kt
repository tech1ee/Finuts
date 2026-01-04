package com.finuts.app.test.fakes

import com.finuts.domain.model.AppLanguage
import com.finuts.domain.model.AppTheme
import com.finuts.domain.model.UserGoal
import com.finuts.domain.model.UserPreferences
import com.finuts.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of PreferencesRepository for ViewModel testing.
 */
class FakePreferencesRepository : PreferencesRepository {

    private val _preferences = MutableStateFlow(UserPreferences())

    override val preferences: Flow<UserPreferences> = _preferences

    override suspend fun setTheme(theme: AppTheme) {
        _preferences.update { it.copy(theme = theme) }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        _preferences.update { it.copy(language = language) }
    }

    override suspend fun setDefaultCurrency(currency: String) {
        _preferences.update { it.copy(defaultCurrency = currency) }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        _preferences.update { it.copy(notificationsEnabled = enabled) }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        _preferences.update { it.copy(biometricEnabled = enabled) }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _preferences.update { it.copy(onboardingCompleted = completed) }
    }

    override suspend fun setUserGoal(goal: UserGoal) {
        _preferences.update { it.copy(userGoal = goal) }
    }

    override suspend fun clearAll() {
        _preferences.value = UserPreferences()
    }

    // Test helpers
    fun setPreferences(newPreferences: UserPreferences) {
        _preferences.value = newPreferences
    }

    fun currentValue(): UserPreferences = _preferences.value
}

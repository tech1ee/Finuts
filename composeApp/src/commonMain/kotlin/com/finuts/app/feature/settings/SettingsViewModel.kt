package com.finuts.app.feature.settings

import androidx.lifecycle.viewModelScope
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.model.AppLanguage
import com.finuts.domain.model.AppTheme
import com.finuts.domain.model.UserPreferences
import com.finuts.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for Settings screen.
 * Handles user preferences management.
 */
class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : BaseViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .catch { emit(UserPreferences()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    fun setTheme(theme: AppTheme) {
        launchSafe { preferencesRepository.setTheme(theme) }
    }

    fun setLanguage(language: AppLanguage) {
        launchSafe { preferencesRepository.setLanguage(language) }
    }

    fun setDefaultCurrency(currency: String) {
        launchSafe { preferencesRepository.setDefaultCurrency(currency) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        launchSafe { preferencesRepository.setNotificationsEnabled(enabled) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        launchSafe { preferencesRepository.setBiometricEnabled(enabled) }
    }

    companion object {
        val SUPPORTED_CURRENCIES = listOf("KZT", "RUB", "USD", "EUR")
    }
}

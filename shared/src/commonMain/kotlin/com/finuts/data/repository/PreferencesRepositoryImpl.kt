package com.finuts.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finuts.domain.model.AppLanguage
import com.finuts.domain.model.AppTheme
import com.finuts.domain.model.UserGoal
import com.finuts.domain.model.UserPreferences
import com.finuts.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed implementation of PreferencesRepository.
 */
class PreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            theme = prefs[KEY_THEME]?.let { AppTheme.valueOf(it) } ?: AppTheme.SYSTEM,
            language = prefs[KEY_LANGUAGE]?.let { code ->
                if (code == LANGUAGE_SYSTEM) AppLanguage.SYSTEM
                else AppLanguage.entries.find { it.code == code }
            } ?: AppLanguage.SYSTEM,
            defaultCurrency = prefs[KEY_CURRENCY] ?: "KZT",
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
            biometricEnabled = prefs[KEY_BIOMETRIC] ?: false,
            onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            userGoal = prefs[KEY_USER_GOAL]?.let { UserGoal.valueOf(it) } ?: UserGoal.NOT_SET
        )
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[KEY_THEME] = theme.name }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[KEY_LANGUAGE] = language.code ?: LANGUAGE_SYSTEM }
    }

    override suspend fun setDefaultCurrency(currency: String) {
        dataStore.edit { it[KEY_CURRENCY] = currency }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BIOMETRIC] = enabled }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setUserGoal(goal: UserGoal) {
        dataStore.edit { it[KEY_USER_GOAL] = goal.name }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    companion object {
        private const val LANGUAGE_SYSTEM = "system"
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_CURRENCY = stringPreferencesKey("default_currency")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_USER_GOAL = stringPreferencesKey("user_goal")
    }
}

package com.finuts.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.finuts.app.core.locale.LocalizedApp
import com.finuts.app.core.locale.appLocale
import com.finuts.app.core.locale.getDefaultLocale
import com.finuts.app.navigation.AppNavigation
import com.finuts.app.theme.FinutsTheme
import com.finuts.domain.model.AppLanguage
import com.finuts.domain.model.UserPreferences
import com.finuts.domain.repository.PreferencesRepository
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App() {
    KoinContext {
        // Get preferences and update locale when language changes
        val preferencesRepository: PreferencesRepository = koinInject()
        val preferences by preferencesRepository.preferences.collectAsState(
            initial = UserPreferences()
        )

        LaunchedEffect(preferences.language) {
            appLocale = when (preferences.language) {
                AppLanguage.SYSTEM -> getDefaultLocale()
                AppLanguage.RUSSIAN -> "ru"
                AppLanguage.ENGLISH -> "en"
                AppLanguage.KAZAKH -> "kk"
            }
        }

        LocalizedApp {
            FinutsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

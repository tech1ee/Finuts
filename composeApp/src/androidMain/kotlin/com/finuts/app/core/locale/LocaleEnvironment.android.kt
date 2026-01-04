package com.finuts.app.core.locale

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

private var defaultLocale: Locale? = null

/**
 * Get system locale on Android.
 */
actual fun getSystemLocale(): String = Locale.getDefault().language

/**
 * Android implementation of LocalizedApp.
 *
 * Updates:
 * 1. Locale.setDefault() - global JVM locale
 * 2. Context.resources.configuration - for resource loading
 * 3. LocalConfiguration - Compose composition local
 *
 * Provides updated LocalConfiguration to the tree, which is what
 * Compose Resources reads to determine the locale for stringResource().
 */
@Composable
actual fun LocalizedApp(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val currentConfig = LocalConfiguration.current

    // Save default locale on first call
    if (defaultLocale == null) {
        defaultLocale = Locale.getDefault()
    }

    val newLocale = remember(appLocale) { Locale(appLocale) }

    // Create updated configuration with new locale
    val updatedConfig = remember(appLocale, currentConfig) {
        Configuration(currentConfig).apply {
            setLocale(newLocale)
        }
    }

    // Apply locale changes as side effect
    LaunchedEffect(appLocale) {
        // Set JVM default locale
        Locale.setDefault(newLocale)

        // Update context resources configuration
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(
            updatedConfig,
            context.resources.displayMetrics
        )
    }

    // key() forces complete recomposition when locale changes
    // This makes all stringResource() calls re-read with new locale
    androidx.compose.runtime.key(appLocale) {
        // Provide updated configuration to Compose tree
        CompositionLocalProvider(
            LocalConfiguration provides updatedConfig,
            LocalAppLocale provides appLocale
        ) {
            content()
        }
    }
}

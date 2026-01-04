package com.finuts.app.core.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

private const val LANG_KEY = "AppleLanguages"

/**
 * iOS implementation of LocalizedApp.
 *
 * NOTE: iOS does not support runtime locale change for resources.
 * NSUserDefaults change only takes effect after app restart.
 *
 * This implementation:
 * 1. Saves locale preference to NSUserDefaults
 * 2. Uses key() to force recomposition (won't help stringResource)
 * 3. Provides LocalAppLocale for UI that needs to know current setting
 *
 * The actual locale change for stringResource() requires app restart.
 * This is standard iOS behavior - Apple expects language changes
 * to require restart.
 */
@Composable
actual fun LocalizedApp(content: @Composable () -> Unit) {
    // Save locale preference to NSUserDefaults
    // This takes effect on next app launch
    LaunchedEffect(appLocale) {
        NSUserDefaults.standardUserDefaults.setObject(
            listOf(appLocale),
            LANG_KEY
        )
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    // key() forces recomposition when locale changes
    // For iOS, this helps UI components that read LocalAppLocale
    // but won't affect stringResource() until restart
    androidx.compose.runtime.key(appLocale) {
        CompositionLocalProvider(LocalAppLocale provides appLocale) {
            content()
        }
    }
}

/**
 * Get the system's preferred locale from iOS.
 * Uses NSBundle.mainBundle.preferredLocalizations for app-specific locale,
 * fallback to "en" if not available.
 */
@Suppress("UNCHECKED_CAST")
actual fun getSystemLocale(): String {
    // Get the system's preferred languages from NSBundle
    val localizations = NSBundle.mainBundle.preferredLocalizations as? List<String>
    return localizations?.firstOrNull() ?: "en"
}

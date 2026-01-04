package com.finuts.app.core.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Get the system's preferred locale code.
 * Returns "ru" if Russian, "en" otherwise.
 */
expect fun getSystemLocale(): String

/**
 * Determine default app locale based on system language.
 * Russian only if system is Russian, otherwise English.
 */
fun getDefaultLocale(): String {
    val systemLocale = getSystemLocale().lowercase()
    return if (systemLocale.startsWith("ru")) "ru" else "en"
}

/**
 * Current app locale code ("en", "ru", "kk").
 * Initialized based on system locale.
 * Changes trigger full UI recomposition via key().
 */
var appLocale by mutableStateOf(getDefaultLocale())

/**
 * Composition local for accessing current locale in UI.
 */
val LocalAppLocale = compositionLocalOf { getDefaultLocale() }

/**
 * Wraps content with locale-aware composition.
 *
 * Platform-specific implementations handle:
 * - Android: Locale.setDefault() + Configuration update via LocalConfiguration
 * - iOS: NSUserDefaults update (takes effect on next app launch)
 *
 * Uses key(appLocale) to force complete recomposition tree rebuild,
 * ensuring Compose Resources re-reads the locale on each change.
 */
@Composable
expect fun LocalizedApp(content: @Composable () -> Unit)

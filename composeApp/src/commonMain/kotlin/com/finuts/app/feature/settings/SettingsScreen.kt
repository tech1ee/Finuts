package com.finuts.app.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.feature.settings.dialogs.SelectionDialog
import com.finuts.app.feature.settings.dialogs.ThemeSelectionDialog
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.navigation.FinutsTopBar
import com.finuts.app.ui.components.settings.SettingsGroup
import com.finuts.app.ui.components.settings.SettingsRow
import com.finuts.app.ui.components.settings.SettingsToggleRow
import com.finuts.domain.model.AppLanguage
import com.finuts.domain.model.AppTheme
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.appearance
import finuts.composeapp.generated.resources.biometric_lock
import finuts.composeapp.generated.resources.default_currency
import finuts.composeapp.generated.resources.language
import finuts.composeapp.generated.resources.notifications
import finuts.composeapp.generated.resources.preferences
import finuts.composeapp.generated.resources.security
import finuts.composeapp.generated.resources.select_currency
import finuts.composeapp.generated.resources.select_language
import finuts.composeapp.generated.resources.settings
import finuts.composeapp.generated.resources.theme
import finuts.composeapp.generated.resources.version
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings Screen with app preferences.
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val prefs by viewModel.preferences.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        FinutsTopBar(title = stringResource(Res.string.settings))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = FinutsSpacing.md, bottom = FinutsSpacing.bottomNavHeight + FinutsSpacing.lg)
        ) {
            // Appearance
            SettingsGroup(
                title = stringResource(Res.string.appearance),
                icon = Icons.Default.Palette,
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            ) {
                SettingsRow(
                    title = stringResource(Res.string.theme),
                    value = prefs.theme.displayName(),
                    onClick = { showThemeDialog = true }
                )
                SettingsRow(
                    title = stringResource(Res.string.language),
                    value = prefs.language.displayName,
                    onClick = { showLanguageDialog = true }
                )
            }

            Spacer(Modifier.height(FinutsSpacing.settingsGroupGap))

            // Preferences
            SettingsGroup(
                title = stringResource(Res.string.preferences),
                icon = Icons.Default.AttachMoney,
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            ) {
                SettingsRow(
                    title = stringResource(Res.string.default_currency),
                    value = prefs.defaultCurrency,
                    onClick = { showCurrencyDialog = true }
                )
            }

            Spacer(Modifier.height(FinutsSpacing.settingsGroupGap))

            // Security
            SettingsGroup(
                title = stringResource(Res.string.security),
                icon = Icons.Default.Lock,
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            ) {
                SettingsToggleRow(
                    title = stringResource(Res.string.notifications),
                    checked = prefs.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
                SettingsToggleRow(
                    title = stringResource(Res.string.biometric_lock),
                    checked = prefs.biometricEnabled,
                    onCheckedChange = { viewModel.setBiometricEnabled(it) }
                )
            }

            Spacer(Modifier.height(FinutsSpacing.sectionGap))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "${stringResource(Res.string.version)} 1.0.0-alpha",
                    style = FinutsTypography.bodySmall,
                    color = FinutsColors.TextTertiary
                )
            }
        }
    }

    // Dialogs
    if (showThemeDialog) {
        ThemeSelectionDialog(
            options = AppTheme.entries.toList(),
            selected = prefs.theme,
            onSelect = { viewModel.setTheme(it); showThemeDialog = false },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showLanguageDialog) {
        SelectionDialog(
            title = stringResource(Res.string.select_language),
            options = AppLanguage.entries.map { it to it.displayName },
            selected = prefs.language,
            onSelect = { viewModel.setLanguage(it); showLanguageDialog = false },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showCurrencyDialog) {
        SelectionDialog(
            title = stringResource(Res.string.select_currency),
            options = SettingsViewModel.SUPPORTED_CURRENCIES.map { it to it },
            selected = prefs.defaultCurrency,
            onSelect = { viewModel.setDefaultCurrency(it); showCurrencyDialog = false },
            onDismiss = { showCurrencyDialog = false }
        )
    }
}

private fun AppTheme.displayName(): String = when (this) {
    AppTheme.LIGHT -> "Light"
    AppTheme.DARK -> "Dark"
    AppTheme.SYSTEM -> "System"
}

package com.finuts.app.feature.settings.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.domain.model.AppTheme
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.cancel
import finuts.composeapp.generated.resources.select_theme
import finuts.composeapp.generated.resources.theme_dark
import finuts.composeapp.generated.resources.theme_light
import finuts.composeapp.generated.resources.theme_system
import org.jetbrains.compose.resources.stringResource

/**
 * Theme selection dialog with localized labels.
 */
@Composable
fun ThemeSelectionDialog(
    options: List<AppTheme>,
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    val title = stringResource(Res.string.select_theme)
    val cancelText = stringResource(Res.string.cancel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = FinutsTypography.headlineSmall, color = FinutsColors.TextPrimary)
        },
        text = {
            Column {
                options.forEach { theme ->
                    val label = when (theme) {
                        AppTheme.LIGHT -> stringResource(Res.string.theme_light)
                        AppTheme.DARK -> stringResource(Res.string.theme_dark)
                        AppTheme.SYSTEM -> stringResource(Res.string.theme_system)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(theme) }
                            .padding(FinutsSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = theme == selected, onClick = { onSelect(theme) })
                        Spacer(Modifier.width(FinutsSpacing.sm))
                        Text(label, style = FinutsTypography.bodyLarge, color = FinutsColors.TextPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Text(
                text = cancelText,
                style = FinutsTypography.labelLarge,
                color = FinutsColors.Accent,
                modifier = Modifier.clickable(onClick = onDismiss).padding(FinutsSpacing.md)
            )
        }
    )
}

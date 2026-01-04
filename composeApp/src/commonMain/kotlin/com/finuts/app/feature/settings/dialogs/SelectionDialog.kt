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
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.cancel
import org.jetbrains.compose.resources.stringResource

/**
 * Generic selection dialog with radio button options.
 */
@Composable
fun <T> SelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val cancelText = stringResource(Res.string.cancel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = FinutsTypography.headlineSmall,
                color = FinutsColors.TextPrimary
            )
        },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(FinutsSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value) })
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

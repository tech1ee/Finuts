package com.finuts.app.ui.components.form

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Consistent label for form fields.
 * Includes spacing below the label.
 */
@Composable
fun FormLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = FinutsTypography.labelMedium,
        color = FinutsColors.TextSecondary,
        modifier = modifier
    )
    Spacer(Modifier.height(FinutsSpacing.xs))
}

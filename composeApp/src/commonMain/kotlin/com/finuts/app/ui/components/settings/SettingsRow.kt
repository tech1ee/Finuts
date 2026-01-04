package com.finuts.app.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Settings Row - Individual setting item
 *
 * Design: Clickable row with title, value, and optional chevron
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 16dp │ Theme                                System  →  │ 16dp│
 * │      │ ← 16sp Regular                  14sp → │ 16dp │      │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Calculated Specs (from DESIGN_SYSTEM.md):
 * - Height: 56dp (touch-friendly)
 * - Horizontal padding: 16dp
 * - Title: 16sp Regular, primary color
 * - Value: 14sp Regular, secondary color
 * - Chevron: 16dp, tertiary color
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FinutsSpacing.settingsRowHeight)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = FinutsSpacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title
        Text(
            text = title,
            style = FinutsTypography.bodyLarge,
            color = FinutsColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        // Value (optional)
        if (value != null) {
            Text(
                text = value,
                style = FinutsTypography.bodyMedium,
                color = FinutsColors.TextSecondary
            )
        }

        // Chevron (optional)
        if (showChevron && onClick != null) {
            Spacer(modifier = Modifier.width(FinutsSpacing.xs))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = FinutsColors.TextTertiary,
                modifier = Modifier.size(FinutsSpacing.iconSmall)
            )
        }
    }
}

/**
 * Settings Row with Toggle Switch
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 16dp │ Notifications                           🔵         │ 16dp│
 * └─────────────────────────────────────────────────────────────┘
 */
@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FinutsSpacing.settingsRowHeight)
            .padding(horizontal = FinutsSpacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title
        Text(
            text = title,
            style = FinutsTypography.bodyLarge,
            color = if (enabled) FinutsColors.TextPrimary else FinutsColors.TextDisabled,
            modifier = Modifier.weight(1f)
        )

        // Toggle Switch
        FinutsSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

package com.finuts.app.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Settings Group Card - Container for grouped settings
 *
 * Design: Grouped card with header icon and title
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 16dp │ 🎨  │ 12dp │ Appearance              ← 18sp SemiBold │
 * ├──────┴─────┴──────┴─────────────────────────────────────────┤
 * │ 16dp │            Theme                    System  →  │ 16dp│
 * │      │                                                │     │
 * ├──────┼────────────────────────────────────────────────┼─────┤
 * │ 16dp │            Language                 Русский →  │ 16dp│
 * └─────────────────────────────────────────────────────────────┘
 *
 * Calculated Specs (from DESIGN_SYSTEM.md):
 * - Corner radius: 16dp
 * - Background: Surface
 * - Border: 1dp Border color
 * - Header height: 56dp
 * - Icon: 24dp, accent color
 * - Icon to title gap: 12dp
 * - Title: 18sp SemiBold
 * - Padding: 16dp horizontal
 */

private val CardShape = RoundedCornerShape(FinutsSpacing.settingsCardRadius)

@Composable
fun SettingsGroup(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(FinutsColors.Surface)
            .border(
                width = 1.dp,
                color = FinutsColors.Border,
                shape = CardShape
            )
    ) {
        // Header with icon and title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FinutsSpacing.settingsHeaderHeight)
                .padding(horizontal = FinutsSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FinutsColors.Accent,
                modifier = Modifier.size(FinutsSpacing.iconMedium)
            )

            Spacer(modifier = Modifier.width(FinutsSpacing.iconToTextGap))

            Text(
                text = title,
                style = FinutsTypography.headlineSmall,
                color = FinutsColors.TextPrimary
            )
        }

        // Divider after header
        HorizontalDivider(
            color = FinutsColors.BorderSubtle,
            thickness = 1.dp
        )

        // Content (rows)
        content()
    }
}

/**
 * Settings Group without header (simple card wrapper)
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(FinutsColors.Surface)
            .border(
                width = 1.dp,
                color = FinutsColors.Border,
                shape = CardShape
            ),
        content = content
    )
}

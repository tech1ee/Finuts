package com.finuts.app.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.icons.FinutsIcons

/**
 * Finuts Top App Bar component.
 *
 * Material Design 3 specifications:
 * - Height: 64dp
 * - Title: left-aligned, BodyLarge
 * - Background: Surface
 * - Actions: IconButtons with 24dp icons
 *
 * @param title The title text to display
 * @param modifier Modifier for the top bar
 * @param showBackButton Whether to show the back navigation button
 * @param onBackClick Callback when back button is clicked
 * @param actions Composable slot for action buttons (right side)
 */
@Composable
fun FinutsTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(FinutsColors.Surface)
            .padding(horizontal = FinutsSpacing.xs)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = FinutsIcons.Back,
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp),
                        tint = FinutsColors.TextPrimary
                    )
                }
            }

            Text(
                text = title,
                style = FinutsTypography.titleLarge,
                color = FinutsColors.TextPrimary,
                modifier = Modifier.padding(
                    start = if (showBackButton) 0.dp else FinutsSpacing.md
                )
            )
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

/**
 * Action button for FinutsTopBar.
 *
 * @param icon The icon to display
 * @param contentDescription Accessibility description
 * @param onClick Callback when button is clicked
 */
@Composable
fun TopBarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = FinutsColors.TextPrimary
        )
    }
}

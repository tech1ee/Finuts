package com.finuts.app.ui.components.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Section Header Component
 *
 * Design: Linear/Notion style section headers
 *
 * Layout:
 * ┌──────────────────────────────────────────────┐
 * │ My Accounts                         See All →│
 * └──────────────────────────────────────────────┘
 *
 * Calculated Specs (from DESIGN_SYSTEM.md):
 * - Title: 18sp SemiBold, primary color
 * - Action: 14sp Medium, accent color
 * - Height: 24dp (text height)
 * - Horizontal padding: uses screen padding from parent
 * - Top margin: 32dp (from previous section) — applied by parent
 * - Bottom margin: 16dp (to content) — applied by parent
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FinutsSpacing.sectionHeaderHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Section Title (18sp SemiBold)
        Text(
            text = title,
            style = FinutsTypography.headlineSmall,
            color = FinutsColors.TextPrimary
        )

        // Optional Action Button
        if (actionText != null && onActionClick != null) {
            val interactionSource = remember { MutableInteractionSource() }

            Text(
                text = actionText,
                style = FinutsTypography.labelLarge,
                color = FinutsColors.Accent,
                modifier = Modifier
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onActionClick
                    )
                    .padding(vertical = FinutsSpacing.xs)
            )
        }
    }
}

/**
 * Section Header with built-in margins
 *
 * Use this variant when you want automatic spacing:
 * - 32dp top margin (from previous section)
 * - 16dp bottom margin (to content)
 */
@Composable
fun SectionHeaderWithMargins(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    SectionHeader(
        title = title,
        actionText = actionText,
        onActionClick = onActionClick,
        modifier = modifier
            .padding(
                top = FinutsSpacing.sectionTopMargin,
                bottom = FinutsSpacing.sectionBottomMargin
            )
    )
}

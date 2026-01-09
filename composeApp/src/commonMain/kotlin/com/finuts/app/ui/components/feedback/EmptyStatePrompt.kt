package com.finuts.app.ui.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Empty State Prompt - Inline version for cards/sections
 *
 * Industry best practice: "Two parts instruction, one part delight"
 * Used when a section has no data but provides guidance and a CTA.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │                                                             │
 * │           No Budget Set                                     │ ← TitleSmall
 * │   Create a budget to track your monthly spending.           │ ← BodySmall, muted
 * │                                                             │
 * │           [ Create Budget ]                                 │ ← Accent button
 * │                                                             │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Specs:
 * - Padding: 16dp vertical, 24dp horizontal
 * - Title: TitleSmall, TextPrimary
 * - Description: BodySmall, TextTertiary
 * - Gap: 4dp between title and description, 12dp before button
 */
@Composable
fun EmptyStatePrompt(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = FinutsSpacing.lg,
                vertical = FinutsSpacing.md
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = FinutsTypography.titleSmall,
            color = FinutsColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.xs))

        Text(
            text = description,
            style = FinutsTypography.bodySmall,
            color = FinutsColors.TextTertiary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.sm))

        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = FinutsColors.Accent,
                contentColor = FinutsColors.OnAccent
            )
        ) {
            Text(
                text = actionLabel,
                style = FinutsTypography.labelMedium
            )
        }
    }
}

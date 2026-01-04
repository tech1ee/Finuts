package com.finuts.app.ui.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Empty State Component
 *
 * Never show dead ends - always provide context and action.
 *
 * Layout:
 * ┌────────────────────────────────┐
 * │                                │
 * │      [Illustration 160dp]      │
 * │                                │
 * │    No transactions yet         │  ← HeadlineSmall
 * │                                │
 * │   Add your first transaction   │  ← BodyMedium, muted
 * │   to start tracking            │
 * │                                │
 * │   [ + Add Transaction ]        │  ← Primary button
 * │                                │
 * └────────────────────────────────┘
 *
 * Specs:
 * - Illustration: 160dp max (minimal, not decorative)
 * - Padding: 32dp
 * - Gap between elements: 16dp
 * - CTA: Full width on mobile, max 280dp on tablet
 */
@Composable
fun EmptyState(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    illustration: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(FinutsSpacing.xl)
                .widthIn(max = 320.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Illustration slot
            if (illustration != null) {
                Box(
                    modifier = Modifier.height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    illustration()
                }
                Spacer(modifier = Modifier.height(FinutsSpacing.lg))
            }

            // Title
            Text(
                text = title,
                style = FinutsTypography.headlineSmall,
                color = FinutsColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.sm))

            // Description
            Text(
                text = description,
                style = FinutsTypography.bodyMedium,
                color = FinutsColors.TextTertiary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.lg))

            // CTA Button
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinutsColors.Accent,
                    contentColor = FinutsColors.OnAccent
                ),
                modifier = Modifier.widthIn(min = 200.dp, max = 280.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = FinutsTypography.labelLarge
                )
            }
        }
    }
}

/**
 * Compact empty state for inline use (e.g., in sections)
 */
@Composable
fun EmptyStateCompact(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FinutsSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = FinutsTypography.bodyMedium,
            color = FinutsColors.TextTertiary,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
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
                    style = FinutsTypography.labelLarge
                )
            }
        }
    }
}

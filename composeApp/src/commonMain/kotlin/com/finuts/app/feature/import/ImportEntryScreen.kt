package com.finuts.app.feature.`import`

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Import Entry Screen - File picker entry point.
 *
 * Layout:
 * ┌─────────────────────────────────────────┐
 * │  ←                                      │
 * │                                         │
 * │         ┌─────────────────────┐         │
 * │         │    📄               │         │  ← 64dp icon, muted
 * │         └─────────────────────┘         │
 * │                                         │
 * │    Импортировать выписку                │  ← headlineSmall
 * │                                         │
 * │    Выберите файл в формате              │  ← bodyMedium, TextTertiary
 * │    CSV, OFX, QIF или PDF                │
 * │                                         │
 * │    ┌─────────────────────────────────┐  │
 * │    │     [ Выбрать файл ]            │  │  ← Primary button, 48dp
 * │    └─────────────────────────────────┘  │
 * │                                         │
 * │    Поддерживаемые банки:                │  ← labelMedium, TextTertiary
 * │    Kaspi, Halyk, Jusan, BCC...          │
 * └─────────────────────────────────────────┘
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportEntryScreen(
    onSelectFile: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FinutsColors.Background
                )
            )
        },
        containerColor = FinutsColors.Background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = FinutsSpacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // File icon
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = FinutsColors.TextTertiary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.lg))

            // Title
            Text(
                text = "Импортировать выписку",
                style = FinutsTypography.headlineSmall,
                color = FinutsColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.sm))

            // Description
            Text(
                text = "Выберите файл в формате\nCSV, OFX, QIF или PDF",
                style = FinutsTypography.bodyMedium,
                color = FinutsColors.TextTertiary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.xl))

            // Select file button
            Button(
                onClick = onSelectFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FinutsSpacing.buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinutsColors.Accent,
                    contentColor = FinutsColors.OnAccent
                )
            ) {
                Text(
                    text = "Выбрать файл",
                    style = FinutsTypography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(FinutsSpacing.xl))

            // Supported banks
            Text(
                text = "Поддерживаемые банки:",
                style = FinutsTypography.labelMedium,
                color = FinutsColors.TextTertiary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.xs))

            Text(
                text = "Kaspi, Halyk, Jusan, BCC...",
                style = FinutsTypography.bodySmall,
                color = FinutsColors.TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

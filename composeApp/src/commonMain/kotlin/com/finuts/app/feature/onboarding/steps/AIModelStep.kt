package com.finuts.app.feature.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.domain.model.DownloadProgress
import com.finuts.domain.model.ModelConfig
import compose.icons.TablerIcons
import compose.icons.tablericons.Bulb
import compose.icons.tablericons.Check
import compose.icons.tablericons.CircleX
import compose.icons.tablericons.Download
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.ai_model_step_cta_download
import finuts.composeapp.generated.resources.ai_model_step_cta_skip
import finuts.composeapp.generated.resources.ai_model_step_description
import finuts.composeapp.generated.resources.ai_model_step_download_later
import finuts.composeapp.generated.resources.ai_model_step_downloading
import finuts.composeapp.generated.resources.ai_model_step_error_retry
import finuts.composeapp.generated.resources.ai_model_step_ready
import finuts.composeapp.generated.resources.ai_model_step_title
import finuts.composeapp.generated.resources.ai_model_step_verifying
import finuts.composeapp.generated.resources.cancel
import finuts.composeapp.generated.resources.continue_action
import finuts.composeapp.generated.resources.download_failed
import finuts.composeapp.generated.resources.recommended
import org.jetbrains.compose.resources.stringResource

/**
 * AI Model setup step in onboarding.
 * Allows users to choose and download an AI model for on-device categorization.
 */
@Composable
fun AIModelStep(
    availableModels: List<ModelConfig>,
    recommendedModelId: String?,
    selectedModelId: String?,
    downloadProgress: DownloadProgress,
    onSelectModel: (String) -> Unit,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(FinutsSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(FinutsSpacing.md))

        // Header icon
        Icon(
            imageVector = TablerIcons.Bulb,
            contentDescription = null,
            modifier = Modifier.size(FinutsSpacing.onboardingIconLarge),
            tint = FinutsColors.Accent
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.md))

        // Title
        Text(
            text = stringResource(Res.string.ai_model_step_title),
            style = FinutsTypography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.xs))

        // Description
        Text(
            text = stringResource(Res.string.ai_model_step_description),
            style = FinutsTypography.bodyMedium,
            textAlign = TextAlign.Center,
            color = FinutsColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.lg))

        // Model selection or progress
        when (downloadProgress) {
            is DownloadProgress.Idle,
            is DownloadProgress.Cancelled -> {
                // Show all available models
                Column(
                    verticalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)
                ) {
                    availableModels.forEach { model ->
                        ModelOptionCard(
                            model = model,
                            isSelected = selectedModelId == model.id,
                            isRecommended = model.id == recommendedModelId,
                            onClick = { onSelectModel(model.id) }
                        )
                    }
                }
            }
            is DownloadProgress.Preparing -> {
                DownloadingCard(
                    modelId = downloadProgress.modelId,
                    progress = 0f,
                    statusText = stringResource(Res.string.ai_model_step_downloading),
                    onCancel = onCancelDownload
                )
            }
            is DownloadProgress.Downloading -> {
                DownloadingCardDetailed(
                    downloadProgress = downloadProgress,
                    onCancel = onCancelDownload
                )
            }
            is DownloadProgress.Verifying -> {
                DownloadingCard(
                    modelId = downloadProgress.modelId,
                    progress = 1f,
                    statusText = stringResource(Res.string.ai_model_step_verifying),
                    onCancel = null
                )
            }
            is DownloadProgress.Completed -> {
                CompletedCard(onContinue = onNext)
            }
            is DownloadProgress.Failed -> {
                ErrorCard(onRetry = onStartDownload)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action button (depends on state)
        when (downloadProgress) {
            is DownloadProgress.Idle,
            is DownloadProgress.Cancelled -> {
                Button(
                    onClick = onStartDownload,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedModelId != null
                ) {
                    Icon(
                        imageVector = TablerIcons.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(FinutsSpacing.sm))
                    Text(text = stringResource(Res.string.ai_model_step_cta_download))
                }
            }
            is DownloadProgress.Completed -> {
                // Continue button shown in CompletedCard
            }
            else -> {
                // No action button while downloading/verifying
            }
        }

        // Skip option (not shown when completed)
        if (downloadProgress !is DownloadProgress.Completed) {
            Spacer(modifier = Modifier.height(FinutsSpacing.md))

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(Res.string.ai_model_step_cta_skip))
            }

            Text(
                text = stringResource(Res.string.ai_model_step_download_later),
                style = FinutsTypography.bodySmall,
                textAlign = TextAlign.Center,
                color = FinutsColors.TextTertiary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ModelOptionCard(
    model: ModelConfig,
    isSelected: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FinutsColors.AccentMuted else FinutsColors.Surface
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, FinutsColors.Accent)
        } else {
            BorderStroke(1.dp, FinutsColors.Border)
        }
    ) {
        Column(modifier = Modifier.padding(FinutsSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)
                    ) {
                        Text(
                            text = model.displayName,
                            style = FinutsTypography.titleMedium
                        )
                        if (isRecommended) {
                            Surface(
                                color = FinutsColors.Accent.copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = stringResource(Res.string.recommended),
                                    style = FinutsTypography.labelSmall,
                                    color = FinutsColors.Accent,
                                    modifier = Modifier.padding(
                                        horizontal = FinutsSpacing.sm,
                                        vertical = 2.dp
                                    )
                                )
                            }
                        }
                    }
                    Text(
                        text = model.description,
                        style = FinutsTypography.bodySmall,
                        color = FinutsColors.TextSecondary
                    )
                }
                Surface(
                    color = FinutsColors.SurfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = model.formattedSize,
                        style = FinutsTypography.labelSmall,
                        modifier = Modifier.padding(
                            horizontal = FinutsSpacing.sm,
                            vertical = FinutsSpacing.xs
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadingCard(
    modelId: String,
    progress: Float,
    statusText: String,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(FinutsSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.ai_model_step_downloading),
                    style = FinutsTypography.titleMedium
                )
                Text(
                    text = statusText,
                    style = FinutsTypography.bodyMedium,
                    color = FinutsColors.Accent
                )
            }

            Spacer(modifier = Modifier.height(FinutsSpacing.md))

            // Progress bar with Finuts styling: 8dp height, 4dp radius
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = FinutsColors.Accent,
                    trackColor = FinutsColors.SurfaceVariant
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = FinutsColors.Accent,
                    trackColor = FinutsColors.SurfaceVariant
                )
            }

            onCancel?.let { cancel ->
                Spacer(modifier = Modifier.height(FinutsSpacing.md))
                OutlinedButton(
                    onClick = cancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(Res.string.cancel))
                }
            }
        }
    }
}

/**
 * Detailed downloading card showing progress, speed, remaining time, and bytes.
 */
@Composable
private fun DownloadingCardDetailed(
    downloadProgress: DownloadProgress.Downloading,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(FinutsSpacing.md)
        ) {
            // Header: Title and percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.ai_model_step_downloading),
                    style = FinutsTypography.titleMedium
                )
                Text(
                    text = "${downloadProgress.progressPercent}%",
                    style = FinutsTypography.titleMedium,
                    color = FinutsColors.Accent
                )
            }

            Spacer(modifier = Modifier.height(FinutsSpacing.sm))

            // Progress bar with Finuts styling: 8dp height, 4dp radius
            LinearProgressIndicator(
                progress = { downloadProgress.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = FinutsColors.Accent,
                trackColor = FinutsColors.SurfaceVariant
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.sm))

            // Details row: bytes downloaded / total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatBytes(downloadProgress.bytesDownloaded)} / ${formatBytes(downloadProgress.totalBytes)}",
                    style = FinutsTypography.bodySmall,
                    color = FinutsColors.TextSecondary
                )

                // Speed and remaining time
                val speedText = formatSpeed(downloadProgress.speedBytesPerSecond)
                val remainingText = formatRemainingTime(downloadProgress.remainingSeconds)
                Text(
                    text = if (speedText.isNotEmpty()) "$speedText • $remainingText" else remainingText,
                    style = FinutsTypography.bodySmall,
                    color = FinutsColors.TextSecondary
                )
            }

            onCancel?.let { cancel ->
                Spacer(modifier = Modifier.height(FinutsSpacing.md))
                OutlinedButton(
                    onClick = cancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(Res.string.cancel))
                }
            }
        }
    }
}

/**
 * Formats bytes to human-readable string (e.g., "512 MB", "1.2 GB")
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> {
            val mb = bytes / (1024.0 * 1024.0)
            if (mb >= 100) "${mb.toLong()} MB" else "${(mb * 10).toLong() / 10.0} MB"
        }
        else -> {
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            "${(gb * 100).toLong() / 100.0} GB"
        }
    }
}

/**
 * Formats download speed to human-readable string (e.g., "9.5 MB/s")
 */
private fun formatSpeed(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0) return ""
    return when {
        bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
        bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
        else -> {
            val mbps = bytesPerSecond / (1024.0 * 1024.0)
            "${(mbps * 10).toLong() / 10.0} MB/s"
        }
    }
}

/**
 * Formats remaining time to human-readable string (e.g., "~2m 30s", "~45s")
 */
private fun formatRemainingTime(seconds: Long): String {
    if (seconds <= 0) return "..."
    return when {
        seconds < 60 -> "~${seconds}s"
        seconds < 3600 -> {
            val minutes = seconds / 60
            val secs = seconds % 60
            if (secs > 0) "~${minutes}m ${secs}s" else "~${minutes}m"
        }
        else -> {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            if (minutes > 0) "~${hours}h ${minutes}m" else "~${hours}h"
        }
    }
}

@Composable
private fun CompletedCard(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.AccentMuted)
    ) {
        Column(
            modifier = Modifier.padding(FinutsSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = TablerIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(FinutsSpacing.iconLarge),
                tint = FinutsColors.Accent
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.md))

            Text(
                text = stringResource(Res.string.ai_model_step_ready),
                style = FinutsTypography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.lg))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(Res.string.continue_action))
            }
        }
    }
}

@Composable
private fun ErrorCard(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.ErrorMuted)
    ) {
        Column(
            modifier = Modifier.padding(FinutsSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = TablerIcons.CircleX,
                contentDescription = null,
                modifier = Modifier.size(FinutsSpacing.iconLarge),
                tint = FinutsColors.Error
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.md))

            Text(
                text = stringResource(Res.string.download_failed),
                style = FinutsTypography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.lg))

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(Res.string.ai_model_step_error_retry))
            }
        }
    }
}

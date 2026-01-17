package com.finuts.app.feature.settings.ai

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.navigation.FinutsTopBar
import com.finuts.app.ui.components.settings.SettingsToggleRow
import com.finuts.domain.model.DownloadProgress
import com.finuts.domain.model.InstalledModel
import com.finuts.domain.model.ModelConfig
import compose.icons.TablerIcons
import compose.icons.tablericons.Bulb
import compose.icons.tablericons.Check
import compose.icons.tablericons.CircleX
import compose.icons.tablericons.Download
import compose.icons.tablericons.Trash
import compose.icons.tablericons.Bug
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.ai_categorization
import finuts.composeapp.generated.resources.ai_categorization_description
import finuts.composeapp.generated.resources.ai_features
import finuts.composeapp.generated.resources.ai_model_not_installed
import finuts.composeapp.generated.resources.ai_model_step_downloading
import finuts.composeapp.generated.resources.ai_model_step_error_retry
import finuts.composeapp.generated.resources.ai_models_coming_soon
import finuts.composeapp.generated.resources.ai_models_coming_soon_description
import finuts.composeapp.generated.resources.available_models
import finuts.composeapp.generated.resources.current_model
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * AI Features settings screen.
 * Allows users to manage AI models and categorization preferences.
 */
@Composable
fun AIFeaturesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLLMDebug: () -> Unit = {},
    viewModel: AIFeaturesViewModel = koinViewModel()
) {
    val installedModels by viewModel.installedModels.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        FinutsTopBar(
            title = stringResource(Res.string.ai_features),
            showBackButton = true,
            onBackClick = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = FinutsSpacing.screenPadding,
                vertical = FinutsSpacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(FinutsSpacing.md)
        ) {
            // AI Categorization Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FinutsColors.Surface)
                ) {
                    Column(modifier = Modifier.padding(FinutsSpacing.md)) {
                        SettingsToggleRow(
                            title = stringResource(Res.string.ai_categorization),
                            checked = preferences.aiCategorizationEnabled,
                            onCheckedChange = { viewModel.setAICategorizationEnabled(it) }
                        )
                        Text(
                            text = stringResource(Res.string.ai_categorization_description),
                            style = FinutsTypography.bodySmall,
                            color = FinutsColors.TextTertiary,
                            modifier = Modifier.padding(
                                start = FinutsSpacing.cardPadding,
                                end = FinutsSpacing.cardPadding
                            )
                        )
                    }
                }
            }

            // Current Model Section
            item {
                SectionHeader(text = stringResource(Res.string.current_model))
            }

            item {
                if (currentModel != null) {
                    InstalledModelCard(
                        model = currentModel!!,
                        isSelected = true,
                        onDelete = { viewModel.deleteModel(currentModel!!.config.id) },
                        onSelect = null
                    )
                } else {
                    NoModelCard()
                }
            }

            // Debug LLM Button (for testing)
            item {
                OutlinedButton(
                    onClick = onNavigateToLLMDebug,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = TablerIcons.Bug,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(FinutsSpacing.xs))
                    Text(text = "Debug LLM")
                }
            }

            // Download Progress (if active)
            if (downloadProgress !is DownloadProgress.Idle &&
                downloadProgress !is DownloadProgress.Cancelled
            ) {
                item {
                    DownloadProgressCard(
                        progress = downloadProgress,
                        onCancel = { viewModel.cancelDownload() },
                        onRetry = {
                            val modelId = when (val p = downloadProgress) {
                                is DownloadProgress.Failed -> p.modelId
                                else -> null
                            }
                            modelId?.let { viewModel.downloadModel(it) }
                        }
                    )
                }
            }

            // Available Models Section
            item {
                Spacer(modifier = Modifier.height(FinutsSpacing.sm))
                SectionHeader(text = stringResource(Res.string.available_models))
            }

            // Check if any models have download URLs configured
            val modelsAvailable = viewModel.availableModels.any { it.downloadUrl.isNotBlank() }

            if (!modelsAvailable) {
                // Show "coming soon" message instead of non-working download buttons
                item {
                    ComingSoonCard()
                }
            } else {
                items(viewModel.availableModels.filter { it.downloadUrl.isNotBlank() }) { model ->
                    val isInstalled = installedModels.any { it.config.id == model.id }
                    val isCurrentlyDownloading = downloadProgress is DownloadProgress.Downloading &&
                        (downloadProgress as DownloadProgress.Downloading).modelId == model.id

                    AvailableModelCard(
                        model = model,
                        isInstalled = isInstalled,
                        isSelected = currentModel?.config?.id == model.id,
                        isDownloading = isCurrentlyDownloading,
                        onDownload = { viewModel.downloadModel(model.id) },
                        onSelect = { viewModel.selectModel(model.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = FinutsTypography.titleSmall,
        color = FinutsColors.TextSecondary,
        modifier = Modifier.padding(vertical = FinutsSpacing.xs)
    )
}

@Composable
private fun NoModelCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.SurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FinutsSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = TablerIcons.Download,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = FinutsColors.TextTertiary
            )
            Spacer(modifier = Modifier.width(FinutsSpacing.md))
            Text(
                text = stringResource(Res.string.ai_model_not_installed),
                style = FinutsTypography.bodyMedium,
                color = FinutsColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ComingSoonCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.SurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FinutsSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = TablerIcons.Bulb,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = FinutsColors.Accent
            )
            Spacer(modifier = Modifier.height(FinutsSpacing.md))
            Text(
                text = stringResource(Res.string.ai_models_coming_soon),
                style = FinutsTypography.titleMedium,
                color = FinutsColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(FinutsSpacing.xs))
            Text(
                text = stringResource(Res.string.ai_models_coming_soon_description),
                style = FinutsTypography.bodySmall,
                color = FinutsColors.TextSecondary,
                modifier = Modifier.padding(horizontal = FinutsSpacing.md)
            )
        }
    }
}

@Composable
private fun InstalledModelCard(
    model: InstalledModel,
    isSelected: Boolean,
    onDelete: () -> Unit,
    onSelect: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FinutsColors.AccentMuted else FinutsColors.Surface
        )
    ) {
        Column(modifier = Modifier.padding(FinutsSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.config.displayName,
                        style = FinutsTypography.titleMedium
                    )
                    Text(
                        text = model.config.description,
                        style = FinutsTypography.bodySmall,
                        color = FinutsColors.TextSecondary
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = TablerIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = FinutsColors.Accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(FinutsSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = TablerIcons.Trash,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(FinutsSpacing.xs))
                    Text(text = "Delete")
                }
                if (onSelect != null && !isSelected) {
                    Button(
                        onClick = onSelect,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Select")
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailableModelCard(
    model: ModelConfig,
    isInstalled: Boolean,
    isSelected: Boolean,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.Surface)
    ) {
        Column(modifier = Modifier.padding(FinutsSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.displayName,
                        style = FinutsTypography.titleMedium
                    )
                    Text(
                        text = model.description,
                        style = FinutsTypography.bodySmall,
                        color = FinutsColors.TextSecondary
                    )
                }
                Surface(
                    color = FinutsColors.SurfaceVariant,
                    shape = RoundedCornerShape(FinutsSpacing.sm)
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

            Spacer(modifier = Modifier.height(FinutsSpacing.md))

            when {
                isDownloading -> {
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(Res.string.ai_model_step_downloading))
                    }
                }
                isInstalled && isSelected -> {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = TablerIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(FinutsSpacing.xs))
                        Text(text = "Selected")
                    }
                }
                isInstalled -> {
                    Button(
                        onClick = onSelect,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Select")
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = TablerIcons.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(FinutsSpacing.xs))
                        Text(text = "Download")
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(
    progress: DownloadProgress,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (progress) {
                is DownloadProgress.Failed -> FinutsColors.ErrorMuted
                is DownloadProgress.Completed -> FinutsColors.AccentMuted
                else -> FinutsColors.Surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(FinutsSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (progress) {
                is DownloadProgress.Downloading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.ai_model_step_downloading),
                            style = FinutsTypography.titleSmall
                        )
                        Text(
                            text = "${progress.progressPercent}%",
                            style = FinutsTypography.bodyMedium,
                            color = FinutsColors.Accent
                        )
                    }
                    Spacer(modifier = Modifier.height(FinutsSpacing.sm))
                    LinearProgressIndicator(
                        progress = { progress.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = FinutsColors.Accent,
                        trackColor = FinutsColors.SurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(FinutsSpacing.md))
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Cancel")
                    }
                }
                is DownloadProgress.Preparing,
                is DownloadProgress.Verifying -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = FinutsColors.Accent,
                        trackColor = FinutsColors.SurfaceVariant
                    )
                }
                is DownloadProgress.Failed -> {
                    Icon(
                        imageVector = TablerIcons.CircleX,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = FinutsColors.Error
                    )
                    Spacer(modifier = Modifier.height(FinutsSpacing.sm))
                    Text(
                        text = "Download failed",
                        style = FinutsTypography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(FinutsSpacing.md))
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(Res.string.ai_model_step_error_retry))
                    }
                }
                is DownloadProgress.Completed -> {
                    Icon(
                        imageVector = TablerIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = FinutsColors.Accent
                    )
                    Spacer(modifier = Modifier.height(FinutsSpacing.sm))
                    Text(
                        text = "Download complete!",
                        style = FinutsTypography.titleSmall
                    )
                }
                else -> { /* Idle, Cancelled - handled by caller */ }
            }
        }
    }
}

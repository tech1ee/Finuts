package com.finuts.app.feature.settings.ai

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.navigation.FinutsTopBar
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.Trash
import compose.icons.tablericons.X
import org.koin.compose.viewmodel.koinViewModel

/**
 * LLM Debug Screen for testing on-device LLM directly.
 * Allows entering custom prompts and viewing responses without full import flow.
 */
@Composable
fun LLMDebugScreen(
    onNavigateBack: () -> Unit,
    viewModel: LLMDebugViewModel = koinViewModel()
) {
    val currentModel by viewModel.currentModel.collectAsState()
    val testState by viewModel.testState.collectAsState()
    val testHistory by viewModel.testHistory.collectAsState()

    var promptText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        FinutsTopBar(
            title = "LLM Debug",
            showBackButton = true,
            onBackClick = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FinutsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(FinutsSpacing.md)
        ) {
            // Model Status
            item {
                Spacer(modifier = Modifier.height(FinutsSpacing.md))
                ModelStatusCard(modelName = currentModel?.config?.displayName)
            }

            // Prompt Input
            item {
                PromptInputCard(
                    promptText = promptText,
                    onPromptChange = { promptText = it },
                    onTest = { viewModel.testPrompt(promptText) },
                    isLoading = testState is LLMTestState.Loading
                )
            }

            // Sample Prompts
            item {
                SamplePromptsCard(
                    samples = viewModel.samplePrompts,
                    onSelect = { promptText = it }
                )
            }

            // Test Result
            item {
                when (val state = testState) {
                    is LLMTestState.Loading -> LoadingCard()
                    is LLMTestState.Success -> ResultCard(result = state.result)
                    is LLMTestState.Error -> ErrorCard(message = state.message)
                    is LLMTestState.Idle -> { /* Nothing */ }
                }
            }

            // Test History
            if (testHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "History",
                            style = FinutsTypography.titleSmall,
                            color = FinutsColors.TextSecondary
                        )
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(
                                imageVector = TablerIcons.Trash,
                                contentDescription = "Clear history",
                                tint = FinutsColors.TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                items(testHistory) { result ->
                    HistoryItemCard(
                        result = result,
                        onClick = { promptText = result.prompt }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(FinutsSpacing.xl)) }
        }
    }
}

@Composable
private fun ModelStatusCard(modelName: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (modelName != null) FinutsColors.AccentMuted else FinutsColors.ErrorMuted
        )
    ) {
        Row(
            modifier = Modifier.padding(FinutsSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (modelName != null) TablerIcons.Check else TablerIcons.X,
                contentDescription = null,
                tint = if (modelName != null) FinutsColors.Accent else FinutsColors.Error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(FinutsSpacing.sm))
            Column {
                Text(
                    text = if (modelName != null) "Model Ready" else "No Model",
                    style = FinutsTypography.titleSmall
                )
                Text(
                    text = modelName ?: "Please select a model in AI Features",
                    style = FinutsTypography.bodySmall,
                    color = FinutsColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun PromptInputCard(
    promptText: String,
    onPromptChange: (String) -> Unit,
    onTest: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.Surface)
    ) {
        Column(modifier = Modifier.padding(FinutsSpacing.md)) {
            Text(
                text = "Test Prompt",
                style = FinutsTypography.titleSmall,
                modifier = Modifier.padding(bottom = FinutsSpacing.sm)
            )

            OutlinedTextField(
                value = promptText,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter a prompt to test...") },
                minLines = 3,
                maxLines = 8
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.md))

            Button(
                onClick = onTest,
                enabled = promptText.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = FinutsColors.TextPrimary
                    )
                } else {
                    Icon(
                        imageVector = TablerIcons.PlayerPlay,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(FinutsSpacing.sm))
                Text(if (isLoading) "Running..." else "Test LLM")
            }
        }
    }
}

@Composable
private fun SamplePromptsCard(samples: List<String>, onSelect: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.SurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(FinutsSpacing.md)) {
            Text(
                text = "Sample Prompts",
                style = FinutsTypography.labelMedium,
                color = FinutsColors.TextSecondary,
                modifier = Modifier.padding(bottom = FinutsSpacing.sm)
            )

            samples.forEachIndexed { index, sample ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = FinutsSpacing.xs)
                        .clickable { onSelect(sample) },
                    color = FinutsColors.Surface,
                    shape = RoundedCornerShape(FinutsSpacing.sm)
                ) {
                    Text(
                        text = sample.take(50) + if (sample.length > 50) "..." else "",
                        style = FinutsTypography.bodySmall,
                        modifier = Modifier.padding(FinutsSpacing.sm),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FinutsSpacing.lg),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = FinutsColors.Accent
            )
            Spacer(modifier = Modifier.width(FinutsSpacing.md))
            Text(
                text = "Running inference...",
                style = FinutsTypography.bodyMedium,
                color = FinutsColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ResultCard(result: LLMTestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.AccentMuted)
    ) {
        Column(modifier = Modifier.padding(FinutsSpacing.md)) {
            Text(
                text = "Response",
                style = FinutsTypography.titleSmall,
                color = FinutsColors.Accent
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.sm))

            Surface(
                color = FinutsColors.Surface,
                shape = RoundedCornerShape(FinutsSpacing.sm)
            ) {
                Text(
                    text = result.response.ifEmpty { "(empty response)" },
                    style = FinutsTypography.bodyMedium,
                    modifier = Modifier.padding(FinutsSpacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(FinutsSpacing.md))

            // Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricChip("${result.durationMs}ms", "Duration")
                MetricChip("${result.inputTokens}", "Input")
                MetricChip("${result.outputTokens}", "Output")
                MetricChip("%.1f".format(result.tokensPerSecond), "tok/s")
            }
        }
    }
}

@Composable
private fun MetricChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = FinutsTypography.titleMedium,
            color = FinutsColors.TextPrimary
        )
        Text(
            text = label,
            style = FinutsTypography.labelSmall,
            color = FinutsColors.TextTertiary
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.ErrorMuted)
    ) {
        Row(
            modifier = Modifier.padding(FinutsSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = TablerIcons.X,
                contentDescription = null,
                tint = FinutsColors.Error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(FinutsSpacing.sm))
            Column {
                Text(
                    text = "Error",
                    style = FinutsTypography.titleSmall,
                    color = FinutsColors.Error
                )
                Text(
                    text = message,
                    style = FinutsTypography.bodySmall,
                    color = FinutsColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun HistoryItemCard(result: LLMTestResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.Surface)
    ) {
        Column(modifier = Modifier.padding(FinutsSpacing.sm)) {
            Text(
                text = result.prompt.take(40) + "...",
                style = FinutsTypography.bodySmall,
                color = FinutsColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(FinutsSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = result.response.take(30) + "...",
                    style = FinutsTypography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${result.durationMs}ms",
                    style = FinutsTypography.labelSmall,
                    color = FinutsColors.TextTertiary
                )
            }
        }
    }
}

// Extension for formatting
private fun String.format(vararg args: Any?): String {
    // Simple format implementation for Kotlin Multiplatform
    var result = this
    args.forEach { arg ->
        result = result.replaceFirst("%.1f", (arg as? Float)?.let { "%.1f".let { f ->
            val intPart = arg.toInt()
            val decPart = ((arg - intPart) * 10).toInt()
            "$intPart.$decPart"
        } } ?: arg.toString())
    }
    return result
}

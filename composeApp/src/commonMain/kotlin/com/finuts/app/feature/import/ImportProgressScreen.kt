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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.import.ProcessingStep
import com.finuts.app.ui.components.import.ProcessingStepIndicator
import com.finuts.app.ui.components.import.StepState
import com.finuts.domain.entity.import.ImportProgress

/**
 * Import Progress Screen - Shows progress through import steps.
 *
 * Layout:
 * ┌─────────────────────────────────────────┐
 * │  ←  Импорт                   1 из 5     │  ← Step counter
 * │─────────────────────────────────────────│
 * │  ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │  ← Progress bar
 * │                                         │
 * │         ┌─────────────────────┐         │
 * │         │    ⟳ (animated)    │         │  ← 48dp spinner
 * │         └─────────────────────┘         │
 * │                                         │
 * │     Определение формата...              │  ← Current step
 * │                                         │
 * │     ┌───────────────────────────────┐   │
 * │     │ ✓ Файл загружен               │   │
 * │     │ ● Определение формата          │   │
 * │     │ ○ Разбор транзакций           │   │
 * │     │ ○ Поиск дубликатов            │   │
 * │     │ ○ Категоризация               │   │
 * │     └───────────────────────────────┘   │
 * │                                         │
 * │        [ Отмена ]                       │
 * └─────────────────────────────────────────┘
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportProgressScreen(
    progress: ImportProgress,
    filename: String,
    onCancel: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (currentStepIndex, currentStepLabel) = getStepInfo(progress)
    val steps = buildStepsList(currentStepIndex)
    val progressFraction = (currentStepIndex + 1f) / 5f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Импорт") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Text(
                        text = "${currentStepIndex + 1} из 5",
                        style = FinutsTypography.labelMedium,
                        color = FinutsColors.TextSecondary,
                        modifier = Modifier.padding(end = FinutsSpacing.md)
                    )
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
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = FinutsColors.Accent,
                trackColor = FinutsColors.ProgressBackground
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FinutsSpacing.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Spinner
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = FinutsColors.Accent,
                    strokeWidth = 4.dp
                )

                Spacer(modifier = Modifier.height(FinutsSpacing.lg))

                // Current step label
                Text(
                    text = "$currentStepLabel...",
                    style = FinutsTypography.titleMedium,
                    color = FinutsColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(FinutsSpacing.xl))

                // Step indicator
                ProcessingStepIndicator(steps = steps)

                Spacer(modifier = Modifier.height(FinutsSpacing.xl))

                // Cancel button
                TextButton(onClick = onCancel) {
                    Text(
                        text = "Отмена",
                        style = FinutsTypography.labelLarge,
                        color = FinutsColors.TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Get current step index and label from progress.
 */
private fun getStepInfo(progress: ImportProgress): Pair<Int, String> {
    return when (progress) {
        is ImportProgress.Idle -> 0 to "Подготовка"
        is ImportProgress.DetectingFormat -> 0 to "Определение формата"
        is ImportProgress.Parsing -> 1 to "Разбор транзакций"
        is ImportProgress.Validating -> 2 to "Проверка данных"
        is ImportProgress.Deduplicating -> 3 to "Поиск дубликатов"
        is ImportProgress.Categorizing -> 4 to "Категоризация"
        is ImportProgress.AwaitingConfirmation -> 4 to "Готово к просмотру"
        is ImportProgress.Saving -> 4 to "Сохранение"
        is ImportProgress.Completed -> 4 to "Завершено"
        is ImportProgress.Failed -> 4 to "Ошибка"
        is ImportProgress.Cancelled -> 4 to "Отменено"
    }
}

/**
 * Build list of processing steps with states.
 */
private fun buildStepsList(currentIndex: Int): List<ProcessingStep> {
    val labels = listOf(
        "Определение формата",
        "Разбор транзакций",
        "Проверка данных",
        "Поиск дубликатов",
        "Категоризация"
    )

    return labels.mapIndexed { index, label ->
        val state = when {
            index < currentIndex -> StepState.COMPLETED
            index == currentIndex -> StepState.CURRENT
            else -> StepState.PENDING
        }
        ProcessingStep(label = label, state = state)
    }
}

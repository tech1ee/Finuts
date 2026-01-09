package com.finuts.app.ui.components.import

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Data class for a single processing step.
 */
data class ProcessingStep(
    val label: String,
    val state: StepState
)

/**
 * State of a processing step.
 */
enum class StepState {
    COMPLETED,
    CURRENT,
    PENDING;

    val isCompleted: Boolean
        get() = this == COMPLETED
}

/**
 * Processing Step Indicator - Shows progress through import steps.
 *
 * Layout:
 * ┌───────────────────────────────────────────┐
 * │ ✓ Файл загружен                           │  ← Completed step (green check)
 * │ ● Определение формата                     │  ← Current step (pulsing dot)
 * │ ○ Разбор транзакций                       │  ← Pending step (muted)
 * │ ○ Поиск дубликатов                        │
 * │ ○ Категоризация                           │
 * └───────────────────────────────────────────┘
 *
 * Specs:
 * - Row height: 40dp
 * - Icon size: 20dp
 * - Icon-to-text gap: 12dp
 * - Current step animation: scale 1.0 → 1.05 → 1.0, 600ms, infinite
 */
@Composable
fun ProcessingStepIndicator(
    steps: List<ProcessingStep>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        steps.forEach { step ->
            StepRow(step = step)
        }
    }
}

@Composable
private fun StepRow(
    step: ProcessingStep,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = FinutsSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepIcon(state = step.state)

        Spacer(modifier = Modifier.width(FinutsSpacing.iconToTextGap))

        Text(
            text = step.label,
            style = FinutsTypography.bodyMedium,
            color = when (step.state) {
                StepState.COMPLETED -> FinutsColors.TextSecondary
                StepState.CURRENT -> FinutsColors.TextPrimary
                StepState.PENDING -> FinutsColors.TextTertiary
            }
        )
    }
}

@Composable
private fun StepIcon(
    state: StepState,
    modifier: Modifier = Modifier
) {
    val iconSize = 20.dp

    when (state) {
        StepState.COMPLETED -> {
            Box(
                modifier = modifier
                    .size(iconSize)
                    .background(FinutsColors.Accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = FinutsColors.OnAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        StepState.CURRENT -> {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = modifier
                    .size(iconSize)
                    .scale(scale)
                    .background(FinutsColors.Accent, CircleShape)
            )
        }

        StepState.PENDING -> {
            Box(
                modifier = modifier
                    .size(iconSize)
                    .background(FinutsColors.ProgressBackground, CircleShape)
            )
        }
    }
}

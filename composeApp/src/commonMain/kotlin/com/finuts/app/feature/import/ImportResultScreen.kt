package com.finuts.app.feature.`import`

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Import Result Screen - Success or error result.
 *
 * Success Layout:
 * ┌─────────────────────────────────────────┐
 * │                                         │
 * │         ┌─────────────────────┐         │
 * │         │    ✓ (animated)    │         │  ← 80dp, green check
 * │         └─────────────────────┘         │
 * │                                         │
 * │        Импорт завершён!                 │
 * │                                         │
 * │    35 транзакций импортировано          │
 * │    12 дубликатов пропущено              │
 * │                                         │
 * │    ┌─────────────────────────────────┐  │
 * │    │  [ Посмотреть транзакции ]      │  │
 * │    └─────────────────────────────────┘  │
 * │                                         │
 * │    ┌─────────────────────────────────┐  │
 * │    │  [ На главную ]                 │  │
 * │    └─────────────────────────────────┘  │
 * └─────────────────────────────────────────┘
 *
 * Failed Layout:
 * ┌─────────────────────────────────────────┐
 * │                                         │
 * │         ┌─────────────────────┐         │
 * │         │    ✕ (red)         │         │  ← 80dp, error icon
 * │         └─────────────────────┘         │
 * │                                         │
 * │        Ошибка импорта                   │
 * │                                         │
 * │    Не удалось разобрать файл.           │
 * │    Проверьте формат и попробуйте        │
 * │    снова.                               │
 * │                                         │
 * │    [ Попробовать снова ]                │
 * │    [ Отмена ]                           │
 * └─────────────────────────────────────────┘
 */
@Composable
fun ImportResultScreen(
    isSuccess: Boolean,
    savedCount: Int,
    skippedCount: Int,
    duplicateCount: Int,
    errorMessage: String?,
    onViewTransactions: () -> Unit,
    onGoHome: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = FinutsColors.Background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(FinutsSpacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isSuccess) {
                SuccessContent(
                    savedCount = savedCount,
                    skippedCount = skippedCount,
                    duplicateCount = duplicateCount,
                    onViewTransactions = onViewTransactions,
                    onGoHome = onGoHome
                )
            } else {
                FailureContent(
                    errorMessage = errorMessage ?: "Произошла неизвестная ошибка",
                    onRetry = onRetry,
                    onCancel = onGoHome
                )
            }
        }
    }
}

@Composable
private fun SuccessContent(
    savedCount: Int,
    skippedCount: Int,
    duplicateCount: Int,
    onViewTransactions: () -> Unit,
    onGoHome: () -> Unit
) {
    // Success icon
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(FinutsColors.Accent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Success",
            tint = FinutsColors.OnAccent,
            modifier = Modifier.size(48.dp)
        )
    }

    Spacer(modifier = Modifier.height(FinutsSpacing.lg))

    // Title
    Text(
        text = "Импорт завершён!",
        style = FinutsTypography.headlineMedium,
        color = FinutsColors.TextPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(FinutsSpacing.md))

    // Stats
    Text(
        text = "$savedCount транзакций импортировано",
        style = FinutsTypography.bodyMedium,
        color = FinutsColors.TextSecondary,
        textAlign = TextAlign.Center
    )

    if (duplicateCount > 0 || skippedCount > 0) {
        val skippedTotal = duplicateCount + skippedCount
        Text(
            text = "$skippedTotal пропущено",
            style = FinutsTypography.bodyMedium,
            color = FinutsColors.TextTertiary,
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(FinutsSpacing.xl))

    // View transactions button
    Button(
        onClick = onViewTransactions,
        modifier = Modifier
            .fillMaxWidth()
            .height(FinutsSpacing.buttonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = FinutsColors.Accent,
            contentColor = FinutsColors.OnAccent
        )
    ) {
        Text(
            text = "Посмотреть транзакции",
            style = FinutsTypography.labelLarge
        )
    }

    Spacer(modifier = Modifier.height(FinutsSpacing.sm))

    // Go home button
    TextButton(
        onClick = onGoHome,
        modifier = Modifier
            .fillMaxWidth()
            .height(FinutsSpacing.buttonHeight)
    ) {
        Text(
            text = "На главную",
            style = FinutsTypography.labelLarge,
            color = FinutsColors.TextSecondary
        )
    }
}

@Composable
private fun FailureContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    // Error icon
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(FinutsColors.Error, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Error",
            tint = FinutsColors.OnError,
            modifier = Modifier.size(48.dp)
        )
    }

    Spacer(modifier = Modifier.height(FinutsSpacing.lg))

    // Title
    Text(
        text = "Ошибка импорта",
        style = FinutsTypography.headlineMedium,
        color = FinutsColors.TextPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(FinutsSpacing.md))

    // Error message
    Text(
        text = errorMessage,
        style = FinutsTypography.bodyMedium,
        color = FinutsColors.TextSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(FinutsSpacing.xl))

    // Retry button
    Button(
        onClick = onRetry,
        modifier = Modifier
            .fillMaxWidth()
            .height(FinutsSpacing.buttonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = FinutsColors.Accent,
            contentColor = FinutsColors.OnAccent
        )
    ) {
        Text(
            text = "Попробовать снова",
            style = FinutsTypography.labelLarge
        )
    }

    Spacer(modifier = Modifier.height(FinutsSpacing.sm))

    // Cancel button
    TextButton(
        onClick = onCancel,
        modifier = Modifier
            .fillMaxWidth()
            .height(FinutsSpacing.buttonHeight)
    ) {
        Text(
            text = "Отмена",
            style = FinutsTypography.labelLarge,
            color = FinutsColors.TextSecondary
        )
    }
}

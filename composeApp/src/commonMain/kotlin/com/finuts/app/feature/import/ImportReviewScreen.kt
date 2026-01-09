package com.finuts.app.feature.`import`

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.import.BulkSelectionBar
import com.finuts.app.ui.components.import.DuplicateWarningCard
import com.finuts.app.ui.components.import.TransactionDuplicateDisplayStatus
import com.finuts.app.ui.components.import.TransactionReviewItem
import com.finuts.domain.entity.import.DuplicateStatus
import com.finuts.domain.entity.import.ReviewableTransaction
import kotlinx.datetime.LocalDate

/**
 * Import Review Screen - Review transactions with edit/select.
 *
 * Layout:
 * ┌─────────────────────────────────────────┐
 * │  ←  Проверка                 3 из 5     │
 * │─────────────────────────────────────────│
 * │  ████████████████████░░░░░░░░░░░░░░░░░  │
 * │                                         │
 * │  Найдено 47 транзакций                  │
 * │  12 дубликатов · 35 новых               │
 * │                                         │
 * │  ┌─────────────────────────────────────┐│
 * │  │ ⚠️ 12 возможных дубликатов          ││
 * │  │ Снимите галочки с тех, которые      ││
 * │  │ не нужно импортировать              ││
 * │  └─────────────────────────────────────┘│
 * │                                         │
 * │  ─────────────────────────────────────  │
 * │                                         │
 * │  ☑ 🍔 Kaspi Gold | Вкусно и точка       │
 * │     15 янв · -2,500 ₸                   │
 * │                                         │
 * │  ... (LazyColumn)                       │
 * │                                         │
 * │─────────────────────────────────────────│
 * │  Выбрано: 35 из 47                      │
 * │  [ Продолжить ]                         │
 * └─────────────────────────────────────────┘
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportReviewScreen(
    transactions: List<ReviewableTransaction>,
    selectedIndices: Set<Int>,
    duplicateCount: Int,
    stepCounterText: String,
    progressFraction: Float,
    onTransactionToggle: (Int, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectDuplicates: () -> Unit,
    onDeselectAll: () -> Unit,
    onContinue: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = transactions.size
    val selectedCount = selectedIndices.size
    val uniqueCount = totalCount - duplicateCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Проверка") },
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
                        text = stepCounterText,
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
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FinutsSpacing.screenPadding)
            ) {
                Text(
                    text = "Выбрано: $selectedCount из $totalCount",
                    style = FinutsTypography.labelMedium,
                    color = FinutsColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(FinutsSpacing.sm))

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FinutsSpacing.buttonHeight),
                    enabled = selectedCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinutsColors.Accent,
                        contentColor = FinutsColors.OnAccent
                    )
                ) {
                    Text(
                        text = "Продолжить",
                        style = FinutsTypography.labelLarge
                    )
                }
            }
        },
        containerColor = FinutsColors.Background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress bar with accessibility
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .semantics {
                        contentDescription = "Прогресс импорта: ${(progressFraction * 100).toInt()}%"
                    },
                color = FinutsColors.Accent,
                trackColor = FinutsColors.ProgressBackground
            )

            // Header with counts
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FinutsSpacing.screenPadding)
            ) {
                Text(
                    text = "Найдено $totalCount транзакций",
                    style = FinutsTypography.headlineSmall,
                    color = FinutsColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(FinutsSpacing.xs))

                Text(
                    text = "$duplicateCount дубликатов · $uniqueCount новых",
                    style = FinutsTypography.bodySmall,
                    color = FinutsColors.TextTertiary
                )

                // Warning card for duplicates
                if (duplicateCount > 0) {
                    Spacer(modifier = Modifier.height(FinutsSpacing.md))
                    DuplicateWarningCard(duplicateCount = duplicateCount)
                }

                // Bulk selection actions
                Spacer(modifier = Modifier.height(FinutsSpacing.sm))
                BulkSelectionBar(
                    onSelectAll = onSelectAll,
                    onDeselectDuplicates = onDeselectDuplicates,
                    onDeselectAll = onDeselectAll,
                    hasDuplicates = duplicateCount > 0
                )
            }

            HorizontalDivider(color = FinutsColors.Border)

            // Transaction list
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = transactions,
                    key = { index, _ -> index }
                ) { index, transaction ->
                    TransactionReviewItem(
                        categoryEmoji = getCategoryEmoji(transaction.transaction.category),
                        description = transaction.transaction.description,
                        date = formatDate(transaction.transaction.date),
                        amount = transaction.transaction.amount,
                        duplicateStatus = mapDuplicateStatus(transaction.duplicateStatus),
                        matchInfo = getMatchInfo(transaction.duplicateStatus),
                        isSelected = index in selectedIndices,
                        onSelectionChange = { selected ->
                            onTransactionToggle(index, selected)
                        }
                    )

                    HorizontalDivider(
                        color = FinutsColors.BorderSubtle,
                        modifier = Modifier.padding(start = FinutsSpacing.listItemDividerInset)
                    )
                }
            }
        }
    }
}

/**
 * Map domain DuplicateStatus to UI display status.
 */
private fun mapDuplicateStatus(status: DuplicateStatus): TransactionDuplicateDisplayStatus {
    return when (status) {
        is DuplicateStatus.Unique -> TransactionDuplicateDisplayStatus.UNIQUE
        is DuplicateStatus.ProbableDuplicate -> TransactionDuplicateDisplayStatus.PROBABLE_DUPLICATE
        is DuplicateStatus.ExactDuplicate -> TransactionDuplicateDisplayStatus.EXACT_DUPLICATE
    }
}

/**
 * Get match info for duplicate status.
 */
private fun getMatchInfo(status: DuplicateStatus): String? {
    return when (status) {
        is DuplicateStatus.ProbableDuplicate -> "Похож на существующую транзакцию"
        is DuplicateStatus.ExactDuplicate -> "Точная копия существующей транзакции"
        else -> null
    }
}

/**
 * Get category emoji from category ID.
 */
private fun getCategoryEmoji(categoryId: String?): String {
    return when (categoryId) {
        "food" -> "🍔"
        "transport" -> "🚗"
        "shopping" -> "🛒"
        "entertainment" -> "🎮"
        "health" -> "💊"
        "utilities" -> "💡"
        "income" -> "💰"
        else -> "📦"
    }
}

/**
 * Format date for display.
 */
private fun formatDate(date: LocalDate): String {
    val months = listOf(
        "янв", "фев", "мар", "апр", "май", "июн",
        "июл", "авг", "сен", "окт", "ноя", "дек"
    )
    return "${date.dayOfMonth} ${months[date.monthNumber - 1]}"
}

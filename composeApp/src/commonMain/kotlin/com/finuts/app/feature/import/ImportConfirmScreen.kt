package com.finuts.app.feature.`import`

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.import.BalanceChangeCard
import com.finuts.app.ui.components.import.ImportSummaryCard
import com.finuts.domain.entity.Account

/**
 * Import Confirm Screen - Account selection and summary.
 *
 * Layout:
 * ┌─────────────────────────────────────────┐
 * │  ←  Подтверждение            4 из 5     │
 * │─────────────────────────────────────────│
 * │  ██████████████████████████████░░░░░░░  │
 * │                                         │
 * │  Счёт для импорта                       │
 * │  ┌─────────────────────────────────────┐│
 * │  │ 💳 Kaspi Gold                    ▼  ││
 * │  │    Текущий баланс: 125,000 ₸        ││
 * │  └─────────────────────────────────────┘│
 * │                                         │
 * │  ─────────────────────────────────────  │
 * │                                         │
 * │  Сводка импорта                         │
 * │  ┌─────────────────────────────────────┐│
 * │  │ Всего транзакций         35        ││
 * │  │ Расходы                  -87,500 ₸ ││
 * │  │ Доходы                   +12,000 ₸ ││
 * │  │ Период          10 янв - 15 янв    ││
 * │  └─────────────────────────────────────┘│
 * │                                         │
 * │  ─────────────────────────────────────  │
 * │                                         │
 * │  Изменение баланса                      │
 * │  ┌─────────────────────────────────────┐│
 * │  │ Было:    125,000 ₸                 ││
 * │  │ Станет:   49,500 ₸   (-75,500 ₸)   ││
 * │  └─────────────────────────────────────┘│
 * │                                         │
 * │─────────────────────────────────────────│
 * │  [ Импортировать 35 транзакций ]        │
 * └─────────────────────────────────────────┘
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportConfirmScreen(
    accounts: List<Account>,
    selectedAccountId: String?,
    transactionCount: Int,
    totalExpenses: Long,
    totalIncome: Long,
    dateRange: String,
    stepCounterText: String,
    progressFraction: Float,
    onAccountSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val currentBalance = selectedAccount?.balance ?: 0L
    val netChange = totalIncome + totalExpenses
    val newBalance = currentBalance + netChange

    var showAccountDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Подтверждение") },
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
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FinutsSpacing.buttonHeight),
                    enabled = selectedAccountId != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinutsColors.Accent,
                        contentColor = FinutsColors.OnAccent
                    )
                ) {
                    Text(
                        text = "Импортировать $transactionCount транзакций",
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FinutsSpacing.screenPadding)
            ) {
                // Account selector
                Text(
                    text = "Счёт для импорта",
                    style = FinutsTypography.labelMedium,
                    color = FinutsColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(FinutsSpacing.sm))

                AccountSelector(
                    selectedAccount = selectedAccount,
                    accounts = accounts,
                    expanded = showAccountDropdown,
                    onExpandChange = { showAccountDropdown = it },
                    onAccountSelect = {
                        onAccountSelect(it.id)
                        showAccountDropdown = false
                    }
                )

                Spacer(modifier = Modifier.height(FinutsSpacing.lg))

                HorizontalDivider(color = FinutsColors.Border)

                Spacer(modifier = Modifier.height(FinutsSpacing.lg))

                // Summary card
                Text(
                    text = "Сводка импорта",
                    style = FinutsTypography.titleMedium,
                    color = FinutsColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(FinutsSpacing.sm))

                ImportSummaryCard(
                    totalTransactions = transactionCount,
                    totalExpenses = totalExpenses,
                    totalIncome = totalIncome,
                    dateRange = dateRange
                )

                Spacer(modifier = Modifier.height(FinutsSpacing.lg))

                HorizontalDivider(color = FinutsColors.Border)

                Spacer(modifier = Modifier.height(FinutsSpacing.lg))

                // Balance change card
                if (selectedAccount != null) {
                    Text(
                        text = "Изменение баланса",
                        style = FinutsTypography.titleMedium,
                        color = FinutsColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(FinutsSpacing.sm))

                    BalanceChangeCard(
                        currentBalance = currentBalance,
                        newBalance = newBalance
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSelector(
    selectedAccount: Account?,
    accounts: List<Account>,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onAccountSelect: (Account) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(shape)
                .background(FinutsColors.SurfaceVariant)
                .border(1.dp, FinutsColors.Border, shape)
                .clickable { onExpandChange(!expanded) }
                .padding(FinutsSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = selectedAccount?.let { "💳 ${it.name}" } ?: "Выберите счёт",
                    style = FinutsTypography.titleMedium,
                    color = FinutsColors.TextPrimary
                )

                if (selectedAccount != null) {
                    Text(
                        text = "Текущий баланс: ${formatMoney(selectedAccount.balance)}",
                        style = FinutsTypography.bodySmall,
                        color = FinutsColors.TextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand",
                tint = FinutsColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = "💳 ${account.name}",
                                style = FinutsTypography.titleMedium
                            )
                            Text(
                                text = formatMoney(account.balance),
                                style = FinutsTypography.bodySmall,
                                color = FinutsColors.TextSecondary
                            )
                        }
                    },
                    onClick = { onAccountSelect(account) }
                )
            }
        }
    }
}

/**
 * Format money value with currency symbol.
 */
private fun formatMoney(amount: Long): String {
    val formatted = kotlin.math.abs(amount).toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
    return if (amount < 0) "-$formatted ₸" else "$formatted ₸"
}

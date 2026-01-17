package com.finuts.app.feature.`import`

import co.touchlab.kermit.Logger
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finuts.app.feature.`import`.components.ImportAccountSelector
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.import.BalanceChangeCard
import com.finuts.app.ui.components.import.ImportSummaryCard
import com.finuts.domain.entity.Account

/**
 * Import Confirm Screen - Step 4 of import flow.
 * Shows account selector, import summary, and balance change preview.
 */
private val log = Logger.withTag("ImportConfirmScreen")

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
    log.d { "RENDER: accounts=${accounts.size}, selectedAccountId=$selectedAccountId, txCount=$transactionCount" }
    log.d { "RENDER: expenses=$totalExpenses, income=$totalIncome, dateRange=$dateRange" }

    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val currentBalance = selectedAccount?.balance ?: 0L
    val netChange = totalIncome + totalExpenses
    val newBalance = currentBalance + netChange
    val currencySymbol = selectedAccount?.currency?.symbol ?: accounts.firstOrNull()?.currency?.symbol ?: "₸"

    log.d { "CALC: selectedAccount=${selectedAccount?.name}, currentBalance=$currentBalance, netChange=$netChange, newBalance=$newBalance, currency=$currencySymbol" }

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
                    onClick = {
                        log.i { "BUTTON CLICKED: Import $transactionCount transactions, accountId=$selectedAccountId" }
                        onConfirm()
                    },
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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

            Column(modifier = Modifier.fillMaxSize().padding(FinutsSpacing.screenPadding)) {
                Text(
                    text = "Счёт для импорта",
                    style = FinutsTypography.labelMedium,
                    color = FinutsColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(FinutsSpacing.sm))

                ImportAccountSelector(
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
                    dateRange = dateRange,
                    currencySymbol = currencySymbol
                )

                Spacer(modifier = Modifier.height(FinutsSpacing.lg))

                HorizontalDivider(color = FinutsColors.Border)

                Spacer(modifier = Modifier.height(FinutsSpacing.lg))
                if (selectedAccount != null) {
                    Text(
                        text = "Изменение баланса",
                        style = FinutsTypography.titleMedium,
                        color = FinutsColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(FinutsSpacing.sm))

                    BalanceChangeCard(
                        currentBalance = currentBalance,
                        newBalance = newBalance,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }
}


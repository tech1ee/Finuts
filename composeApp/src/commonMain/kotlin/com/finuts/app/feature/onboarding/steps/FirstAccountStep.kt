package com.finuts.app.feature.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.domain.entity.AccountType
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.onboarding_account_balance
import finuts.composeapp.generated.resources.onboarding_account_balance_hint
import finuts.composeapp.generated.resources.onboarding_account_create
import finuts.composeapp.generated.resources.onboarding_account_description
import finuts.composeapp.generated.resources.onboarding_account_name
import finuts.composeapp.generated.resources.onboarding_account_skip
import finuts.composeapp.generated.resources.onboarding_account_title
import finuts.composeapp.generated.resources.onboarding_account_type
import org.jetbrains.compose.resources.stringResource

/**
 * First account setup step with inline form.
 * Allows user to create their first account directly in the onboarding flow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirstAccountStep(
    accountName: String,
    accountType: AccountType,
    initialBalance: String,
    selectedCurrency: String,
    accountNameError: String?,
    balanceError: String?,
    isCreating: Boolean,
    onNameChange: (String) -> Unit,
    onTypeChange: (AccountType) -> Unit,
    onBalanceChange: (String) -> Unit,
    onCreateAccount: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FinutsSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(FinutsSpacing.md))

        Text(
            text = stringResource(Res.string.onboarding_account_title),
            style = FinutsTypography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.xs))

        Text(
            text = stringResource(Res.string.onboarding_account_description),
            style = FinutsTypography.bodyMedium,
            textAlign = TextAlign.Center,
            color = FinutsColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.xl))

        // Account name
        OutlinedTextField(
            value = accountName,
            onValueChange = onNameChange,
            label = { Text(stringResource(Res.string.onboarding_account_name)) },
            isError = accountNameError != null,
            supportingText = accountNameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.md))

        // Account type
        Text(
            text = stringResource(Res.string.onboarding_account_type),
            style = FinutsTypography.labelLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.sm))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)
        ) {
            AccountTypeChip(AccountType.CASH, "Cash", accountType == AccountType.CASH) {
                onTypeChange(AccountType.CASH)
            }
            AccountTypeChip(AccountType.BANK_ACCOUNT, "Bank", accountType == AccountType.BANK_ACCOUNT) {
                onTypeChange(AccountType.BANK_ACCOUNT)
            }
            AccountTypeChip(AccountType.DEBIT_CARD, "Debit", accountType == AccountType.DEBIT_CARD) {
                onTypeChange(AccountType.DEBIT_CARD)
            }
            AccountTypeChip(AccountType.CREDIT_CARD, "Credit", accountType == AccountType.CREDIT_CARD) {
                onTypeChange(AccountType.CREDIT_CARD)
            }
            AccountTypeChip(AccountType.SAVINGS, "Savings", accountType == AccountType.SAVINGS) {
                onTypeChange(AccountType.SAVINGS)
            }
        }

        Spacer(modifier = Modifier.height(FinutsSpacing.md))

        // Initial balance
        OutlinedTextField(
            value = initialBalance,
            onValueChange = { value ->
                // Only allow numbers and decimal separators
                if (value.isEmpty() || value.matches(Regex("^\\d*[.,]?\\d{0,2}$"))) {
                    onBalanceChange(value)
                }
            },
            label = { Text(stringResource(Res.string.onboarding_account_balance)) },
            placeholder = { Text(stringResource(Res.string.onboarding_account_balance_hint)) },
            isError = balanceError != null,
            supportingText = balanceError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = { Text(selectedCurrency) }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.md)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
                enabled = !isCreating
            ) {
                Text(text = stringResource(Res.string.onboarding_account_skip))
            }

            Button(
                onClick = onCreateAccount,
                modifier = Modifier.weight(1f),
                enabled = !isCreating && accountName.isNotBlank()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(FinutsSpacing.md),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(Res.string.onboarding_account_create))
                }
            }
        }
    }
}

@Composable
private fun AccountTypeChip(
    type: AccountType,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FinutsColors.AccentMuted else FinutsColors.Surface
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, FinutsColors.Accent)
        } else {
            BorderStroke(1.dp, FinutsColors.Border)
        }
    ) {
        Text(
            text = label,
            style = FinutsTypography.labelLarge,
            color = if (isSelected) FinutsColors.Accent else FinutsColors.TextPrimary,
            modifier = Modifier.padding(horizontal = FinutsSpacing.md, vertical = FinutsSpacing.sm)
        )
    }
}

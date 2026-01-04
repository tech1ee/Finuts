package com.finuts.app.feature.transactions.quickadd.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Account selection chips for quick add transaction.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddAccountPicker(
    accounts: List<Pair<String, String>>,
    selectedAccountId: String?,
    onAccountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            text = "Account",
            style = FinutsTypography.labelMedium,
            color = FinutsColors.TextSecondary
        )
        Spacer(Modifier.height(FinutsSpacing.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)) {
            accounts.forEach { (id, name) ->
                FilterChip(
                    selected = selectedAccountId == id,
                    onClick = { onAccountChange(id) },
                    label = { Text(name, style = FinutsTypography.labelMedium) }
                )
            }
        }
    }
}

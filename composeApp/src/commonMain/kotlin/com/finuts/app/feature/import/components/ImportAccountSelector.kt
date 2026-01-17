package com.finuts.app.feature.`import`.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.utils.formatAsMoney
import com.finuts.domain.entity.Account

/**
 * Account selector dropdown for import flow.
 */
@Composable
fun ImportAccountSelector(
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
                        text = "Текущий баланс: ${selectedAccount.balance.formatAsMoney()}",
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
                                text = account.balance.formatAsMoney(),
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

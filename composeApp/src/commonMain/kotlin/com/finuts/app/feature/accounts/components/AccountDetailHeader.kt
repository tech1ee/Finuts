package com.finuts.app.feature.accounts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsMoneyTypography
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.icons.AccountIcon
import com.finuts.app.ui.icons.getAccountColor
import com.finuts.domain.entity.Account

/**
 * Account header showing icon, balance and type.
 */
@Composable
fun AccountDetailHeader(
    account: Account,
    modifier: Modifier = Modifier
) {
    val iconColor = getAccountColor(account.type)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FinutsSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            AccountIcon(accountType = account.type, size = 32.dp, tint = iconColor)
        }

        Spacer(Modifier.height(FinutsSpacing.md))

        Text(
            text = formatMoney(account.balance, account.currency.symbol),
            style = FinutsMoneyTypography.displayMedium,
            color = FinutsColors.TextPrimary
        )

        Spacer(Modifier.height(FinutsSpacing.xxs))

        Text(
            text = "Current Balance",
            style = FinutsTypography.bodyMedium,
            color = FinutsColors.TextTertiary
        )

        Spacer(Modifier.height(FinutsSpacing.xs))

        Text(
            text = account.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            style = FinutsTypography.labelMedium,
            color = FinutsColors.TextSecondary
        )
    }
}

private fun formatMoney(amount: Long, symbol: String): String {
    val whole = amount / 100
    val fraction = kotlin.math.abs(amount % 100)
    val formatted = whole.toString().reversed().chunked(3).joinToString(" ").reversed()
    return "$symbol $formatted.${fraction.toString().padStart(2, '0')}"
}

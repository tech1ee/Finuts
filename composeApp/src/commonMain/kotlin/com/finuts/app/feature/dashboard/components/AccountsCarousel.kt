package com.finuts.app.feature.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.feature.dashboard.utils.formatAccountType
import com.finuts.app.feature.dashboard.utils.formatMoney
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.ui.components.cards.AccountCard
import com.finuts.domain.entity.Account

/**
 * Horizontal carousel of account cards on Dashboard.
 */
@Composable
fun AccountsCarousel(
    accounts: List<Account>,
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = FinutsSpacing.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.carouselItemGap)
    ) {
        items(
            items = accounts,
            key = { it.id }
        ) { account ->
            AccountCard(
                bankName = account.name,
                accountNumber = formatAccountType(account.type.name),
                balance = formatMoney(account.balance, account.currency.symbol),
                onClick = { onAccountClick(account.id) }
            )
        }
    }
}

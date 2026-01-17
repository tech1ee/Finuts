package com.finuts.app.feature.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.feature.dashboard.utils.formatMoney
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.ui.components.cards.AccountCard
import com.finuts.app.ui.components.cards.AccountType
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.AccountType as DomainAccountType

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
                name = account.name,
                type = account.type.toUiAccountType(),
                balance = formatMoney(account.balance, account.currency.symbol),
                onClick = { onAccountClick(account.id) }
            )
        }
    }
}

/** Maps domain AccountType to UI AccountType. */
private fun DomainAccountType.toUiAccountType(): AccountType = when (this) {
    DomainAccountType.CASH -> AccountType.CASH
    DomainAccountType.DEBIT_CARD -> AccountType.DEBIT_CARD
    DomainAccountType.CREDIT_CARD -> AccountType.CREDIT_CARD
    DomainAccountType.SAVINGS -> AccountType.SAVINGS
    DomainAccountType.INVESTMENT -> AccountType.INVESTMENT
    DomainAccountType.BANK_ACCOUNT -> AccountType.DEBIT_CARD
    DomainAccountType.CRYPTO -> AccountType.INVESTMENT
    DomainAccountType.OTHER -> AccountType.CASH
}

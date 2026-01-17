package com.finuts.app.feature.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.finuts.app.feature.dashboard.components.AccountsCarousel
import com.finuts.app.feature.dashboard.utils.formatMoney
import com.finuts.app.feature.dashboard.utils.hexToColor
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.ui.components.navigation.FinutsTopBar
import com.finuts.app.ui.components.navigation.TopBarAction
import com.finuts.app.ui.icons.FinutsIcons
import com.finuts.app.ui.components.cards.CategorySpending
import com.finuts.app.ui.components.cards.CategorySpendingList
import com.finuts.app.ui.components.cards.FirstTransactionPromptCard
import com.finuts.app.ui.components.cards.MonthlyOverviewCard
import com.finuts.app.ui.components.cards.calculateFinancialHealth
import com.finuts.app.ui.components.feedback.EmptyStateCompact
import com.finuts.app.ui.components.hero.BalanceHeroCard
import com.finuts.app.ui.components.list.SectionHeader
import com.finuts.domain.entity.Account
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.add_account
import finuts.composeapp.generated.resources.manage
import finuts.composeapp.generated.resources.monthly_overview
import finuts.composeapp.generated.resources.my_accounts
import finuts.composeapp.generated.resources.no_accounts
import finuts.composeapp.generated.resources.top_categories
import finuts.composeapp.generated.resources.total_balance
import org.jetbrains.compose.resources.stringResource

/**
 * Main Dashboard content with all sections.
 */
@Composable
fun DashboardContent(
    totalBalance: Long,
    accounts: List<Account>,
    monthlySpending: Long,
    monthlyIncome: Long,
    monthlyBudget: Long?,
    categorySpending: List<DashboardCategorySpending>,
    healthStatus: HealthStatus,
    periodLabel: String,
    showFirstTransactionPrompt: Boolean,
    onAccountClick: (String) -> Unit,
    onSeeAllAccounts: () -> Unit,
    onAddTransaction: () -> Unit,
    onCreateBudget: () -> Unit,
    onImportClick: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencySymbol = accounts.firstOrNull()?.currency?.symbol ?: "₸"

    val uiCategories = remember(categorySpending) {
        categorySpending.map { cat ->
            CategorySpending(
                id = cat.id,
                name = cat.name,
                icon = cat.icon,
                amount = cat.amount,
                percentage = cat.percentage,
                color = hexToColor(cat.colorHex)
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        FinutsTopBar(
            title = "Dashboard",
            actions = {
                TopBarAction(
                    icon = FinutsIcons.Import,
                    contentDescription = "Import",
                    onClick = onImportClick
                )
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = FinutsSpacing.md,
                bottom = FinutsSpacing.bottomNavHeight + FinutsSpacing.lg
            )
        ) {
            // Hero Balance Card
        item {
            BalanceHeroCard(
                balance = formatMoney(totalBalance, currencySymbol),
                balanceLabel = stringResource(Res.string.total_balance),
                onAddTransaction = onAddTransaction,
                onSend = onSend,
                onReceive = onReceive,
                onHistory = onHistory,
                modifier = Modifier.padding(horizontal = FinutsSpacing.md)
            )
        }

        // First Transaction Prompt (inline, doesn't replace content)
        if (showFirstTransactionPrompt) {
            item { Spacer(modifier = Modifier.height(FinutsSpacing.md)) }

            item {
                FirstTransactionPromptCard(
                    onAddTransaction = onAddTransaction,
                    modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(FinutsSpacing.lg)) }

        // Accounts Section
        item {
            SectionHeader(
                title = stringResource(Res.string.my_accounts),
                actionText = if (accounts.isNotEmpty()) stringResource(Res.string.manage) else null,
                onActionClick = onSeeAllAccounts,
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            )
        }

        item { Spacer(modifier = Modifier.height(FinutsSpacing.sectionHeaderGap)) }

        item {
            if (accounts.isEmpty()) {
                EmptyStateCompact(
                    message = stringResource(Res.string.no_accounts),
                    actionLabel = stringResource(Res.string.add_account),
                    onAction = onSeeAllAccounts
                )
            } else {
                AccountsCarousel(accounts = accounts, onAccountClick = onAccountClick)
            }
        }

        item { Spacer(modifier = Modifier.height(FinutsSpacing.sectionGap)) }

        // Monthly Overview Section
        item {
            SectionHeader(
                title = stringResource(Res.string.monthly_overview),
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            )
        }

        item { Spacer(modifier = Modifier.height(FinutsSpacing.sectionHeaderGap)) }

        item {
            MonthlyOverviewCard(
                spent = monthlySpending,
                budget = monthlyBudget,
                periodLabel = periodLabel,
                currencySymbol = currencySymbol,
                comparisonToLastMonth = 0f,
                onCreateBudget = onCreateBudget,
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            )
        }

        item { Spacer(modifier = Modifier.height(FinutsSpacing.sectionGap)) }

        // Top Categories Section
        item {
            SectionHeader(
                title = stringResource(Res.string.top_categories),
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            )
        }

        item { Spacer(modifier = Modifier.height(FinutsSpacing.sectionHeaderGap)) }

        item {
            CategorySpendingList(
                categories = uiCategories,
                currencySymbol = currencySymbol,
                onAddTransaction = onAddTransaction,
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            )
        }
        }
    }
}

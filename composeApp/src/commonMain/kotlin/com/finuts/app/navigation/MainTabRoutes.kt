package com.finuts.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.finuts.app.feature.accounts.AccountsScreen
import com.finuts.app.feature.budgets.BudgetsScreen
import com.finuts.app.feature.dashboard.DashboardScreen
import com.finuts.app.feature.reports.ReportsScreen
import com.finuts.app.feature.settings.SettingsScreen
import com.finuts.app.feature.transactions.TransactionsScreen

/**
 * Main tab routes: Dashboard, Transactions, Budgets, Settings.
 * Plus Accounts (accessible from Dashboard) and Reports.
 */
fun NavGraphBuilder.mainTabRoutes(
    navController: NavController,
    onShowQuickAdd: () -> Unit
) {
    composable<Route.Dashboard> {
        DashboardScreen(
            onAccountClick = { navController.navigate(Route.AccountDetail(it)) },
            onAddTransactionClick = onShowQuickAdd,
            onAddAccountClick = { navController.navigate(Route.AddAccount) },
            onSeeAllAccountsClick = { navController.navigate(Route.Accounts) },
            onNavigateToHistory = { navController.navigate(Route.Transactions) },
            onCreateBudgetClick = { navController.navigate(Route.AddBudget) },
            onNavigateToImport = { navController.navigate(Route.Import) }
        )
    }

    composable<Route.Accounts> {
        AccountsScreen(
            onAccountClick = { navController.navigate(Route.AccountDetail(it)) },
            onAddAccountClick = { navController.navigate(Route.AddAccount) }
        )
    }

    composable<Route.Transactions> {
        TransactionsScreen(
            onTransactionClick = { navController.navigate(Route.TransactionDetail(it)) },
            onAddTransactionClick = onShowQuickAdd,
            onNavigateToImport = { navController.navigate(Route.Import) }
        )
    }

    composable<Route.Budgets> {
        BudgetsScreen(
            onBudgetClick = { navController.navigate(Route.BudgetDetail(it)) },
            onAddBudgetClick = { navController.navigate(Route.AddBudget) }
        )
    }

    composable<Route.Settings> {
        SettingsScreen(
            onNavigateToCategories = { navController.navigate(Route.CategoryManagement) },
            onNavigateToAIFeatures = { navController.navigate(Route.AIFeatures) }
        )
    }

    composable<Route.Reports> {
        ReportsScreen()
    }
}

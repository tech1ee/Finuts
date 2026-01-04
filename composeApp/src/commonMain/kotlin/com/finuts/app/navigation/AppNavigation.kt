package com.finuts.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.compose.runtime.collectAsState
import com.finuts.app.feature.accounts.AccountDetailScreen
import com.finuts.app.feature.accounts.AccountsScreen
import com.finuts.app.feature.accounts.AddEditAccountScreen
import com.finuts.app.feature.budgets.AddEditBudgetScreen
import com.finuts.app.feature.budgets.BudgetDetailScreen
import com.finuts.app.feature.budgets.BudgetsScreen
import com.finuts.app.feature.dashboard.DashboardScreen
import com.finuts.app.feature.onboarding.OnboardingScreen
import com.finuts.app.feature.reports.ReportsScreen
import com.finuts.app.feature.settings.SettingsScreen
import com.finuts.app.feature.transactions.AddEditTransactionScreen
import com.finuts.app.feature.transactions.QuickAddSheet
import com.finuts.app.feature.transactions.TransactionDetailScreen
import com.finuts.app.feature.transactions.TransactionsScreen
import com.finuts.app.feature.transfers.AddTransferScreen
import com.finuts.app.ui.components.navigation.PillBottomNavBar
import com.finuts.app.ui.components.navigation.PillNavItem
import com.finuts.domain.model.UserPreferences
import com.finuts.domain.repository.PreferencesRepository
import org.koin.compose.koinInject

/**
 * App Navigation with Floating Pill Bottom Bar
 *
 * Architecture v3.0:
 * - No Scaffold - content fills entire screen
 * - Floating pill nav bar overlays at bottom
 * - Screens handle their own bottom padding (bottomNavHeight)
 *
 * 4 Main Tabs (Separation of Concerns):
 * - HOME → Dashboard (Financial STATE: balance, accounts, charts, health)
 * - HISTORY → Transactions (Financial HISTORY: what happened with money)
 * - BUDGETS → Budgets (Financial GOALS: budgets, savings)
 * - SETTINGS → Settings (Configuration)
 *
 * Accounts accessed via Dashboard "Manage" link
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Check onboarding status
    val preferencesRepository: PreferencesRepository = koinInject()
    val preferences by preferencesRepository.preferences.collectAsState(
        initial = UserPreferences()
    )
    val startDestination: Route = if (preferences.onboardingCompleted) {
        Route.Dashboard
    } else {
        Route.Onboarding
    }

    // Show pill nav only on main tabs (not on onboarding)
    val showPillNav = currentDestination?.let { dest ->
        dest.hasRoute<Route.Dashboard>() ||
        dest.hasRoute<Route.Transactions>() ||
        dest.hasRoute<Route.Budgets>() ||
        dest.hasRoute<Route.Settings>()
    } ?: false

    // Map current route to PillNavItem
    val selectedNavItem = when {
        currentDestination?.hasRoute<Route.Dashboard>() == true -> PillNavItem.HOME
        currentDestination?.hasRoute<Route.Transactions>() == true -> PillNavItem.HISTORY
        currentDestination?.hasRoute<Route.Budgets>() == true -> PillNavItem.BUDGETS
        currentDestination?.hasRoute<Route.Settings>() == true -> PillNavItem.SETTINGS
        else -> PillNavItem.HOME
    }

    // Content with floating pill overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Main navigation content (fills entire screen)
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            // Onboarding
            composable<Route.Onboarding> {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Route.Dashboard) {
                            popUpTo(Route.Onboarding) { inclusive = true }
                        }
                    },
                    onNavigateToAddAccount = {
                        navController.navigate(Route.AddAccount(source = "onboarding")) {
                            popUpTo(Route.Onboarding) { inclusive = true }
                        }
                    }
                )
            }

            // Main tabs
            composable<Route.Dashboard> {
                DashboardScreen(
                    onAccountClick = { accountId ->
                        navController.navigate(Route.AccountDetail(accountId))
                    },
                    onAddTransactionClick = {
                        navController.navigate(Route.AddTransaction)
                    },
                    onAddAccountClick = {
                        navController.navigate(Route.AddAccount())
                    },
                    onSeeAllAccountsClick = {
                        navController.navigate(Route.Accounts)
                    },
                    onNavigateToHistory = {
                        navController.navigate(Route.Transactions)
                    }
                )
            }

            composable<Route.Accounts> {
                AccountsScreen(
                    onAccountClick = { accountId ->
                        navController.navigate(Route.AccountDetail(accountId))
                    },
                    onAddAccountClick = {
                        navController.navigate(Route.AddAccount())
                    }
                )
            }

            composable<Route.Transactions> {
                TransactionsScreen(
                    onTransactionClick = { transactionId ->
                        navController.navigate(Route.TransactionDetail(transactionId))
                    },
                    onAddTransactionClick = {
                        navController.navigate(Route.AddTransaction)
                    }
                )
            }

            composable<Route.Budgets> {
                BudgetsScreen(
                    onBudgetClick = { budgetId ->
                        navController.navigate(Route.BudgetDetail(budgetId))
                    },
                    onAddBudgetClick = {
                        navController.navigate(Route.AddBudget)
                    }
                )
            }

            composable<Route.Settings> {
                SettingsScreen()
            }

            composable<Route.Reports> {
                ReportsScreen()
            }

            // Detail screens
            composable<Route.AccountDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.AccountDetail>()
                AccountDetailScreen(
                    accountId = route.accountId,
                    onNavigateBack = { navController.popBackStack() },
                    onEditAccount = { navController.navigate(Route.EditAccount(it)) },
                    onTransactionClick = { navController.navigate(Route.TransactionDetail(it)) },
                    onAddTransaction = { navController.navigate(Route.AddTransaction) }
                )
            }

            composable<Route.TransactionDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.TransactionDetail>()
                TransactionDetailScreen(
                    transactionId = route.transactionId,
                    onNavigateBack = { navController.popBackStack() },
                    onEditTransaction = { navController.navigate(Route.EditTransaction(it)) }
                )
            }

            composable<Route.BudgetDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.BudgetDetail>()
                BudgetDetailScreen(
                    budgetId = route.budgetId,
                    onNavigateBack = { navController.popBackStack() },
                    onEditBudget = { navController.navigate(Route.EditBudget(it)) },
                    onTransactionClick = { navController.navigate(Route.TransactionDetail(it)) }
                )
            }

            // Create/Edit screens
            composable<Route.AddTransaction> {
                QuickAddSheet(
                    onDismiss = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }

            composable<Route.AddAccount> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.AddAccount>()
                AddEditAccountScreen(
                    accountId = null,
                    onNavigateBack = {
                        if (route.source == "onboarding") {
                            navController.navigate(Route.Dashboard) {
                                popUpTo<Route.AddAccount> { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable<Route.EditAccount> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.EditAccount>()
                AddEditAccountScreen(
                    accountId = route.accountId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.AddBudget> {
                AddEditBudgetScreen(
                    budgetId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.EditBudget> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.EditBudget>()
                AddEditBudgetScreen(
                    budgetId = route.budgetId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.EditTransaction> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.EditTransaction>()
                AddEditTransactionScreen(
                    transactionId = route.transactionId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.AddTransfer> {
                AddTransferScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Floating Pill Navigation (overlays content)
        if (showPillNav) {
            PillBottomNavBar(
                selectedItem = selectedNavItem,
                onItemSelected = { item ->
                    navController.navigateToPillNavItem(item)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Navigate to pill nav item with proper back stack behavior
 */
private fun NavHostController.navigateToPillNavItem(item: PillNavItem) {
    val route: Route = when (item) {
        PillNavItem.HOME -> Route.Dashboard
        PillNavItem.HISTORY -> Route.Transactions
        PillNavItem.BUDGETS -> Route.Budgets
        PillNavItem.SETTINGS -> Route.Settings
    }

    navigate(route) {
        popUpTo(Route.Dashboard) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title)
    }
}

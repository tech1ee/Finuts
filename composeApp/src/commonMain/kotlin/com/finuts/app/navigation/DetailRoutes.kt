package com.finuts.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.finuts.app.feature.accounts.AccountDetailScreen
import com.finuts.app.feature.budgets.BudgetDetailScreen
import com.finuts.app.feature.settings.ai.AIFeaturesScreen
import com.finuts.app.feature.settings.ai.LLMDebugScreen
import com.finuts.app.feature.transactions.TransactionDetailScreen

/**
 * Detail screen routes: AccountDetail, TransactionDetail, BudgetDetail.
 */
fun NavGraphBuilder.detailRoutes(
    navController: NavController,
    onShowQuickAdd: () -> Unit
) {
    composable<Route.AccountDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.AccountDetail>()
        AccountDetailScreen(
            accountId = route.accountId,
            onNavigateBack = { navController.popBackStack() },
            onEditAccount = { navController.navigate(Route.EditAccount(it)) },
            onTransactionClick = { navController.navigate(Route.TransactionDetail(it)) },
            onAddTransaction = onShowQuickAdd
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

    composable<Route.AIFeatures> {
        AIFeaturesScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToLLMDebug = { navController.navigate(Route.LLMDebug) }
        )
    }

    composable<Route.LLMDebug> {
        LLMDebugScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}

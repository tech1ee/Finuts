package com.finuts.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.finuts.app.feature.accounts.AddEditAccountScreen
import com.finuts.app.feature.budgets.AddEditBudgetScreen
import com.finuts.app.feature.categories.AddEditCategoryScreen
import com.finuts.app.feature.categories.CategoryManagementScreen
import com.finuts.app.feature.transactions.AddEditTransactionScreen
import com.finuts.app.feature.transfers.AddTransferScreen
import com.finuts.domain.entity.CategoryType

/**
 * Create/Edit screen routes: accounts, budgets, transactions, transfers, categories.
 */
fun NavGraphBuilder.createEditRoutes(navController: NavController) {
    // Account routes
    composable<Route.AddAccount> {
        AddEditAccountScreen(
            accountId = null,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<Route.EditAccount> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.EditAccount>()
        AddEditAccountScreen(
            accountId = route.accountId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    // Budget routes
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

    // Transaction routes
    composable<Route.EditTransaction> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.EditTransaction>()
        AddEditTransactionScreen(
            transactionId = route.transactionId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    // Transfer routes
    composable<Route.AddTransfer> {
        AddTransferScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    // Category management routes
    composable<Route.CategoryManagement> {
        CategoryManagementScreen(
            onNavigateBack = { navController.popBackStack() },
            onAddCategory = { type ->
                navController.navigate(Route.AddCategory(type.name))
            },
            onEditCategory = { categoryId ->
                navController.navigate(Route.EditCategory(categoryId))
            }
        )
    }

    composable<Route.AddCategory> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.AddCategory>()
        AddEditCategoryScreen(
            categoryId = null,
            defaultType = CategoryType.valueOf(route.type),
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<Route.EditCategory> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.EditCategory>()
        AddEditCategoryScreen(
            categoryId = route.categoryId,
            defaultType = CategoryType.EXPENSE,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}

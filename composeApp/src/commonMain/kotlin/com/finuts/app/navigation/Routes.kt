package com.finuts.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using @Serializable (Navigation 2.9.1+)
 * All routes are compile-time checked - no string-based errors
 */
@Serializable
sealed interface Route {
    // Onboarding (shown before main app if not completed)
    @Serializable data object Onboarding : Route

    // Main tabs (bottom navigation)
    @Serializable data object Dashboard : Route
    @Serializable data object Accounts : Route
    @Serializable data object Transactions : Route
    @Serializable data object Budgets : Route
    @Serializable data object Reports : Route
    @Serializable data object Settings : Route

    // Detail screens (pass IDs as strings for Room compatibility)
    @Serializable data class AccountDetail(val accountId: String) : Route
    @Serializable data class TransactionDetail(val transactionId: String) : Route
    @Serializable data class BudgetDetail(val budgetId: String) : Route

    // Create/Edit screens
    @Serializable data object AddAccount : Route
    @Serializable data class EditAccount(val accountId: String) : Route
    @Serializable data object AddTransaction : Route
    @Serializable data class EditTransaction(val transactionId: String) : Route
    @Serializable data object AddBudget : Route
    @Serializable data class EditBudget(val budgetId: String) : Route
    @Serializable data object AddTransfer : Route

    // Category management
    @Serializable data object CategoryManagement : Route
    @Serializable data class AddCategory(val type: String = "EXPENSE") : Route
    @Serializable data class EditCategory(val categoryId: String) : Route

    // Bank import
    @Serializable data object Import : Route
}

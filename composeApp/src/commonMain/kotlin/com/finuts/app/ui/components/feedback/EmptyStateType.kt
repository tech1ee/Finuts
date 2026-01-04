package com.finuts.app.ui.components.feedback

/**
 * Defines contextual empty states for different screens and data conditions.
 * Used to show appropriate messaging and CTAs based on user's current state.
 */
sealed interface EmptyStateType {
    // Dashboard empty states
    data object DashboardNoAccounts : EmptyStateType
    data object DashboardNoTransactions : EmptyStateType

    // Transactions screen empty states
    data object TransactionsNoAccounts : EmptyStateType
    data object TransactionsNoData : EmptyStateType

    // Budgets screen empty states
    data object BudgetsEmpty : EmptyStateType

    // Reports screen empty states
    data object ReportsInsufficientData : EmptyStateType
}

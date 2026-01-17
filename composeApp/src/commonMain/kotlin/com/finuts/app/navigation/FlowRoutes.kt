package com.finuts.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.finuts.app.feature.onboarding.OnboardingScreen
import com.finuts.app.feature.`import`.ImportScreen

/**
 * Flow routes: Onboarding and Import flows.
 * These are multi-step user journeys.
 */
fun NavGraphBuilder.flowRoutes(navController: NavController) {
    composable<Route.Onboarding> {
        OnboardingScreen(
            onComplete = {
                navController.navigate(Route.Dashboard) {
                    popUpTo(Route.Onboarding) { inclusive = true }
                }
            }
        )
    }

    composable<Route.Import> {
        ImportScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToTransactions = {
                navController.navigate(Route.Transactions) {
                    popUpTo(Route.Dashboard) { saveState = true }
                    launchSingleTop = true
                }
            }
        )
    }
}

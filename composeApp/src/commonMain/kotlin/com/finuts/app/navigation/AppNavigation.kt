package com.finuts.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finuts.app.feature.transactions.QuickAddSheet
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
 * - HOME -> Dashboard (Financial STATE)
 * - HISTORY -> Transactions (Financial HISTORY)
 * - BUDGETS -> Budgets (Financial GOALS)
 * - SETTINGS -> Settings (Configuration)
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Quick Add Sheet state (shown as overlay, not navigation route)
    var showQuickAddSheet by remember { mutableStateOf(false) }

    // Check onboarding status
    val preferencesRepository: PreferencesRepository = koinInject()
    val preferences by preferencesRepository.preferences.collectAsState(
        initial = UserPreferences()
    )
    val startDestination = NavigationLogic.determineStartDestination(
        preferences.onboardingCompleted
    )

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
            flowRoutes(navController)
            mainTabRoutes(navController) { showQuickAddSheet = true }
            detailRoutes(navController) { showQuickAddSheet = true }
            createEditRoutes(navController)
        }

        // Floating Pill Navigation (overlays content)
        if (showPillNav) {
            PillBottomNavBar(
                selectedItem = selectedNavItem,
                onItemSelected = { navController.navigateToPillNavItem(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Quick Add Transaction Sheet (modal overlay)
        if (showQuickAddSheet) {
            QuickAddSheet(
                onDismiss = { showQuickAddSheet = false },
                onSuccess = { showQuickAddSheet = false }
            )
        }
    }
}

/**
 * Navigate to pill nav item with proper back stack behavior.
 */
private fun NavHostController.navigateToPillNavItem(item: PillNavItem) {
    val route = NavigationLogic.mapPillNavItemToRoute(item)
    navigate(route) {
        popUpTo(Route.Dashboard) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

package com.finuts.app.navigation

import com.finuts.app.ui.components.navigation.PillNavItem

/**
 * Pure functions for navigation logic.
 * Extracted from AppNavigation for testability (SOLID: SRP).
 *
 * All functions are stateless and side-effect free.
 */
object NavigationLogic {

    /** Main tabs that show the pill navigation bar */
    private val MAIN_TABS = setOf(
        Route.Dashboard::class,
        Route.Transactions::class,
        Route.Budgets::class,
        Route.Settings::class
    )

    /**
     * Determines if the pill navigation bar should be visible.
     * Only visible on main tabs (Dashboard, Transactions, Budgets, Settings).
     *
     * @param route Current navigation route (null if unknown)
     * @return true if pill nav should be shown
     */
    fun shouldShowPillNav(route: Route?): Boolean {
        return route != null && isMainTab(route)
    }

    /**
     * Maps current route to PillNavItem for highlighting.
     * Returns HOME as default for non-main-tab routes.
     *
     * @param route Current navigation route (null if unknown)
     * @return Corresponding PillNavItem
     */
    fun mapRouteToPillNavItem(route: Route?): PillNavItem {
        return when (route) {
            is Route.Dashboard -> PillNavItem.HOME
            is Route.Transactions -> PillNavItem.HISTORY
            is Route.Budgets -> PillNavItem.BUDGETS
            is Route.Settings -> PillNavItem.SETTINGS
            else -> PillNavItem.HOME
        }
    }

    /**
     * Maps PillNavItem to Route for navigation.
     *
     * @param item Selected navigation item
     * @return Target route for the item
     */
    fun mapPillNavItemToRoute(item: PillNavItem): Route {
        return when (item) {
            PillNavItem.HOME -> Route.Dashboard
            PillNavItem.HISTORY -> Route.Transactions
            PillNavItem.BUDGETS -> Route.Budgets
            PillNavItem.SETTINGS -> Route.Settings
        }
    }

    /**
     * Determines start destination based on onboarding status.
     *
     * @param onboardingCompleted true if user completed onboarding
     * @return Dashboard if completed, Onboarding otherwise
     */
    fun determineStartDestination(onboardingCompleted: Boolean): Route {
        return if (onboardingCompleted) {
            Route.Dashboard
        } else {
            Route.Onboarding
        }
    }

    /**
     * Checks if a route is one of the main tabs.
     *
     * @param route Route to check
     * @return true if route is a main tab
     */
    fun isMainTab(route: Route): Boolean {
        return route::class in MAIN_TABS
    }
}

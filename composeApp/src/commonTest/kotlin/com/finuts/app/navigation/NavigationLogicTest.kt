package com.finuts.app.navigation

import com.finuts.app.ui.components.navigation.PillNavItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for NavigationLogic - pure functions for navigation state.
 * Extracted from AppNavigation for testability (TDD approach).
 */
class NavigationLogicTest {

    // === shouldShowPillNav Tests ===

    @Test
    fun `shouldShowPillNav returns true for Dashboard`() {
        assertTrue(NavigationLogic.shouldShowPillNav(Route.Dashboard))
    }

    @Test
    fun `shouldShowPillNav returns true for Transactions`() {
        assertTrue(NavigationLogic.shouldShowPillNav(Route.Transactions))
    }

    @Test
    fun `shouldShowPillNav returns true for Budgets`() {
        assertTrue(NavigationLogic.shouldShowPillNav(Route.Budgets))
    }

    @Test
    fun `shouldShowPillNav returns true for Settings`() {
        assertTrue(NavigationLogic.shouldShowPillNav(Route.Settings))
    }

    @Test
    fun `shouldShowPillNav returns false for Onboarding`() {
        assertFalse(NavigationLogic.shouldShowPillNav(Route.Onboarding))
    }

    @Test
    fun `shouldShowPillNav returns false for AccountDetail`() {
        assertFalse(NavigationLogic.shouldShowPillNav(Route.AccountDetail("acc-1")))
    }

    @Test
    fun `shouldShowPillNav returns false for TransactionDetail`() {
        assertFalse(NavigationLogic.shouldShowPillNav(Route.TransactionDetail("tx-1")))
    }

    @Test
    fun `shouldShowPillNav returns false for AddAccount`() {
        assertFalse(NavigationLogic.shouldShowPillNav(Route.AddAccount))
    }

    @Test
    fun `shouldShowPillNav returns false for CategoryManagement`() {
        assertFalse(NavigationLogic.shouldShowPillNav(Route.CategoryManagement))
    }

    @Test
    fun `shouldShowPillNav returns false for Import`() {
        assertFalse(NavigationLogic.shouldShowPillNav(Route.Import))
    }

    @Test
    fun `shouldShowPillNav returns false for null route`() {
        assertFalse(NavigationLogic.shouldShowPillNav(null))
    }

    // === mapRouteToPillNavItem Tests ===

    @Test
    fun `mapRouteToPillNavItem returns HOME for Dashboard`() {
        assertEquals(PillNavItem.HOME, NavigationLogic.mapRouteToPillNavItem(Route.Dashboard))
    }

    @Test
    fun `mapRouteToPillNavItem returns HISTORY for Transactions`() {
        assertEquals(PillNavItem.HISTORY, NavigationLogic.mapRouteToPillNavItem(Route.Transactions))
    }

    @Test
    fun `mapRouteToPillNavItem returns BUDGETS for Budgets`() {
        assertEquals(PillNavItem.BUDGETS, NavigationLogic.mapRouteToPillNavItem(Route.Budgets))
    }

    @Test
    fun `mapRouteToPillNavItem returns SETTINGS for Settings`() {
        assertEquals(PillNavItem.SETTINGS, NavigationLogic.mapRouteToPillNavItem(Route.Settings))
    }

    @Test
    fun `mapRouteToPillNavItem returns HOME for non-main-tab routes`() {
        // Non-main-tab routes default to HOME
        assertEquals(PillNavItem.HOME, NavigationLogic.mapRouteToPillNavItem(Route.Onboarding))
        assertEquals(PillNavItem.HOME, NavigationLogic.mapRouteToPillNavItem(Route.AccountDetail("acc-1")))
        assertEquals(PillNavItem.HOME, NavigationLogic.mapRouteToPillNavItem(Route.Import))
    }

    @Test
    fun `mapRouteToPillNavItem returns HOME for null route`() {
        assertEquals(PillNavItem.HOME, NavigationLogic.mapRouteToPillNavItem(null))
    }

    // === mapPillNavItemToRoute Tests ===

    @Test
    fun `mapPillNavItemToRoute returns Dashboard for HOME`() {
        assertEquals(Route.Dashboard, NavigationLogic.mapPillNavItemToRoute(PillNavItem.HOME))
    }

    @Test
    fun `mapPillNavItemToRoute returns Transactions for HISTORY`() {
        assertEquals(Route.Transactions, NavigationLogic.mapPillNavItemToRoute(PillNavItem.HISTORY))
    }

    @Test
    fun `mapPillNavItemToRoute returns Budgets for BUDGETS`() {
        assertEquals(Route.Budgets, NavigationLogic.mapPillNavItemToRoute(PillNavItem.BUDGETS))
    }

    @Test
    fun `mapPillNavItemToRoute returns Settings for SETTINGS`() {
        assertEquals(Route.Settings, NavigationLogic.mapPillNavItemToRoute(PillNavItem.SETTINGS))
    }

    // === determineStartDestination Tests ===

    @Test
    fun `determineStartDestination returns Dashboard when onboarding completed`() {
        assertEquals(Route.Dashboard, NavigationLogic.determineStartDestination(onboardingCompleted = true))
    }

    @Test
    fun `determineStartDestination returns Onboarding when onboarding not completed`() {
        assertEquals(Route.Onboarding, NavigationLogic.determineStartDestination(onboardingCompleted = false))
    }

    // === isMainTab Tests ===

    @Test
    fun `isMainTab returns true for all main tabs`() {
        assertTrue(NavigationLogic.isMainTab(Route.Dashboard))
        assertTrue(NavigationLogic.isMainTab(Route.Transactions))
        assertTrue(NavigationLogic.isMainTab(Route.Budgets))
        assertTrue(NavigationLogic.isMainTab(Route.Settings))
    }

    @Test
    fun `isMainTab returns false for detail screens`() {
        assertFalse(NavigationLogic.isMainTab(Route.AccountDetail("acc-1")))
        assertFalse(NavigationLogic.isMainTab(Route.TransactionDetail("tx-1")))
        assertFalse(NavigationLogic.isMainTab(Route.BudgetDetail("budget-1")))
    }

    @Test
    fun `isMainTab returns false for create-edit screens`() {
        assertFalse(NavigationLogic.isMainTab(Route.AddAccount))
        assertFalse(NavigationLogic.isMainTab(Route.EditAccount("acc-1")))
        assertFalse(NavigationLogic.isMainTab(Route.AddBudget))
        assertFalse(NavigationLogic.isMainTab(Route.EditBudget("budget-1")))
        assertFalse(NavigationLogic.isMainTab(Route.AddTransfer))
    }

    @Test
    fun `isMainTab returns false for secondary screens`() {
        assertFalse(NavigationLogic.isMainTab(Route.Onboarding))
        assertFalse(NavigationLogic.isMainTab(Route.CategoryManagement))
        assertFalse(NavigationLogic.isMainTab(Route.Import))
        assertFalse(NavigationLogic.isMainTab(Route.Accounts))
        assertFalse(NavigationLogic.isMainTab(Route.Reports))
    }
}

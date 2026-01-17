package com.finuts.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for UserPreferences and related enums.
 */
class UserPreferencesTest {

    @Test
    fun `default UserPreferences has system theme`() {
        val prefs = UserPreferences()
        assertEquals(AppTheme.SYSTEM, prefs.theme)
    }

    @Test
    fun `default UserPreferences has system language`() {
        val prefs = UserPreferences()
        assertEquals(AppLanguage.SYSTEM, prefs.language)
    }

    @Test
    fun `default UserPreferences has KZT currency`() {
        val prefs = UserPreferences()
        assertEquals("KZT", prefs.defaultCurrency)
    }

    @Test
    fun `default UserPreferences has notifications enabled`() {
        val prefs = UserPreferences()
        assertTrue(prefs.notificationsEnabled)
    }

    @Test
    fun `default UserPreferences has biometric disabled`() {
        val prefs = UserPreferences()
        assertFalse(prefs.biometricEnabled)
    }

    @Test
    fun `UserPreferences can be created with custom values`() {
        val prefs = UserPreferences(
            theme = AppTheme.DARK,
            language = AppLanguage.RUSSIAN,
            defaultCurrency = "USD",
            notificationsEnabled = false,
            biometricEnabled = true
        )

        assertEquals(AppTheme.DARK, prefs.theme)
        assertEquals(AppLanguage.RUSSIAN, prefs.language)
        assertEquals("USD", prefs.defaultCurrency)
        assertFalse(prefs.notificationsEnabled)
        assertTrue(prefs.biometricEnabled)
    }

    @Test
    fun `UserPreferences copy works correctly`() {
        val original = UserPreferences()
        val modified = original.copy(theme = AppTheme.LIGHT)

        assertEquals(AppTheme.SYSTEM, original.theme)
        assertEquals(AppTheme.LIGHT, modified.theme)
        assertEquals(original.language, modified.language)
    }

    // AppTheme tests
    @Test
    fun `AppTheme has three values`() {
        assertEquals(3, AppTheme.entries.size)
    }

    @Test
    fun `AppTheme contains LIGHT DARK and SYSTEM`() {
        val themes = AppTheme.entries.map { it.name }
        assertTrue("LIGHT" in themes)
        assertTrue("DARK" in themes)
        assertTrue("SYSTEM" in themes)
    }

    // AppLanguage tests
    @Test
    fun `AppLanguage has four values`() {
        assertEquals(4, AppLanguage.entries.size)
    }

    @Test
    fun `AppLanguage SYSTEM has null code`() {
        assertNull(AppLanguage.SYSTEM.code)
    }

    @Test
    fun `AppLanguage RUSSIAN has ru code`() {
        assertEquals("ru", AppLanguage.RUSSIAN.code)
    }

    @Test
    fun `AppLanguage KAZAKH has kk code`() {
        assertEquals("kk", AppLanguage.KAZAKH.code)
    }

    @Test
    fun `AppLanguage ENGLISH has en code`() {
        assertEquals("en", AppLanguage.ENGLISH.code)
    }

    @Test
    fun `AppLanguage SYSTEM has System displayName`() {
        assertEquals("System", AppLanguage.SYSTEM.displayName)
    }

    @Test
    fun `AppLanguage RUSSIAN has Russian displayName`() {
        assertEquals("Русский", AppLanguage.RUSSIAN.displayName)
    }

    @Test
    fun `AppLanguage KAZAKH has Kazakh displayName`() {
        assertEquals("Қазақша", AppLanguage.KAZAKH.displayName)
    }

    @Test
    fun `AppLanguage ENGLISH has English displayName`() {
        assertEquals("English", AppLanguage.ENGLISH.displayName)
    }

    @Test
    fun `can find AppLanguage by code`() {
        val russian = AppLanguage.entries.find { it.code == "ru" }
        assertEquals(AppLanguage.RUSSIAN, russian)
    }

    @Test
    fun `all AppTheme values can be used in UserPreferences`() {
        AppTheme.entries.forEach { theme ->
            val prefs = UserPreferences(theme = theme)
            assertEquals(theme, prefs.theme)
        }
    }

    @Test
    fun `all AppLanguage values can be used in UserPreferences`() {
        AppLanguage.entries.forEach { language ->
            val prefs = UserPreferences(language = language)
            assertEquals(language, prefs.language)
        }
    }

    // Onboarding-related tests
    @Test
    fun `default UserPreferences has onboardingCompleted false`() {
        val prefs = UserPreferences()
        assertFalse(prefs.onboardingCompleted)
    }

    @Test
    fun `default UserPreferences has NOT_SET userGoal`() {
        val prefs = UserPreferences()
        assertEquals(UserGoal.NOT_SET, prefs.userGoal)
    }

    @Test
    fun `UserPreferences can be created with onboarding fields`() {
        val prefs = UserPreferences(
            onboardingCompleted = true,
            userGoal = UserGoal.TRACK_SPENDING
        )

        assertTrue(prefs.onboardingCompleted)
        assertEquals(UserGoal.TRACK_SPENDING, prefs.userGoal)
    }

    @Test
    fun `UserPreferences copy works for onboarding fields`() {
        val original = UserPreferences()
        val modified = original.copy(
            onboardingCompleted = true,
            userGoal = UserGoal.SAVE_MONEY
        )

        assertFalse(original.onboardingCompleted)
        assertEquals(UserGoal.NOT_SET, original.userGoal)
        assertTrue(modified.onboardingCompleted)
        assertEquals(UserGoal.SAVE_MONEY, modified.userGoal)
    }

    // UserGoal enum tests
    @Test
    fun `UserGoal has five values`() {
        assertEquals(5, UserGoal.entries.size)
    }

    @Test
    fun `UserGoal contains all expected values`() {
        val goals = UserGoal.entries.map { it.name }
        assertTrue("NOT_SET" in goals)
        assertTrue("TRACK_SPENDING" in goals)
        assertTrue("SAVE_MONEY" in goals)
        assertTrue("GET_ORGANIZED" in goals)
        assertTrue("BUDGET_BETTER" in goals)
    }

    @Test
    fun `all UserGoal values can be used in UserPreferences`() {
        UserGoal.entries.forEach { goal ->
            val prefs = UserPreferences(userGoal = goal)
            assertEquals(goal, prefs.userGoal)
        }
    }

    @Test
    fun `UserGoal NOT_SET has correct displayNameKey`() {
        assertEquals("goal_not_set", UserGoal.NOT_SET.displayNameKey)
    }

    @Test
    fun `UserGoal TRACK_SPENDING has correct displayNameKey`() {
        assertEquals("goal_track_spending", UserGoal.TRACK_SPENDING.displayNameKey)
    }

    @Test
    fun `UserGoal SAVE_MONEY has correct displayNameKey`() {
        assertEquals("goal_save_money", UserGoal.SAVE_MONEY.displayNameKey)
    }

    @Test
    fun `UserGoal GET_ORGANIZED has correct displayNameKey`() {
        assertEquals("goal_get_organized", UserGoal.GET_ORGANIZED.displayNameKey)
    }

    @Test
    fun `UserGoal BUDGET_BETTER has correct displayNameKey`() {
        assertEquals("goal_budget_better", UserGoal.BUDGET_BETTER.displayNameKey)
    }

    // AI Features tests
    @Test
    fun `default UserPreferences has aiCategorizationEnabled true`() {
        val prefs = UserPreferences()
        assertTrue(prefs.aiCategorizationEnabled)
    }

    @Test
    fun `default UserPreferences has null selectedModelId`() {
        val prefs = UserPreferences()
        assertNull(prefs.selectedModelId)
    }

    @Test
    fun `default UserPreferences has aiModelDownloadedInOnboarding false`() {
        val prefs = UserPreferences()
        assertFalse(prefs.aiModelDownloadedInOnboarding)
    }

    @Test
    fun `UserPreferences can be created with AI fields`() {
        val prefs = UserPreferences(
            aiCategorizationEnabled = false,
            selectedModelId = "compact",
            aiModelDownloadedInOnboarding = true
        )

        assertFalse(prefs.aiCategorizationEnabled)
        assertEquals("compact", prefs.selectedModelId)
        assertTrue(prefs.aiModelDownloadedInOnboarding)
    }

    @Test
    fun `UserPreferences copy works for AI fields`() {
        val original = UserPreferences()
        val modified = original.copy(
            aiCategorizationEnabled = false,
            selectedModelId = "standard"
        )

        assertTrue(original.aiCategorizationEnabled)
        assertNull(original.selectedModelId)
        assertFalse(modified.aiCategorizationEnabled)
        assertEquals("standard", modified.selectedModelId)
    }
}

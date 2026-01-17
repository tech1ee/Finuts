package com.finuts.domain.model

/**
 * User preferences model.
 * Contains app-wide settings stored in DataStore.
 */
data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val defaultCurrency: String = "KZT",
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val userGoal: UserGoal = UserGoal.NOT_SET,

    // AI Features
    /** Whether AI auto-categorization is enabled */
    val aiCategorizationEnabled: Boolean = true,
    /** Currently selected AI model ID (null if none) */
    val selectedModelId: String? = null,
    /** Whether user downloaded AI model during onboarding */
    val aiModelDownloadedInOnboarding: Boolean = false
)

/**
 * User's primary financial goal selected during onboarding.
 * Used to personalize app experience and recommendations.
 */
enum class UserGoal(val displayNameKey: String) {
    NOT_SET("goal_not_set"),
    TRACK_SPENDING("goal_track_spending"),
    SAVE_MONEY("goal_save_money"),
    GET_ORGANIZED("goal_get_organized"),
    BUDGET_BETTER("goal_budget_better")
}

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

/**
 * App language options.
 * SYSTEM: Use system language (Russian if system is Russian, English otherwise)
 */
enum class AppLanguage(val code: String?, val displayName: String) {
    SYSTEM(null, "System"),
    RUSSIAN("ru", "Русский"),
    KAZAKH("kk", "Қазақша"),
    ENGLISH("en", "English")
}

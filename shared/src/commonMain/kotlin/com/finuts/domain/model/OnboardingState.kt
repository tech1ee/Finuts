package com.finuts.domain.model

/**
 * Represents the steps in the onboarding flow.
 * Used by OnboardingViewModel to manage the onboarding navigation.
 */
sealed interface OnboardingStep {
    /** Welcome screen with value proposition */
    data object Welcome : OnboardingStep

    /** Currency and language selection - personalization from the start */
    data object CurrencyLanguage : OnboardingStep

    /** Goal selection screen - user picks their financial goals (multiple selection) */
    data object GoalSelection : OnboardingStep

    /** First account setup - inline form to create first account */
    data object FirstAccountSetup : OnboardingStep

    /** Completion screen - success message and transition to main app */
    data object Completion : OnboardingStep
}

/**
 * Possible results when completing onboarding.
 */
sealed interface OnboardingResult {
    /** User completed full onboarding flow */
    data class Completed(val goal: UserGoal) : OnboardingResult

    /** User skipped onboarding */
    data object Skipped : OnboardingResult
}

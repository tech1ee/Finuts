package com.finuts.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for OnboardingStep and OnboardingResult sealed classes.
 */
class OnboardingStateTest {

    // OnboardingStep tests
    @Test
    fun `OnboardingStep Welcome is a singleton`() {
        val step1 = OnboardingStep.Welcome
        val step2 = OnboardingStep.Welcome
        assertEquals(step1, step2)
    }

    @Test
    fun `OnboardingStep GoalSelection is a singleton`() {
        val step1 = OnboardingStep.GoalSelection
        val step2 = OnboardingStep.GoalSelection
        assertEquals(step1, step2)
    }

    @Test
    fun `OnboardingStep FirstAccountSetup is a singleton`() {
        val step1 = OnboardingStep.FirstAccountSetup
        val step2 = OnboardingStep.FirstAccountSetup
        assertEquals(step1, step2)
    }

    @Test
    fun `OnboardingStep AIModelSetup is a singleton`() {
        val step1 = OnboardingStep.AIModelSetup
        val step2 = OnboardingStep.AIModelSetup
        assertEquals(step1, step2)
    }

    @Test
    fun `OnboardingStep Completion is a singleton`() {
        val step1 = OnboardingStep.Completion
        val step2 = OnboardingStep.Completion
        assertEquals(step1, step2)
    }

    @Test
    fun `OnboardingStep steps are distinct types`() {
        val welcome: OnboardingStep = OnboardingStep.Welcome
        val goalSelection: OnboardingStep = OnboardingStep.GoalSelection
        val firstAccount: OnboardingStep = OnboardingStep.FirstAccountSetup
        val aiModelSetup: OnboardingStep = OnboardingStep.AIModelSetup
        val completion: OnboardingStep = OnboardingStep.Completion

        assertIs<OnboardingStep.Welcome>(welcome)
        assertIs<OnboardingStep.GoalSelection>(goalSelection)
        assertIs<OnboardingStep.FirstAccountSetup>(firstAccount)
        assertIs<OnboardingStep.AIModelSetup>(aiModelSetup)
        assertIs<OnboardingStep.Completion>(completion)
    }

    // OnboardingResult tests
    @Test
    fun `OnboardingResult Completed contains goal`() {
        val result = OnboardingResult.Completed(UserGoal.TRACK_SPENDING)
        assertEquals(UserGoal.TRACK_SPENDING, result.goal)
    }

    @Test
    fun `OnboardingResult Completed with different goals are not equal`() {
        val result1 = OnboardingResult.Completed(UserGoal.TRACK_SPENDING)
        val result2 = OnboardingResult.Completed(UserGoal.SAVE_MONEY)
        assertTrue(result1 != result2)
    }

    @Test
    fun `OnboardingResult Completed with same goal are equal`() {
        val result1 = OnboardingResult.Completed(UserGoal.BUDGET_BETTER)
        val result2 = OnboardingResult.Completed(UserGoal.BUDGET_BETTER)
        assertEquals(result1, result2)
    }

    @Test
    fun `OnboardingResult Skipped is a singleton`() {
        val skipped1 = OnboardingResult.Skipped
        val skipped2 = OnboardingResult.Skipped
        assertEquals(skipped1, skipped2)
    }

    @Test
    fun `OnboardingResult can be used with when expression`() {
        val completed: OnboardingResult = OnboardingResult.Completed(UserGoal.GET_ORGANIZED)
        val skipped: OnboardingResult = OnboardingResult.Skipped

        val completedMessage = when (completed) {
            is OnboardingResult.Completed -> "Completed with goal: ${completed.goal}"
            OnboardingResult.Skipped -> "Skipped"
        }

        val skippedMessage = when (skipped) {
            is OnboardingResult.Completed -> "Completed with goal: ${skipped.goal}"
            OnboardingResult.Skipped -> "Skipped"
        }

        assertEquals("Completed with goal: GET_ORGANIZED", completedMessage)
        assertEquals("Skipped", skippedMessage)
    }
}

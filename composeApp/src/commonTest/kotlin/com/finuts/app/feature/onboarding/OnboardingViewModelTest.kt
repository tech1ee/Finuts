package com.finuts.app.feature.onboarding

import app.cash.turbine.test
import com.finuts.app.test.fakes.FakePreferencesRepository
import com.finuts.domain.model.OnboardingStep
import com.finuts.domain.model.UserGoal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for OnboardingViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var viewModel: OnboardingViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferencesRepository = FakePreferencesRepository()
        viewModel = OnboardingViewModel(preferencesRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Initial state tests
    @Test
    fun `initial step is Welcome`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertIs<OnboardingStep.Welcome>(state.currentStep)
        }
    }

    @Test
    fun `initial selectedGoal is NOT_SET`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(UserGoal.NOT_SET, state.selectedGoal)
        }
    }

    @Test
    fun `initial canSkip is false on Welcome`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.canSkip)
        }
    }

    // Navigation tests
    @Test
    fun `onNext from Welcome goes to GoalSelection`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.GoalSelection>(state.currentStep)
        }
    }

    @Test
    fun `onNext from GoalSelection goes to FirstAccountSetup`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // -> GoalSelection
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // -> FirstAccountSetup
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.FirstAccountSetup>(state.currentStep)
        }
    }

    @Test
    fun `onNext from FirstAccountSetup goes to Completion`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // -> GoalSelection
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // -> FirstAccountSetup
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // -> Completion
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.Completion>(state.currentStep)
        }
    }

    @Test
    fun `onBack from GoalSelection goes to Welcome`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // -> GoalSelection
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.onBack() // -> Welcome
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.Welcome>(state.currentStep)
        }
    }

    @Test
    fun `onBack from Welcome does nothing`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertIs<OnboardingStep.Welcome>(initial.currentStep)

            viewModel.onBack()
            testDispatcher.scheduler.advanceUntilIdle()

            // No new emission, still on Welcome
            expectNoEvents()
        }
    }

    // Goal selection tests
    @Test
    fun `selectGoal updates selectedGoal`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.selectGoal(UserGoal.TRACK_SPENDING)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(UserGoal.TRACK_SPENDING, state.selectedGoal)
        }
    }

    @Test
    fun `selecting different goals updates correctly`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.selectGoal(UserGoal.SAVE_MONEY)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(UserGoal.SAVE_MONEY, awaitItem().selectedGoal)

            viewModel.selectGoal(UserGoal.BUDGET_BETTER)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(UserGoal.BUDGET_BETTER, awaitItem().selectedGoal)
        }
    }

    // Skip tests
    @Test
    fun `canSkip is true after Welcome step`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome, canSkip = false

            viewModel.onNext() // -> GoalSelection
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state.canSkip)
        }
    }

    @Test
    fun `onSkip completes onboarding and saves to preferences`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // -> GoalSelection (canSkip becomes true)
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.onSkip()
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify preferences updated
            assertTrue(preferencesRepository.currentValue().onboardingCompleted)
        }
    }

    // Complete onboarding tests
    @Test
    fun `completeOnboarding saves goal to preferences`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.selectGoal(UserGoal.GET_ORGANIZED)
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.completeOnboarding()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(UserGoal.GET_ORGANIZED, preferencesRepository.currentValue().userGoal)
        }
    }

    @Test
    fun `completeOnboarding sets onboardingCompleted to true`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.completeOnboarding()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(preferencesRepository.currentValue().onboardingCompleted)
        }
    }

    @Test
    fun `completeOnboarding emits NavigateToDashboard event`() = runTest {
        viewModel.events.test {
            viewModel.completeOnboarding()
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertIs<OnboardingEvent.NavigateToDashboard>(event)
        }
    }

    @Test
    fun `onSkip emits NavigateToDashboard event`() = runTest {
        // Move past Welcome to enable skip
        viewModel.onNext()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.events.test {
            viewModel.onSkip()
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertIs<OnboardingEvent.NavigateToDashboard>(event)
        }
    }

    // Progress indicator tests
    @Test
    fun `progressPercent is 0 on Welcome`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0f, state.progressPercent)
        }
    }

    @Test
    fun `progressPercent is 33 on GoalSelection`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(0.33f, state.progressPercent, 0.01f)
        }
    }

    @Test
    fun `progressPercent is 66 on FirstAccountSetup`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // GoalSelection
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // FirstAccountSetup
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(0.66f, state.progressPercent, 0.01f)
        }
    }

    @Test
    fun `progressPercent is 100 on Completion`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // GoalSelection
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // FirstAccountSetup
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // Completion
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(1f, state.progressPercent)
        }
    }
}

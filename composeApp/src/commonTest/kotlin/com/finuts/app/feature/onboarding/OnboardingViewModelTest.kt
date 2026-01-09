package com.finuts.app.feature.onboarding

import app.cash.turbine.test
import com.finuts.app.test.fakes.FakeAccountRepository
import com.finuts.app.test.fakes.FakePreferencesRepository
import com.finuts.domain.entity.AccountType
import com.finuts.domain.model.AppLanguage
import com.finuts.domain.model.OnboardingStep
import com.finuts.domain.model.UserGoal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for OnboardingViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var viewModel: OnboardingViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferencesRepository = FakePreferencesRepository()
        accountRepository = FakeAccountRepository()
        viewModel = OnboardingViewModel(preferencesRepository, accountRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== Initial State Tests ==========

    @Test
    fun `initial step is Welcome`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertIs<OnboardingStep.Welcome>(state.currentStep)
        }
    }

    @Test
    fun `initial selectedGoals is empty`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.selectedGoals.isEmpty())
        }
    }

    @Test
    fun `initial selectedCurrency is KZT`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("KZT", state.selectedCurrency)
        }
    }

    @Test
    fun `initial selectedLanguage is SYSTEM`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(AppLanguage.SYSTEM, state.selectedLanguage)
        }
    }

    @Test
    fun `initial canSkip is false on Welcome`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.canSkip)
        }
    }

    @Test
    fun `initial account form fields are empty`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.accountName)
            assertEquals(AccountType.CASH, state.accountType)
            assertEquals("", state.initialBalance)
            assertNull(state.accountNameError)
            assertNull(state.balanceError)
            assertFalse(state.isCreatingAccount)
            assertNull(state.createdAccountId)
            assertNull(state.createdAccountName)
        }
    }

    // ========== Navigation Tests (5-step flow) ==========

    @Test
    fun `onNext from Welcome goes to CurrencyLanguage`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext()
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.CurrencyLanguage>(state.currentStep)
        }
    }

    @Test
    fun `onNext from CurrencyLanguage goes to GoalSelection`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // -> CurrencyLanguage
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // -> GoalSelection
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.GoalSelection>(state.currentStep)
        }
    }

    @Test
    fun `onNext from GoalSelection goes to FirstAccountSetup`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // -> CurrencyLanguage
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // -> GoalSelection
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // -> FirstAccountSetup
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.FirstAccountSetup>(state.currentStep)
        }
    }

    @Test
    fun `onNext from FirstAccountSetup goes to Completion`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // -> CurrencyLanguage
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // -> GoalSelection
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // -> FirstAccountSetup
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // -> Completion
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.Completion>(state.currentStep)
        }
    }

    @Test
    fun `onBack from CurrencyLanguage goes to Welcome`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // -> CurrencyLanguage
            advanceUntilIdle()
            awaitItem()

            viewModel.onBack() // -> Welcome
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.Welcome>(state.currentStep)
        }
    }

    @Test
    fun `onBack from GoalSelection goes to CurrencyLanguage`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // -> CurrencyLanguage
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // -> GoalSelection
            advanceUntilIdle()
            awaitItem()

            viewModel.onBack() // -> CurrencyLanguage
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.CurrencyLanguage>(state.currentStep)
        }
    }

    @Test
    fun `onBack from Welcome does nothing`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertIs<OnboardingStep.Welcome>(initial.currentStep)

            viewModel.onBack()
            advanceUntilIdle()

            // No new emission, still on Welcome
            expectNoEvents()
        }
    }

    // ========== Currency Selection Tests ==========

    @Test
    fun `selectCurrency updates selectedCurrency`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.selectCurrency("USD")
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("USD", state.selectedCurrency)
        }
    }

    @Test
    fun `selecting different currencies updates correctly`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.selectCurrency("EUR")
            advanceUntilIdle()
            assertEquals("EUR", awaitItem().selectedCurrency)

            viewModel.selectCurrency("RUB")
            advanceUntilIdle()
            assertEquals("RUB", awaitItem().selectedCurrency)
        }
    }

    // ========== Language Selection Tests ==========

    @Test
    fun `selectLanguage updates selectedLanguage`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.selectLanguage(AppLanguage.RUSSIAN)
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(AppLanguage.RUSSIAN, state.selectedLanguage)
        }
    }

    @Test
    fun `selecting different languages updates correctly`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.selectLanguage(AppLanguage.ENGLISH)
            advanceUntilIdle()
            assertEquals(AppLanguage.ENGLISH, awaitItem().selectedLanguage)

            viewModel.selectLanguage(AppLanguage.KAZAKH)
            advanceUntilIdle()
            assertEquals(AppLanguage.KAZAKH, awaitItem().selectedLanguage)
        }
    }

    // ========== Goal Selection Tests (Multiple) ==========

    @Test
    fun `toggleGoal adds goal to selectedGoals`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.toggleGoal(UserGoal.TRACK_SPENDING)
            advanceUntilIdle()

            val state = awaitItem()
            assertTrue(UserGoal.TRACK_SPENDING in state.selectedGoals)
            assertEquals(1, state.selectedGoals.size)
        }
    }

    @Test
    fun `toggleGoal removes goal if already selected`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.toggleGoal(UserGoal.SAVE_MONEY)
            advanceUntilIdle()
            awaitItem() // Has SAVE_MONEY

            viewModel.toggleGoal(UserGoal.SAVE_MONEY)
            advanceUntilIdle()

            val state = awaitItem()
            assertFalse(UserGoal.SAVE_MONEY in state.selectedGoals)
            assertTrue(state.selectedGoals.isEmpty())
        }
    }

    @Test
    fun `can select multiple goals`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.toggleGoal(UserGoal.TRACK_SPENDING)
            advanceUntilIdle()
            awaitItem()

            viewModel.toggleGoal(UserGoal.SAVE_MONEY)
            advanceUntilIdle()
            awaitItem()

            viewModel.toggleGoal(UserGoal.BUDGET_BETTER)
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(3, state.selectedGoals.size)
            assertTrue(UserGoal.TRACK_SPENDING in state.selectedGoals)
            assertTrue(UserGoal.SAVE_MONEY in state.selectedGoals)
            assertTrue(UserGoal.BUDGET_BETTER in state.selectedGoals)
        }
    }

    // ========== Account Form Tests ==========

    @Test
    fun `updateAccountName updates state and clears error`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.updateAccountName("My Wallet")
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("My Wallet", state.accountName)
            assertNull(state.accountNameError)
        }
    }

    @Test
    fun `updateAccountType updates state`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.updateAccountType(AccountType.CREDIT_CARD)
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(AccountType.CREDIT_CARD, state.accountType)
        }
    }

    @Test
    fun `updateInitialBalance updates state and clears error`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.updateInitialBalance("1000.50")
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("1000.50", state.initialBalance)
            assertNull(state.balanceError)
        }
    }

    // ========== Create Account Tests ==========

    @Test
    fun `createAccount with empty name shows error`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.createAccount()
            advanceUntilIdle()

            val state = awaitItem()
            assertNotNull(state.accountNameError)
            assertFalse(state.isCreatingAccount)
        }
    }

    @Test
    fun `createAccount with invalid balance shows error`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.updateAccountName("Test Account")
            advanceUntilIdle()
            awaitItem()

            viewModel.updateInitialBalance("abc")
            advanceUntilIdle()
            awaitItem()

            viewModel.createAccount()
            advanceUntilIdle()

            val state = awaitItem()
            assertNotNull(state.balanceError)
            assertFalse(state.isCreatingAccount)
        }
    }

    @Test
    fun `createAccount with valid data creates account and moves to Completion`() = runTest {
        // Create ViewModel with this test scope so coroutines execute in our control
        val testViewModel = OnboardingViewModel(preferencesRepository, accountRepository, this)

        // Navigate to FirstAccountSetup
        testViewModel.onNext() // -> CurrencyLanguage
        testViewModel.onNext() // -> GoalSelection
        testViewModel.onNext() // -> FirstAccountSetup
        assertIs<OnboardingStep.FirstAccountSetup>(testViewModel.uiState.value.currentStep)

        // Fill form
        testViewModel.updateAccountName("My Wallet")
        testViewModel.updateInitialBalance("500.00")
        assertEquals("My Wallet", testViewModel.uiState.value.accountName)
        assertEquals("500.00", testViewModel.uiState.value.initialBalance)

        // Create account
        testViewModel.createAccount()
        advanceUntilIdle() // Run all coroutines

        // Verify final state
        val finalState = testViewModel.uiState.value
        assertIs<OnboardingStep.Completion>(finalState.currentStep)
        assertFalse(finalState.isCreatingAccount)
        assertNotNull(finalState.createdAccountId)
        assertEquals("My Wallet", finalState.createdAccountName)

        // Verify account was actually created in repository
        accountRepository.getAllAccounts().test {
            val accounts = awaitItem()
            assertEquals(1, accounts.size)
            assertEquals("My Wallet", accounts.first().name)
            assertEquals(50000L, accounts.first().balance) // 500.00 * 100
        }
    }

    @Test
    fun `createAccount with comma decimal separator works`() = runTest {
        // Create ViewModel with this test scope so coroutines execute in our control
        val testViewModel = OnboardingViewModel(preferencesRepository, accountRepository, this)

        // Fill form with comma decimal separator
        testViewModel.updateAccountName("Test")
        testViewModel.updateInitialBalance("1000,50")

        // Create account
        testViewModel.createAccount()
        advanceUntilIdle() // Run all coroutines

        // Verify final state
        val state = testViewModel.uiState.value
        assertNull(state.balanceError) // Should work with comma
        assertNotNull(state.createdAccountId)

        // Verify account was created with correct balance
        accountRepository.getAllAccounts().test {
            val accounts = awaitItem()
            assertEquals(100050L, accounts.first().balance) // 1000.50 * 100
        }
    }

    // ========== Skip Account Creation Tests ==========

    @Test
    fun `skipAccountCreation moves to Completion without creating account`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            // Navigate to FirstAccountSetup
            viewModel.onNext() // -> CurrencyLanguage
            advanceUntilIdle()
            awaitItem()
            viewModel.onNext() // -> GoalSelection
            advanceUntilIdle()
            awaitItem()
            viewModel.onNext() // -> FirstAccountSetup
            advanceUntilIdle()
            awaitItem()

            viewModel.skipAccountCreation()
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.Completion>(state.currentStep)
            assertNull(state.createdAccountId)
            assertNull(state.createdAccountName)
        }

        // Verify no account was created
        accountRepository.getAllAccounts().test {
            val accounts = awaitItem()
            assertTrue(accounts.isEmpty())
        }
    }

    // ========== Skip Tests ==========

    @Test
    fun `canSkip is true on CurrencyLanguage step`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome, canSkip = false

            viewModel.onNext() // -> CurrencyLanguage
            advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state.canSkip)
        }
    }

    @Test
    fun `canSkip is false on Completion step`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            // Navigate through all steps to Completion
            viewModel.onNext() // -> CurrencyLanguage
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // -> GoalSelection
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // -> FirstAccountSetup
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // -> Completion
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.Completion>(state.currentStep)
            assertFalse(state.canSkip)
        }
    }

    @Test
    fun `onSkip completes onboarding and saves to preferences`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // -> CurrencyLanguage (canSkip becomes true)
            advanceUntilIdle()
            awaitItem()

            viewModel.onSkip()
            advanceUntilIdle()

            // Verify preferences updated
            assertTrue(preferencesRepository.currentValue().onboardingCompleted)
        }
    }

    // ========== Complete Onboarding Tests ==========

    @Test
    fun `completeOnboarding saves goal to preferences`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.toggleGoal(UserGoal.GET_ORGANIZED)
            advanceUntilIdle()
            awaitItem()

            viewModel.completeOnboarding()
            advanceUntilIdle()

            assertEquals(UserGoal.GET_ORGANIZED, preferencesRepository.currentValue().userGoal)
        }
    }

    @Test
    fun `completeOnboarding saves currency and language to preferences`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.selectCurrency("EUR")
            advanceUntilIdle()
            awaitItem()

            viewModel.selectLanguage(AppLanguage.ENGLISH)
            advanceUntilIdle()
            awaitItem()

            viewModel.completeOnboarding()
            advanceUntilIdle()

            val prefs = preferencesRepository.currentValue()
            assertEquals("EUR", prefs.defaultCurrency)
            assertEquals(AppLanguage.ENGLISH, prefs.language)
        }
    }

    @Test
    fun `completeOnboarding sets onboardingCompleted to true`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.completeOnboarding()
            advanceUntilIdle()

            assertTrue(preferencesRepository.currentValue().onboardingCompleted)
        }
    }

    @Test
    fun `completeOnboarding emits NavigateToDashboard event`() = runTest {
        viewModel.events.test {
            viewModel.completeOnboarding()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<OnboardingEvent.NavigateToDashboard>(event)
        }
    }

    @Test
    fun `onSkip emits NavigateToDashboard event`() = runTest {
        // Move past Welcome to enable skip
        viewModel.onNext()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onSkip()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<OnboardingEvent.NavigateToDashboard>(event)
        }
    }

    // ========== Progress Indicator Tests (5-step flow) ==========

    @Test
    fun `progressPercent is 0 on Welcome`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0f, state.progressPercent)
        }
    }

    @Test
    fun `progressPercent is 25 on CurrencyLanguage`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext()
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(0.25f, state.progressPercent, 0.01f)
        }
    }

    @Test
    fun `progressPercent is 50 on GoalSelection`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // CurrencyLanguage
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // GoalSelection
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(0.50f, state.progressPercent, 0.01f)
        }
    }

    @Test
    fun `progressPercent is 75 on FirstAccountSetup`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // CurrencyLanguage
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // GoalSelection
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // FirstAccountSetup
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(0.75f, state.progressPercent, 0.01f)
        }
    }

    @Test
    fun `progressPercent is 100 on Completion`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.onNext() // CurrencyLanguage
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // GoalSelection
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // FirstAccountSetup
            advanceUntilIdle()
            awaitItem()

            viewModel.onNext() // Completion
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(1f, state.progressPercent)
        }
    }

    // ========== Step Index Tests (HorizontalPager integration) ==========

    @Test
    fun `setStep with index 0 goes to Welcome`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial Welcome

            viewModel.onNext() // Go to CurrencyLanguage
            advanceUntilIdle()
            awaitItem()

            viewModel.setStep(0) // Back to Welcome
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.Welcome>(state.currentStep)
        }
    }

    @Test
    fun `setStep with index 1 goes to CurrencyLanguage`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.setStep(1)
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.CurrencyLanguage>(state.currentStep)
        }
    }

    @Test
    fun `setStep with index 2 goes to GoalSelection`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.setStep(2)
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.GoalSelection>(state.currentStep)
        }
    }

    @Test
    fun `setStep with index 3 goes to FirstAccountSetup`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.setStep(3)
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.FirstAccountSetup>(state.currentStep)
        }
    }

    @Test
    fun `setStep with index 4 goes to Completion`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Welcome

            viewModel.setStep(4)
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<OnboardingStep.Completion>(state.currentStep)
        }
    }

    @Test
    fun `currentStepIndex returns correct index for each step`() = runTest {
        viewModel.uiState.test {
            val welcomeState = awaitItem()
            assertEquals(0, welcomeState.currentStepIndex)

            viewModel.onNext() // CurrencyLanguage
            advanceUntilIdle()
            assertEquals(1, awaitItem().currentStepIndex)

            viewModel.onNext() // GoalSelection
            advanceUntilIdle()
            assertEquals(2, awaitItem().currentStepIndex)

            viewModel.onNext() // FirstAccountSetup
            advanceUntilIdle()
            assertEquals(3, awaitItem().currentStepIndex)

            viewModel.onNext() // Completion
            advanceUntilIdle()
            assertEquals(4, awaitItem().currentStepIndex)
        }
    }

    // ========== Deprecated API Compatibility Tests ==========

    @Test
    @Suppress("DEPRECATION")
    fun `deprecated selectGoal still works for single selection`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.selectGoal(UserGoal.TRACK_SPENDING)
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(UserGoal.TRACK_SPENDING, state.selectedGoal)
            assertEquals(setOf(UserGoal.TRACK_SPENDING), state.selectedGoals)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `deprecated selectedGoal returns first goal from set`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.toggleGoal(UserGoal.SAVE_MONEY)
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(UserGoal.SAVE_MONEY, state.selectedGoal)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `deprecated selectedGoal returns NOT_SET when empty`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(UserGoal.NOT_SET, state.selectedGoal)
        }
    }
}

package com.finuts.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.AccountType
import com.finuts.domain.entity.Currency
import com.finuts.domain.model.AppLanguage
import com.finuts.domain.model.OnboardingStep
import com.finuts.domain.model.UserGoal
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel for the onboarding flow.
 * Manages step navigation, goal selection, and inline account creation.
 *
 * @param coroutineScope Optional CoroutineScope for testing. Uses viewModelScope by default.
 */
class OnboardingViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val accountRepository: AccountRepository,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>()
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    fun onNext() {
        _uiState.update { state ->
            val nextStep = when (state.currentStep) {
                OnboardingStep.Welcome -> OnboardingStep.CurrencyLanguage
                OnboardingStep.CurrencyLanguage -> OnboardingStep.GoalSelection
                OnboardingStep.GoalSelection -> OnboardingStep.FirstAccountSetup
                OnboardingStep.FirstAccountSetup -> OnboardingStep.Completion
                OnboardingStep.Completion -> OnboardingStep.Completion
            }
            state.copy(
                currentStep = nextStep,
                canSkip = nextStep !in listOf(OnboardingStep.Welcome, OnboardingStep.Completion),
                progressPercent = calculateProgress(nextStep)
            )
        }
    }

    fun onBack() {
        _uiState.update { state ->
            val previousStep = when (state.currentStep) {
                OnboardingStep.Welcome -> return@update state
                OnboardingStep.CurrencyLanguage -> OnboardingStep.Welcome
                OnboardingStep.GoalSelection -> OnboardingStep.CurrencyLanguage
                OnboardingStep.FirstAccountSetup -> OnboardingStep.GoalSelection
                OnboardingStep.Completion -> OnboardingStep.FirstAccountSetup
            }
            state.copy(
                currentStep = previousStep,
                canSkip = previousStep !in listOf(OnboardingStep.Welcome, OnboardingStep.Completion),
                progressPercent = calculateProgress(previousStep)
            )
        }
    }

    /**
     * Set step by index for HorizontalPager integration.
     * Index mapping: 0=Welcome, 1=CurrencyLanguage, 2=GoalSelection, 3=FirstAccountSetup, 4=Completion
     */
    fun setStep(index: Int) {
        val step = indexToStep(index) ?: return
        _uiState.update { state ->
            state.copy(
                currentStep = step,
                canSkip = step !in listOf(OnboardingStep.Welcome, OnboardingStep.Completion),
                progressPercent = calculateProgress(step)
            )
        }
    }

    private fun indexToStep(index: Int): OnboardingStep? = when (index) {
        0 -> OnboardingStep.Welcome
        1 -> OnboardingStep.CurrencyLanguage
        2 -> OnboardingStep.GoalSelection
        3 -> OnboardingStep.FirstAccountSetup
        4 -> OnboardingStep.Completion
        else -> null
    }

    // Currency & Language selection
    fun selectCurrency(currency: String) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }

    fun selectLanguage(language: AppLanguage) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    // Goal selection (multiple)
    fun toggleGoal(goal: UserGoal) {
        _uiState.update { state ->
            val newGoals = if (goal in state.selectedGoals) {
                state.selectedGoals - goal
            } else {
                state.selectedGoals + goal
            }
            state.copy(selectedGoals = newGoals)
        }
    }

    @Deprecated("Use toggleGoal for multiple selection")
    fun selectGoal(goal: UserGoal) {
        _uiState.update { it.copy(selectedGoals = setOf(goal)) }
    }

    // Account form fields
    fun updateAccountName(name: String) {
        _uiState.update { it.copy(accountName = name, accountNameError = null) }
    }

    fun updateAccountType(type: AccountType) {
        _uiState.update { it.copy(accountType = type) }
    }

    fun updateInitialBalance(balance: String) {
        _uiState.update { it.copy(initialBalance = balance, balanceError = null) }
    }

    // Create account inline
    @OptIn(ExperimentalUuidApi::class)
    fun createAccount() {
        val state = _uiState.value

        // Validate
        val nameError = if (state.accountName.isBlank()) "Account name is required" else null
        val balanceError = state.initialBalance.takeIf { it.isNotBlank() }?.let { balance ->
            // Return error only if parsing fails
            if (balance.replace(",", ".").toDoubleOrNull() == null) "Invalid balance" else null
        }

        if (nameError != null || balanceError != null) {
            _uiState.update { it.copy(accountNameError = nameError, balanceError = balanceError) }
            return
        }

        _uiState.update { it.copy(isCreatingAccount = true) }

        scope.launch {
            try {
                val now = Instant.fromEpochMilliseconds(
                    kotlin.time.Clock.System.now().toEpochMilliseconds()
                )
                val balanceInCents = state.initialBalance.takeIf { it.isNotBlank() }
                    ?.replace(",", ".")
                    ?.toDoubleOrNull()
                    ?.times(100)
                    ?.toLong() ?: 0L

                val account = Account(
                    id = Uuid.random().toString(),
                    name = state.accountName.trim(),
                    type = state.accountType,
                    currency = getCurrencyForCode(state.selectedCurrency),
                    balance = balanceInCents,
                    initialBalance = balanceInCents,
                    icon = null,
                    color = null,
                    isArchived = false,
                    createdAt = now,
                    updatedAt = now
                )
                accountRepository.createAccount(account)
                _uiState.update {
                    it.copy(
                        isCreatingAccount = false,
                        createdAccountId = account.id,
                        createdAccountName = account.name
                    )
                }
                onNext() // Move to Completion
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingAccount = false,
                        accountNameError = "Failed to create account: ${e.message}"
                    )
                }
            }
        }
    }

    fun skipAccountCreation() {
        onNext() // Move to Completion without creating account
    }

    fun onSkip() {
        if (!_uiState.value.canSkip) return
        scope.launch {
            savePreferencesAndComplete()
        }
    }

    fun completeOnboarding() {
        scope.launch {
            savePreferencesAndComplete()
        }
    }

    private suspend fun savePreferencesAndComplete() {
        val state = _uiState.value
        // Save selected goals (first one as primary for backward compatibility)
        state.selectedGoals.firstOrNull()?.let { preferencesRepository.setUserGoal(it) }
        // Save currency and language
        preferencesRepository.setDefaultCurrency(state.selectedCurrency)
        preferencesRepository.setLanguage(state.selectedLanguage)
        preferencesRepository.setOnboardingCompleted(true)
        _events.emit(OnboardingEvent.NavigateToDashboard)
    }

    private fun calculateProgress(step: OnboardingStep): Float = when (step) {
        OnboardingStep.Welcome -> 0f
        OnboardingStep.CurrencyLanguage -> 0.25f
        OnboardingStep.GoalSelection -> 0.50f
        OnboardingStep.FirstAccountSetup -> 0.75f
        OnboardingStep.Completion -> 1f
    }

    private fun getCurrencyForCode(code: String): Currency = when (code) {
        "KZT" -> Currency("KZT", "₸", "Kazakhstani Tenge")
        "RUB" -> Currency("RUB", "₽", "Russian Ruble")
        "USD" -> Currency("USD", "$", "US Dollar")
        "EUR" -> Currency("EUR", "€", "Euro")
        else -> Currency(code, code, code)
    }
}

/**
 * UI state for the onboarding screen.
 */
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val canSkip: Boolean = false,
    val progressPercent: Float = 0f,
    // Currency & Language
    val selectedCurrency: String = "KZT",
    val selectedLanguage: AppLanguage = AppLanguage.SYSTEM,
    // Goals (multiple selection)
    val selectedGoals: Set<UserGoal> = emptySet(),
    // Account form
    val accountName: String = "",
    val accountType: AccountType = AccountType.CASH,
    val initialBalance: String = "",
    val accountNameError: String? = null,
    val balanceError: String? = null,
    val isCreatingAccount: Boolean = false,
    val createdAccountId: String? = null,
    val createdAccountName: String? = null
) {
    /** Current step as index for HorizontalPager. */
    val currentStepIndex: Int
        get() = when (currentStep) {
            OnboardingStep.Welcome -> 0
            OnboardingStep.CurrencyLanguage -> 1
            OnboardingStep.GoalSelection -> 2
            OnboardingStep.FirstAccountSetup -> 3
            OnboardingStep.Completion -> 4
        }

    @Deprecated("Use selectedGoals for multiple selection")
    val selectedGoal: UserGoal
        get() = selectedGoals.firstOrNull() ?: UserGoal.NOT_SET

    companion object {
        const val TOTAL_STEPS = 5
    }
}

/**
 * One-time events from the onboarding flow.
 */
sealed interface OnboardingEvent {
    data object NavigateToDashboard : OnboardingEvent
}

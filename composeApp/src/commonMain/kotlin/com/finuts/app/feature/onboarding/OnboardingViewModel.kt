package com.finuts.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finuts.domain.model.OnboardingStep
import com.finuts.domain.model.UserGoal
import com.finuts.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the onboarding flow.
 * Manages step navigation, goal selection, and onboarding completion.
 */
class OnboardingViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>()
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    fun onNext() {
        _uiState.update { state ->
            val nextStep = when (state.currentStep) {
                OnboardingStep.Welcome -> OnboardingStep.GoalSelection
                OnboardingStep.GoalSelection -> OnboardingStep.FirstAccountSetup
                OnboardingStep.FirstAccountSetup -> OnboardingStep.Completion
                OnboardingStep.Completion -> OnboardingStep.Completion
            }
            state.copy(
                currentStep = nextStep,
                canSkip = nextStep != OnboardingStep.Welcome,
                progressPercent = calculateProgress(nextStep)
            )
        }
    }

    fun onBack() {
        _uiState.update { state ->
            val previousStep = when (state.currentStep) {
                OnboardingStep.Welcome -> return@update state // No change
                OnboardingStep.GoalSelection -> OnboardingStep.Welcome
                OnboardingStep.FirstAccountSetup -> OnboardingStep.GoalSelection
                OnboardingStep.Completion -> OnboardingStep.FirstAccountSetup
            }
            state.copy(
                currentStep = previousStep,
                canSkip = previousStep != OnboardingStep.Welcome,
                progressPercent = calculateProgress(previousStep)
            )
        }
    }

    fun selectGoal(goal: UserGoal) {
        _uiState.update { it.copy(selectedGoal = goal) }
    }

    fun onSkip() {
        if (!_uiState.value.canSkip) return
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            _events.emit(OnboardingEvent.NavigateToDashboard)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val goal = _uiState.value.selectedGoal
            preferencesRepository.setUserGoal(goal)
            preferencesRepository.setOnboardingCompleted(true)
            _events.emit(OnboardingEvent.NavigateToDashboard)
        }
    }

    private fun calculateProgress(step: OnboardingStep): Float = when (step) {
        OnboardingStep.Welcome -> 0f
        OnboardingStep.GoalSelection -> 0.33f
        OnboardingStep.FirstAccountSetup -> 0.66f
        OnboardingStep.Completion -> 1f
    }
}

/**
 * UI state for the onboarding screen.
 */
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val selectedGoal: UserGoal = UserGoal.NOT_SET,
    val canSkip: Boolean = false,
    val progressPercent: Float = 0f
)

/**
 * One-time events from the onboarding flow.
 */
sealed interface OnboardingEvent {
    data object NavigateToDashboard : OnboardingEvent
}

package com.finuts.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.AccountType
import com.finuts.domain.entity.Currency
import com.finuts.domain.model.AppLanguage
import com.finuts.domain.model.DownloadProgress
import com.finuts.domain.model.ModelConfig
import com.finuts.domain.model.OnboardingStep
import com.finuts.domain.model.UserGoal
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.repository.ModelRepository
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
import co.touchlab.kermit.Logger
import com.finuts.app.core.locale.appLocale
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel for the onboarding flow.
 * Manages step navigation, goal selection, inline account creation, and AI model setup.
 *
 * @param coroutineScope Optional CoroutineScope for testing. Uses viewModelScope by default.
 */
class OnboardingViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val accountRepository: AccountRepository,
    private val modelRepository: ModelRepository? = null,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val logger = Logger.withTag("OnboardingViewModel")
    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>()
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    init {
        println("=== DEBUG: OnboardingViewModel init, modelRepository=${modelRepository != null} ===")
        logger.i { "OnboardingViewModel init, modelRepository=${modelRepository != null}" }

        // Collect download progress from ModelRepository
        modelRepository?.let { repo ->
            scope.launch {
                logger.d { "Starting to collect downloadProgress flow" }
                repo.downloadProgress.collect { progress ->
                    logger.d { "downloadProgress updated: $progress" }
                    _uiState.update { it.copy(modelDownloadProgress = progress) }
                }
            }
            // Load all available models and recommended model
            scope.launch {
                logger.i { "Loading available models..." }
                try {
                    val allModels = repo.availableModels.filter { it.downloadUrl.isNotBlank() }
                    val recommended = repo.getRecommendedModel()
                    logger.i { "Available models: ${allModels.size}, Recommended: ${recommended.id}" }
                    _uiState.update {
                        it.copy(
                            availableModels = allModels,
                            recommendedModelId = recommended.id,
                            selectedModelId = recommended.id
                        )
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Failed to load models" }
                }
            }
        } ?: run {
            logger.w { "ModelRepository is NULL - AI features disabled" }
        }
    }

    fun onNext() {
        val currentStep = _uiState.value.currentStep
        println("=== DEBUG: onNext() called, currentStep=$currentStep ===")
        logger.i { "onNext() called, currentStep=$currentStep" }

        _uiState.update { state ->
            val nextStep = when (state.currentStep) {
                OnboardingStep.Welcome -> OnboardingStep.CurrencyLanguage
                OnboardingStep.CurrencyLanguage -> OnboardingStep.GoalSelection
                OnboardingStep.GoalSelection -> OnboardingStep.FirstAccountSetup
                OnboardingStep.FirstAccountSetup -> OnboardingStep.AIModelSetup
                OnboardingStep.AIModelSetup -> OnboardingStep.Completion
                OnboardingStep.Completion -> OnboardingStep.Completion
            }
            logger.i { "onNext() transitioning: $currentStep -> $nextStep" }
            state.copy(
                currentStep = nextStep,
                canSkip = nextStep !in listOf(
                    OnboardingStep.Welcome,
                    OnboardingStep.Completion
                ),
                progressPercent = calculateProgress(nextStep)
            )
        }

        println("=== DEBUG: onNext() done, new step=${_uiState.value.currentStep}, index=${_uiState.value.currentStepIndex} ===")
        logger.i { "onNext() done, new currentStepIndex=${_uiState.value.currentStepIndex}" }
    }

    fun onBack() {
        _uiState.update { state ->
            val previousStep = when (state.currentStep) {
                OnboardingStep.Welcome -> return@update state
                OnboardingStep.CurrencyLanguage -> OnboardingStep.Welcome
                OnboardingStep.GoalSelection -> OnboardingStep.CurrencyLanguage
                OnboardingStep.FirstAccountSetup -> OnboardingStep.GoalSelection
                OnboardingStep.AIModelSetup -> OnboardingStep.FirstAccountSetup
                OnboardingStep.Completion -> OnboardingStep.AIModelSetup
            }
            state.copy(
                currentStep = previousStep,
                canSkip = previousStep !in listOf(
                    OnboardingStep.Welcome,
                    OnboardingStep.Completion
                ),
                progressPercent = calculateProgress(previousStep)
            )
        }
    }

    /**
     * Set step by index for HorizontalPager integration.
     * Index mapping: 0=Welcome, 1=CurrencyLanguage, 2=GoalSelection,
     * 3=FirstAccountSetup, 4=AIModelSetup, 5=Completion
     */
    fun setStep(index: Int) {
        logger.i { "setStep($index) called" }
        val step = indexToStep(index) ?: return
        logger.i { "setStep: index $index -> step $step" }
        _uiState.update { state ->
            state.copy(
                currentStep = step,
                canSkip = step !in listOf(
                    OnboardingStep.Welcome,
                    OnboardingStep.Completion
                ),
                progressPercent = calculateProgress(step)
            )
        }
    }

    private fun indexToStep(index: Int): OnboardingStep? = when (index) {
        0 -> OnboardingStep.Welcome
        1 -> OnboardingStep.CurrencyLanguage
        2 -> OnboardingStep.GoalSelection
        3 -> OnboardingStep.FirstAccountSetup
        4 -> OnboardingStep.AIModelSetup
        5 -> OnboardingStep.Completion
        else -> null
    }

    // Currency & Language selection
    fun selectCurrency(currency: String) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }

    fun selectLanguage(language: AppLanguage) {
        val currentLanguage = _uiState.value.selectedLanguage
        if (language == currentLanguage) return

        // Update UI state
        _uiState.update { it.copy(selectedLanguage = language) }

        // Apply language change immediately
        // On Android: LocalConfiguration update triggers recomposition
        // On iOS: NSUserDefaults saved, takes effect on restart
        appLocale = language.toLocaleCode()

        // Save preference
        scope.launch {
            preferencesRepository.setLanguage(language)
        }

        // Reset onboarding to Welcome step for a fresh start
        _uiState.update { state ->
            state.copy(
                currentStep = OnboardingStep.Welcome,
                canSkip = false,
                progressPercent = 0f
            )
        }
    }

    private fun AppLanguage.toLocaleCode(): String = when (this) {
        AppLanguage.ENGLISH -> "en"
        AppLanguage.RUSSIAN -> "ru"
        AppLanguage.KAZAKH -> "kk"
        AppLanguage.SYSTEM -> com.finuts.app.core.locale.getDefaultLocale()
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
        logger.i { "createAccount() called" }
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
                logger.i { "Account created successfully: ${account.name}, calling onNext()" }
                onNext() // Move to AIModelSetup
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
        println("=== DEBUG: skipAccountCreation() called ===")
        logger.i { "skipAccountCreation() called" }
        onNext() // Move to AIModelSetup without creating account
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
        OnboardingStep.CurrencyLanguage -> 0.2f
        OnboardingStep.GoalSelection -> 0.4f
        OnboardingStep.FirstAccountSetup -> 0.6f
        OnboardingStep.AIModelSetup -> 0.8f
        OnboardingStep.Completion -> 1f
    }

    // AI Model functions
    fun selectModelId(modelId: String) {
        logger.d { "selectModelId: $modelId" }
        _uiState.update { it.copy(selectedModelId = modelId) }
    }

    fun downloadModel() {
        val modelId = _uiState.value.selectedModelId
        logger.i { "downloadModel() called, selectedModelId=$modelId" }

        if (modelId == null) {
            logger.w { "downloadModel() aborted - no model selected" }
            return
        }

        if (modelRepository == null) {
            logger.e { "downloadModel() failed - modelRepository is NULL" }
            return
        }

        scope.launch {
            logger.i { "Calling modelRepository.downloadModel($modelId)" }
            try {
                val result = modelRepository.downloadModel(modelId)
                logger.i { "downloadModel result: isSuccess=${result.isSuccess}" }
                if (result.isFailure) {
                    logger.e { "downloadModel failed: ${result.exceptionOrNull()?.message}" }
                }
            } catch (e: Exception) {
                logger.e(e) { "downloadModel threw exception" }
            }
        }
    }

    fun cancelModelDownload() {
        logger.i { "cancelModelDownload() called" }
        modelRepository?.cancelDownload()
    }

    fun skipModelDownload() {
        logger.i { "skipModelDownload() called" }
        _uiState.update { it.copy(skipAIModel = true) }
        onNext()
    }

    fun onModelDownloadComplete() {
        val currentSelectedModelId = _uiState.value.selectedModelId
        logger.i { "onModelDownloadComplete() called, selectedModelId=$currentSelectedModelId" }

        if (currentSelectedModelId == null) {
            logger.e { "ERROR: selectedModelId is null in onModelDownloadComplete!" }
            onNext()
            return
        }

        scope.launch {
            logger.i { "Saving and selecting model: $currentSelectedModelId" }
            preferencesRepository.setSelectedModelId(currentSelectedModelId)
            preferencesRepository.setAIModelDownloadedInOnboarding(true)

            // FIX BUG #1: Actually select the model in repository
            // This updates ModelRepository.currentModel so OnDeviceLLMProvider.isAvailable() returns true
            val selectResult = modelRepository?.selectModel(currentSelectedModelId)
            if (selectResult == null) {
                logger.e { "modelRepository is null, cannot select model" }
            } else {
                selectResult.onFailure { e ->
                    logger.e(e) { "Failed to select model $currentSelectedModelId after download" }
                }.onSuccess {
                    logger.i { "Model $currentSelectedModelId selected successfully in repository" }
                }
            }
        }
        onNext()
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
    val createdAccountName: String? = null,
    // AI Model
    val availableModels: List<ModelConfig> = emptyList(),
    val recommendedModelId: String? = null,
    val selectedModelId: String? = null,
    val modelDownloadProgress: DownloadProgress = DownloadProgress.Idle,
    val skipAIModel: Boolean = false
) {
    /** Current step as index for HorizontalPager. */
    val currentStepIndex: Int
        get() = when (currentStep) {
            OnboardingStep.Welcome -> 0
            OnboardingStep.CurrencyLanguage -> 1
            OnboardingStep.GoalSelection -> 2
            OnboardingStep.FirstAccountSetup -> 3
            OnboardingStep.AIModelSetup -> 4
            OnboardingStep.Completion -> 5
        }

    @Deprecated("Use selectedGoals for multiple selection")
    val selectedGoal: UserGoal
        get() = selectedGoals.firstOrNull() ?: UserGoal.NOT_SET

    companion object {
        const val TOTAL_STEPS = 6
    }
}

/**
 * One-time events from the onboarding flow.
 */
sealed interface OnboardingEvent {
    data object NavigateToDashboard : OnboardingEvent
}

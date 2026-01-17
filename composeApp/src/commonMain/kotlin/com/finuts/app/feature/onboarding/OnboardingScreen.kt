package com.finuts.app.feature.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.feature.onboarding.steps.AIModelStep
import com.finuts.app.feature.onboarding.steps.CompletionStep
import com.finuts.app.feature.onboarding.steps.CurrencyLanguageStep
import com.finuts.app.feature.onboarding.steps.FirstAccountStep
import com.finuts.app.feature.onboarding.steps.GoalSelectionStep
import com.finuts.app.feature.onboarding.steps.WelcomeStep
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.ui.components.navigation.PageIndicator
import com.finuts.domain.model.OnboardingStep
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.onboarding_skip
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import co.touchlab.kermit.Logger

/**
 * Main onboarding screen container.
 * Uses HorizontalPager for swipe navigation with dot indicators.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = uiState.currentStepIndex,
        pageCount = { OnboardingUiState.TOTAL_STEPS }
    )

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OnboardingEvent.NavigateToDashboard -> onComplete()
            }
        }
    }

    // Sync pager with viewModel when viewModel changes step (via buttons)
    LaunchedEffect(uiState.currentStepIndex) {
        Logger.withTag("OnboardingScreen").i {
            "LaunchedEffect triggered: stepIndex=${uiState.currentStepIndex}, pagerPage=${pagerState.currentPage}"
        }
        if (pagerState.currentPage != uiState.currentStepIndex) {
            Logger.withTag("OnboardingScreen").i {
                "Animating pager to page ${uiState.currentStepIndex}"
            }
            pagerState.animateScrollToPage(uiState.currentStepIndex)
        }
    }

    // Sync viewModel with pager when user swipes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { page ->
                if (page != uiState.currentStepIndex) {
                    viewModel.setStep(page)
                }
            }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(FinutsSpacing.md))

            // Skip button (visible on skippable steps, not on Welcome or Completion)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FinutsSpacing.touchTarget)
                    .padding(horizontal = FinutsSpacing.sm),
                contentAlignment = Alignment.CenterEnd
            ) {
                // Hide global Skip on AIModelSetup (has its own skip with context)
                if (uiState.canSkip &&
                    uiState.currentStep != OnboardingStep.Completion &&
                    uiState.currentStep != OnboardingStep.AIModelSetup) {
                    TextButton(onClick = { viewModel.onSkip() }) {
                        Text(text = stringResource(Res.string.onboarding_skip))
                    }
                }
            }

            // Step content with swipe navigation
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true
            ) { page ->
                when (page) {
                    0 -> WelcomeStep(
                        onNext = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                    1 -> CurrencyLanguageStep(
                        selectedCurrency = uiState.selectedCurrency,
                        selectedLanguage = uiState.selectedLanguage,
                        onCurrencySelected = { viewModel.selectCurrency(it) },
                        onLanguageSelected = { viewModel.selectLanguage(it) },
                        onNext = {
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }
                    )
                    2 -> GoalSelectionStep(
                        selectedGoals = uiState.selectedGoals,
                        onGoalToggled = { viewModel.toggleGoal(it) },
                        onNext = {
                            scope.launch { pagerState.animateScrollToPage(3) }
                        }
                    )
                    3 -> FirstAccountStep(
                        accountName = uiState.accountName,
                        accountType = uiState.accountType,
                        initialBalance = uiState.initialBalance,
                        selectedCurrency = uiState.selectedCurrency,
                        accountNameError = uiState.accountNameError,
                        balanceError = uiState.balanceError,
                        isCreating = uiState.isCreatingAccount,
                        onNameChange = { viewModel.updateAccountName(it) },
                        onTypeChange = { viewModel.updateAccountType(it) },
                        onBalanceChange = { viewModel.updateInitialBalance(it) },
                        onCreateAccount = { viewModel.createAccount() },
                        onSkip = { viewModel.skipAccountCreation() }
                    )
                    4 -> AIModelStep(
                        availableModels = uiState.availableModels,
                        recommendedModelId = uiState.recommendedModelId,
                        selectedModelId = uiState.selectedModelId,
                        downloadProgress = uiState.modelDownloadProgress,
                        onSelectModel = { viewModel.selectModelId(it) },
                        onStartDownload = { viewModel.downloadModel() },
                        onCancelDownload = { viewModel.cancelModelDownload() },
                        onSkip = { viewModel.skipModelDownload() },
                        onNext = { viewModel.onModelDownloadComplete() }
                    )
                    5 -> CompletionStep(
                        createdAccountName = uiState.createdAccountName,
                        onComplete = { viewModel.completeOnboarding() }
                    )
                }
            }

            // Page indicator dots
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = FinutsSpacing.xl),
                contentAlignment = Alignment.Center
            ) {
                PageIndicator(
                    pageCount = OnboardingUiState.TOTAL_STEPS,
                    currentPage = pagerState.currentPage
                )
            }
        }
    }
}

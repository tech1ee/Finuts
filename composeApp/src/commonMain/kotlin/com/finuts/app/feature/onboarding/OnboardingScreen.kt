package com.finuts.app.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.feature.onboarding.steps.CompletionStep
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.feature.onboarding.steps.FirstAccountStep
import com.finuts.app.feature.onboarding.steps.GoalSelectionStep
import com.finuts.app.feature.onboarding.steps.WelcomeStep
import com.finuts.domain.model.OnboardingStep
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.onboarding_skip
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main onboarding screen container.
 * Manages step navigation and progress indication.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onNavigateToAddAccount: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OnboardingEvent.NavigateToDashboard -> onComplete()
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress indicator
            if (uiState.progressPercent > 0f) {
                LinearProgressIndicator(
                    progress = { uiState.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FinutsSpacing.lg, vertical = FinutsSpacing.sm)
                )
            }

            // Skip button (visible after Welcome)
            if (uiState.canSkip && uiState.currentStep != OnboardingStep.Completion) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FinutsSpacing.sm),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = { viewModel.onSkip() }) {
                        Text(text = stringResource(Res.string.onboarding_skip))
                    }
                }
            }

            // Step content with animations
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                },
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    OnboardingStep.Welcome -> WelcomeStep(
                        onNext = { viewModel.onNext() }
                    )
                    OnboardingStep.GoalSelection -> GoalSelectionStep(
                        selectedGoal = uiState.selectedGoal,
                        onGoalSelected = { viewModel.selectGoal(it) },
                        onNext = { viewModel.onNext() }
                    )
                    OnboardingStep.FirstAccountSetup -> FirstAccountStep(
                        onAddAccount = {
                            viewModel.completeOnboarding()
                            onNavigateToAddAccount()
                        },
                        onSkip = { viewModel.onNext() }
                    )
                    OnboardingStep.Completion -> CompletionStep(
                        onComplete = { viewModel.completeOnboarding() }
                    )
                }
            }
        }
    }
}

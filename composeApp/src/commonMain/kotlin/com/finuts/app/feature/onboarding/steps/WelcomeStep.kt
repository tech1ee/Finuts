package com.finuts.app.feature.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import compose.icons.TablerIcons
import compose.icons.tablericons.CurrencyDollar
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.onboarding_welcome_cta
import finuts.composeapp.generated.resources.onboarding_welcome_description
import finuts.composeapp.generated.resources.onboarding_welcome_title
import org.jetbrains.compose.resources.stringResource

/**
 * Welcome step - introduces the app value proposition.
 */
@Composable
fun WelcomeStep(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(FinutsSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = TablerIcons.CurrencyDollar,
            contentDescription = null,
            modifier = Modifier.size(FinutsSpacing.onboardingHeroIconSize),
            tint = FinutsColors.Accent
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.xl))

        Text(
            text = stringResource(Res.string.onboarding_welcome_title),
            style = FinutsTypography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.md))

        Text(
            text = stringResource(Res.string.onboarding_welcome_description),
            style = FinutsTypography.bodyLarge,
            textAlign = TextAlign.Center,
            color = FinutsColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.xxl))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(Res.string.onboarding_welcome_cta))
        }
    }
}

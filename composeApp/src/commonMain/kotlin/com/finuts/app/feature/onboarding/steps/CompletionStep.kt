package com.finuts.app.feature.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import compose.icons.tablericons.CircleCheck
import compose.icons.tablericons.Wallet
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.onboarding_complete_account_created
import finuts.composeapp.generated.resources.onboarding_complete_cta
import finuts.composeapp.generated.resources.onboarding_complete_description
import finuts.composeapp.generated.resources.onboarding_complete_title
import org.jetbrains.compose.resources.stringResource

/**
 * Completion step - success message with created account preview.
 */
@Composable
fun CompletionStep(
    createdAccountName: String?,
    onComplete: () -> Unit,
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
            imageVector = TablerIcons.CircleCheck,
            contentDescription = null,
            modifier = Modifier.size(FinutsSpacing.onboardingHeroIconSize),
            tint = FinutsColors.Accent
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.xl))

        Text(
            text = stringResource(Res.string.onboarding_complete_title),
            style = FinutsTypography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.md))

        Text(
            text = stringResource(Res.string.onboarding_complete_description),
            style = FinutsTypography.bodyLarge,
            textAlign = TextAlign.Center,
            color = FinutsColors.TextSecondary
        )

        // Show created account card if one was created
        if (createdAccountName != null) {
            Spacer(modifier = Modifier.height(FinutsSpacing.xl))
            AccountCreatedCard(accountName = createdAccountName)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(Res.string.onboarding_complete_cta))
        }
    }
}

@Composable
private fun AccountCreatedCard(accountName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinutsColors.AccentMuted)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FinutsSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.md)
        ) {
            Icon(
                imageVector = TablerIcons.Wallet,
                contentDescription = null,
                modifier = Modifier.size(FinutsSpacing.accountLogoSize),
                tint = FinutsColors.Accent
            )
            Text(
                text = stringResource(Res.string.onboarding_complete_account_created, accountName),
                style = FinutsTypography.titleMedium
            )
        }
    }
}

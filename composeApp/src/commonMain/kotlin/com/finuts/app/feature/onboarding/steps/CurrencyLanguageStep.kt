package com.finuts.app.feature.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.domain.model.AppLanguage
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.onboarding_currency_language_cta
import finuts.composeapp.generated.resources.onboarding_currency_title
import finuts.composeapp.generated.resources.onboarding_language_title
import org.jetbrains.compose.resources.stringResource

/**
 * Currency and language selection step.
 * Allows user to personalize the app from the start.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurrencyLanguageStep(
    selectedCurrency: String,
    selectedLanguage: AppLanguage,
    onCurrencySelected: (String) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(FinutsSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(FinutsSpacing.lg))

        // Currency section
        Text(
            text = stringResource(Res.string.onboarding_currency_title),
            style = FinutsTypography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.md))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)
        ) {
            CurrencyOption("KZT", "₸", selectedCurrency == "KZT") { onCurrencySelected("KZT") }
            CurrencyOption("RUB", "₽", selectedCurrency == "RUB") { onCurrencySelected("RUB") }
            CurrencyOption("USD", "$", selectedCurrency == "USD") { onCurrencySelected("USD") }
            CurrencyOption("EUR", "€", selectedCurrency == "EUR") { onCurrencySelected("EUR") }
        }

        Spacer(modifier = Modifier.height(FinutsSpacing.xxl))

        // Language section
        Text(
            text = stringResource(Res.string.onboarding_language_title),
            style = FinutsTypography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.md))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)
        ) {
            LanguageOption(AppLanguage.SYSTEM, selectedLanguage == AppLanguage.SYSTEM) {
                onLanguageSelected(AppLanguage.SYSTEM)
            }
            LanguageOption(AppLanguage.RUSSIAN, selectedLanguage == AppLanguage.RUSSIAN) {
                onLanguageSelected(AppLanguage.RUSSIAN)
            }
            LanguageOption(AppLanguage.KAZAKH, selectedLanguage == AppLanguage.KAZAKH) {
                onLanguageSelected(AppLanguage.KAZAKH)
            }
            LanguageOption(AppLanguage.ENGLISH, selectedLanguage == AppLanguage.ENGLISH) {
                onLanguageSelected(AppLanguage.ENGLISH)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(Res.string.onboarding_currency_language_cta))
        }
    }
}

@Composable
private fun CurrencyOption(
    code: String,
    symbol: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(width = 80.dp, height = 72.dp)
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FinutsColors.AccentMuted else FinutsColors.Surface
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, FinutsColors.Accent)
        } else {
            BorderStroke(1.dp, FinutsColors.Border)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(FinutsSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = symbol,
                style = FinutsTypography.headlineMedium,
                color = if (isSelected) FinutsColors.Accent else FinutsColors.TextSecondary
            )
            Text(
                text = code,
                style = FinutsTypography.labelMedium,
                color = FinutsColors.TextSecondary
            )
        }
    }
}

@Composable
private fun LanguageOption(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FinutsColors.AccentMuted else FinutsColors.Surface
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, FinutsColors.Accent)
        } else {
            BorderStroke(1.dp, FinutsColors.Border)
        }
    ) {
        Text(
            text = language.displayName,
            style = FinutsTypography.titleMedium,
            color = if (isSelected) FinutsColors.Accent else FinutsColors.TextPrimary,
            modifier = Modifier.padding(horizontal = FinutsSpacing.md, vertical = FinutsSpacing.sm)
        )
    }
}

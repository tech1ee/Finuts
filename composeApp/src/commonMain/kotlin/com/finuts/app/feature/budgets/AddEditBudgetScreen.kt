package com.finuts.app.feature.budgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.finuts.app.feature.budgets.components.BudgetForm
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.form.FormTopBar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddEditBudgetScreen(
    budgetId: String?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditBudgetViewModel = koinViewModel { parametersOf(budgetId) }
) {
    val formState by viewModel.formState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val categories by viewModel.categories.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            if (event is NavigationEvent.PopBackStack) onNavigateBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = FinutsSpacing.lg, bottom = FinutsSpacing.bottomNavHeight + FinutsSpacing.lg)
    ) {
        FormTopBar(
            title = getScreenTitle(viewModel.isEditMode),
            onBackClick = { viewModel.onBackClick() }
        )

        Spacer(Modifier.height(FinutsSpacing.lg))

        BudgetForm(
            formState = formState,
            categories = categories,
            onNameChange = viewModel::onNameChange,
            onAmountChange = viewModel::onAmountChange,
            onCategoryChange = viewModel::onCategoryChange,
            onPeriodChange = viewModel::onPeriodChange,
            onCurrencyChange = viewModel::onCurrencyChange,
            modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
        )

        Spacer(Modifier.height(FinutsSpacing.xl))

        Button(
            onClick = { viewModel.save() },
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = FinutsColors.Accent,
                contentColor = FinutsColors.OnAccent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinutsSpacing.screenPadding)
                .height(FinutsSpacing.buttonHeight)
        ) {
            Text(text = getSaveButtonLabel(isSaving), style = FinutsTypography.labelLarge)
        }
    }
}

// === Testable Helper Functions ===

fun getScreenTitle(isEditMode: Boolean): String = if (isEditMode) "Edit Budget" else "Add Budget"

fun getSaveButtonLabel(isSaving: Boolean): String = if (isSaving) "Saving..." else "Save"

package com.finuts.app.feature.budgets.states

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.ui.components.feedback.EmptyState
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.add_budget
import finuts.composeapp.generated.resources.no_budgets
import finuts.composeapp.generated.resources.no_budgets_desc
import org.jetbrains.compose.resources.stringResource

/**
 * Empty state for budgets screen.
 */
@Composable
fun BudgetsEmptyState(
    onAddBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = stringResource(Res.string.no_budgets),
        description = stringResource(Res.string.no_budgets_desc),
        actionLabel = stringResource(Res.string.add_budget),
        onAction = onAddBudget,
        modifier = modifier
    )
}

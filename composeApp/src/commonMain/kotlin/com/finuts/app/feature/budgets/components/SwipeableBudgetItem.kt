package com.finuts.app.feature.budgets.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.ui.components.budget.BudgetListItem
import com.finuts.domain.entity.BudgetProgress
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.delete
import org.jetbrains.compose.resources.stringResource

/**
 * Swipeable budget list item with delete action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableBudgetItem(
    budgetProgress: BudgetProgress,
    onClick: () -> Unit,
    onDeactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDeactivate()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground() },
        enableDismissFromStartToEnd = false,
        modifier = modifier
    ) {
        BudgetListItem(budgetProgress = budgetProgress, onClick = onClick)
    }
}

@Composable
private fun SwipeBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = FinutsSpacing.screenPadding),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(Res.string.delete),
            tint = FinutsColors.Expense
        )
    }
}

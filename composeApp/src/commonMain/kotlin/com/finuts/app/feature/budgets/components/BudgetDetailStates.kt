package com.finuts.app.feature.budgets.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsTypography

@Composable
fun BudgetLoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun BudgetErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        Text(
            text = message,
            style = FinutsTypography.bodyLarge,
            color = FinutsColors.Expense
        )
    }
}

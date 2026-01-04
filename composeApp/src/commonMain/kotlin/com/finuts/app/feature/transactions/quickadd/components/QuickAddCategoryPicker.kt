package com.finuts.app.feature.transactions.quickadd.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.icons.CategoryIcon

/**
 * Category selection chips with icons for quick add transaction.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddCategoryPicker(
    categories: List<Pair<String, String>>,
    selectedCategoryId: String?,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            text = "Category",
            style = FinutsTypography.labelMedium,
            color = FinutsColors.TextSecondary
        )
        Spacer(Modifier.height(FinutsSpacing.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)) {
            categories.forEach { (id, label) ->
                FilterChip(
                    selected = selectedCategoryId == id,
                    onClick = { onCategoryChange(id) },
                    label = { Text(label, style = FinutsTypography.labelMedium) },
                    leadingIcon = {
                        CategoryIcon(
                            iconKey = id,
                            size = 18.dp,
                            tint = if (selectedCategoryId == id)
                                FinutsColors.Accent
                            else
                                FinutsColors.TextSecondary
                        )
                    }
                )
            }
        }
    }
}

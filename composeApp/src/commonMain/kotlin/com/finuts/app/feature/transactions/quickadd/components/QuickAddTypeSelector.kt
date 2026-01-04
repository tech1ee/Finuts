package com.finuts.app.feature.transactions.quickadd.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsTypography
import com.finuts.domain.entity.TransactionType

/**
 * Segmented button row for selecting transaction type (Income/Expense).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTypeSelector(
    selectedType: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        TransactionType.entries.forEachIndexed { index, type ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index, TransactionType.entries.size),
                onClick = { onTypeChange(type) },
                selected = selectedType == type
            ) {
                Text(
                    text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = FinutsTypography.labelMedium
                )
            }
        }
    }
}

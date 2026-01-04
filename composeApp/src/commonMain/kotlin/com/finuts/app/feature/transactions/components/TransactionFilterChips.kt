package com.finuts.app.feature.transactions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.feature.transactions.TransactionFilter
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Horizontal row of filter chips for transaction types.
 */
@Composable
fun TransactionFilterChips(
    selected: TransactionFilter,
    onSelect: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = FinutsSpacing.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)
    ) {
        TransactionFilter.entries.forEach { filter ->
            item {
                FilterChip(
                    selected = selected == filter,
                    onClick = { onSelect(filter) },
                    label = {
                        Text(
                            text = stringResource(filter.labelRes),
                            style = FinutsTypography.labelMedium
                        )
                    }
                )
            }
        }
    }
}

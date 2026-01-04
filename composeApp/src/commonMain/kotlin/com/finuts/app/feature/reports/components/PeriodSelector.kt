package com.finuts.app.feature.reports.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.domain.entity.ReportPeriod

/**
 * Horizontal selector for report time periods.
 * Shows filter chips for each available period.
 */
@Composable
fun PeriodSelector(
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods = listOf(
        ReportPeriod.THIS_WEEK,
        ReportPeriod.THIS_MONTH,
        ReportPeriod.LAST_MONTH,
        ReportPeriod.THIS_YEAR
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(periods) { period ->
            PeriodChip(
                period = period,
                isSelected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) }
            )
        }
    }
}

@Composable
private fun PeriodChip(
    period: ReportPeriod,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = period.displayLabel,
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = FinutsColors.Accent,
            selectedLabelColor = FinutsColors.OnAccent
        )
    )
}

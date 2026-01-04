package com.finuts.app.feature.transactions.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.icons.FinutsIcons
import com.finuts.domain.entity.Transaction
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Info section showing transaction details in rows.
 * Displays: Account, Category (if exists), Date, Time.
 */
@Composable
fun TransactionDetailInfo(
    transaction: Transaction,
    accountName: String,
    categoryName: String?,
    modifier: Modifier = Modifier
) {
    val localDateTime = transaction.date.toLocalDateTime(TimeZone.currentSystemDefault())
    val dateString = formatDate(localDateTime)
    val timeString = formatTime(localDateTime)

    Column(modifier = modifier.fillMaxWidth()) {
        InfoRow(
            icon = FinutsIcons.Accounts,
            label = "Account",
            value = accountName
        )

        HorizontalDivider(
            color = FinutsColors.BorderSubtle,
            modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
        )

        categoryName?.let { name ->
            InfoRow(
                icon = FinutsIcons.Tag,
                label = "Category",
                value = name
            )
            HorizontalDivider(
                color = FinutsColors.BorderSubtle,
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            )
        }

        InfoRow(
            icon = FinutsIcons.Calendar,
            label = "Date",
            value = dateString
        )

        HorizontalDivider(
            color = FinutsColors.BorderSubtle,
            modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
        )

        InfoRow(
            icon = FinutsIcons.Clock,
            label = "Time",
            value = timeString
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FinutsSpacing.screenPadding, vertical = FinutsSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = FinutsColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(FinutsSpacing.md))

        Text(
            text = label,
            style = FinutsTypography.bodyMedium,
            color = FinutsColors.TextSecondary
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = value,
            style = FinutsTypography.bodyMedium,
            color = FinutsColors.TextPrimary
        )
    }
}

private fun formatDate(dateTime: kotlinx.datetime.LocalDateTime): String {
    val month = dateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "${dateTime.dayOfMonth} $month ${dateTime.year}"
}

private fun formatTime(dateTime: kotlinx.datetime.LocalDateTime): String {
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}

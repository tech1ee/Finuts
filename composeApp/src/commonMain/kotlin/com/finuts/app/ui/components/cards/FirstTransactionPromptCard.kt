package com.finuts.app.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.add_transaction
import finuts.composeapp.generated.resources.start_tracking_desc
import finuts.composeapp.generated.resources.start_tracking_title
import org.jetbrains.compose.resources.stringResource

/**
 * First Transaction Prompt Card
 *
 * Inline prompt shown on Dashboard when user has accounts
 * but no transactions yet. Encourages adding first transaction.
 *
 * Layout (vertical):
 * +-----------------------------------------------------------+
 * | [+] Начните отслеживать расходы                           |
 * |     Добавьте первую транзакцию для получения аналитики    |
 * |                                                           |
 * |                           [ + Добавить транзакцию ]       |
 * +-----------------------------------------------------------+
 */

private val CardShape = RoundedCornerShape(12.dp)

@Composable
fun FirstTransactionPromptCard(
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(FinutsColors.Accent.copy(alpha = 0.1f))
            .padding(FinutsSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "+",
                style = FinutsTypography.headlineSmall,
                color = FinutsColors.Accent,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(FinutsSpacing.iconToTextGap))

            Column {
                Text(
                    text = stringResource(Res.string.start_tracking_title),
                    style = FinutsTypography.titleMedium,
                    color = FinutsColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(Res.string.start_tracking_desc),
                    style = FinutsTypography.bodyMedium,
                    color = FinutsColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(FinutsSpacing.sm))

        FilledTonalButton(
            onClick = onAddTransaction,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = stringResource(Res.string.add_transaction))
        }
    }
}

package com.finuts.app.ui.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.ui.components.cards.AccountType
import com.finuts.domain.entity.AccountType as DomainAccountType

/**
 * Account icon component with optional colored background container.
 *
 * Displays a Lucide icon based on the account type.
 * Supports both standalone icons and icons with background containers.
 *
 * @param accountType The account type enum
 * @param modifier Modifier for the icon or container
 * @param tint Icon color (default: TextSecondary)
 * @param size Icon size in dp (default: 24dp)
 * @param showContainer Whether to show a rounded background container
 * @param containerSize Size of the background container (default: 32dp)
 * @param containerColor Background color for the container
 */
@Composable
fun AccountIcon(
    accountType: AccountType,
    modifier: Modifier = Modifier,
    tint: Color = FinutsColors.TextSecondary,
    size: Dp = 24.dp,
    showContainer: Boolean = false,
    containerSize: Dp = 32.dp,
    containerColor: Color = tint.copy(alpha = 0.15f)
) {
    val icon = getAccountIcon(accountType)

    if (showContainer) {
        Box(
            modifier = modifier
                .size(containerSize)
                .clip(RoundedCornerShape(FinutsSpacing.sm))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = accountType.name,
                modifier = Modifier.size(size),
                tint = tint
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = accountType.name,
            modifier = modifier.size(size),
            tint = tint
        )
    }
}

/**
 * Account icon component using string key.
 *
 * @param iconKey The account icon key (e.g., "card", "cash", "savings")
 */
@Composable
fun AccountIcon(
    iconKey: String,
    modifier: Modifier = Modifier,
    tint: Color = FinutsColors.TextSecondary,
    size: Dp = 24.dp,
    showContainer: Boolean = false,
    containerSize: Dp = 32.dp,
    containerColor: Color = tint.copy(alpha = 0.15f)
) {
    val icon = getAccountIconByKey(iconKey)

    if (showContainer) {
        Box(
            modifier = modifier
                .size(containerSize)
                .clip(RoundedCornerShape(FinutsSpacing.sm))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconKey,
                modifier = Modifier.size(size),
                tint = tint
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = iconKey,
            modifier = modifier.size(size),
            tint = tint
        )
    }
}

/**
 * Maps AccountType enum to Lucide icons.
 */
fun getAccountIcon(type: AccountType): ImageVector {
    return when (type) {
        AccountType.CASH -> FinutsIcons.Cash
        AccountType.DEBIT_CARD -> FinutsIcons.Card
        AccountType.CREDIT_CARD -> FinutsIcons.Card
        AccountType.SAVINGS -> FinutsIcons.Savings
        AccountType.INVESTMENT -> FinutsIcons.Investment
    }
}

/**
 * Maps account icon key strings to Lucide icons.
 *
 * Supports multiple naming conventions:
 * - Lowercase: "card", "cash"
 * - With underscores: "debit_card", "credit_card"
 */
fun getAccountIconByKey(key: String): ImageVector {
    return when (key.lowercase()) {
        "cash", "banknote", "money" -> FinutsIcons.Cash
        "card", "debit", "debit_card" -> FinutsIcons.Card
        "credit", "credit_card" -> FinutsIcons.Card
        "savings", "piggy", "piggy_bank" -> FinutsIcons.Savings
        "investment", "invest", "stocks", "trading" -> FinutsIcons.Investment
        "bank", "building", "institution" -> FinutsIcons.Bank
        "wallet" -> FinutsIcons.Wallet
        else -> FinutsIcons.Wallet
    }
}

/**
 * Get the recommended color for an account type.
 * Returns a semantic color that visually represents the account type.
 */
fun getAccountColor(type: AccountType): Color {
    return when (type) {
        AccountType.CASH -> FinutsColors.Accent
        AccountType.DEBIT_CARD -> FinutsColors.Transfer
        AccountType.CREDIT_CARD -> FinutsColors.Expense
        AccountType.SAVINGS -> FinutsColors.Income
        AccountType.INVESTMENT -> FinutsColors.Tertiary
    }
}

/**
 * Account icon component for domain AccountType.
 */
@Composable
fun AccountIcon(
    accountType: DomainAccountType,
    modifier: Modifier = Modifier,
    tint: Color = FinutsColors.TextSecondary,
    size: Dp = 24.dp,
    showContainer: Boolean = false,
    containerSize: Dp = 32.dp,
    containerColor: Color = tint.copy(alpha = 0.15f)
) {
    val icon = getDomainAccountIcon(accountType)

    if (showContainer) {
        Box(
            modifier = modifier
                .size(containerSize)
                .clip(RoundedCornerShape(FinutsSpacing.sm))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = accountType.name,
                modifier = Modifier.size(size),
                tint = tint
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = accountType.name,
            modifier = modifier.size(size),
            tint = tint
        )
    }
}

/**
 * Maps domain AccountType enum to Lucide icons.
 */
fun getDomainAccountIcon(type: DomainAccountType): ImageVector {
    return when (type) {
        DomainAccountType.CASH -> FinutsIcons.Cash
        DomainAccountType.BANK_ACCOUNT -> FinutsIcons.Bank
        DomainAccountType.CREDIT_CARD -> FinutsIcons.Card
        DomainAccountType.DEBIT_CARD -> FinutsIcons.Card
        DomainAccountType.SAVINGS -> FinutsIcons.Savings
        DomainAccountType.INVESTMENT -> FinutsIcons.Investment
        DomainAccountType.CRYPTO -> FinutsIcons.Investment
        DomainAccountType.OTHER -> FinutsIcons.Wallet
    }
}

/**
 * Get the recommended color for a domain account type.
 */
fun getAccountColor(type: DomainAccountType): Color {
    return when (type) {
        DomainAccountType.CASH -> FinutsColors.Accent
        DomainAccountType.BANK_ACCOUNT -> FinutsColors.Transfer
        DomainAccountType.CREDIT_CARD -> FinutsColors.Expense
        DomainAccountType.DEBIT_CARD -> FinutsColors.Transfer
        DomainAccountType.SAVINGS -> FinutsColors.Income
        DomainAccountType.INVESTMENT -> FinutsColors.Tertiary
        DomainAccountType.CRYPTO -> FinutsColors.Tertiary
        DomainAccountType.OTHER -> FinutsColors.TextSecondary
    }
}

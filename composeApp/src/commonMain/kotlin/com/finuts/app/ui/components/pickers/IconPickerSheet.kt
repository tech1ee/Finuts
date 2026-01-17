package com.finuts.app.ui.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.icons.CategoryIcon
import com.finuts.domain.registry.IconRegistry

/**
 * Icon Picker Sheet - Bottom sheet for selecting category icon from IconRegistry.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Select Icon                                            ✕   │
 * │─────────────────────────────────────────────────────────────│
 * │  FOOD                                                       │
 * │  🛒   ☕   🍕   🚚   🍳                                     │
 * │─────────────────────────────────────────────────────────────│
 * │  TRANSPORT                                                  │
 * │  🚗  🚌  ⛽  🚕  ✈️                                         │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Specs:
 * - Grid: 5 columns
 * - Bottom sheet: 300dp height
 * - Groups from IconRegistry (13 categories)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerSheet(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FinutsColors.Surface,
        modifier = modifier
    ) {
        IconPickerContent(
            selectedIcon = selectedIcon,
            onIconSelected = { icon ->
                onIconSelected(icon)
                onDismiss()
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun IconPickerContent(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val iconRegistry = remember { IconRegistry() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = FinutsSpacing.lg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinutsSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Icon",
                style = FinutsTypography.titleMedium,
                color = FinutsColors.TextPrimary
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = FinutsColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(FinutsSpacing.md))

        // Icon grid by group
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(horizontal = FinutsSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(350.dp)
        ) {
            iconRegistry.iconGroups.forEach { (groupName, icons) ->
                // Group header spans full width
                item(span = { GridItemSpan(5) }) {
                    Text(
                        text = formatGroupName(groupName),
                        style = FinutsTypography.labelSmall,
                        color = FinutsColors.TextTertiary,
                        modifier = Modifier.padding(
                            top = FinutsSpacing.md,
                            bottom = FinutsSpacing.xs
                        )
                    )
                }
                items(icons) { iconKey ->
                    IconPickerItem(
                        iconKey = iconKey,
                        isSelected = iconKey == selectedIcon,
                        onClick = { onIconSelected(iconKey) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IconPickerItem(
    iconKey: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) FinutsColors.Accent.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = FinutsColors.Accent,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CategoryIcon(
            iconKey = iconKey,
            size = 24.dp,
            tint = if (isSelected) FinutsColors.Accent else FinutsColors.TextSecondary
        )
    }
}

/**
 * Formats group name for display (e.g., "food" -> "FOOD")
 */
private fun formatGroupName(groupName: String): String {
    return groupName.uppercase()
}

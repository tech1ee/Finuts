package com.finuts.app.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsElevation
import com.finuts.app.theme.FinutsMotion
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTheme
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.nav_budgets
import finuts.composeapp.generated.resources.nav_history
import finuts.composeapp.generated.resources.nav_home
import finuts.composeapp.generated.resources.nav_settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Pill-Shaped Bottom Navigation Bar
 *
 * Floating pill navigation following reference design:
 * ┌─────────────────────────────────────────────────┐
 * │        🏠        📋        📊        ⚙️        │
 * │       Home    History   Budgets  Settings       │
 * └─────────────────────────────────────────────────┘
 *
 * Navigation Architecture (v3.0):
 * - HOME: Financial STATE (balance, accounts, charts, health)
 * - HISTORY: Financial HISTORY (transactions, what happened)
 * - BUDGETS: Financial GOALS (budgets, savings goals)
 * - SETTINGS: Configuration
 *
 * Calculated Specs (from DESIGN_SYSTEM.md):
 * - Width: 240dp (4 × 48 + 48 padding)
 * - Height: 56dp (compact pill)
 * - Corner radius: 28dp (half height — pill shape)
 * - Bottom margin: 16dp + safe area
 * - Background: #0A0A0A
 * - Border: 1dp rgba(255,255,255,0.06)
 * - Item touch target: 48dp × 48dp
 * - Icon size: 24dp
 * - Gap between items: 12dp
 * - Active: white icon
 * - Inactive: #6B7280 (50% opacity feel)
 * - Press: scale(0.95), 100ms
 */

private val PillShape = RoundedCornerShape(FinutsSpacing.navPillRadius)

enum class PillNavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelRes: StringResource
) {
    HOME(Icons.Filled.Home, Icons.Outlined.Home, Res.string.nav_home),
    HISTORY(Icons.Filled.Receipt, Icons.Outlined.Receipt, Res.string.nav_history),
    BUDGETS(Icons.Filled.PieChart, Icons.Outlined.PieChart, Res.string.nav_budgets),
    SETTINGS(Icons.Filled.Settings, Icons.Outlined.Settings, Res.string.nav_settings)
}

@Composable
fun PillBottomNavBar(
    selectedItem: PillNavItem,
    onItemSelected: (PillNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val navColors = FinutsTheme.navColors
    val safeAreaPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = FinutsSpacing.navPillBottom + safeAreaPadding.calculateBottomPadding()
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .width(FinutsSpacing.navPillWidth) // 240dp fixed width
                .height(FinutsSpacing.navPillHeight) // 56dp
                .shadow(
                    elevation = FinutsElevation.navPill,
                    shape = PillShape
                )
                .clip(PillShape)
                .background(navColors.pill)
                .border(
                    width = 1.dp,
                    color = FinutsColors.GlassBorder,
                    shape = PillShape
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillNavItem.entries.forEach { item ->
                PillNavItemButton(
                    item = item,
                    isSelected = item == selectedItem,
                    onClick = { onItemSelected(item) }
                )
            }
        }
    }
}

@Composable
private fun PillNavItemButton(
    item: PillNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val navColors = FinutsTheme.navColors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) navColors.active else navColors.inactive,
        animationSpec = FinutsMotion.microTween(),
        label = "iconColor"
    )

    val scale = if (isPressed) FinutsMotion.navItemPressScale else 1f

    Box(
        modifier = Modifier
            .size(FinutsSpacing.navItemSize) // 48dp touch target
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = stringResource(item.labelRes),
            tint = iconColor,
            modifier = Modifier.size(FinutsSpacing.navIconSize) // 24dp
        )
    }
}

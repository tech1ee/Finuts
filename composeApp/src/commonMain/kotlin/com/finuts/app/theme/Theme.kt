package com.finuts.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Finuts Theme - Sophisticated Minimalism
 *
 * Design philosophy: Linear/Notion/Copilot Money style
 * Features:
 * - Emerald Green accent color
 * - Clear visual hierarchy
 * - Finance-specific semantic colors
 */

// ============================================================
// MATERIAL COLOR SCHEMES
// ============================================================

private val LightColorScheme = lightColorScheme(
    primary = FinutsColors.Accent,
    onPrimary = FinutsColors.OnAccent,
    primaryContainer = FinutsColors.AccentMuted,
    onPrimaryContainer = FinutsColors.AccentHover,
    secondary = FinutsColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EAF6),
    onSecondaryContainer = Color(0xFF3949AB),
    tertiary = FinutsColors.Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE7F6),
    onTertiaryContainer = Color(0xFF5E35B1),
    background = FinutsColors.Background,
    onBackground = FinutsColors.TextPrimary,
    surface = FinutsColors.Surface,
    onSurface = FinutsColors.TextPrimary,
    surfaceVariant = FinutsColors.SurfaceVariant,
    onSurfaceVariant = FinutsColors.TextSecondary,
    outline = FinutsColors.Border,
    outlineVariant = FinutsColors.BorderSubtle,
    error = FinutsColors.Error,
    onError = FinutsColors.OnError,
    errorContainer = FinutsColors.ErrorMuted,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = FinutsColors.Accent,
    onPrimary = Color.White,
    primaryContainer = FinutsColors.AccentMutedDark,
    onPrimaryContainer = FinutsColors.Accent,
    secondary = FinutsColors.TransferDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = FinutsColors.TransferDark,
    tertiary = Color(0xFFC4B5FD),
    onTertiary = Color(0xFF2E1065),
    background = FinutsColors.BackgroundDark,
    onBackground = FinutsColors.TextPrimaryDark,
    surface = FinutsColors.SurfaceDark,
    onSurface = FinutsColors.TextPrimaryDark,
    surfaceVariant = FinutsColors.SurfaceVariantDark,
    onSurfaceVariant = FinutsColors.TextSecondaryDark,
    outline = FinutsColors.BorderDark,
    outlineVariant = FinutsColors.BorderSubtleDark,
    error = FinutsColors.ExpenseDark,
    onError = Color(0xFF450A0A)
)

// ============================================================
// SEMANTIC COLORS (Finance-specific)
// ============================================================

data class FinutsSemanticColors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
    val warning: Color
)

private val LightSemanticColors = FinutsSemanticColors(
    income = FinutsColors.Income,
    expense = FinutsColors.Expense,
    transfer = FinutsColors.Transfer,
    warning = FinutsColors.Warning
)

private val DarkSemanticColors = FinutsSemanticColors(
    income = FinutsColors.IncomeDark,
    expense = FinutsColors.ExpenseDark,
    transfer = FinutsColors.TransferDark,
    warning = FinutsColors.WarningDark
)

val LocalFinutsSemanticColors = staticCompositionLocalOf { LightSemanticColors }

// ============================================================
// HERO COLORS (Dark balance card)
// ============================================================

data class FinutsHeroColors(
    val cardBackground: Color,
    val cardBackgroundVariant: Color,
    val onCard: Color,
    val onCardVariant: Color
)

private val HeroColors = FinutsHeroColors(
    cardBackground = FinutsColors.HeroGradientStart,
    cardBackgroundVariant = FinutsColors.HeroGradientEnd,
    onCard = FinutsColors.HeroText,
    onCardVariant = FinutsColors.HeroTextSecondary
)

val LocalFinutsHeroColors = staticCompositionLocalOf { HeroColors }

// ============================================================
// NAVIGATION COLORS (Pill bottom nav)
// ============================================================

data class FinutsNavColors(
    val pill: Color,
    val active: Color,
    val inactive: Color
)

private val NavColors = FinutsNavColors(
    pill = FinutsColors.NavBackground,
    active = FinutsColors.NavActive,
    inactive = FinutsColors.NavInactive
)

val LocalFinutsNavColors = staticCompositionLocalOf { NavColors }

// ============================================================
// PROGRESS COLORS (Semantic progress states)
// ============================================================

data class FinutsProgressColors(
    val onTrack: Color,
    val behind: Color,
    val overdue: Color,
    val background: Color
)

private val LightProgressColors = FinutsProgressColors(
    onTrack = FinutsColors.ProgressOnTrack,
    behind = FinutsColors.ProgressBehind,
    overdue = FinutsColors.ProgressOverdue,
    background = FinutsColors.ProgressBackground
)

private val DarkProgressColors = FinutsProgressColors(
    onTrack = FinutsColors.ProgressOnTrack,
    behind = FinutsColors.ProgressBehind,
    overdue = FinutsColors.ProgressOverdue,
    background = FinutsColors.ProgressBackgroundDark
)

val LocalFinutsProgressColors = staticCompositionLocalOf { LightProgressColors }

// ============================================================
// TEXT COLORS (For direct access)
// ============================================================

data class FinutsTextColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val disabled: Color
)

private val LightTextColors = FinutsTextColors(
    primary = FinutsColors.TextPrimary,
    secondary = FinutsColors.TextSecondary,
    tertiary = FinutsColors.TextTertiary,
    disabled = FinutsColors.TextDisabled
)

private val DarkTextColors = FinutsTextColors(
    primary = FinutsColors.TextPrimaryDark,
    secondary = FinutsColors.TextSecondaryDark,
    tertiary = FinutsColors.TextTertiaryDark,
    disabled = FinutsColors.TextDisabledDark
)

val LocalFinutsTextColors = staticCompositionLocalOf { LightTextColors }

// ============================================================
// THEME COMPOSABLE
// ============================================================

@Composable
fun FinutsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors
    val progressColors = if (darkTheme) DarkProgressColors else LightProgressColors
    val textColors = if (darkTheme) DarkTextColors else LightTextColors

    CompositionLocalProvider(
        LocalFinutsSemanticColors provides semanticColors,
        LocalFinutsHeroColors provides HeroColors,
        LocalFinutsNavColors provides NavColors,
        LocalFinutsProgressColors provides progressColors,
        LocalFinutsTextColors provides textColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FinutsTypography,
            content = content
        )
    }
}

// ============================================================
// THEME ACCESSORS
// ============================================================

object FinutsTheme {
    val semanticColors: FinutsSemanticColors
        @Composable
        get() = LocalFinutsSemanticColors.current

    val heroColors: FinutsHeroColors
        @Composable
        get() = LocalFinutsHeroColors.current

    val navColors: FinutsNavColors
        @Composable
        get() = LocalFinutsNavColors.current

    val progressColors: FinutsProgressColors
        @Composable
        get() = LocalFinutsProgressColors.current

    val textColors: FinutsTextColors
        @Composable
        get() = LocalFinutsTextColors.current
}

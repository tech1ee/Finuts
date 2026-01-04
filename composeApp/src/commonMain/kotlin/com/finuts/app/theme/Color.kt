package com.finuts.app.theme

import androidx.compose.ui.graphics.Color

/**
 * Finuts Color System - Sophisticated Minimalism
 *
 * Design philosophy: Linear/Notion/Copilot Money style
 * Accent: Emerald Green for trust + growth
 * Hierarchy: Clear layering with purposeful contrast
 */
object FinutsColors {

    // ============================================================
    // BRAND ACCENT (Emerald Green - Trust + Growth)
    // ============================================================
    val Accent = Color(0xFF10B981)           // Primary brand color
    val AccentHover = Color(0xFF059669)      // Darker on interaction
    val AccentMuted = Color(0xFFD1FAE5)      // Tinted backgrounds (light)
    val AccentMutedDark = Color(0xFF064E3B)  // Tinted backgrounds (dark)
    val OnAccent = Color(0xFFFFFFFF)

    // ============================================================
    // SEMANTIC COLORS (Finance-specific)
    // ============================================================
    val Income = Color(0xFF10B981)           // Green (same as accent)
    val Expense = Color(0xFFEF4444)          // Red
    val Transfer = Color(0xFF6366F1)         // Indigo
    val Warning = Color(0xFFF59E0B)          // Amber

    // Dark mode semantic (desaturated for eye comfort)
    val IncomeDark = Color(0xFF34D399)
    val ExpenseDark = Color(0xFFF87171)
    val TransferDark = Color(0xFF818CF8)
    val WarningDark = Color(0xFFFBBF24)

    // ============================================================
    // BACKGROUNDS (Layered system)
    // ============================================================
    // Light theme
    val Background = Color(0xFFFAFAFA)       // Base layer
    val Surface = Color(0xFFFFFFFF)          // Cards, elevated
    val SurfaceVariant = Color(0xFFF5F5F7)   // Subtle differentiation
    val SurfaceElevated = Color(0xFFFFFFFF)  // Modals, sheets

    // Dark theme (NOT pure black - softer)
    val BackgroundDark = Color(0xFF0A0A0A)       // Base layer
    val SurfaceDark = Color(0xFF141414)          // Cards
    val SurfaceVariantDark = Color(0xFF1C1C1E)   // Hero areas, nav
    val SurfaceElevatedDark = Color(0xFF1E1E1E)  // Modals

    // ============================================================
    // TEXT HIERARCHY (Critical for readability)
    // ============================================================
    // Light theme
    val TextPrimary = Color(0xFF1A1A1A)      // Headlines, amounts
    val TextSecondary = Color(0xFF6B6B6B)    // Labels, descriptions
    val TextTertiary = Color(0xFF9CA3AF)     // Hints, timestamps
    val TextDisabled = Color(0xFFD1D5DB)     // Disabled state

    // Dark theme
    val TextPrimaryDark = Color(0xFFF9FAFB)
    val TextSecondaryDark = Color(0xFF9CA3AF)
    val TextTertiaryDark = Color(0xFF6B7280)
    val TextDisabledDark = Color(0xFF4B5563)

    // ============================================================
    // BORDERS (Subtle, purposeful)
    // ============================================================
    // Light theme
    val Border = Color(0xFFE5E7EB)           // Default
    val BorderSubtle = Color(0xFFF3F4F6)     // Dividers
    val BorderStrong = Color(0xFFD1D5DB)     // Focus, hover

    // Dark theme (white-based for visibility)
    val BorderDark = Color(0xFF374151)
    val BorderSubtleDark = Color(0xFF1F2937)
    val BorderStrongDark = Color(0xFF4B5563)

    // Glass effect borders (for hero cards)
    val GlassBorder = Color(0x0FFFFFFF)      // 6% white
    val GlassBorderHover = Color(0x1AFFFFFF) // 10% white

    // ============================================================
    // HERO CARD (Dark balance display)
    // ============================================================
    val HeroGradientStart = Color(0xFF0A0A0A)
    val HeroGradientEnd = Color(0xFF141414)
    val HeroText = Color(0xFFFFFFFF)
    val HeroTextSecondary = Color(0xFF9CA3AF)
    val HeroButtonBg = Color(0x14FFFFFF)     // 8% white - glass effect
    val HeroButtonBgHover = Color(0x1FFFFFFF) // 12% white

    // ============================================================
    // NAVIGATION
    // ============================================================
    val NavBackground = Color(0xFF0A0A0A)
    val NavActive = Color(0xFFFFFFFF)
    val NavInactive = Color(0xFF6B7280)

    // ============================================================
    // PROGRESS INDICATORS
    // ============================================================
    val ProgressOnTrack = Color(0xFF10B981)  // Green: on track
    val ProgressBehind = Color(0xFFF59E0B)   // Amber: behind schedule
    val ProgressOverdue = Color(0xFFEF4444)  // Red: overdue
    val ProgressBackground = Color(0xFFE5E7EB)
    val ProgressBackgroundDark = Color(0xFF374151)

    // ============================================================
    // INTERACTIVE STATES
    // ============================================================
    val Hover = Color(0x0A000000)            // 4% black overlay
    val Pressed = Color(0x14000000)          // 8% black overlay
    val HoverDark = Color(0x0AFFFFFF)        // 4% white overlay
    val PressedDark = Color(0x14FFFFFF)      // 8% white overlay

    // ============================================================
    // ERROR / DESTRUCTIVE
    // ============================================================
    val Error = Color(0xFFEF4444)
    val ErrorMuted = Color(0xFFFEE2E2)
    val OnError = Color(0xFFFFFFFF)

    // ============================================================
    // CATEGORY COLORS (Charts - muted palette)
    // ============================================================
    val CategoryFood = Color(0xFFF87171)         // Rose
    val CategoryTransport = Color(0xFF38BDF8)    // Sky
    val CategoryShopping = Color(0xFFFBBF24)     // Amber
    val CategoryHealth = Color(0xFF34D399)       // Emerald
    val CategoryEntertainment = Color(0xFFA78BFA) // Violet
    val CategoryUtilities = Color(0xFF60A5FA)    // Blue
    val CategoryEducation = Color(0xFF818CF8)    // Indigo
    val CategoryInvestment = Color(0xFF22D3EE)   // Cyan
    val CategoryIncome = Color(0xFF4ADE80)       // Green
    val CategoryOther = Color(0xFF9CA3AF)        // Gray

    // Muted versions for icon backgrounds
    val CategoryFoodMuted = Color(0xFFFEE2E2)
    val CategoryTransportMuted = Color(0xFFE0F2FE)
    val CategoryShoppingMuted = Color(0xFFFEF3C7)
    val CategoryHealthMuted = Color(0xFFD1FAE5)
    val CategoryEntertainmentMuted = Color(0xFFEDE9FE)
    val CategoryUtilitiesMuted = Color(0xFFDBEAFE)
    val CategoryEducationMuted = Color(0xFFE0E7FF)
    val CategoryInvestmentMuted = Color(0xFFCFFAFE)
    val CategoryIncomeMuted = Color(0xFFDCFCE7)
    val CategoryOtherMuted = Color(0xFFF3F4F6)

    // ============================================================
    // LEGACY COMPATIBILITY (for existing components)
    // ============================================================
    val Primary = Accent
    val OnPrimary = OnAccent
    val Secondary = Color(0xFF6366F1)        // Indigo
    val Tertiary = Color(0xFF8B5CF6)         // Violet

    val OnBackground = TextPrimary
    val OnSurface = TextPrimary
    val OnSurfaceVariant = TextSecondary

    // Old card colors (mapped to new)
    val CardDark = SurfaceVariantDark
    val CardDarkVariant = SurfaceDark
    val OnCardDark = HeroText
    val OnCardDarkVariant = HeroTextSecondary

    // Old nav colors (mapped to new)
    val NavPill = NavBackground
    val NavPillActive = NavActive
    val NavPillInactive = NavInactive

    // Old border tokens
    val BorderSubtleLight = BorderSubtle
    val BorderDefaultLight = Border
    val BorderStrongLight = BorderStrong
}

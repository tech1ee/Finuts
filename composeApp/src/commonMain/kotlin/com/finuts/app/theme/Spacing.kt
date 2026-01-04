package com.finuts.app.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Finuts Spacing System — Calculated 8dp Grid
 *
 * Mathematical foundation:
 * - Base unit: 8dp (industry standard)
 * - Progression: 8 ÷ 4 → 8 ÷ 2 → 8 × 1 → 8 × 2 → 8 × 3 → 8 × 4 → 8 × 6
 * - All values are mathematically derived, not arbitrary
 *
 * Reference: docs/design-system/DESIGN_SYSTEM.md
 */
object FinutsSpacing {
    // ═══════════════════════════════════════════════════════════════
    // CALCULATED 8dp GRID
    // ═══════════════════════════════════════════════════════════════
    val none: Dp = 0.dp
    val xxs: Dp = 2.dp     // 8 ÷ 4 — Micro gaps (text-icon tight)
    val xs: Dp = 4.dp      // 8 ÷ 2 — Tight spacing
    val sm: Dp = 8.dp      // 8 × 1 — Base unit, component internal
    val md: Dp = 16.dp     // 8 × 2 — Default padding, cards
    val lg: Dp = 24.dp     // 8 × 3 — Hero to section gap
    val xl: Dp = 32.dp     // 8 × 4 — Section gaps (between major sections)
    val xxl: Dp = 48.dp    // 8 × 6 — Hero spacing
    val xxxl: Dp = 64.dp   // 8 × 8 — Extra large spacing

    // ═══════════════════════════════════════════════════════════════
    // SEMANTIC SPACING
    // ═══════════════════════════════════════════════════════════════
    val screenPadding: Dp = 16.dp          // Standard screen edge
    val cardPadding: Dp = 16.dp            // Internal card padding
    val cardPaddingCompact: Dp = 12.dp     // Compact card padding
    val heroCardPadding: Dp = 24.dp        // Hero card internal
    val sectionGap: Dp = 32.dp             // Between major sections (xl)
    val sectionHeaderGap: Dp = 16.dp       // Section header to content
    val carouselItemGap: Dp = 8.dp         // Between carousel cards
    val listItemDividerInset: Dp = 68.dp   // 16 + 40 + 12 (padding + icon + gap)

    // ═══════════════════════════════════════════════════════════════
    // ICON SIZES & GAPS
    // ═══════════════════════════════════════════════════════════════
    val iconSmall: Dp = 16.dp              // Chevrons, small indicators
    val iconMedium: Dp = 24.dp             // Standard icons
    val iconLarge: Dp = 40.dp              // List item icons
    val iconToTextGap: Dp = 12.dp          // Icon to text in list items
    val heroIconSize: Dp = 18.dp           // Hero action button icons

    // ═══════════════════════════════════════════════════════════════
    // TOUCH TARGETS (WCAG Accessibility)
    // ═══════════════════════════════════════════════════════════════
    val touchTarget: Dp = 48.dp            // WCAG minimum
    val touchTargetiOS: Dp = 44.dp         // iOS HIG minimum
    val buttonHeight: Dp = 48.dp           // Standard button height

    // ═══════════════════════════════════════════════════════════════
    // HERO BALANCE CARD
    // Calculated: ~164dp height, 24dp radius
    // ═══════════════════════════════════════════════════════════════
    val heroMinHeight: Dp = 164.dp         // Content + 48dp padding
    val heroHeight: Dp = 164.dp            // Alias for heroMinHeight
    val heroCornerRadius: Dp = 24.dp       // Premium feel
    val heroActionButtonHeight: Dp = 48.dp // Touch target
    val heroActionButtonRadius: Dp = 12.dp // Button corners
    val heroActionButtonMinWidth: Dp = 80.dp

    // ═══════════════════════════════════════════════════════════════
    // ACCOUNT CARD (Carousel)
    // Calculated: (361 - 8 - 8) ÷ 2 = 172.5 ≈ 172dp
    // Golden ratio height: 172 ÷ 1.34 ≈ 128dp
    // ═══════════════════════════════════════════════════════════════
    val accountCardWidth: Dp = 172.dp      // Shows 2 cards + peek
    val accountCardHeight: Dp = 128.dp     // Golden ratio proportion
    val accountCardRadius: Dp = 12.dp      // Standard card corners
    val accountAccentBarWidth: Dp = 4.dp   // Left accent bar
    val accountLogoSize: Dp = 32.dp        // Bank/account logo

    // ═══════════════════════════════════════════════════════════════
    // TRANSACTION LIST ITEM
    // Calculated: 64dp (optimized from 72dp)
    // ═══════════════════════════════════════════════════════════════
    val transactionItemHeight: Dp = 64.dp  // Compact, efficient
    val listItemHeight: Dp = 64.dp         // Alias for transactionItemHeight
    val transactionIconSize: Dp = 40.dp    // Category icon container
    val transactionIconRadius: Dp = 10.dp  // Icon container corners

    // ═══════════════════════════════════════════════════════════════
    // SETTINGS
    // ═══════════════════════════════════════════════════════════════
    val settingsRowHeight: Dp = 56.dp      // Touch-friendly rows
    val settingsHeaderHeight: Dp = 56.dp   // Card header with icon
    val settingsCardRadius: Dp = 16.dp     // Settings card corners
    val settingsGroupGap: Dp = 16.dp       // Between setting groups

    // ═══════════════════════════════════════════════════════════════
    // TOGGLE SWITCH (Custom)
    // 52×32dp track, 28dp thumb
    // ═══════════════════════════════════════════════════════════════
    val toggleTrackWidth: Dp = 52.dp
    val toggleTrackHeight: Dp = 32.dp
    val toggleThumbSize: Dp = 28.dp
    val toggleThumbMargin: Dp = 2.dp       // Thumb margin from edge

    // ═══════════════════════════════════════════════════════════════
    // BOTTOM NAVIGATION PILL
    // 4 tabs, 56dp height, 240dp width
    // ═══════════════════════════════════════════════════════════════
    val navPillWidth: Dp = 240.dp          // 4 × 48 + 48 padding
    val navPillHeight: Dp = 56.dp          // Compact pill
    val navPillRadius: Dp = 28.dp          // Half height (pill shape)
    val navPillBottom: Dp = 16.dp          // + safe area inset
    val navItemSize: Dp = 48.dp            // Touch target
    val navItemGap: Dp = 12.dp             // Between items
    val navIconSize: Dp = 24.dp            // Nav icons
    val bottomNavHeight: Dp = 80.dp        // 56dp pill + 16dp margin + 8dp buffer

    // ═══════════════════════════════════════════════════════════════
    // SECTION HEADER
    // ═══════════════════════════════════════════════════════════════
    val sectionHeaderHeight: Dp = 24.dp    // Text height
    val sectionTopMargin: Dp = 32.dp       // From previous section
    val sectionBottomMargin: Dp = 16.dp    // To content

    // ═══════════════════════════════════════════════════════════════
    // CHIPS & BADGES
    // ═══════════════════════════════════════════════════════════════
    val chipHeight: Dp = 32.dp             // Standard chip
    val chipRadius: Dp = 16.dp             // Pill shape
    val badgeSize: Dp = 20.dp              // Small badges

    // ═══════════════════════════════════════════════════════════════
    // PROGRESS & CHARTS
    // ═══════════════════════════════════════════════════════════════
    val progressHeight: Dp = 8.dp          // Linear progress
    val progressRadius: Dp = 4.dp          // Rounded ends
    val chartDonutSize: Dp = 200.dp        // Donut chart
    val chartStrokeWidth: Dp = 24.dp       // Donut stroke
    val budgetRingSize: Dp = 160.dp        // Budget ring
    val budgetRingStroke: Dp = 12.dp       // Ring stroke

    // ═══════════════════════════════════════════════════════════════
    // DASHBOARD CARDS (v3.0 Architecture)
    // ═══════════════════════════════════════════════════════════════
    val progressCardHeight: Dp = 80.dp     // SpendingProgressCard
    val categoryItemHeight: Dp = 56.dp     // Single CategorySpendingItem
    val categoryListHeight: Dp = 168.dp    // 3 items × 56dp
    val healthCardHeight: Dp = 72.dp       // FinancialHealthCard
    val summaryCardHeight: Dp = 140.dp     // TransactionSummaryCard

    // ═══════════════════════════════════════════════════════════════
    // EMPTY STATES
    // ═══════════════════════════════════════════════════════════════
    val emptyStateIconSize: Dp = 64.dp     // Large centered icon
    val emptyStateIconToTitle: Dp = 16.dp  // Icon to title gap
    val emptyStateTitleToDesc: Dp = 8.dp   // Title to description gap
    val emptyStateDescToAction: Dp = 24.dp // Description to button gap

    // ═══════════════════════════════════════════════════════════════
    // ONBOARDING
    // ═══════════════════════════════════════════════════════════════
    val onboardingHeroIconSize: Dp = 120.dp   // Welcome, Completion hero icons
    val onboardingIconLarge: Dp = 100.dp      // FirstAccount icon
}

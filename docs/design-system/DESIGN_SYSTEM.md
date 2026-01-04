# Finuts UI Design System

**Version:** 2.0 (Harmonious Minimalism)
**Last Updated:** December 30, 2025
**Status:** Active

---

## Table of Contents

1. [Design Philosophy](#1-design-philosophy)
2. [Screen Dimensions](#2-screen-dimensions)
3. [Spacing System](#3-spacing-system)
4. [Typography Scale](#4-typography-scale)
5. [Color Palette](#5-color-palette)
6. [Component Specifications](#6-component-specifications)
7. [Screen Layouts](#7-screen-layouts)
8. [Animation & Motion](#8-animation--motion)
9. [Interaction States](#9-interaction-states)
10. [Responsive Behavior](#10-responsive-behavior)
11. [Accessibility](#11-accessibility)
12. [Dark Mode](#12-dark-mode)
13. [Empty & Loading States](#13-empty--loading-states)

---

## 1. Design Philosophy

### Core Principle: Harmonious Minimalism

> "Perfection is achieved not when there is nothing more to add, but when there is nothing left to take away." — Antoine de Saint-Exupéry

### Mathematical Foundation

All measurements derive from:
- **Base unit:** 8dp (consistent with 8-point grid)
- **Golden ratio:** 1.618 (for visual harmony)
- **Type scale:** 1.25 (Major Third — balanced hierarchy)

### Visual Harmony Rules

| Rule | Value | Application |
|------|-------|-------------|
| Spacing Rhythm | 8 → 16 → 24 → 32 → 48 | Strict progression |
| Size Ratios | 1.5x–2x | Between hierarchy levels |
| White Space | 62% : 38% | Content : breathing room |
| Touch Targets | 48dp minimum | Accessibility |
| Corner Radius | 12dp cards, 8dp buttons | Consistency |

---

## 2. Screen Dimensions

### Target Device: iPhone 15 / Pixel 7

| Property | Value | Notes |
|----------|-------|-------|
| Screen width | 393dp | Design baseline |
| Screen height | 852dp | Design baseline |
| Status bar | 59dp (iOS) / 24dp (Android) | Top inset |
| Bottom safe area | 34dp (iOS) / 0dp (Android) | Home indicator |

### Content Area Calculation

```
Screen width:        393dp
Horizontal margins:  -32dp (16dp × 2)
─────────────────────────────
Content width:       361dp (usable for cards)

Screen height:       852dp
Status bar:          -59dp
Bottom nav:          -64dp
Bottom safe:         -16dp
Top padding:         -16dp
─────────────────────────────
Scrollable height:   697dp
```

---

## 3. Spacing System

### Base Grid: 8dp

| Token | Value | Use Case | Formula |
|-------|-------|----------|---------|
| `xxs` | 2dp | Micro gaps (text-icon) | 8 ÷ 4 |
| `xs` | 4dp | Tight spacing | 8 ÷ 2 |
| `sm` | 8dp | Base unit | 8 × 1 |
| `md` | 16dp | Default padding | 8 × 2 |
| `lg` | 24dp | Section gaps | 8 × 3 |
| `xl` | 32dp | Major sections | 8 × 4 |
| `xxl` | 48dp | Hero spacing | 8 × 6 |

### Semantic Spacing

| Context | Value | Rationale |
|---------|-------|-----------|
| Screen edge margin | 16dp | Standard mobile edge |
| Card internal padding | 16dp | Consistent with edge |
| Between list items | 0dp | Divider provides separation |
| Between sections | 32dp | 2× card padding = clear break |
| Hero to content | 24dp | 1.5× card padding |
| Icon to text | 8dp | Base unit |
| Text line gap | 4dp | Half base unit |

### Kotlin Implementation

```kotlin
object FinutsSpacing {
    val none = 0.dp
    val xxs = 2.dp    // Micro adjustments only
    val xs = 4.dp     // Icon-text gaps
    val sm = 8.dp     // Component internal
    val md = 16.dp    // Card padding, list item
    val lg = 24.dp    // Section gaps
    val xl = 32.dp    // Major sections
    val xxl = 48.dp   // Hero spacing

    // Semantic aliases
    val screenPadding = md           // 16dp
    val cardPadding = md             // 16dp
    val sectionGap = xl              // 32dp
    val heroToContent = lg           // 24dp
    val iconToText = sm              // 8dp
}
```

---

## 4. Typography Scale

### Scale Ratio: 1.25 (Major Third)

| Level | Size | Line Height | Weight | Use |
|-------|------|-------------|--------|-----|
| Display L | 48sp | 56sp (1.17×) | Bold | Hero balance |
| Display M | 36sp | 44sp (1.22×) | Bold | Page totals |
| Headline L | 28sp | 36sp (1.29×) | SemiBold | Page titles |
| Headline M | 22sp | 28sp (1.27×) | SemiBold | Section titles |
| Title M | 16sp | 24sp (1.5×) | Medium | List item primary |
| Body L | 16sp | 24sp (1.5×) | Regular | Content text |
| Body M | 14sp | 20sp (1.43×) | Regular | Secondary text |
| Body S | 12sp | 16sp (1.33×) | Regular | Tertiary text |
| Label L | 14sp | 20sp | Medium | Buttons |
| Label S | 11sp | 14sp | Medium | Badges |

### Hierarchy Visualization

```
48sp ████████████████████████  Hero Balance (₸1,780,000)
     ↓ 1.33×
36sp ██████████████████        Page Total
     ↓ 1.29×
28sp ██████████████            Page Title (Settings)
     ↓ 1.27×
22sp ███████████               Section Header (My Accounts)
     ↓ 1.38×
16sp ████████                  List Item Title
     ↓ 1.14×
14sp ███████                   Body/Description
     ↓ 1.17×
12sp ██████                    Tertiary (timestamps)
```

### Money Typography

All monetary values use tabular figures for alignment:

```kotlin
object FinutsMoneyTypography {
    val displayLarge = TextStyle(
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum"  // Tabular figures
    )

    val title = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum"
    )
}
```

---

## 5. Color Palette

### Brand Colors

| Color | Hex | HSL | Use |
|-------|-----|-----|-----|
| Accent (Emerald) | #10B981 | 160° 83% 39% | Brand, CTAs |
| Income | #10B981 | 160° 83% 39% | Positive amounts |
| Expense | #EF4444 | 0° 84% 60% | Negative amounts |
| Transfer | #6366F1 | 239° 84% 67% | Neutral transfers |
| Warning | #F59E0B | 38° 92% 50% | Alerts |

### Background Layers

| Layer | Light | Dark | Use |
|-------|-------|------|-----|
| Background | #FAFAFA | #0A0A0A | Base layer |
| Surface | #FFFFFF | #141414 | Cards |
| SurfaceVariant | #F5F5F7 | #1C1C1E | Subtle differentiation |

### Text Hierarchy

| Type | Light | Dark | Contrast |
|------|-------|------|----------|
| Primary | #1A1A1A | #F9FAFB | 16.1:1 ✓ |
| Secondary | #6B6B6B | #9CA3AF | 5.74:1 ✓ |
| Tertiary | #9CA3AF | #6B7280 | 3.02:1 (large) |

### Borders

| Type | Light | Dark |
|------|-------|------|
| Default | #E5E7EB | #374151 |
| Subtle | #F3F4F6 | #1F2937 |
| Strong | #D1D5DB | #4B5563 |

---

## 6. Component Specifications

### 6.1 Hero Balance Card

```
┌─────────────────────────────────────────────────────────────┐
│ ← 24dp padding                                     24dp → │
│                                                             │
│   Total Balance                    ← 12sp, tertiary        │
│   ↓ 4dp                                                     │
│   ₸1,780,000.00                    ← 48sp, Bold, White     │
│                                                             │
│   ↓ 24dp                                                    │
│                                                             │
│   ┌─────────┐ 8dp ┌─────────┐ 8dp ┌─────────┐              │
│   │  + Add  │     │ ↑ Send  │     │↓Receive │              │
│   └─────────┘     └─────────┘     └─────────┘              │
│   ← 48dp height, 12dp radius                               │
│                                                             │
│ ← 24dp padding                                     24dp → │
└─────────────────────────────────────────────────────────────┘
```

**Specifications:**
- Width: 361dp (screen - 32dp margins)
- Height: ~164dp (content-based)
- Corner radius: 24dp
- Background: Gradient #0A0A0A → #141414
- Border: 1dp rgba(255,255,255,0.06)
- Button height: 48dp
- Button min width: 80dp
- Button corner radius: 12dp
- Button background: rgba(255,255,255,0.08)

### 6.2 Account Card (Carousel)

```
┌─────────────────────────────────────┐
│█│                                   │
│█│  ┌────┐  Cash            ← 16sp  │
│█│  │ 💵 │  CASH            ← 12sp  │
│█│  └────┘                          │
│█│                                   │
│█│  ₸35,000.00              ← 20sp  │
│█│  Available               ← 12sp  │
│█│                                   │
└─────────────────────────────────────┘
← 4dp accent bar
```

**Specifications:**
- Width: 172dp
- Height: 128dp
- Aspect ratio: 1.34:1
- Corner radius: 12dp
- Accent bar: 4dp width
- Internal padding: 16dp
- Logo: 32×32dp, 8dp radius
- Gap between header and balance: 12dp

### 6.3 Transaction List Item

```
┌───────────────────────────────────────────────────────────┐
│ 16dp │ ┌────┐ │ 12dp │ Magnum           │ -₸12,500.00 │ 16dp │
│      │ │ 🛒 │ │      │ Shopping • 14:32 │             │      │
│      │ └────┘ │      │                  │             │      │
└───────────────────────────────────────────────────────────┘
                         64dp height
```

**Specifications:**
- Height: 64dp
- Horizontal padding: 16dp
- Icon container: 40×40dp, 10dp radius
- Icon to text gap: 12dp
- Divider: 1dp, starts at 68dp from left

### 6.4 Section Header

```
My Accounts                                    See All →
├─── 18sp SemiBold ───┤                   ├── 14sp accent ──┤
```

**Specifications:**
- Title: 18sp SemiBold, primary color
- Action: 14sp Medium, accent color
- Top margin: 32dp (from previous section)
- Bottom margin: 16dp (to content)

### 6.5 Bottom Navigation Pill

```
┌─────────────────────────────────────────────────────────────┐
│        🏠        💳        📊        ⚙️                   │
└─────────────────────────────────────────────────────────────┘
```

**Specifications:**
- Width: 240dp
- Height: 56dp
- Corner radius: 28dp (pill)
- Bottom margin: 16dp + safe area
- Background: #0A0A0A
- Border: 1dp rgba(255,255,255,0.06)
- Icon size: 24dp
- Touch target: 48×48dp

### 6.6 Settings Group Card

**Specifications:**
- Corner radius: 16dp
- Background: Surface
- Border: 1dp Border color
- Header height: 56dp
- Header icon: 24dp, accent color
- Row height: 56dp
- Divider: 1dp BorderSubtle

### 6.7 Custom Toggle (FinutsSwitch)

**Specifications:**
- Track: 52×32dp
- Thumb: 28dp diameter
- Corner radius: 16dp (half height)
- Thumb margin: 2dp from edge
- OFF track: #E5E7EB
- ON track: #10B981 (Accent)
- Animation: 150ms ease-out

---

## 7. Screen Layouts

### 7.1 Dashboard Screen

```
┌─────────────────────────────────────────┐
│ Status Bar (59dp)                       │
├─────────────────────────────────────────┤
│ 16dp                                    │
├─────────────────────────────────────────┤
│ HERO BALANCE CARD (~164dp)              │
├─────────────────────────────────────────┤
│ 24dp gap                                │
├─────────────────────────────────────────┤
│ My Accounts                    See All  │
│ 16dp gap                                │
├─────────────────────────────────────────┤
│ Account Cards Carousel (128dp)          │
├─────────────────────────────────────────┤
│ 32dp gap                                │
├─────────────────────────────────────────┤
│ Recent Transactions            See All  │
│ 16dp gap                                │
├─────────────────────────────────────────┤
│ Transaction 1 (64dp)                    │
│ Transaction 2 (64dp)                    │
│ Transaction 3 (64dp)                    │
│ ...                                     │
├─────────────────────────────────────────┤
│ 80dp bottom padding                     │
├─────────────────────────────────────────┤
│ PILL NAV (56dp + safe area)             │
└─────────────────────────────────────────┘
```

### 7.2 Accounts Screen

```
┌─────────────────────────────────────────┐
│ Status Bar                              │
├─────────────────────────────────────────┤
│ 24dp                                    │
├─────────────────────────────────────────┤
│ Accounts (28sp)                         │
│ 8dp                                     │
│ Total Balance (14sp tertiary)           │
│ ₸1,780,000.00 (36sp Bold)              │
├─────────────────────────────────────────┤
│ 32dp                                    │
├─────────────────────────────────────────┤
│ Active (12sp tertiary)                  │
│ 12dp                                    │
├─────────────────────────────────────────┤
│ Account List Items (64dp each)          │
├─────────────────────────────────────────┤
│ 24dp                                    │
│ + Add Account (text button)             │
├─────────────────────────────────────────┤
│ PILL NAV                                │
└─────────────────────────────────────────┘
```

### 7.3 Settings Screen

```
┌─────────────────────────────────────────┐
│ Status Bar                              │
├─────────────────────────────────────────┤
│ 24dp                                    │
├─────────────────────────────────────────┤
│ Settings (28sp)                         │
│ 24dp                                    │
├─────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐ │
│ │ 🎨 Appearance                       │ │
│ ├─────────────────────────────────────┤ │
│ │ Theme                      System → │ │
│ │ Language                  Русский → │ │
│ └─────────────────────────────────────┘ │
│ 16dp                                    │
│ ┌─────────────────────────────────────┐ │
│ │ 💰 Preferences                      │ │
│ ├─────────────────────────────────────┤ │
│ │ Currency                      KZT → │ │
│ └─────────────────────────────────────┘ │
│ 16dp                                    │
│ ┌─────────────────────────────────────┐ │
│ │ 🔒 Security                         │ │
│ ├─────────────────────────────────────┤ │
│ │ Notifications                    🔵 │ │
│ │ Biometric Lock                   ⚪ │ │
│ └─────────────────────────────────────┘ │
│ 32dp                                    │
│ Version 1.0.0-alpha (centered, 12sp)    │
├─────────────────────────────────────────┤
│ PILL NAV                                │
└─────────────────────────────────────────┘
```

---

## 8. Animation & Motion

### Motion Principles

| Duration | Value | Use Case |
|----------|-------|----------|
| Micro | 100ms | Press feedback |
| Standard | 150ms | UI transitions |
| Emphasis | 200ms | Page transitions |
| Maximum | 300ms | Never exceed |

### Easing

All animations use `ease-out` (FastOutSlowInEasing) for natural deceleration.

### Component Animations

| Component | Trigger | Animation | Duration |
|-----------|---------|-----------|----------|
| Card | Press | scale(0.98) | 100ms |
| Button | Press | scale(0.96) | 100ms |
| Toggle | Change | translateX + color | 150ms |
| List item | Press | alpha(0.7) | 100ms |
| Page | Navigate | fadeIn + slideUp | 200ms |
| Nav item | Select | scale(1.1) | 150ms |

### Implementation

```kotlin
// Standard press effect
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.98f else 1.0f,
    animationSpec = tween(
        durationMillis = 100,
        easing = FastOutSlowInEasing
    )
)

Modifier.graphicsLayer {
    scaleX = scale
    scaleY = scale
}
```

---

## 9. Interaction States

### State Matrix

| Component | Default | Hover | Pressed | Disabled |
|-----------|---------|-------|---------|----------|
| Card | Surface | +2% overlay | scale(0.98) | 50% opacity |
| Button | Accent | AccentHover | scale(0.96) | 40% opacity |
| List item | Transparent | 4% overlay | 8% overlay | 50% opacity |
| Toggle ON | Accent | — | — | 40% opacity |
| Toggle OFF | Border | — | — | 20% opacity |

### Overlay Colors

```kotlin
// Light theme
val HoverOverlay = Color(0x0A000000)   // 4% black
val PressedOverlay = Color(0x14000000) // 8% black

// Dark theme
val HoverOverlayDark = Color(0x0AFFFFFF)   // 4% white
val PressedOverlayDark = Color(0x14FFFFFF) // 8% white
```

---

## 10. Responsive Behavior

### Breakpoints

| Device | Width | Adaptation |
|--------|-------|------------|
| iPhone SE | 375dp | 1.5 cards visible |
| iPhone 15 | 393dp | 2 cards visible |
| iPhone 15 Plus | 430dp | 2.2 cards visible |
| iPad Mini | 744dp | 4 cards visible |

### Scaling Rules

| Component | Phone | Tablet |
|-----------|-------|--------|
| Hero padding | 24dp | 32dp |
| Card width | 172dp | 200dp |
| Screen margins | 16dp | 24dp |
| Section gap | 32dp | 48dp |
| Nav width | 240dp | 280dp |

---

## 11. Accessibility

### Touch Targets

All interactive elements must be at least 48dp.

| Component | Minimum | Actual |
|-----------|---------|--------|
| Hero button | 48dp | 48×80dp ✓ |
| Account card | 48dp | 172×128dp ✓ |
| Transaction | 48dp | 361×64dp ✓ |
| Settings row | 48dp | 361×56dp ✓ |
| Toggle | 48dp | 52×32dp ⚠️ |
| Nav item | 48dp | 48×48dp ✓ |

### Color Contrast (WCAG AA)

| Text | Background | Contrast | Required | Status |
|------|------------|----------|----------|--------|
| Primary | Surface | 16.1:1 | 4.5:1 | ✓ |
| Secondary | Surface | 5.74:1 | 4.5:1 | ✓ |
| Tertiary | Surface | 3.02:1 | 3:1 | ✓ (large) |
| White | Hero | 19.4:1 | 4.5:1 | ✓ |

### Focus Indicators

```kotlin
Modifier.focusable()
    .border(
        width = 2.dp,
        color = if (isFocused) FinutsColors.Accent else Color.Transparent,
        shape = shape
    )
```

---

## 12. Dark Mode

### Color Mapping

| Light | Dark | Notes |
|-------|------|-------|
| #FAFAFA | #0A0A0A | Background (not pure black) |
| #FFFFFF | #141414 | Surface |
| #1A1A1A | #F9FAFB | Text Primary |
| #6B6B6B | #9CA3AF | Text Secondary |
| #E5E7EB | #374151 | Border |
| #10B981 | #10B981 | Accent (unchanged) |
| #10B981 | #34D399 | Income (desaturated) |
| #EF4444 | #F87171 | Expense (desaturated) |

### Rules

1. No pure black (#000000) — minimum #0A0A0A
2. Desaturate semantic colors by 10-20%
3. Text should be #F9FAFB, not pure white
4. Maintain same visual weight relationships
5. Test on OLED screens for smearing

---

## 13. Empty & Loading States

### Empty States

| Screen | Icon | Title | Description |
|--------|------|-------|-------------|
| Dashboard (no accounts) | 💳 | Welcome to Finuts! | Add your first account |
| Dashboard (no transactions) | 📝 | No transactions yet | Add expense or income |
| Accounts | 🏦 | No accounts | Add bank, card, or cash |
| Filtered empty | 🔍 | Nothing found | Change your filters |

### Empty State Layout

```
Icon container: 64dp circle, AccentMuted background
Title to icon gap: 16dp
Description to title gap: 8dp
Button to description gap: 24dp
```

### Loading Skeletons

| Component | Animation |
|-----------|-----------|
| Hero card | Full shimmer, 1.5s |
| Account card | Shape shimmer, 1.5s |
| Transaction | Icon + 2 lines, 1.5s |
| Settings row | Text shimmer, 1.5s |

### Error States

| Error | Display | Recovery |
|-------|---------|----------|
| Network | Toast + retry | Pull-to-refresh |
| Load failure | Inline message | Retry button |
| Save failure | Snackbar | Manual retry |

---

## Verification Checklist

### Before Implementation

- [ ] All spacing: multiples of 8dp
- [ ] All touch targets: ≥ 48dp
- [ ] All text contrast: ≥ 4.5:1
- [ ] All animations: ≤ 200ms
- [ ] Corner radii: consistent
- [ ] Dark mode colors: defined
- [ ] Empty states: defined
- [ ] Loading states: defined

### After Implementation

- [ ] Visual rhythm: consistent
- [ ] Press feedback: immediate
- [ ] Navigation: intuitive (4 tabs)
- [ ] Text: readable at all sizes
- [ ] Dark mode: polished
- [ ] Scrolling: smooth (60fps)
- [ ] Empty states: helpful

---

## References

- [Material Design 3](https://m3.material.io)
- [Apple HIG](https://developer.apple.com/design/human-interface-guidelines)
- [8-Point Grid](https://spec.fm/specifics/8-pt-grid)
- [WCAG 2.1 AA](https://www.w3.org/WAI/WCAG21/quickref/)

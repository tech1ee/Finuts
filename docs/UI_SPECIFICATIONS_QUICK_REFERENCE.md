# Finuts UI Specifications - Quick Reference

**For:** Kotlin Multiplatform Compose Implementation
**Base Standard:** Material Design 3 (Android) + iOS HIG
**Updated:** 2025-12-30

---

## SPACING GRID (8dp baseline)

```
xs = 4dp    (minimal gaps)
sm = 8dp    (base unit)
md = 16dp   (standard padding)
lg = 24dp   (section gaps)
xl = 32dp   (extra spacing)
```

**Screen margins:** 16dp (both platforms)
**Section gaps:** 24dp (between dashboard sections)

---

## TYPOGRAPHY

### Display (Heroes)
- **Display S:** 36dp font, 44dp line height → Balance amount in hero card

### Headlines (Sections)
- **Headline L:** 32dp → Rare, full-screen titles
- **Headline S:** 24dp → Card titles
- **Title L:** 22dp → Page titles
- **Title M:** 16dp → Subheadings

### Body (Content)
- **Body L:** 16dp → Primary text, descriptions
- **Body M:** 14dp → Default body copy
- **Body S:** 12dp → Secondary text, labels

---

## COMPONENT HEIGHTS

| Component | Height | Notes |
|-----------|--------|-------|
| Status Bar | 25dp (Android) / 44pt (iOS) | Don't use |
| Top App Bar | 56dp/pt | Standard header |
| Hero Card | 164-180dp/pt | Balance display |
| List Item (1-line) | 56dp/pt | Single entry |
| List Item (2-line) | 72dp/pt | Transaction default |
| Bottom Nav | 56dp (Android) / 49pt + 34pt safe (iOS) | Navigation |
| Button (standard) | 40dp/pt | Primary actions |
| Toggle Switch | 31pt (iOS) / 32dp (Android) | Settings |

---

## CORNER RADIUS (Shape Scale)

```
4dp   = extraSmall  (minimal)
8dp   = small       (buttons)
12dp  = medium      (cards - DEFAULT)
16dp  = large       (hero cards, FAB)
24dp  = extraLarge  (dialogs, modals)
```

---

## HERO CARD (Dashboard)

```
Width:        328dp (Android) / 340pt (iOS)
Height:       164dp/pt
Radius:       16dp
Padding:      16dp all sides
Elevation:    2dp
Gap below:    24dp

Content:
├─ Label:     12dp (Body S) "Available Balance"
├─ Amount:    36dp (Display S) "$5,234.50"
└─ Subtext:   12dp (Body S) "Updated 2 hours ago"
```

---

## BOTTOM NAVIGATION

### Android
- Height: 56dp (fixed, no safe area below)
- Icons: 24dp × 24dp
- Labels: 12sp (optional, stacked)
- Spacing: Equal distribution

### iOS
- Height: 49pt (no padding)
- Safe Area Below: 34pt (home indicator)
- Icons: 24pt (SF Symbols)
- Labels: 10pt
- Safe Area Above: 0pt (extends to edge)

---

## LIST ITEMS

### Single-Line (56dp height)
```
[Icon 24dp] [16dp] Text [flex] [Value]
```
- Left padding: 16dp
- Right padding: 16dp
- Icon-to-text: 16dp gap

### Two-Line (72dp height)
```
[Icon 24dp] [Title (16dp Body M)] [Value]
            [Subtitle (12dp Body S)]
```
- Vertical padding: 12dp
- Text alignment: centered vertically

---

## SETTINGS ROWS

```
Height:     56dp/pt (minimum)
Padding:    16dp horizontal
            8dp vertical (min)

Toggle Row:
├─ Label:   16dp from left
├─ Switch:  51pt × 31pt (iOS) / 52dp × 32dp (Android)
└─ Right:   16dp from right, vertically centered
```

**Section Structure:**
```
Group Header (44dp) - Title M text
├─ Row 1 (56dp)
├─ Divider (1dp, 72dp left indent)
├─ Row 2 (56dp)
└─ Divider (1dp)

Between groups: 24dp vertical gap
Group padding: 16dp sides
```

---

## TOUCH TARGETS

**Minimum sizes:**
- Android: 48dp × 48dp
- iOS: 44pt × 44pt
- Buttons: At least these dimensions (larger preferred)
- Icons in nav: 24dp/pt (inside 48-56dp target area)

---

## CAROUSEL / SCROLLABLE CARDS

```
Card Gap:     12-16dp between cards
Page Gutter:  16-24dp at edges
Peek Effect:  Show ~15% of next card
Card Width:   Full-width minus gutters
Card Height:  180-200dp (16:9 or 2:1 aspect)
```

---

## DIALOG / MODAL PATTERNS

```
Width:           320dp (max 90% screen width)
Padding:         24dp top/bottom, 24dp sides
Corner Radius:   24dp (extraLarge)
Title Size:      Title L (22dp)
Content Padding: 24dp
Button Height:   40dp
Button Padding:  8dp sides, 12dp vertical
```

---

## TOP APP BAR

```
Compact Height:   56dp/pt
Large Height:     96dp (with large title)
Padding:          16dp from edges
Title Size:       Title L (22dp)
Icon Size:        24dp
Icon Spacing:     8dp between icons
```

---

## COLOR SEMANTICS (Recommended)

```
Primary:        Brand color (CTA, highlights)
Secondary:      Muted primary
Tertiary:       Accent (rarely used)
Success:        Green (positive balance, gains)
Error:          Red (negative balance, losses)
Warning:        Amber (alerts, pending)
Info:           Blue (information)
Surface:        Background
OnSurface:      Text (high contrast)
Secondary Text: @60% opacity
Disabled:       @38% opacity
```

---

## SAFE AREAS

### Android
- Left/Right: 0dp (system handles)
- Top: Below status bar (handled by layouts)
- Bottom: 0dp (tabs/nav not included in safe area)

### iOS
- **Top without nav:** 59pt (includes status bar)
- **Top with nav bar:** 97-149pt (depends on nav type)
- **Bottom without tabs:** 34pt (home indicator)
- **Bottom with tabs:** 0pt (tab bar handles spacing)
- **Total bottom space needed:** 49pt (tab) + 34pt (safe) = 83pt

---

## COMPOSE MULTIPLATFORM CODE PATTERNS

### Spacing
```kotlin
// In your theme
object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

// Usage
Box(modifier = Modifier.padding(AppSpacing.md))
```

### Typography
```kotlin
// Material 3 defaults (use these)
Text(
    "Label",
    style = MaterialTheme.typography.bodySmall  // 12dp
)
Text(
    "$5,234",
    style = MaterialTheme.typography.displaySmall  // 36dp
)
```

### Card
```kotlin
Card(
    shape = MaterialTheme.shapes.medium,  // 12dp
    modifier = Modifier
        .padding(AppSpacing.md)
        .height(180.dp)
) {
    // Content
}
```

### List Item
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .height(72.dp)
        .padding(horizontal = AppSpacing.md),
    verticalAlignment = Alignment.CenterVertically
) {
    // Icon (24dp)
    // Text (flex)
    // Value
}
```

---

## IMPLEMENTATION PRIORITIES

### Phase 1 (MVP)
- [x] Hero card (328×180dp, 16dp radius)
- [x] Bottom navigation (56dp Android, 49pt iOS)
- [x] List items (72dp two-line)
- [x] Settings rows (56dp with toggles)
- [x] Typography hierarchy (5 main sizes)

### Phase 2 (Polish)
- [ ] Shadows & elevation
- [ ] Smooth animations
- [ ] Responsive typography
- [ ] Accessible contrast ratios

### Phase 3 (Refinement)
- [ ] Custom branding (colors, custom shapes)
- [ ] Adaptive layouts (tablet support)
- [ ] Dark mode (if needed)
- [ ] Motion design

---

## VALIDATION CHECKLIST

Before shipping a screen:

- [ ] All text legible at font sizes specified
- [ ] Touch targets minimum 44pt (iOS) / 48dp (Android)
- [ ] 16dp margins on screen edges
- [ ] 24dp gaps between major sections
- [ ] Corner radii match spec (12dp cards, 16dp hero)
- [ ] Colors have sufficient contrast (WCAG AA)
- [ ] Spacing consistent (multiples of 8dp)
- [ ] Safe areas respected (especially iOS notch)
- [ ] List items correct height (56dp or 72dp)
- [ ] Bottom nav height correct (56dp / 49pt + safe)

---

## RESOURCES

- Full specs: `docs/research/2025-12-30-premium-ui-specifications.md`
- Material Design 3: https://m3.material.io/
- iOS HIG: https://developer.apple.com/design/
- Compose Multiplatform: https://www.jetbrains.com/help/kotlin-multiplatform-dev/

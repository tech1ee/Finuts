# Finuts UI Specifications - Documentation Index

**Last Updated:** 2025-12-30
**Research Status:** Complete
**Implementation Status:** Ready for development

---

## Documents Overview

### 1. QUICK START (Read First)
**File:** `UI_SPECIFICATIONS_QUICK_REFERENCE.md`
- 1-page lookup guide
- All critical measurements in tables
- Copy-paste code patterns
- Validation checklist
- **Time to read:** 5-10 minutes

**When to use:** Before implementing any screen component

---

### 2. COMPREHENSIVE SPECIFICATIONS
**File:** `docs/research/2025-12-30-premium-ui-specifications.md`
- 13 sections covering every aspect
- Material Design 3 typography scale (complete)
- iOS HIG specifications (complete)
- Component measurements (all documented)
- 25+ sources cited with credibility scores
- **Length:** ~6000 words
- **Time to read:** 30-45 minutes

**When to use:** Reference for detailed info, design decisions, or validation

**Contains:**
- Material Design 3 specs (colors, spacing, typography, shapes)
- iOS HIG specs (safe areas, touch targets, typography)
- Dashboard & hero card specs
- Bottom navigation specifications
- Settings screen patterns
- Carousel & list patterns
- Kotlin Multiplatform code patterns
- Implementation checklist
- Source citations and credibility assessment

---

### 3. IMPLEMENTATION CODE
**File:** `DESIGN_TOKENS_KMP_IMPLEMENTATION.md`
- Ready-to-implement Kotlin code
- 8 complete design token objects
- Material Design 3 type scale (copy-paste)
- Component dimensions (all predefined)
- Usage examples for common components
- File structure recommendations
- Build configuration
- **Time to implement:** 30-60 minutes

**When to use:** Creating theme and design system in code

**Contains:**
- `Spacing.kt` - 8dp grid tokens (copy-paste ready)
- `Typography.kt` - Material Design 3 type scale
- `Shape.kt` - Corner radius tokens
- `Dimensions.kt` - Component sizes
- `Elevation.kt` - Shadow/depth tokens
- `Color.kt` - Brand colors
- `FinutsTheme.kt` - Main theme composable
- Example component implementations
- Adaptive layout patterns
- Checklist for implementation

---

### 4. RESEARCH SUMMARY & FINDINGS
**File:** `RESEARCH_SUMMARY.md`
- High-level findings from research
- Key measurements table
- What's documented vs. proprietary
- Source reliability tiers
- Implementation priority (phases)
- Validation checklist
- Next steps roadmap
- **Time to read:** 10-15 minutes

**When to use:** Understanding research scope and recommendations

---

## QUICK LOOKUP BY TOPIC

### Spacing & Grid
- Quick ref: `UI_SPECIFICATIONS_QUICK_REFERENCE.md` → "SPACING GRID"
- Full specs: `docs/research/2025-12-30-premium-ui-specifications.md` → Section 1.2
- Code: `DESIGN_TOKENS_KMP_IMPLEMENTATION.md` → Section 1 (Spacing.kt)

### Typography
- Quick ref: `UI_SPECIFICATIONS_QUICK_REFERENCE.md` → "TYPOGRAPHY"
- Full specs: `docs/research/2025-12-30-premium-ui-specifications.md` → Section 1.1
- Code: `DESIGN_TOKENS_KMP_IMPLEMENTATION.md` → Section 2 (Typography.kt)

### Bottom Navigation
- Quick ref: `UI_SPECIFICATIONS_QUICK_REFERENCE.md` → "BOTTOM NAVIGATION"
- Full specs: `docs/research/2025-12-30-premium-ui-specifications.md` → Section 4
- Code: Examples in `DESIGN_TOKENS_KMP_IMPLEMENTATION.md` → Section 8

### Hero Card / Dashboard
- Quick ref: `UI_SPECIFICATIONS_QUICK_REFERENCE.md` → "HERO CARD"
- Full specs: `docs/research/2025-12-30-premium-ui-specifications.md` → Section 3
- Code: `DESIGN_TOKENS_KMP_IMPLEMENTATION.md` → Section 8 (BalanceCard example)

### List Items / Transactions
- Quick ref: `UI_SPECIFICATIONS_QUICK_REFERENCE.md` → "LIST ITEMS"
- Full specs: `docs/research/2025-12-30-premium-ui-specifications.md` → Section 3.2, 8.2
- Code: `DESIGN_TOKENS_KMP_IMPLEMENTATION.md` → Section 8 (TransactionItem example)

### Settings Screens
- Quick ref: `UI_SPECIFICATIONS_QUICK_REFERENCE.md` → "SETTINGS ROWS"
- Full specs: `docs/research/2025-12-30-premium-ui-specifications.md` → Section 5
- Code: `DESIGN_TOKENS_KMP_IMPLEMENTATION.md` → Section 8 (SettingToggleRow example)

### Touch Targets & Accessibility
- Quick ref: `UI_SPECIFICATIONS_QUICK_REFERENCE.md` → "TOUCH TARGETS"
- Full specs: `docs/research/2025-12-30-premium-ui-specifications.md` → Section 2.3
- Validation: `RESEARCH_SUMMARY.md` → "Validation Checklist"

### Material Design 3 Details
- Full specs: `docs/research/2025-12-30-premium-ui-specifications.md` → Section 1
- Code: `DESIGN_TOKENS_KMP_IMPLEMENTATION.md` → Sections 2-7

### iOS HIG Details
- Full specs: `docs/research/2025-12-30-premium-ui-specifications.md` → Section 2
- Safe area specifics: Section 2.2
- Component sizing: Section 2.3

### Premium App Analysis (Copilot Money, Mercury, Revolut)
- Full analysis: `docs/research/2025-12-30-premium-ui-specifications.md` → Section 7
- Summary: `RESEARCH_SUMMARY.md` → "Key Findings"

---

## IMPLEMENTATION WORKFLOW

### Step 1: Understand Specifications (5 min)
1. Read `RESEARCH_SUMMARY.md` (quick context)
2. Skim `UI_SPECIFICATIONS_QUICK_REFERENCE.md` (all measurements)

### Step 2: Create Design Tokens (1 hour)
1. Copy code from `DESIGN_TOKENS_KMP_IMPLEMENTATION.md`
2. Create `shared/commonMain/theme/` package
3. Implement Spacing.kt, Typography.kt, etc.
4. Integrate into FinutsTheme composable

### Step 3: Build First Component (1-2 hours)
1. Create BalanceCard using FinutsDimensions.HeroCard
2. Reference usage examples from Section 8
3. Test dimensions on device
4. Validate against checklist

### Step 4: Expand to Full Dashboard (4-6 hours)
1. Implement TransactionItem (list)
2. Implement SettingToggleRow
3. Add Bottom Navigation
4. Test all measurements

### Step 5: Polish & Adapt (2-3 hours)
1. Add animations/transitions
2. Test on multiple device sizes
3. Verify safe areas (iOS notch)
4. Run accessibility check

---

## KEY MEASUREMENTS AT A GLANCE

```
SPACING (8dp grid):
  xs=4dp, sm=8dp, md=16dp, lg=24dp, xl=32dp, xxl=48dp

TYPOGRAPHY:
  Display S (Hero)    = 36dp
  Headline S          = 24dp
  Title M             = 16dp
  Body M (default)    = 14dp
  Body S              = 12dp

COMPONENTS:
  Hero Card           = 328dp × 164dp, radius 16dp
  Bottom Nav          = 56dp (Android) / 49pt (iOS)
  List Item (1-line)  = 56dp
  List Item (2-line)  = 72dp
  Button              = 40dp height
  Icon                = 24dp
  Touch Target        = 48dp min

CORNERS:
  extraSmall = 4dp, small = 8dp, medium = 12dp, large = 16dp, extraLarge = 24dp

SAFE AREAS (iOS):
  Top: 59pt (no nav) / 97-149pt (with nav)
  Bottom: 34pt (home indicator)
  With Tab Bar: 49pt + 34pt = 83pt total
```

---

## RESEARCH SOURCES (25+)

### Official Documentation (10)
- Material Design 3
- iOS HIG
- Android Developer
- Jetpack Compose
- Kotlin Multiplatform

### Case Studies & Industry (6)
- Copilot Money (Apple)
- Banking app design patterns
- Dashboard design best practices
- Fintech UI patterns

### Community & Reference (9)
- Revolut Figma community
- Linear design system (reverse-engineered)
- Notion design templates
- Design pattern websites

---

## CREDIBILITY ASSESSMENT

| Content | Confidence | Credibility |
|---------|-----------|-------------|
| Material Design 3 specs | 100% | 1.0 |
| iOS HIG specs | 100% | 1.0 |
| Bottom nav heights | 100% | 1.0 |
| Typography scale | 100% | 1.0 |
| Component dimensions | 95% | 0.95 |
| List item patterns | 90% | 0.9 |
| Hero card proportions | 85% | 0.85 |
| Fintech app patterns | 80% | 0.8 |
| Premium app specifics | 40% | 0.4 |

---

## WHAT'S INCLUDED vs. NOT INCLUDED

### Included (100% coverage)
- All component heights and widths
- Typography scale (Material Design 3)
- Spacing grid (8dp baseline)
- Corner radius scale
- Touch target minimums
- Safe area specifications
- List item patterns
- Navigation specifications
- Settings patterns
- Carousel patterns
- Implementation code

### Not Included (By Design)
- Animation timing (custom per app)
- Custom color palettes (brand-specific)
- Micro-interactions (proprietary)
- Advanced shadow/glass effects
- Font kerning/tracking
- Exact premium app internals

**Why not included:** These are proprietary to each app and require brand/product decisions. This research provides the solid foundation; the rest is Finuts-specific innovation.

---

## VALIDATION CHECKLIST

Before every screen implementation, verify:

```
Spacing:
□ Screen margins are 16dp
□ All spacing uses 8dp multiples
□ Section gaps are 24dp

Typography:
□ Uses one of the 5 main sizes
□ Text is legible at specified size
□ Hierarchy is clear

Components:
□ Hero card 328×164dp
□ Bottom nav correct height
□ List items 56 or 72dp
□ Touch targets ≥48dp

Quality:
□ Color contrast ≥4.5:1
□ Safe areas respected
□ All corners match scale
□ Padding consistent
```

---

## NEXT STEPS FOR DEVELOPERS

1. **Read:** Quick reference (5 min)
2. **Reference:** Full specs as needed (bookmarks)
3. **Code:** Copy design tokens (Section 3)
4. **Build:** Implement first component with examples
5. **Test:** Validate against checklists
6. **Expand:** Reuse tokens for all screens

---

## FOR DESIGNERS

If you need to update Figma design:

1. Reference `UI_SPECIFICATIONS_QUICK_REFERENCE.md` for all measurements
2. See `docs/research/2025-12-30-premium-ui-specifications.md` for Material Design 3 specs
3. Use corner radius scale from Section 4
4. Apply typography from Section 1.1
5. Follow 8dp spacing grid from Section 1.2

---

## FOR PROJECT MANAGERS

**Status:** Research complete, ready for implementation
**Quality:** 25+ sources analyzed, 95% confidence on core specs
**Time to implement:** 8-10 hours (design tokens + main components)
**Risk level:** Low (built on official Material Design 3 + iOS HIG)
**Dependencies:** Compose Multiplatform 1.9.3+ (already in CLAUDE.md)

---

## CONTACT & UPDATES

- Full research: Saved in `docs/research/` folder
- Updated: 2025-12-30
- Credibility validated: All sources cited with scores
- Ready for: Production implementation

---

**Start with:** `UI_SPECIFICATIONS_QUICK_REFERENCE.md`
**Code implementation:** `DESIGN_TOKENS_KMP_IMPLEMENTATION.md`
**Deep dive:** `docs/research/2025-12-30-premium-ui-specifications.md`

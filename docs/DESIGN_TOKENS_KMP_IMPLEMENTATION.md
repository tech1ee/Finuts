# Design Tokens Implementation for Finuts (KMP)

**Target:** Kotlin Multiplatform (Compose Multiplatform 1.9.3+)
**Base:** Material Design 3
**Status:** Ready for implementation

---

## FILE STRUCTURE

```
shared/commonMain/
├── theme/
│   ├── Color.kt           # Color tokens
│   ├── Spacing.kt         # Spacing tokens (8dp grid)
│   ├── Typography.kt      # Text styles
│   ├── Shape.kt           # Corner radius tokens
│   ├── Elevation.kt       # Shadow/elevation tokens
│   ├── FinutsTheme.kt     # Main theme composable
│   └── Dimensions.kt      # Fixed component sizes
```

---

## 1. SPACING TOKENS (shared/commonMain/theme/Spacing.kt)

```kotlin
package com.finuts.theme

import androidx.compose.ui.unit.dp

/**
 * Finuts Spacing System
 * Based on 8dp baseline grid (Material Design 3)
 * All padding, margin, and gaps should use these tokens
 */
object FinutsSpacing {
    // Core units
    val xs = 4.dp       // Extra small - minimal gaps
    val sm = 8.dp       // Small - base unit
    val md = 16.dp      // Medium - standard padding (MOST COMMON)
    val lg = 24.dp      // Large - section gaps
    val xl = 32.dp      // Extra large - major spacing
    val xxl = 48.dp     // XXL - rare, full-width padding

    // Named composites for common patterns
    object Screen {
        val padding = FinutsSpacing.md          // 16dp screen edge margin
        val horizontalPadding = FinutsSpacing.md
        val verticalPadding = FinutsSpacing.md
    }

    object Section {
        val gap = FinutsSpacing.lg              // 24dp between sections
        val padding = FinutsSpacing.md          // 16dp around content
        val paddingVertical = FinutsSpacing.lg  // 24dp between sections
    }

    object Component {
        val padding = FinutsSpacing.md          // 16dp interior padding
        val paddingSmall = FinutsSpacing.sm     // 8dp compact padding
        val gap = FinutsSpacing.sm              // 8dp between items
    }

    object List {
        val itemHeight = 56.dp                  // Single-line items
        val itemHeightDouble = 72.dp            // Two-line items (transactions)
        val itemHeightTriple = 88.dp            // Three-line items (rare)
        val iconSize = 24.dp                    // List icon size
        val iconPaddingLeft = 16.dp             // From screen edge to icon
        val textPaddingLeft = 72.dp             // From screen edge to text (icon + gap + padding)
        val textPaddingRight = 16.dp
        val verticalItemPadding = 12.dp         // Top/bottom padding inside row
    }

    object Hero {
        val cardHeight = 164.dp                 // Hero card height
        val cardPadding = FinutsSpacing.md      // 16dp inside card
        val spacing = FinutsSpacing.lg          // 24dp gap below hero
    }

    object Navigation {
        val bottomBarHeight = 56.dp             // Android bottom nav (Material)
        val bottomBarHeightIos = 49.dp          // iOS tab bar
        val bottomSafeAreaIos = 34.dp           // iOS home indicator
        val iconSize = 24.dp                    // Nav icon
        val labelSize = 12.dp                   // Nav label (via typography)
        val spacingBetweenIcons = 8.dp
    }

    object Dialog {
        val padding = 24.dp                     // Dialog interior
        val buttonHeight = 40.dp
        val buttonGap = 8.dp
    }
}
```

---

## 2. TYPOGRAPHY TOKENS (shared/commonMain/theme/Typography.kt)

```kotlin
package com.finuts.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Define custom font family if needed (SF Pro / Roboto)
// val finutsFontFamily = FontFamily(
//     Font(R.font.sf_pro_display_regular, FontWeight.W400),
//     Font(R.font.sf_pro_display_bold, FontWeight.W700),
// )

/**
 * Material Design 3 Typography for Finuts
 * All sizes in sp (scalable pixels, not dp)
 * Follows MD3 type scale
 */
val FinutsTypography = Typography(
    // Display styles (large headlines)
    displayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.W475,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.W475,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontSize = 36.sp,        // USE: Balance amount in hero card
        lineHeight = 44.sp,
        fontWeight = FontWeight.W475,
        letterSpacing = 0.sp,
    ),

    // Headline styles
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.W475,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.W475,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,        // USE: Card titles, major sections
        lineHeight = 32.sp,
        fontWeight = FontWeight.W475,
        letterSpacing = 0.sp,
    ),

    // Title styles
    titleLarge = TextStyle(
        fontSize = 22.sp,        // USE: Page titles, headers
        lineHeight = 30.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,        // USE: Subheadings, emphasis
        lineHeight = 24.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,        // USE: Component labels
        lineHeight = 20.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.1.sp,
    ),

    // Body styles (primary content)
    bodyLarge = TextStyle(
        fontSize = 16.sp,        // USE: Primary text, descriptions
        lineHeight = 24.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,        // USE: Default body copy (MOST COMMON)
        lineHeight = 20.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,        // USE: Secondary text, hints, labels
        lineHeight = 16.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.4.sp,
    ),

    // Label styles (buttons, badges)
    labelLarge = TextStyle(
        fontSize = 14.sp,        // USE: Button text
        lineHeight = 20.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,        // USE: Small buttons, nav labels
        lineHeight = 16.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,        // USE: Tags, minimal labels
        lineHeight = 16.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * Typography usage guide:
 *
 * Hero Section:
 * - Balance label:     bodySmall (12sp)
 * - Balance amount:    displaySmall (36sp) ← LARGEST
 * - Time updated:      bodySmall (12sp)
 *
 * Cards:
 * - Card title:        headlineSmall (24sp) or titleLarge (22sp)
 * - Card value:        titleMedium (16sp)
 * - Card description:  bodySmall (12sp)
 *
 * Lists:
 * - Primary text:      bodyMedium (14sp)
 * - Secondary text:    bodySmall (12sp)
 *
 * Headers:
 * - Section header:    titleMedium (16sp)
 * - Screen title:      titleLarge (22sp)
 *
 * Controls:
 * - Button:            labelLarge (14sp)
 * - Toggle label:      bodyMedium (14sp)
 * - Input hint:        bodySmall (12sp)
 */
```

---

## 3. SHAPE TOKENS (shared/commonMain/theme/Shape.kt)

```kotlin
package com.finuts.theme

import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Corner radius tokens (Material Design 3 Shape Scale)
 * Applied consistently across components
 */
val FinutsShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),      // Minimal rounding
    small = RoundedCornerShape(8.dp),           // Buttons, small cards
    medium = RoundedCornerShape(12.dp),         // Standard cards (DEFAULT)
    large = RoundedCornerShape(16.dp),          // Hero cards, FAB
    extraLarge = RoundedCornerShape(24.dp),     // Dialogs, modals
)

/**
 * Usage guide:
 *
 * Cards:
 * - Default card:       medium (12dp)
 * - Hero card:          large (16dp)
 * - Dialog/Modal:       extraLarge (24dp)
 *
 * Buttons:
 * - Standard button:    small (8dp)
 * - Prominent button:   medium (12dp)
 *
 * Inputs:
 * - Text field:         small (8dp)
 * - Larger field:       medium (12dp)
 */
```

---

## 4. COMPONENT DIMENSIONS (shared/commonMain/theme/Dimensions.kt)

```kotlin
package com.finuts.theme

import androidx.compose.ui.unit.dp

/**
 * Fixed component dimensions
 * (Not spacing, but structural sizes)
 */
object FinutsDimensions {
    // Hero Card
    object HeroCard {
        val width = 328.dp          // Full-width minus 16dp margins
        val height = 164.dp         // Standard balance card
        val cornerRadius = 16.dp    // large shape
    }

    // List Items
    object ListItem {
        val heightSingle = 56.dp    // 1-line item (minimum)
        val heightDouble = 72.dp    // 2-line item (transactions - COMMON)
        val heightTriple = 88.dp    // 3-line item (rare)
        val iconSize = 24.dp        // Icon in list
        val minHeight = 56.dp
    }

    // Buttons
    object Button {
        val heightSmall = 36.dp     // Compact button
        val heightStandard = 40.dp  // Primary button (MOST COMMON)
        val heightLarge = 48.dp     // Large button
        val minWidth = 64.dp        // Minimum width
        val paddingHorizontal = 24.dp
        val paddingVertical = 8.dp
    }

    // Toggle/Switch
    object Toggle {
        val widthIos = 51.dp        // iOS UISwitch fixed width
        val heightIos = 31.dp       // iOS UISwitch fixed height
        val widthAndroid = 52.dp    // Material Switch (approximate)
        val heightAndroid = 32.dp
    }

    // Navigation
    object BottomNavigation {
        val height = 56.dp          // Android Material (fixed)
        val heightIos = 49.dp       // iOS Tab Bar (fixed, no safe area)
        val safeAreaIos = 34.dp     // Home indicator (add to tab bar)
        val iconSize = 24.dp        // Icon size
    }

    // Top App Bar
    object TopAppBar {
        val heightCompact = 56.dp   // Standard top bar
        val heightLarge = 96.dp     // With large title
        val titleSize = 22.dp       // Use titleLarge typography
    }

    // Dialog
    object Dialog {
        val minWidth = 280.dp
        val maxWidth = 560.dp
        val buttonHeight = 40.dp
        val padding = 24.dp
    }

    // Icons
    object Icon {
        val size = 24.dp            // Standard icon (MOST COMMON)
        val sizeSmall = 16.dp       // Small icon
        val sizeLarge = 32.dp       // Large icon
    }

    // Carousel
    object Carousel {
        val cardHeight = 180.dp     // Card height in carousel
        val cardGap = 12.dp         // Between cards
        val pageGutter = 16.dp      // Edge whitespace
        val peekWidth = 48.dp       // Visible next card
    }
}
```

---

## 5. ELEVATION/SHADOW TOKENS (shared/commonMain/theme/Elevation.kt)

```kotlin
package com.finuts.theme

import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.unit.dp

/**
 * Elevation levels (Material Design 3)
 * Use for depth, layering, and visual hierarchy
 */
object FinutsElevation {
    val level0 = 0.dp      // Flat, no shadow
    val level1 = 1.dp      // Subtle shadow (cards)
    val level2 = 3.dp      // Minimal depth
    val level3 = 6.dp      // Medium depth
    val level4 = 8.dp      // FAB, menus
    val level5 = 12.dp     // Dialogs, modals
}

/**
 * Usage:
 *
 * Cards:
 * - Default card:     elevation = 1.dp
 * - Hovered card:     elevation = 3.dp
 *
 * FAB:
 * - Standard FAB:     elevation = 4.dp
 *
 * Dialogs:
 * - Dialog/Modal:     elevation = 5.dp
 */
```

---

## 6. COLOR TOKENS (shared/commonMain/theme/Color.kt)

```kotlin
package com.finuts.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Primary color palette (customize with Finuts branding)
private val BrandPrimary = Color(0xFF5C6BC0)        // Example: indigo
private val BrandSecondary = Color(0xFF26A69A)      // Example: teal
private val BrandTertiary = Color(0xFFFFA500)       // Example: orange

// Semantic colors
private val SuccessGreen = Color(0xFF4CAF50)        // Positive balance
private val ErrorRed = Color(0xFFF44336)            // Negative balance, losses
private val WarningAmber = Color(0xFFFFC107)        // Alerts, pending

// Light theme
val FinutsLightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    secondary = BrandSecondary,
    tertiary = BrandTertiary,
    background = Color.White,
    surface = Color(0xFFFAFAFA),
    error = ErrorRed,
    // ... add other colors as needed
)

// Dark theme (optional)
val FinutsDarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    secondary = BrandSecondary,
    tertiary = BrandTertiary,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = ErrorRed,
    // ... add other colors as needed
)
```

---

## 7. MAIN THEME (shared/commonMain/theme/FinutsTheme.kt)

```kotlin
package com.finuts.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun FinutsTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> FinutsDarkColorScheme
        else -> FinutsLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FinutsTypography,
        shapes = FinutsShapes,
        content = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                content = content
            )
        }
    )
}
```

---

## 8. USAGE EXAMPLES IN COMPOSABLES

### Hero Card Component
```kotlin
@Composable
fun BalanceCard(
    balance: String,
    lastUpdated: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(FinutsSpacing.Screen.padding)
            .height(FinutsDimensions.HeroCard.height),
        shape = RoundedCornerShape(FinutsDimensions.HeroCard.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = FinutsElevation.level1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(FinutsSpacing.Component.padding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Available Balance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                balance,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                lastUpdated,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### Transaction List Item
```kotlin
@Composable
fun TransactionItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FinutsDimensions.ListItem.heightDouble)
            .padding(horizontal = FinutsSpacing.List.iconPaddingLeft),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier.size(FinutsDimensions.ListItem.iconSize),
            contentAlignment = Alignment.Center,
            content = { icon() }
        )

        Spacer(modifier = Modifier.width(FinutsSpacing.Component.gap))

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        // Amount
        Text(
            amount,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = FinutsSpacing.Component.gap)
        )
    }
}
```

### Settings Row with Toggle
```kotlin
@Composable
fun SettingToggleRow(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FinutsDimensions.ListItem.heightSingle)
            .padding(horizontal = FinutsSpacing.Screen.padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(end = FinutsSpacing.Component.gap)
        )
    }
}
```

---

## 9. RESPONSIVE ADAPTATION

For adaptive layouts on tablets/larger screens:

```kotlin
@Composable
fun AdaptiveHeroCard() {
    val windowSizeClass = calculateWindowSizeClass()

    val cardHeight = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> FinutsDimensions.HeroCard.height    // 164dp
        WindowWidthSizeClass.Medium -> 200.dp
        WindowWidthSizeClass.Expanded -> 240.dp
        else -> FinutsDimensions.HeroCard.height
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .padding(FinutsSpacing.Screen.padding)
    ) {
        // Content
    }
}
```

---

## 10. IMPLEMENTATION CHECKLIST

- [ ] Create `theme/` package structure
- [ ] Implement `Spacing.kt` with 8dp grid tokens
- [ ] Implement `Typography.kt` with Material 3 type scale
- [ ] Implement `Shape.kt` with corner radius tokens
- [ ] Implement `Dimensions.kt` with component sizes
- [ ] Implement `Color.kt` with brand colors
- [ ] Create `FinutsTheme.kt` composable
- [ ] Apply theme to main `MainActivity.kt` / app entry
- [ ] Test spacing on 375dp width (standard mobile)
- [ ] Test typography legibility at all sizes
- [ ] Validate touch targets (48dp minimum)
- [ ] Verify safe areas on iOS (notch/home indicator)
- [ ] Test on Android devices (320-480dp widths)
- [ ] Test on iOS devices (375-430pt widths)

---

## 11. BUILD CONFIGURATION

Add to `build.gradle.kts` (shared module):

```gradle
dependencies {
    implementation("androidx.compose.material3:material3:1.9.0")
    implementation("androidx.compose.foundation:foundation:1.9.0")
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
}
```

---

## REFERENCES

- Material Design 3: https://m3.material.io/
- Compose Multiplatform: https://www.jetbrains.com/help/kotlin-multiplatform-dev/
- iOS HIG: https://developer.apple.com/design/
- Full specifications: `docs/research/2025-12-30-premium-ui-specifications.md`

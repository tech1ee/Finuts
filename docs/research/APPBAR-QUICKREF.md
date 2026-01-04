# App Bar Design Quick Reference for Finuts

> **TL;DR:** Use **64dp** (Android/M3) and **44pt** (iOS) for app bars. Handle safe areas with `WindowInsets`. Dashboard = no app bar. Details = simple fixed bar. Transaction lists = collapsing medium bar.

---

## 1. Three App Bar Patterns (Pick One Per Screen)

### Pattern A: Hero Dashboard (No App Bar)
**When:** Home/Dashboard screens
**Implementation (Compose):**
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(gradient)
        .windowInsetsPadding(WindowInsets.statusBars)
) {
    // Account card, balance, etc.
}
```

**Implementation (SwiftUI):**
```swift
ZStack {
    Color.blue.ignoresSafeArea(edges: .top)
    VStack { /* content */ }
}
```

### Pattern B: Simple Top App Bar
**When:** Detail/Settings screens
**Implementation (Compose):**
```kotlin
Scaffold(
    topBar = {
        TopAppBar(
            title = { Text("Transaction Details") },
            navigationIcon = { BackButton() }
        )
    }
) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding)) {
        // content
    }
}
```

**Implementation (SwiftUI):**
```swift
NavigationStack {
    VStack { /* content */ }
        .navigationTitle("Transaction Details")
        .navigationBarBackButtonHidden(false)
}
```

### Pattern C: Collapsing Header
**When:** Transaction lists, portfolios
**Implementation (Compose):**
```kotlin
val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

Scaffold(
    topBar = {
        MediumTopAppBar(
            title = { Text("Transactions") },
            scrollBehavior = scrollBehavior
        )
    },
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
) { innerPadding ->
    LazyColumn(modifier = Modifier.padding(innerPadding)) {
        // list items
    }
}
```

---

## 2. Height Specifications

| Screen Type | Android | iOS | Notes |
|-------------|---------|-----|-------|
| App Bar | 64dp | 44pt | Material 3 standard |
| Status Bar | 24dp (system) | 20pt standard, 54pt with Dynamic Island | Auto-managed |
| **Total Top Space** | **88dp** | **64-84pt** | Always account for this |

---

## 3. Safe Area Handling (Most Important!)

### Automatic (Recommended)
```kotlin
// Both Android and iOS - use this!
Scaffold(
    topBar = { TopAppBar(...) }
) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding)) {
        // Content automatically respects safe area
    }
}
```

### Manual Control (Rare)
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)  // Adds top padding
) {
    // Your content
}
```

### SwiftUI
```swift
NavigationStack {
    VStack { /* content */ }
        // Safe area respected automatically
}
```

---

## 4. Color & Typography Consistency

### Create Shared Theme
```kotlin
object AppBarTheme {
    val colors = TopAppBarColors(
        containerColor = Colors.primary,
        titleContentColor = Colors.onPrimary,
        navigationIconContentColor = Colors.onPrimary,
        actionIconContentColor = Colors.onPrimary
    )
}
```

### Use Everywhere
```kotlin
TopAppBar(
    title = { Text("Screen Title") },
    colors = AppBarTheme.colors  // ← Consistency!
)
```

**Standard Values:**
- **Font Size:** 16sp/17pt (title)
- **Icon Size:** 24dp/24pt
- **Horizontal Padding:** 24dp/16pt
- **Icon Spacing:** 12dp/12pt

---

## 5. Device-Specific Gotchas

| Issue | Solution |
|-------|----------|
| **iPhone 14+ Dynamic Island hides content** | Use `ignoresSafeArea` or `windowInsetsPadding` (automatic) |
| **iPad Landscape no status bar** | Use `WindowSizeClass` to detect; adjust layout |
| **App bar height jumps between screens** | Always use 64dp (M3); avoid mixing 56dp/64dp |
| **Status bar color doesn't match app bar** | Android: `enableEdgeToEdge()` in MainActivity |
| **Laggy scroll on collapsing header** | Profile with DevTools; minimize recomposition |
| **RTL languages text overlap** | Test with Arabic/Hebrew; use `layoutDirection` modifier |

---

## 6. Platform-Specific Code

### Kotlin Multiplatform Pattern

**commonMain:**
```kotlin
@Composable
expect fun Modifier.platformAppBarSafeArea(): Modifier

@Composable
fun WalletScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Wallet") })
        }
    ) { innerPadding ->
        Box(modifier = Modifier.platformAppBarSafeArea().padding(innerPadding)) {
            // content
        }
    }
}
```

**androidMain:**
```kotlin
@Composable
actual fun Modifier.platformAppBarSafeArea(): Modifier =
    this.windowInsetsPadding(WindowInsets.statusBars)
```

**iosMain:**
```kotlin
@Composable
actual fun Modifier.platformAppBarSafeArea(): Modifier =
    this.windowInsetsPadding(WindowInsets.statusBars)
```

---

## 7. Fintech Best Practices

✅ **DO:**
- Use consistent app bar color across all screens
- Always show back button (never hide navigation)
- Test on iPhone 14 Pro (Dynamic Island) and iPad
- Use collapsing headers for lists only
- Keep app bar height standard (64dp/44pt)
- Respect safe area automatically with Scaffold

❌ **DON'T:**
- Mix 56dp and 64dp heights on same app
- Hide app bar on scroll for critical actions
- Use hamburger menus in finance (breaks trust)
- Ignore Dynamic Island safe area on iOS
- Hardcode status bar height (use API)
- Overlap content behind app bar without intent

---

## 8. Scroll Behavior Quick Guide

| Behavior | Use Case | Effect |
|----------|----------|--------|
| **enterAlwaysScrollBehavior** | None (rarely used) | Expands/collapses with every scroll |
| **exitUntilCollapsedScrollBehavior** | ✅ Lists, portfolios | Collapses on up, stays until user scrolls to end |
| **pinnedScrollBehavior** | Tablet, critical actions | Stays visible always |

**Recommended:** Use `exitUntilCollapsedScrollBehavior` for most screens.

---

## 9. Testing Checklist

- [ ] Dashboard looks full-bleed on iPhone/Android (no content gap under status bar)
- [ ] Detail screens show app bar consistently
- [ ] Back button works on all detail screens
- [ ] Collapsing header doesn't stutter while scrolling
- [ ] Safe area respected: Nothing hidden under Dynamic Island (iPhone 14+)
- [ ] Landscape mode: App bar still visible and usable
- [ ] Status bar color matches app bar (especially Android)
- [ ] Padding consistent: 24dp (Android), 16pt (iOS)
- [ ] Title font size: 16sp (Android), 17pt (iOS)
- [ ] Icons visible and tappable (min 44pt/48dp)

---

## 10. Implementation Priority

### Phase 1 (Week 1)
- [ ] Create `AppBarTheme` object with consistent colors
- [ ] Implement simple app bars for all detail screens
- [ ] Add safe area handling to Dashboard

### Phase 2 (Week 2)
- [ ] Add collapsing headers to transaction lists
- [ ] Test on iPhone 14 Pro and iPad
- [ ] Fix any Dynamic Island safe area issues

### Phase 3 (Polish)
- [ ] Performance profiling of scroll behavior
- [ ] Accessibility testing
- [ ] RTL language testing (Russian, Kazakh, Arabic)

---

## 11. References

- **Material Design 3:** https://m3.material.io/components/app-bars/specs
- **Android Developers:** https://developer.android.com/develop/ui/compose/components/app-bars
- **SwiftUI Safe Area:** https://fatbobman.com/en/posts/safearea/
- **Kotlin Multiplatform:** https://kotlinlang.org/docs/multiplatform/compose-adaptive-layouts.html

---

**Last Updated:** 2025-12-30
**For:** Finuts Mobile App (Kotlin Multiplatform + Compose)
**Status:** Ready to implement

# ADR-005: KoalaPlot for Chart Visualization

**Date:** 2026-01-03
**Status:** Accepted
**Decision Makers:** User + Claude

---

## Context

Finuts needs chart visualization for the Reports feature (Iteration 9). Key requirements:
- Pie/donut charts for category spending breakdown
- Full Kotlin Multiplatform support (iOS + Android)
- Composable API aligned with project architecture
- Reasonable maturity for production use

## Options Considered

### Option 1: KoalaPlot (v0.10.4)
- **Pros:**
  - Full pie/donut chart support
  - Complete KMP support (iOS, Android, Desktop, Web)
  - Composable API matches our architecture
  - Active development, 695 GitHub stars
  - Apache-2.0 license
- **Cons:**
  - Alpha status (API may change)
  - Less documentation than mature libraries

### Option 2: Vico (v2.3.6)
- **Pros:**
  - Very mature and stable (2.9k stars)
  - Excellent line/bar chart support
  - Works with both Compose and View system
  - Active development
- **Cons:**
  - No pie/donut support yet (planned for v2)
  - Would need to wait or use alternative for our needs

### Option 3: Custom Canvas
- **Pros:**
  - Full control over rendering
  - No external dependencies
  - Maximum performance potential
- **Cons:**
  - Significant development time
  - Need to implement animations, accessibility
  - Maintenance burden

### Option 4: Vico + Custom Canvas Hybrid
- **Pros:**
  - Use Vico for line/bar when available
  - Custom Canvas for pie/donut
- **Cons:**
  - Inconsistent API across chart types
  - More code to maintain

## Decision

**Use KoalaPlot (v0.10.4) for all charting needs.**

## Rationale

1. **Immediate pie/donut support** - Critical for category spending visualization
2. **Full KMP support** - Works on both iOS and Android out of the box
3. **Composable API** - Aligns with our declarative UI architecture
4. **Active maintenance** - Regular releases, responsive maintainers
5. **Future-proof** - Line/bar charts also available when needed

The alpha status is acceptable because:
- We can pin the version in gradle
- Our chart usage is standard (no edge cases)
- Breaking changes can be addressed during updates

## Consequences

### Positive
- Unified charting solution for all chart types
- Consistent API across the codebase
- Faster implementation than custom solution

### Negative
- Dependency on external library
- May need updates when API changes
- Limited customization compared to custom Canvas

### Mitigations
- Pin exact version: `io.github.koalaplot:koalaplot-core:0.10.4`
- Wrap chart components in internal composables (adapter pattern)
- Monitor GitHub releases for breaking changes

## Implementation

```kotlin
// composeApp/build.gradle.kts
dependencies {
    implementation("io.github.koalaplot:koalaplot-core:0.10.4")
}
```

## References

- [KoalaPlot GitHub](https://github.com/KoalaPlot/koalaplot-core)
- [Research: Charts, Reports, AI](../research/2026-01-03-charts-reports-ai.md)
- [John O'Reilly: Compose Multiplatform Charts](https://johnoreilly.dev/posts/compose-multiplatform-chart/)

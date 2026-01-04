# ADR-002: Linear-style Shapes (8dp max radius)

**Date:** 2025-12-29
**Status:** DEPRECATED (2026-01-04)
**Superseded by:** DESIGN_SYSTEM.md v2.0 "Harmonious Minimalism"
**Context:** Need consistent corner radius system

> **DEPRECATION NOTICE:** This ADR has been superseded by the updated design system
> documented in `docs/design-system/DESIGN_SYSTEM.md`. The new system uses 12-24dp
> corner radii for cards and components to achieve a more refined, premium aesthetic.
> See FinutsSpacing for current values: heroCornerRadius (24dp), accountCardRadius (12dp),
> heroActionButtonRadius (12dp), settingsCardRadius (16dp).

## Decision

Use Linear-style shapes with maximum 8dp border radius.

## Rationale

- Premium, minimalist look (inspired by Linear)
- Subtle, not cartoony
- Consistent across all components

## Consequences

- Maximum corner radius: 8dp
- Cards: 8dp
- Buttons: 6dp
- Inputs: 4dp
- Full round only for avatars/icons

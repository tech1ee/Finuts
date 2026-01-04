# ADR-001: Pure 8dp Grid System

**Date:** 2025-12-29
**Status:** Accepted
**Context:** Need consistent spacing system for UI

## Decision

Use pure 8dp grid system for all spacing and sizing.

## Rationale

- Industry standard (Material Design, Notion, Linear)
- Easy mental math (8, 16, 24, 32, 40, 48...)
- Works well with typography baseline

## Consequences

- All spacing values must be multiples of 8dp
- Exception: 4dp for micro-adjustments
- Applied to: padding, margin, gaps, component sizing

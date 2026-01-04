# ADR-004: Amplitude + Sentry

**Date:** 2025-12-29
**Status:** Accepted
**Context:** Need analytics and crash reporting for mobile apps

## Decision

Use Amplitude for analytics and Sentry for crash reporting.

## Rationale

- Amplitude: best-in-class product analytics
- Sentry: KMP support, detailed crash reports
- Both have generous free tiers

## Consequences

- Amplitude SDK for user events
- Sentry SDK for crash/error tracking
- Privacy-first: no PII in analytics

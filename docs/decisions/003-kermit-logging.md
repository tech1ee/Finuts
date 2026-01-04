# ADR-003: Kermit for Logging

**Date:** 2025-12-29
**Status:** Accepted
**Context:** Need cross-platform logging solution for KMP

## Decision

Use Kermit for logging across all platforms.

## Rationale

- Native KMP support
- Integrates with Crashlytics/Sentry
- Minimal overhead
- Simple API

## Consequences

- All logging via Kermit Logger
- Production: warnings and errors only
- Debug: verbose logging enabled

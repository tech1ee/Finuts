# Finuts Documentation

AI-powered personal finance app for iOS and Android.

## Overview

| Attribute | Value |
|-----------|-------|
| **Name** | Finuts |
| **Type** | Personal Finance App |
| **Platforms** | iOS, Android |
| **Technology** | Kotlin Multiplatform + Compose Multiplatform |
| **Target Market** | Kazakhstan → CIS → Global |
| **Languages** | Russian, Kazakh, English |

## Key Features

- Account management (multiple wallets, cards)
- Transaction tracking with AI categorization
- Budget planning and monitoring
- AI-powered financial insights
- Bank statement import (Kaspi, Halyk PDF)
- Receipt OCR scanning
- Financial goals tracking

## Tech Stack

- **Language**: Kotlin 2.3.0
- **UI**: Compose Multiplatform 1.9.3
- **Database**: Room 2.7.1 (KMP) + SQLCipher
- **DI**: Koin 4.1.0
- **Network**: Ktor 3.3.3
- **Analytics**: Amplitude
- **Crash Reporting**: Sentry

## Documentation Structure

```
docs/
├── README.md           # This file
├── architecture.md     # System architecture
├── changelog.md        # Change history
├── roadmap.md          # Milestones and plans
├── session-log.md      # Work session summary
├── decision-log.md     # ADR summary
├── prd/                # Product requirements
├── research/           # Research reports
├── sessions/           # Session notes
├── decisions/          # Architecture Decision Records
├── milestones/         # Completed milestone records
└── sync/               # Notion sync state
```

## Quick Links

- [PRD](./prd/) - Product Requirements
- [Research](./research/) - Research reports
- [Roadmap](./roadmap.md) - Development timeline
- [Architecture](./architecture.md) - System design

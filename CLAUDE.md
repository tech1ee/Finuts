# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## ⛔ Mandatory Rules

### 🧪 Test-Driven Development (TDD) — MANDATORY

**ALL code work starts with tests. No exceptions.**

#### Red-Green-Refactor Cycle
1. **RED**: Write a failing test first (describes desired behavior)
2. **GREEN**: Write minimal code to make the test pass
3. **REFACTOR**: Improve code while keeping tests green

#### TDD Rules
- **New feature** → write test first, then implementation
- **Bug fix** → write test reproducing the bug first, then fix
- **Refactoring** → ensure tests exist, then refactor
- **Changing existing code** → update/add tests first

#### Forbidden
- ❌ Writing code without tests
- ❌ Committing code with failing tests
- ❌ Deleting tests for a "quick" fix
- ❌ Using mocks when fakes can be used instead

#### Test Coverage Targets
| Layer | Minimum | Target |
|-------|---------|--------|
| Domain (Use Cases) | 80% | 90% |
| Data (Repositories) | 70% | 80% |
| Presentation (ViewModels) | 60% | 70% |
| UI (Compose) | 30% | 40% |

#### Testing Tools
- `kotlin.test` — core annotations and assertions
- `kotlinx-coroutines-test` — coroutine testing
- `Turbine` — Flow/StateFlow testing
- `Kover` — code coverage (minimum 65%)

### Before Any Code Change
- **Tests first, code second**: Write/update tests before implementing changes
- **Study first, change second**: Thoroughly analyze existing code and plan changes before implementation
- **Zero tolerance**: No breaking changes, no rule violations, no architectural compromises
- **No shortcuts**: Code duplication, anti-patterns, and temporary solutions are strictly forbidden
- **Quality from the start**: Everything must be production-ready immediately

### No Fakes
- **No simulations, mocks, stubs, or fakes** in production code
- Solve real problems with real implementations
- All code must be functional and correct from the first commit

### File Size Limits
- **Maximum 200 lines** per file (strict limit)
- **250 lines** = mandatory refactoring trigger
- Before refactoring: research all code paths and dependencies thoroughly

### Code Quality Standards
- **SOLID principles** — mandatory in all code
- **Clean Architecture** — strict layer separation
- **Clean Code** — readable, maintainable, self-documenting
- **Industry best practices** — always follow current standards
- **Exemplary code quality** — every file should be a reference example

### Skills Usage
- **Use Skills extensively** — leverage available skills for all applicable tasks
- **Research**: Always use `/deep-researcher` skill for any research tasks. Never browse the internet directly
- **Project management**: Use `/project-product-manager` skill for task tracking and project coordination

### Documentation
- **docs/ folder** — all project documentation lives in `docs/`
- **Chronological log** — always maintain event history and session logs
- **Keep docs updated** — document decisions, changes, and milestones

## Project Overview

**Finuts** is an AI-powered personal finance mobile application for iOS and Android built with Kotlin Multiplatform (KMP) and Compose Multiplatform. Target markets: Kazakhstan → CIS → Global. Languages: Russian, Kazakh, English.

## Technology Stack

*Updated: December 29, 2025 (based on deep research)*

| Component | Technology | Version | Status |
|-----------|------------|---------|--------|
| Language | Kotlin | **2.3.0** | ✅ Stable |
| UI | Compose Multiplatform | **1.9.3** | ✅ iOS Stable |
| Build | Gradle + Version Catalog | 8.7+ | ✅ |
| Database | Room (KMP) | **2.7.1** | ✅ KMP Stable |
| Encryption | SQLCipher | Latest | ✅ |
| DI | Koin | 4.1.0 | ✅ |
| Network | Ktor Client | 3.3.3 | ✅ |
| Serialization | kotlinx.serialization | **1.9.0** | ✅ |
| ViewModel | Lifecycle ViewModel | **2.9.6** | ✅ Official KMP |
| Navigation | Navigation Compose | **2.9.1** | ✅ Official KMP |
| Images | Coil | **3.3.0** | ✅ KMP |
| Date/Time | kotlinx-datetime | 0.7.1 | ✅ |
| Coroutines | kotlinx-coroutines | 1.10.2 | ✅ |
| Logging | **Kermit** | **2.0.4** | ✅ Recommended |
| Preferences | DataStore | **1.2.0** | ✅ KMP Stable |
| Analytics | **Amplitude** | Latest | ✅ KMP |
| Crash Reporting | **Sentry** | Latest | ✅ KMP |

### AI SDKs
| Provider | Library | Version |
|----------|---------|---------|
| OpenAI | openai-kotlin | 4.0.1 |
| Claude | anthropic-sdk-kotlin | 0.25.2 |
| Gemini | Firebase AI Logic | KMP in progress |

### Platform Requirements
- **Android**: Min SDK 26, Target SDK 35
- **iOS**: Min iOS 15.0, Xcode 15+

## Project Structure

```
finuts/
├── composeApp/              # Shared Compose UI (~95% shared)
│   ├── commonMain/          # Cross-platform UI code
│   ├── androidMain/         # Android-specific UI
│   └── iosMain/             # iOS-specific UI
├── shared/                  # Business logic (100% shared)
│   └── commonMain/
│       ├── domain/          # Use cases, entities
│       ├── data/            # Repositories, data sources
│       └── core/            # DI modules, utilities
├── features/                # Feature modules
│   ├── wallet/              # Accounts, transactions
│   ├── budget/              # Budgets, goals
│   ├── insights/            # AI recommendations
│   ├── reports/             # Charts, exports
│   └── import/              # Bank statement parsing
├── androidApp/              # Android entry point
└── iosApp/                  # iOS entry point (Xcode project)
```

## Architecture

- **Pattern**: Clean Architecture + MVVM
- **State Management**: ViewModel + StateFlow + MutableStateFlow
- **Code Sharing**: ~85% total (~100% business logic, ~95% UI)
- **Data Flow**: Privacy-first, local-first (data stays on device)

### Platform-Specific Code
| Component | Android | iOS |
|-----------|---------|-----|
| OCR | Google ML Kit | Vision Framework |
| Payments | Google Play Billing | StoreKit 2 |
| Biometrics | BiometricPrompt | LocalAuthentication |

## Build Commands

```bash
# Build all
./gradlew build

# Run Android app
./gradlew :androidApp:installDebug

# Run tests
./gradlew test
./gradlew :shared:testDebugUnitTest    # Android unit tests
./gradlew :shared:iosSimulatorArm64Test # iOS tests

# Lint
./gradlew lint
./gradlew detekt

# Clean
./gradlew clean
```

## AI Integration Guidelines

Three-layer cost optimization strategy (target: <$0.01/user/month):
1. **Layer 1 (80%)**: Rule-based categorization + merchant DB + on-device ML
2. **Layer 2 (15%)**: GPT-4o-mini / Claude Haiku / Gemini Flash
3. **Layer 3 (5%)**: GPT-4o for complex analysis only

**Privacy**: Never send PII to AI APIs. Use aggregated/anonymized data only.

## Key Conventions

- Use `expect`/`actual` for platform-specific implementations (OCR, payments, biometrics)
- Repository pattern for data access
- Use cases in domain layer for business logic
- ViewModels expose `StateFlow` to Compose UI
- Koin modules per feature for DI
- Room entities with SQLCipher encryption for sensitive financial data
- Kermit for logging (supports Crashlytics/Sentry integration)
- DataStore for preferences (type-safe, coroutines-based)
- Official AndroidX Navigation for type-safe routing with deep linking

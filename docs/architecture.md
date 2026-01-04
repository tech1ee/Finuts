# Finuts Architecture

## Overview

Finuts uses Clean Architecture with MVVM pattern, built on Kotlin Multiplatform (KMP) with Compose Multiplatform UI.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────┐
│                     CLIENTS                          │
├─────────────────────────────────────────────────────┤
│       Android              iOS                       │
│    (Compose UI)        (Compose UI)                  │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────┴─────────────────────────────┐
│                 SHARED CODE (KMP)                    │
│                    ~85% code                         │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌─────────────────────────────────────────────┐    │
│  │              PRESENTATION LAYER              │    │
│  │  ViewModels + StateFlow + Compose Screens    │    │
│  └─────────────────────────────────────────────┘    │
│                        │                             │
│  ┌─────────────────────────────────────────────┐    │
│  │               DOMAIN LAYER                   │    │
│  │  Use Cases + Entities + Repository Interface │    │
│  └─────────────────────────────────────────────┘    │
│                        │                             │
│  ┌─────────────────────────────────────────────┐    │
│  │                DATA LAYER                    │    │
│  │  Repositories + DataSources + Mappers        │    │
│  └─────────────────────────────────────────────┘    │
│                                                      │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────┴─────────────────────────────┐
│                   PLATFORM LAYER                     │
├─────────────────────────────────────────────────────┤
│  Android:              │  iOS:                       │
│  - Room Database       │  - Room Database            │
│  - ML Kit OCR          │  - Vision Framework OCR     │
│  - Play Billing        │  - StoreKit 2               │
│  - BiometricPrompt     │  - LocalAuthentication      │
└─────────────────────────────────────────────────────┘
```

## Layer Details

### Presentation Layer
- **ViewModels**: AndroidX ViewModel 2.9.6 (official KMP)
- **State**: StateFlow + MutableStateFlow
- **UI**: Compose Multiplatform 1.9.3
- **Navigation**: Navigation Compose 2.9.1

### Domain Layer
- **Entities**: Core business models (Account, Transaction, Budget, etc.)
- **Use Cases**: Single-responsibility business operations
- **Repository Interfaces**: Abstraction for data access

### Data Layer
- **Repositories**: Implementation of domain interfaces
- **Local DataSource**: Room 2.7.1 + SQLCipher
- **Remote DataSource**: Ktor 3.3.3

### Platform Layer (expect/actual)
| Feature | Android | iOS |
|---------|---------|-----|
| OCR | ML Kit | Vision Framework |
| Payments | Play Billing | StoreKit 2 |
| Biometrics | BiometricPrompt | LocalAuthentication |

## Module Structure

```
finuts/
├── composeApp/                 # Shared Compose UI (~95%)
│   ├── commonMain/
│   │   └── kotlin/com/finuts/app/
│   │       ├── ui/
│   │       │   ├── screens/
│   │       │   ├── components/
│   │       │   │   ├── cards/
│   │       │   │   ├── navigation/
│   │       │   │   ├── feedback/
│   │       │   │   ├── inputs/
│   │       │   │   ├── progress/
│   │       │   │   └── charts/
│   │       │   └── modifiers/
│   │       ├── theme/
│   │       └── navigation/
│   ├── androidMain/
│   └── iosMain/
│
├── shared/                     # Business logic (100%)
│   └── commonMain/
│       └── kotlin/com/finuts/
│           ├── domain/
│           │   ├── entities/
│           │   ├── usecases/
│           │   └── repositories/
│           ├── data/
│           │   ├── repositories/
│           │   ├── datasources/
│           │   └── mappers/
│           └── core/
│               ├── di/
│               └── utils/
│
├── features/                   # Feature modules
│   ├── wallet/
│   ├── budget/
│   ├── insights/
│   ├── reports/
│   └── import/
│
├── androidApp/
└── iosApp/
```

## Data Flow

```
User Action → ViewModel → UseCase → Repository → DataSource → Database
                                                      ↓
User ← UI State ← ViewModel ← UseCase ← Repository ← Mapper ← Entity
```

## Dependency Injection

- **Framework**: Koin 4.1.0
- **Pattern**: Module per feature + core modules
- **Scope**: Activity/Fragment for Android, App-wide for iOS

## Security

| Level | Implementation |
|-------|---------------|
| Storage | SQLCipher (AES-256) |
| Network | TLS 1.3, Certificate Pinning |
| Auth | Biometric + PIN fallback |
| AI | No PII in prompts, aggregated data only |

## AI Integration

Three-layer cost optimization:

1. **Layer 1 (80%)**: Rule-based + Merchant DB + On-device ML
2. **Layer 2 (15%)**: GPT-4o-mini / Claude Haiku / Gemini Flash
3. **Layer 3 (5%)**: GPT-4o for complex analysis

Target: <$0.01/user/month

---

## Feature Architecture Documentation

| Feature | Document | Status |
|---------|----------|--------|
| Transactions | [transactions.md](architecture/transactions.md) | Complete |
| Transfers | [transactions.md](architecture/transactions.md) | Complete |
| Reports | Included in transactions | Complete |
| Budgets | TBD | Planned |
| AI Categorization | TBD | Planned |

# Finuts

AI-powered personal finance app for iOS and Android built with Kotlin Multiplatform.

[![CI](https://github.com/tech1ee/Finuts/actions/workflows/ci.yml/badge.svg)](https://github.com/tech1ee/Finuts/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-blue.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.9.3-green.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)

## Features

- Multi-account wallet management
- AI-powered transaction categorization
- Budget planning and monitoring
- Financial insights and recommendations
- Bank statement import (Kaspi, Halyk PDF)
- Receipt OCR scanning
- Financial goals tracking

## Tech Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Kotlin | 2.3.0 |
| UI | Compose Multiplatform | 1.9.3 |
| Database | Room (KMP) + SQLCipher | 2.7.1 |
| DI | Koin | 4.1.0 |
| Network | Ktor | 3.3.3 |
| Analytics | Amplitude | 1.16.8 |
| Crash Reporting | Sentry | 7.19.1 |

## Project Structure

```
finuts/
├── composeApp/          # Shared Compose UI (~95% shared)
├── shared/              # Business logic (100% shared)
├── androidApp/          # Android entry point
├── iosApp/              # iOS entry point (Xcode project)
├── config/              # Build configurations
└── docs/                # Documentation
```

## Requirements

- **Android Studio**: Ladybug (2024.2.1) or later
- **Xcode**: 15.0+ (for iOS)
- **JDK**: 17+
- **Gradle**: 8.11.1

## Getting Started

### Clone the repository

```bash
git clone https://github.com/tech1ee/Finuts.git
cd Finuts
```

### Build Android

```bash
./gradlew :androidApp:assembleDebug
```

### Build iOS

```bash
cd iosApp
pod install
open iosApp.xcworkspace
```

### Run Tests

```bash
./gradlew test
```

### Run Linter

```bash
./gradlew detekt
```

## Architecture

- **Pattern**: Clean Architecture + MVVM
- **State Management**: ViewModel + StateFlow
- **Code Sharing**: ~85% total (~100% business logic, ~95% UI)
- **Data Flow**: Privacy-first, local-first (data stays on device)

## Platform Support

| Platform | Min Version | Status |
|----------|-------------|--------|
| Android | API 26 (8.0) | Supported |
| iOS | 15.0 | Supported |

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## Security

For security concerns, please see our [Security Policy](SECURITY.md).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Documentation

See [docs/](docs/) for detailed documentation.

# Research Report: KMP Tech Stack for Finuts

**Date:** 2025-12-29
**Sources Evaluated:** 25+
**Research Depth:** Deep (multi-source verification)

## Executive Summary

Comprehensive research of the current Kotlin Multiplatform (KMP) tech stack for the Finuts personal finance app. Key findings: Compose Multiplatform iOS is now stable (1.8.0+), Room KMP is stable (2.7.1), official ViewModel and Navigation KMP support available. Recommended stack updated with verified December 2025 versions.

---

## Key Findings

### 1. Kotlin Version
- **Current Stable**: 2.3.0 (released December 2025)
- **Source**: [JetBrains Kotlin Blog](https://blog.jetbrains.com/kotlin/2025/12/kotlin-2-3-0-released/)
- **Highlights**: Java 25 support, improved Swift export, Gradle 9.0 compatibility

### 2. Compose Multiplatform
- **Current Stable**: 1.9.3 (December 2025)
- **iOS Status**: STABLE since 1.8.0 (May 2025)
- **Web Status**: Beta since 1.9.0
- **Requirement**: Kotlin 2.1.0+ (K2 compiler only)
- **Source**: [Compose Multiplatform 1.8.0 Release](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/)

### 3. Room KMP
- **Current Stable**: 2.7.1
- **Status**: First stable KMP release was 2.7.0 (April 2025)
- **Platforms**: Android, iOS, JVM, macOS, Linux
- **Note**: All DAO functions for non-Android must be `suspend`
- **Source**: [Android Developers - Room KMP](https://developer.android.com/kotlin/multiplatform/room)

### 4. ViewModel & Lifecycle
- **Current Version**: 2.9.6
- **Status**: Official KMP support since 2.9.0 (May 2025)
- **Platforms**: JVM, Native (iOS, macOS), Web
- **Source**: [Android Developers - ViewModel KMP](https://developer.android.com/kotlin/multiplatform/viewmodel)

### 5. Navigation
- **Current Version**: 2.9.1
- **Artifact**: `org.jetbrains.androidx.navigation:navigation-compose`
- **Features**: Type-safe, deep linking, back gesture support
- **Recommendation**: Use official navigation over Voyager/Decompose for new projects
- **Source**: [Kotlin Docs - Navigation](https://kotlinlang.org/docs/multiplatform/compose-navigation.html)

### 6. Dependency Injection
| Library | Status | Best For |
|---------|--------|----------|
| Koin 4.1.0 | Recommended | KMP projects, simpler DSL, faster builds |
| kotlin-inject | Alternative | Dagger/Hilt migrants, compile-time safety |
| Kodein | Viable | Existing Kodein projects |

- **Source**: [Infinum - Koin vs kotlin-inject](https://infinum.com/blog/koin-vs-kotlin-inject-dependency-injection/)

### 7. Logging
| Library | Version | Maintainer | Status |
|---------|---------|------------|--------|
| Kermit | 2.0.4 | Touchlab | Recommended |
| Napier | 2.7.x | AAkira | Active |

- **Recommendation**: Kermit (better Crashlytics integration, more active maintenance)
- **Source**: [Kermit Documentation](https://kermit.touchlab.co/docs/)

### 8. Networking
- **Ktor Client**: 3.3.3 (latest)
- **Features**: WebRTC support, multipart improvements, Wasm support
- **Source**: [Ktor 3.3.0 Release](https://ktor.io/docs/whats-new-330.html)

### 9. Serialization
- **kotlinx.serialization**: 1.9.0 stable (1.10.0-RC available)
- **Note**: Based on Kotlin 2.3.0
- **Source**: [GitHub Releases](https://github.com/Kotlin/kotlinx.serialization/releases)

### 10. Image Loading
| Library | Version | Status |
|---------|---------|--------|
| Coil 3 | 3.3.0 | Recommended (mature, full KMP) |
| Kamel | 1.0.8 | Alternative (Ktor-focused) |

- **Source**: [Coil GitHub](https://github.com/coil-kt/coil)

### 11. Preferences/Settings
| Library | Version | Status |
|---------|---------|--------|
| DataStore | 1.2.0 | Recommended (official, KMP stable) |
| multiplatform-settings | Latest | Alternative (platform interop) |

- **Source**: [Android Developers - DataStore KMP](https://developer.android.com/kotlin/multiplatform/datastore)

### 12. Date/Time
- **kotlinx-datetime**: 0.7.1
- **Note**: 0.7.0 had breaking changes (Instant moved to kotlin.time)
- **Source**: [GitHub Releases](https://github.com/Kotlin/kotlinx-datetime/releases)

### 13. Coroutines
- **kotlinx-coroutines**: 1.10.2
- **Features**: Flow.any/all/none operators, improved error handling
- **Source**: [GitHub Releases](https://github.com/Kotlin/kotlinx.coroutines/releases)

---

## AI SDKs for Kotlin

| Provider | Library | Version | Type |
|----------|---------|---------|------|
| OpenAI | aallam/openai-kotlin | 4.0.1 | Unofficial, mature KMP |
| Claude | xemantic/anthropic-sdk-kotlin | 0.25.2 | Unofficial KMP |
| Gemini | Firebase AI Logic | In progress | GSoC 2025 project |
| Multi-provider | tddworks/openai-kotlin | 0.2.3 | Supports OpenAI, Anthropic, Gemini |

**Sources:**
- [openai-kotlin](https://github.com/aallam/openai-kotlin)
- [anthropic-sdk-kotlin](https://github.com/xemantic/anthropic-sdk-kotlin)
- [GSoC 2025 - Gemini KMP](https://kotlinfoundation.org/news/gsoc-2025-gemini-vertex-firebase/)

---

## Analytics & Crash Reporting

### Firebase Status
- **Official KMP Support**: NO
- **Community Options**: GitLive Firebase Kotlin SDK (partial support)

### Recommended Alternatives
| Service | Type | KMP Support |
|---------|------|-------------|
| Amplitude | Analytics | Yes |
| Sentry | Crash Reporting | Yes |
| Mixpanel | Analytics | Yes |
| Posthog | Analytics | Yes |

**Source**: [Firebase UserVoice - KMP Request](https://firebase.uservoice.com/forums/948424-general/suggestions/46591717-support-kotlin-multiplatform-kmp-in-the-sdks)

---

## PDF & OCR

### PDF Parsing
- **No true KMP library exists**
- **Solutions**:
  - pdfmp - Chromium pdfium wrapper (viewing)
  - compose-pdf - KMP PDF viewing
  - expect/actual with PDFBox (JVM) + native APIs

### OCR
| Platform | Library | Version |
|----------|---------|---------|
| Android | Google ML Kit | text-recognition:16.0.0 |
| iOS | Vision Framework | Native |

- **Integration**: Use expect/actual pattern
- **Source**: [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition/v2)

---

## Recommended libs.versions.toml

```toml
[versions]
kotlin = "2.3.0"
compose-multiplatform = "1.9.3"
agp = "8.7.3"

# AndroidX / Jetpack KMP
room = "2.7.1"
datastore = "1.2.0"
lifecycle = "2.9.6"
navigation = "2.9.1"

# Kotlin Libraries
coroutines = "1.10.2"
serialization = "1.9.0"
datetime = "0.7.1"

# Networking
ktor = "3.3.3"

# DI
koin = "4.1.0"

# Images
coil = "3.3.0"

# Logging
kermit = "2.0.4"

# AI
openai-kotlin = "4.0.1"
anthropic-kotlin = "0.25.2"
```

---

## Community Sentiment

### Positive
- "Compose Multiplatform for iOS is now production-ready" - JetBrains
- "Room KMP has been a game changer for cross-platform database code" - Reddit
- "Official ViewModel KMP means no more abstractions" - Android Developers

### Concerns
- Firebase lack of official KMP support is frustrating for teams already using Firebase
- PDF parsing remains a gap in the KMP ecosystem
- Some developers prefer Voyager/Decompose navigation for more control

---

## Sources

| # | Source | Type | Credibility |
|---|--------|------|-------------|
| 1 | [Kotlin 2.3.0 Release](https://blog.jetbrains.com/kotlin/2025/12/kotlin-2-3-0-released/) | Official | 0.95 |
| 2 | [Compose MP 1.8.0](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released/) | Official | 0.95 |
| 3 | [Compose MP 1.9.0](https://blog.jetbrains.com/kotlin/2025/09/compose-multiplatform-1-9-0-compose-for-web-beta/) | Official | 0.95 |
| 4 | [Room KMP Setup](https://developer.android.com/kotlin/multiplatform/room) | Official | 0.95 |
| 5 | [ViewModel KMP](https://developer.android.com/kotlin/multiplatform/viewmodel) | Official | 0.95 |
| 6 | [Navigation KMP](https://kotlinlang.org/docs/multiplatform/compose-navigation.html) | Official | 0.95 |
| 7 | [Koin vs kotlin-inject](https://infinum.com/blog/koin-vs-kotlin-inject-dependency-injection/) | Blog | 0.85 |
| 8 | [Ktor 3.3.0](https://ktor.io/docs/whats-new-330.html) | Official | 0.95 |
| 9 | [openai-kotlin](https://github.com/aallam/openai-kotlin) | GitHub | 0.85 |
| 10 | [anthropic-sdk-kotlin](https://github.com/xemantic/anthropic-sdk-kotlin) | GitHub | 0.85 |
| 11 | [Kermit Docs](https://kermit.touchlab.co/docs/) | Official | 0.90 |
| 12 | [DataStore KMP](https://developer.android.com/kotlin/multiplatform/datastore) | Official | 0.95 |

---

## Research Methodology

- **Queries used**: 18 decomposed sub-questions
- **Search sources**: JetBrains Blog, Android Developers, GitHub, Reddit, Slack Kotlin, Medium
- **Sources found**: 30+
- **Sources used**: 25 (after quality filter)
- **Research date**: 2025-12-29

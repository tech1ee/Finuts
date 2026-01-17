# ADR 002: SOLID - Convert Singletons to Injectable Classes

**Status:** Accepted
**Date:** 2026-01-17
**Context:** Tier 1.3 - SOLID Violations Fix

---

## Context and Problem Statement

**Current State:** 4 utility classes implemented as Kotlin `object` singletons:
1. `FormatDetector` - **CONVERTED** ✅ (was object, now class - 1 usage)
2. `NumberParser` - object (6 usages in 4 files)
3. `DateParser` - object (2 usages in 2 files)
4. `MerchantNormalizer` - object (3 usages in 2 files)

**Total usages to update:** 12 (1 already done)

**Problem:** Singletons violate **Dependency Inversion Principle (DIP)**:
- Cannot inject via Koin DI
- Hard to test (cannot mock/fake)
- Tight coupling to global state
- Makes testing dependent code difficult

---

## Decision Drivers

1. **Testability:** Need to mock/fake these utilities in unit tests
2. **DI Compliance:** CLAUDE.md requires proper dependency injection
3. **SOLID Principles:** Follow industry best practices
4. **Minimal Impact:** Keep changes small and safe

---

## Decision

**Convert all 4 singletons to regular classes** and inject via constructor.

### Conversion Strategy

**Before:**
```kotlin
object FormatDetector {
    fun detect(filename: String, bytes: ByteArray): DocumentType { ... }
}

// Usage
val type = FormatDetector.detect(name, data)
```

**After:**
```kotlin
class FormatDetector {
    fun detect(filename: String, bytes: ByteArray): DocumentType { ... }
}

// Koin module
single { FormatDetector() }

// Usage (injected via constructor)
class ImportFileProcessor(
    private val formatDetector: FormatDetector
) {
    val type = formatDetector.detect(name, data)
}
```

---

## Implementation Plan

### Phase 1: FormatDetector ✅ DONE

**Status:** Object → Class conversion COMPLETE
**File:** `shared/src/commonMain/kotlin/com/finuts/data/import/FormatDetector.kt`
**Change:** `object FormatDetector` → `class FormatDetector` ✅

**Remaining work:**
1. Update 1 usage in `ImportFileProcessor.kt:28`
2. Register in Koin DI module

### Phase 2: NumberParser (6 usages in 4 files)

**File:** `shared/src/commonMain/kotlin/com/finuts/data/import/utils/NumberParser.kt:13`
**Current:** `object NumberParser { ... }`
**Target:** `class NumberParser { ... }`

**Usages to update:**
1. `CsvParser.kt:174` - `NumberParser.parse(amountStr.trim(), NumberLocale.AUTO)`
2. `CsvParser.kt:180` - `NumberParser.parse(amountStr.trim(), NumberLocale.AUTO)`
3. `CsvParser.kt:192` - `NumberParser.parse(it, NumberLocale.AUTO)`
4. `OfxParser.kt:138` - `NumberParser.parse(amountStr.trim(), NumberLocale.US)`
5. `QifParser.kt:137` - `NumberParser.parse(amountStr.trim(), NumberLocale.AUTO)`
6. `BankStatementParser.kt:136` - `NumberParser.parse(cleaned, NumberLocale.AUTO)`

**Injection targets:**
- `CsvParser` (add `numberParser: NumberParser` to constructor)
- `OfxParser` (add `numberParser: NumberParser` to constructor)
- `QifParser` (add `numberParser: NumberParser` to constructor)
- `BankStatementParser` (add `numberParser: NumberParser` to constructor)

### Phase 3: DateParser (2 usages in 2 files)

**File:** `shared/src/commonMain/kotlin/com/finuts/data/import/utils/DateParser.kt:9`
**Current:** `object DateParser { ... }`
**Target:** `class DateParser { ... }`

**Usages to update:**
1. `CsvParser.kt:174` - `DateParser.parse(dateStr.trim())`
2. `BankStatementParser.kt:123` - `DateParser.parseOrNull(dateStr.trim(), DateFormat.AUTO)`

**Injection targets:**
- `CsvParser` (add `dateParser: DateParser` to constructor)
- `BankStatementParser` (add `dateParser: DateParser` to constructor)

### Phase 4: MerchantNormalizer (3 usages in 2 files)

**File:** `shared/src/commonMain/kotlin/com/finuts/data/categorization/MerchantNormalizer.kt:16`
**Current:** `object MerchantNormalizer { ... }`
**Target:** `class MerchantNormalizer { ... }`

**Usages to update:**
1. `LearnedMerchantRepositoryImpl.kt:31` - `MerchantNormalizer.normalize(description)`
2. `LearnFromCorrectionUseCase.kt:82` - `MerchantNormalizer.normalize(merchantName)`
3. `LearnFromCorrectionUseCase.kt:131` - `MerchantNormalizer.toPattern(normalizedMerchant)`

**Injection targets:**
- `LearnedMerchantRepositoryImpl` (add `merchantNormalizer: MerchantNormalizer` to constructor)
- `LearnFromCorrectionUseCase` (add `merchantNormalizer: MerchantNormalizer` to constructor)

---

## Testing Strategy

**For each singleton:**

1. **Keep existing behavior** - no logic changes
2. **Add constructor injection tests:**
   ```kotlin
   @Test
   fun `can inject FormatDetector via constructor`() {
       val detector = FormatDetector()
       val result = detector.detect("test.pdf", byteArrayOf())
       // Assert behavior unchanged
   }
   ```
3. **Test mockability:**
   ```kotlin
   class FakeFormatDetector : FormatDetector() {
       override fun detect(...) = DocumentType.PDF
   }
   ```

---

## Consequences

### Positive ✅

- **Testable:** Can mock/fake utilities in tests
- **SOLID Compliant:** Proper Dependency Inversion
- **DI Integrated:** All dependencies managed by Koin
- **Flexible:** Easy to swap implementations
- **Maintainable:** Clear dependency graph

### Negative ⚠️

- **Verbosity:** Slightly more code (constructor params)
- **Migration Effort:** Need to update 13 usages

### Neutral

- **Performance:** No impact (Koin `single` creates once)
- **Memory:** No impact (same as object singleton)

---

## Validation Criteria

**Success = ALL of these true:**

- ✅ 0 `object` utility classes remaining
- ✅ All 4 classes registered in Koin modules
- ✅ All 13 usages updated to inject via constructor
- ✅ All existing tests pass
- ✅ New injection tests added
- ✅ Detekt clean (0 violations)

---

## Koin DI Module Registration

All 4 utilities will be registered in `shared/src/commonMain/kotlin/com/finuts/core/di/DataModule.kt`:

```kotlin
val dataModule = module {
    // Utilities
    single { FormatDetector() }
    single { NumberParser() }
    single { DateParser() }
    single { MerchantNormalizer() }

    // Parsers (now with injected utilities)
    single {
        CsvParser(
            numberParser = get(),
            dateParser = get()
        )
    }
    single {
        OfxParser(
            numberParser = get()
        )
    }
    single {
        QifParser(
            numberParser = get()
        )
    }
    single {
        BankStatementParser(
            numberParser = get(),
            dateParser = get()
        )
    }
    single {
        ImportFileProcessor(
            csvParser = get(),
            ofxParser = get(),
            qifParser = get(),
            pdfParser = getOrNull(),
            formatDetector = get()
        )
    }

    // Repositories (now with injected utilities)
    single<LearnedMerchantRepository> {
        LearnedMerchantRepositoryImpl(
            dao = get(),
            merchantNormalizer = get()
        )
    }

    // Use Cases (now with injected utilities)
    factory {
        LearnFromCorrectionUseCase(
            correctionRepository = get(),
            merchantRepository = get(),
            merchantNormalizer = get()
        )
    }
}
```

---

## Implementation Order

**Prioritized by complexity (easiest first):**

1. **FormatDetector** ✅ DONE (object → class conversion complete)
   - Remaining: Update 1 usage + Koin registration - 10 min
2. **MerchantNormalizer** (3 usages in 2 files) - 20 min
3. **DateParser** (2 usages in 2 files) - 15 min
4. **NumberParser** (6 usages in 4 files) - 25 min

**Total estimated:** ~70 minutes remaining (20 min already spent on FormatDetector conversion)

---

## Detailed Implementation Steps

### Step 1: Complete FormatDetector (10 min)

1. Update `ImportFileProcessor.kt` constructor to inject `formatDetector`
2. Change line 28 from `FormatDetector.detect(...)` to `formatDetector.detect(...)`
3. Add `single { FormatDetector() }` to DataModule
4. Update `ImportFileProcessor` registration in DataModule to inject `formatDetector = get()`
5. Verify no compilation errors

### Step 2: MerchantNormalizer (20 min)

1. Change `object MerchantNormalizer` → `class MerchantNormalizer` in MerchantNormalizer.kt:16
2. Update `LearnedMerchantRepositoryImpl` constructor to inject `merchantNormalizer`
3. Change line 31 from `MerchantNormalizer.normalize(...)` to `merchantNormalizer.normalize(...)`
4. Update `LearnFromCorrectionUseCase` constructor to inject `merchantNormalizer`
5. Change lines 82, 131 from `MerchantNormalizer.*` to `merchantNormalizer.*`
6. Add `single { MerchantNormalizer() }` to DataModule
7. Update repository and use case registrations in DataModule
8. Verify no compilation errors

### Step 3: DateParser (15 min)

1. Change `object DateParser` → `class DateParser` in DateParser.kt:9
2. Update `CsvParser` constructor to inject `dateParser`
3. Change line 174 from `DateParser.parse(...)` to `dateParser.parse(...)`
4. Update `BankStatementParser` constructor to inject `dateParser`
5. Change line 123 from `DateParser.parseOrNull(...)` to `dateParser.parseOrNull(...)`
6. Add `single { DateParser() }` to DataModule
7. Update parser registrations in DataModule
8. Verify no compilation errors

### Step 4: NumberParser (25 min)

1. Change `object NumberParser` → `class NumberParser` in NumberParser.kt:13
2. Update `CsvParser` constructor to inject `numberParser`
3. Change lines 174, 180, 192 from `NumberParser.parse(...)` to `numberParser.parse(...)`
4. Update `OfxParser` constructor to inject `numberParser`
5. Change line 138 from `NumberParser.parse(...)` to `numberParser.parse(...)`
6. Update `QifParser` constructor to inject `numberParser`
7. Change line 137 from `NumberParser.parse(...)` to `numberParser.parse(...)`
8. Update `BankStatementParser` constructor to inject `numberParser`
9. Change line 136 from `NumberParser.parse(...)` to `numberParser.parse(...)`
10. Add `single { NumberParser() }` to DataModule
11. Update all parser registrations in DataModule
12. Verify no compilation errors

---

**Next Action:** Complete FormatDetector injection (Step 1)

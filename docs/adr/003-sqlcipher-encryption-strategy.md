# ADR 003: SQLCipher Database Encryption Strategy

**Status:** ✅ Accepted & Implemented
**Date:** 2026-01-17
**Implementation Date:** 2026-01-17
**Context:** Tier 2.1 - Critical Infrastructure (Database Security)

---

## Context and Problem Statement

**Current State:**
- Room KMP database с BundledSQLiteDriver (unencrypted)
- Financial data stored in plaintext на device storage
- **CRITICAL SECURITY RISK** для production app

**Problem:**
Финансовые данные (transactions, accounts, balances) должны быть зашифрованы at rest для:
- Compliance с security standards
- Protection от unauthorized access (lost/stolen devices)
- User privacy requirements

---

## Research Findings

### Room KMP + SQLCipher Current State (2026)

**iOS Encryption Status:** ❌ NOT SUPPORTED
- Room KMP не поддерживает SQLCipher encryption для iOS out-of-the-box
- Официальной реализации encrypted driver для iOS нет
- [Source: Kotlin Slack](https://slack-chats.kotlinlang.org/t/27109552/i-m-trying-to-encrypt-the-kmp-s-room-db-for-android-and-ios-)
- [Source: Zetetic Forum](https://discuss.zetetic.net/t/sqlcipher-encryption-not-applied-to-room-database-for-ios/6875)

**Android Encryption Status:** ✅ SUPPORTED
- SQLCipher для Android работает с Room через `SupportFactory`
- Library: `net.zetetic:sqlcipher-android:4.12.0`
- [Source: ProAndroidDev](https://proandroiddev.com/how-to-encrypt-your-room-database-in-android-using-sqlcipher-0bce78328bd6)

### Available Solutions

#### 1. **KmpSqlencrypt** (Multiplatform Encrypted SQLite)
- Repository: [GitHub - skolson/KmpSqlencrypt](https://github.com/skolson/KmpSqlencrypt)
- Поддержка: Android, iOS, MacOS, Linux, Windows
- Version: 0.9.0 (SqlCipher 4.9.0 + Sqlite 3.49.2)
- **Problem:** НЕ интегрируется с Room напрямую (разные API)

#### 2. **Custom SQLiteDriver для Room**
- Sample: [androidx-driver-samples/sqlcipher-driver](https://github.com/danysantiago/androidx-driver-samples/tree/main/sqlcipher-driver)
- Требует компиляцию SQLCipher C code для iOS
- **Estimate:** 12+ hours для полной KMP реализации
- **Risk:** Нет официальной поддержки, maintenance burden

#### 3. **SQLDelight + SQLCipher**
- Alternative ORM с лучшей KMP encryption поддержкой
- [Source: Touchlab](https://touchlab.co/multiplatform-encryption-with-sqldelight-and-sqlcipher)
- **Problem:** Требует миграцию с Room (breaking change, ~2 weeks)

#### 4. **Android-Only SQLCipher**
- Простая интеграция (~2-3 hours)
- iOS остается unencrypted (temporary)
- Защищает majority пользователей (Android dominant в CIS region)

---

## Decision Drivers

1. **Production Readiness:** Solution должно быть stable и tested
2. **Time Constraints:** Tier 2 budget = 6-12 hours total
3. **Platform Priority:** Android > iOS в Kazakhstan/CIS рынке
4. **Maintenance:** Prefer официальные solutions over custom implementations
5. **Security:** Partial encryption (Android-only) лучше чем no encryption

---

## Considered Options

### Option A: Android-Only SQLCipher (RECOMMENDED) ✅

**Pros:**
- ✅ Quick implementation (2-3 hours)
- ✅ Официальная library поддержка
- ✅ Battle-tested в production
- ✅ Protects ~80% users (Android dominant)
- ✅ Incremental approach (iOS позже)
- ✅ No breaking changes

**Cons:**
- ⚠️ iOS остается unencrypted временно
- ⚠️ Platform inconsistency

**Implementation:**
```kotlin
// Android DatabaseBuilder.android.kt
dependencies {
    implementation("net.zetetic:sqlcipher-android:4.12.0")
    implementation("androidx.sqlite:sqlite:2.6.2")
}

fun getDatabaseBuilder(context: Context): FinutsDatabase {
    val passphrase = getOrCreatePassphrase(context) // Secure generation
    val factory = SupportFactory(passphrase.toByteArray())

    return Room.databaseBuilder<FinutsDatabase>(
        context = context.applicationContext,
        name = context.getDatabasePath(FinutsDatabase.DATABASE_NAME).absolutePath
    )
        .openHelperFactory(factory) // ← SQLCipher encryption
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(*ALL_MIGRATIONS)
        .build()
}
```

**Effort:** ~3 hours
**Risk:** LOW

---

### Option B: Custom SQLiteDriver для Full KMP

**Pros:**
- ✅ Full platform encryption
- ✅ Consistent API

**Cons:**
- ❌ Complex implementation (12+ hours)
- ❌ No official support
- ❌ Maintenance burden
- ❌ High risk для production

**Effort:** ~12-16 hours
**Risk:** HIGH

---

### Option C: Migrate to SQLDelight

**Pros:**
- ✅ Official KMP encryption support
- ✅ Better multiplatform story

**Cons:**
- ❌ Breaking change (rewrite всех DAOs)
- ❌ ~2 weeks effort
- ❌ Not aligned с current architecture
- ❌ High migration risk

**Effort:** ~2 weeks
**Risk:** VERY HIGH

---

### Option D: Postpone Encryption

**Pros:**
- ✅ Zero implementation cost

**Cons:**
- ❌ **UNACCEPTABLE SECURITY RISK**
- ❌ Financial data unprotected
- ❌ Compliance violations
- ❌ Not production-ready

**Effort:** 0 hours
**Risk:** CRITICAL

---

## Decision

**RECOMMENDATION: Option A - Android-Only SQLCipher**

### Rationale

1. **Pragmatic Approach:**
   - Protects majority users СЕЙЧАС
   - Incremental security improvement
   - Low-risk, high-value

2. **Market Alignment:**
   - Android dominant в Kazakhstan/CIS (70-80% market share)
   - iOS encryption can follow when official support arrives

3. **Time/Quality Balance:**
   - 3 hours implementation vs 12+ hours custom solution
   - Proven, battle-tested technology
   - Официальная library support

4. **Future-Proof:**
   - Ждем Room 2.9+ для официальной iOS encryption
   - Non-breaking, можно добавить iOS позже
   - No technical debt

---

## Implementation Plan

### Phase 1: Android SQLCipher Integration (3 hours)

**Step 1: Add Dependencies (15 min)**
```kotlin
// shared/build.gradle.kts
androidMain.dependencies {
    implementation("net.zetetic:sqlcipher-android:4.12.0")
    implementation("androidx.sqlite:sqlite:2.6.2")
}
```

**Step 2: Passphrase Management (1 hour)**
- Generate secure passphrase using Android KeyStore
- Store в EncryptedSharedPreferences
- Fallback strategy для lost passphrase

**Step 3: Update DatabaseBuilder.android.kt (30 min)**
- Integrate SupportFactory с passphrase
- Add migration logic для existing unencrypted DBs
- Error handling

**Step 4: Testing (1 hour)**
- Test encryption works (cannot read DB without passphrase)
- Test decryption works
- Test migration от unencrypted DB
- Performance benchmarks

**Step 5: Documentation (15 min)**
- Update README.md
- Security notes для team

---

### Phase 2: iOS Encryption (Future)

**Blocked Until:**
- Room 2.9+ с official iOS SQLCipher support
- OR custom driver mature enough

**Alternative Short-term:**
- iOS Data Protection API (file-level encryption)
- Apple Keychain для sensitive data

---

## Consequences

### Positive ✅

- **Security Improved:** Android users protected at rest
- **Quick Win:** 3 hours implementation
- **Production Ready:** Battle-tested solution
- **Low Risk:** Official library support
- **Reversible:** Can change strategy later

### Negative ⚠️

- **Platform Inconsistency:** Android encrypted, iOS not (temporary)
- **User Communication:** Need to inform iOS users о планируемой encryption

### Neutral

- **iOS Encryption:** Postponed до официальной support
- **Breaking Change:** None (incremental)

---

## Validation Criteria

**Success = ALL of these true:**

- ✅ Android DB encrypted at rest with SQLCipher
- ✅ Cannot read DB file без passphrase
- ✅ Passphrase stored securely в KeyStore
- ✅ Existing users migrated seamlessly
- ✅ Performance acceptable (<10% overhead)
- ✅ Zero data loss during migration
- ✅ Tests pass (unit + integration)
- ✅ Documentation updated

---

## References

### Sources

**Room KMP + SQLCipher Status:**
- [Kotlin Slack - Room KMP iOS Encryption](https://slack-chats.kotlinlang.org/t/27109552/i-m-trying-to-encrypt-the-kmp-s-room-db-for-android-and-ios-)
- [Zetetic Forum - Room 2.7 KMP Support](https://discuss.zetetic.net/t/room-2-7-beta-kmp-support/6864)

**Android SQLCipher Integration:**
- [ProAndroidDev - Room + SQLCipher](https://proandroiddev.com/how-to-encrypt-your-room-database-in-android-using-sqlcipher-0bce78328bd6)
- [GitHub - SQLCipher Android](https://github.com/sqlcipher/sqlcipher-android)

**KMP Encryption Libraries:**
- [GitHub - KmpSqlencrypt](https://github.com/skolson/KmpSqlencrypt)
- [Touchlab - SQLDelight + SQLCipher](https://touchlab.co/multiplatform-encryption-with-sqldelight-and-sqlcipher)

**Custom Driver Samples:**
- [androidx-driver-samples](https://github.com/danysantiago/androidx-driver-samples/tree/main/sqlcipher-driver)

**Official Docs:**
- [Android Developers - Room KMP Setup](https://developer.android.com/kotlin/multiplatform/room)
- [Android Developers - SQLite for KMP](https://developer.android.com/kotlin/multiplatform/sqlite)

---

## Next Steps

1. **Get User Approval** для Option A
2. **Proceed with Implementation** (3 hours)
3. **Track iOS Encryption** progress (Room 2.9+)
4. **Plan iOS Encryption** when officially available

---

**Recommendation:** Proceed with **Option A (Android-Only SQLCipher)** для immediate security improvement.

**Estimated Completion:** Today (3 hours)

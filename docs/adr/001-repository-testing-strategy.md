# ADR 001: Repository Layer Testing Strategy

**Status:** Accepted
**Date:** 2026-01-17
**Deciders:** Technical Team
**Context:** Phase 1.1 - Repository Layer Testing

---

## Context and Problem Statement

Current state: 7 out of 8 repositories have NO tests (0% coverage).
- AccountRepositoryImpl - NO TESTS
- TransactionRepositoryImpl - NO TESTS
- BudgetRepositoryImpl - NO TESTS
- CategoryRepositoryImpl - NO TESTS
- CategoryCorrectionRepositoryImpl - NO TESTS
- LearnedMerchantRepositoryImpl - NO TESTS
- PreferencesRepositoryImpl - NO TESTS

Target: 70%+ test coverage for Data Layer (currently 55%)

**Problem:** How to effectively test Room-based repositories in Kotlin Multiplatform?

---

## Decision Drivers

- **Quality:** Need high confidence in data layer correctness
- **Multiplatform:** Tests must work on Android, iOS, and JVM
- **Maintainability:** Tests should be easy to write and understand
- **Coverage:** Must achieve 70%+ coverage target
- **TDD:** Following CLAUDE.md requirement for test-first development
- **Best Practices:** Follow industry standards for Room + KMP testing

---

## Considered Options

### Option 1: Mock DAOs
**Pros:**
- Fast test execution
- No database setup needed
- Easy to control behavior

**Cons:**
- Doesn't test real Room integration
- Won't catch SQL query errors
- Won't catch mapper errors
- Less confidence in correctness

### Option 2: In-Memory Room Database (CHOSEN)
**Pros:**
- Tests real Room integration
- Catches SQL errors and mapper bugs
- High confidence in correctness
- Tests actual Flow behavior
- Industry best practice for Room

**Cons:**
- Slightly slower than mocks
- Requires platform-specific setup (expect/actual)
- More complex initial setup

### Option 3: Fake Implementations Only
**Pros:**
- Simple to implement
- Fast execution
- Already have some fakes

**Cons:**
- Fakes need their own tests (already done)
- Doesn't test real repositories
- Duplicate effort (fake + real)

---

## Decision Outcome

**Chosen:** Option 2 - In-Memory Room Database

**Rationale:**
- Maximum confidence in data layer correctness
- Tests real Room SQL queries, DAOs, and mappers
- Industry best practice for Repository + Room testing
- Aligns with CLAUDE.md requirement for "real work" (no simulations)
- Catches integration bugs that mocks would miss

---

## Implementation Strategy

### 1. Test Database Setup (expect/actual)

**Structure:**
```
shared/src/
├── commonTest/kotlin/
│   └── com/finuts/test/database/
│       ├── TestDatabaseBuilder.kt (expect)
│       └── BaseDatabaseTest.kt
├── androidTest/kotlin/
│   └── com/finuts/test/database/
│       └── TestDatabaseBuilder.android.kt (actual)
└── iosTest/kotlin/
    └── com/finuts/test/database/
        └── TestDatabaseBuilder.ios.kt (actual)
```

**TestDatabaseBuilder.kt (commonTest)**:
```kotlin
expect fun getTestDatabase(): FinutsDatabase
```

**TestDatabaseBuilder.android.kt**:
```kotlin
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

actual fun getTestDatabase(): FinutsDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room.inMemoryDatabaseBuilder(context, FinutsDatabase::class.java)
        .allowMainThreadQueries() // Only for tests
        .build()
}
```

**TestDatabaseBuilder.ios.kt**:
```kotlin
import androidx.room.Room
import platform.Foundation.NSHomeDirectory

actual fun getTestDatabase(): FinutsDatabase {
    val dbBuilder = Room.inMemoryDatabaseBuilder<FinutsDatabase>()
    return dbBuilder.build()
}
```

### 2. Base Test Class

**BaseDatabaseTest.kt (commonTest)**:
```kotlin
abstract class BaseDatabaseTest : BaseTest() {
    protected lateinit var database: FinutsDatabase

    @BeforeTest
    override fun setUp() {
        super.setUp()
        database = getTestDatabase()
    }

    @AfterTest
    override fun tearDown() {
        database.close()
        super.tearDown()
    }

    protected suspend fun clearAllTables() {
        database.clearAllTables()
    }
}
```

### 3. Test Structure for Each Repository

**Pattern (AAA - Arrange, Act, Assert):**

```kotlin
class AccountRepositoryImplTest : BaseDatabaseTest() {
    private lateinit var accountDao: AccountDao
    private lateinit var repository: AccountRepositoryImpl

    @BeforeTest
    override fun setUp() {
        super.setUp()
        accountDao = database.accountDao()
        repository = AccountRepositoryImpl(accountDao)
    }

    @Test
    fun `createAccount inserts account successfully`() = runTest {
        // Arrange
        val account = TestData.account(id = "test-1", name = "Test Account")

        // Act
        repository.createAccount(account)

        // Assert
        repository.getAccountById("test-1").test {
            val result = awaitItem()
            assertEquals("Test Account", result?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllAccounts returns all accounts as Flow`() = runTest {
        // Arrange
        val account1 = TestData.account(id = "1", name = "Account 1")
        val account2 = TestData.account(id = "2", name = "Account 2")

        // Act & Assert
        repository.getAllAccounts().test {
            // Initial empty state
            assertEquals(emptyList(), awaitItem())

            repository.createAccount(account1)
            assertEquals(1, awaitItem().size)

            repository.createAccount(account2)
            val accounts = awaitItem()
            assertEquals(2, accounts.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateAccount modifies existing account`() = runTest {
        // Arrange
        val account = TestData.account(id = "1", name = "Original")
        repository.createAccount(account)

        // Act
        val updated = account.copy(name = "Updated")
        repository.updateAccount(updated)

        // Assert
        repository.getAccountById("1").test {
            assertEquals("Updated", awaitItem()?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteAccount removes account`() = runTest {
        // Arrange
        val account = TestData.account(id = "1")
        repository.createAccount(account)

        // Act
        repository.deleteAccount("1")

        // Assert
        repository.getAllAccounts().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `archiveAccount sets isArchived flag`() = runTest {
        // Arrange
        val account = TestData.account(id = "1", isArchived = false)
        repository.createAccount(account)

        // Act
        repository.archiveAccount("1")

        // Assert
        repository.getAccountById("1").test {
            assertEquals(true, awaitItem()?.isArchived)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getActiveAccounts excludes archived accounts`() = runTest {
        // Arrange
        val active = TestData.account(id = "1", isArchived = false)
        val archived = TestData.account(id = "2", isArchived = true)
        repository.createAccount(active)
        repository.createAccount(archived)

        // Act & Assert
        repository.getActiveAccounts().test {
            val accounts = awaitItem()
            assertEquals(1, accounts.size)
            assertEquals("1", accounts.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### 4. Test Coverage Requirements

| Repository | Methods | Target Coverage | Priority |
|------------|---------|-----------------|----------|
| AccountRepositoryImpl | 6 | 100% | P0 |
| TransactionRepositoryImpl | 8 | 100% | P0 |
| BudgetRepositoryImpl | ~6 | 85% | P0 |
| CategoryRepositoryImpl | ~5 | 85% | P1 |
| CategoryCorrectionRepositoryImpl | ~4 | 70% | P1 |
| LearnedMerchantRepositoryImpl | ~4 | 70% | P1 |
| PreferencesRepositoryImpl | ~6 | 85% | P1 |

**Total Target:** 70%+ for Data Layer

### 5. Dependencies to Add

**shared/build.gradle.kts:**
```kotlin
commonTest {
    dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.turbine) // For Flow testing
        implementation(libs.androidx.room.runtime) // For Room testing support
    }
}

androidTest {
    dependencies {
        implementation(libs.androidx.test.runner)
        implementation(libs.androidx.test.core) // For ApplicationProvider
        implementation(libs.androidx.room.runtime)
    }
}
```

**versions.toml:**
```toml
[versions]
turbine = "1.0.0"
androidx-test = "1.5.0"

[libraries]
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidx-test" }
androidx-test-core = { module = "androidx.test:core", version.ref = "androidx-test" }
```

---

## Testing Tools

| Tool | Purpose | Usage |
|------|---------|-------|
| **kotlin.test** | Assertions and test annotations | `@Test`, `@BeforeTest`, `assertEquals()` |
| **kotlinx-coroutines-test** | Coroutine testing | `runTest`, `StandardTestDispatcher` |
| **Turbine** | Flow testing | `.test { }`, `awaitItem()`, `cancelAndIgnoreRemainingEvents()` |
| **Room In-Memory** | Database testing | Real Room integration tests |

---

## Testing Principles (CLAUDE.md Compliance)

1. **Test-First (TDD):** Write tests BEFORE implementation (already implemented, so write tests now)
2. **Real Work:** Use real Room database, not mocks/simulations
3. **No Fakes in Production:** Fakes only in test code
4. **100% Critical Paths:** All CRUD operations tested
5. **Coverage Target:** 70%+ for repositories
6. **AAA Pattern:** Arrange → Act → Assert
7. **Descriptive Names:** Test names describe expected behavior
8. **One Assertion per Test:** Focus on single behavior

---

## Test Naming Convention

```kotlin
@Test
fun `methodName expected behavior when condition`() = runTest {
    // Example: `getAllAccounts returns empty list when no accounts exist`
}
```

**Alternative:**
```kotlin
@Test
fun methodName_ExpectedBehavior_WhenCondition() = runTest {
    // Example: getAllAccounts_ReturnsEmptyList_WhenNoAccountsExist
}
```

**Chosen:** Backtick style (more readable in Kotlin)

---

## Success Metrics

**Pre-Implementation (Current):**
- Repository tests: 0
- Data layer coverage: 55%
- Tested repositories: 0/7 (0%)

**Post-Implementation (Target):**
- Repository tests: 40+ (6 tests per repository × 7 repositories)
- Data layer coverage: 70%+
- Tested repositories: 7/7 (100%)
- All tests green
- CI/CD enforces coverage

---

## Consequences

### Positive
- ✅ High confidence in data layer correctness
- ✅ Catch bugs early (SQL errors, mapper bugs, Flow issues)
- ✅ Easy to refactor with test safety net
- ✅ Documentation through tests (living specs)
- ✅ CI/CD can enforce coverage
- ✅ Aligns with industry best practices

### Negative
- ⚠️ Initial setup effort (expect/actual database builder)
- ⚠️ Tests slightly slower than pure mocks
- ⚠️ Need to understand Turbine API for Flow testing

### Mitigation
- Setup is one-time cost, reused for all tests
- In-memory DB is still fast (<100ms per test)
- Turbine has simple, intuitive API

---

## References

- [Room Testing Documentation - Android Developers](https://developer.android.com/training/data-storage/room/testing-db)
- [Testing Kotlin Flows - Android Developers](https://developer.android.com/kotlin/flow/test)
- [Turbine Documentation - Cash App](https://github.com/cashapp/turbine)
- [Room in KMP - AndroidX](https://developer.android.com/kotlin/multiplatform/room)

---

**Implementation Timeline:** Week 1 of Phase 1
**Next Steps:** Implement TestDatabaseBuilder (expect/actual) → BaseDatabaseTest → Repository tests

---

*Document created: January 17, 2026*
*Version: 1.0*
*Status: Accepted - Ready for implementation*

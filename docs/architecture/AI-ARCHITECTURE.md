# Finuts AI Architecture — Complete Implementation Guide

**Version:** 2.0
**Date:** 2026-01-09
**Status:** Ready for Implementation

---

## 1. Executive Summary

Finuts использует **5-уровневую AI систему** для максимальной автоматизации финансового менеджмента с приоритетом на приватность (local-first) и минимальные затраты (~$0.02/user/month).

### Ключевые принципы
- **Privacy-First**: 80% обработки локально
- **Cost-Optimized**: Каскад от бесплатного к платному
- **User Learning**: Система учится на корректировках
- **Minimal Friction**: Минимум действий от пользователя

---

## 2. AI Categorization Pipeline

### 2.1 Five-Tier Cascade

```
Transaction Input
       ↓
┌──────────────────────────────────────────────────────────────┐
│  TIER 0: User Learned Mappings                               │
│  ├── Source: User corrections                                │
│  ├── Accuracy: 99% (user-confirmed)                          │
│  ├── Cost: $0                                                │
│  └── Privacy: 🟢 100% local                                  │
└──────────────────────────────────────────────────────────────┘
       ↓ not found
┌──────────────────────────────────────────────────────────────┐
│  TIER 1: MerchantDatabase + RuleBasedCategorizer             │
│  ├── Source: 312 hardcoded KZ patterns                       │
│  ├── Accuracy: 88-92%                                        │
│  ├── Cost: $0                                                │
│  └── Privacy: 🟢 100% local                                  │
└──────────────────────────────────────────────────────────────┘
       ↓ not found
┌──────────────────────────────────────────────────────────────┐
│  TIER 1.5: On-Device ML (TFLite/CoreML)                      │
│  ├── Source: DistilBERT quantized INT8                       │
│  ├── Accuracy: 90-94%                                        │
│  ├── Cost: $0                                                │
│  └── Privacy: 🟢 100% local                                  │
└──────────────────────────────────────────────────────────────┘
       ↓ confidence < 0.7
┌──────────────────────────────────────────────────────────────┐
│  TIER 2: GPT-4o-mini / Claude Haiku                          │
│  ├── Source: Cloud LLM (anonymized data)                     │
│  ├── Accuracy: 95%                                           │
│  ├── Cost: $0.03/1K transactions                             │
│  └── Privacy: 🟡 Anonymized (PII removed)                    │
└──────────────────────────────────────────────────────────────┘
       ↓ complex case
┌──────────────────────────────────────────────────────────────┐
│  TIER 3: GPT-4o (Premium users only)                         │
│  ├── Source: Cloud LLM (anonymized data)                     │
│  ├── Accuracy: 98%                                           │
│  ├── Cost: $0.30/1K transactions                             │
│  └── Privacy: 🟡 Anonymized (PII removed)                    │
└──────────────────────────────────────────────────────────────┘
```

### 2.2 Current Implementation Status

| Tier | Status | Files |
|------|--------|-------|
| 0 | ❌ Not implemented | — |
| 1 | ✅ Done | `MerchantDatabase.kt`, `RuleBasedCategorizer.kt` |
| 1.5 | ❌ Not implemented | — |
| 2 | ⚠️ Code exists, disabled | `AICategorizer.kt` |
| 3 | ⚠️ Code exists, disabled | `AICategorizer.kt` |

---

## 3. Database Schema Extensions

### 3.1 New Tables Required

```sql
-- Tier 0: User corrections storage
CREATE TABLE category_corrections (
    id TEXT PRIMARY KEY,
    transaction_id TEXT NOT NULL,
    original_category_id TEXT,
    corrected_category_id TEXT NOT NULL,
    merchant_name TEXT,
    merchant_normalized TEXT,
    created_at INTEGER NOT NULL,
    FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

-- Tier 0: Learned merchant mappings
CREATE TABLE learned_merchants (
    id TEXT PRIMARY KEY,
    merchant_pattern TEXT NOT NULL,
    category_id TEXT NOT NULL,
    confidence REAL NOT NULL DEFAULT 0.9,
    source TEXT NOT NULL, -- 'user', 'ml', 'collaborative'
    sample_count INTEGER NOT NULL DEFAULT 1,
    last_used_at INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    UNIQUE(merchant_pattern)
);

-- Recurring transaction patterns
CREATE TABLE recurring_patterns (
    id TEXT PRIMARY KEY,
    merchant_pattern TEXT,
    expected_amount INTEGER NOT NULL,
    amount_variance REAL NOT NULL DEFAULT 0.1,
    frequency_days INTEGER NOT NULL,
    frequency_variance INTEGER NOT NULL DEFAULT 3,
    next_expected_date INTEGER,
    category_id TEXT,
    is_subscription INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'active',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- Indices for performance
CREATE INDEX idx_corrections_merchant ON category_corrections(merchant_normalized);
CREATE INDEX idx_learned_merchants_pattern ON learned_merchants(merchant_pattern);
CREATE INDEX idx_recurring_status ON recurring_patterns(status);
```

### 3.2 Migration Strategy

```kotlin
// Migration 3 → 4
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        // Category corrections table
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS category_corrections (
                id TEXT PRIMARY KEY NOT NULL,
                transaction_id TEXT NOT NULL,
                original_category_id TEXT,
                corrected_category_id TEXT NOT NULL,
                merchant_name TEXT,
                merchant_normalized TEXT,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
            )
        """)
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_corrections_merchant ON category_corrections(merchant_normalized)")

        // Learned merchants table
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS learned_merchants (
                id TEXT PRIMARY KEY NOT NULL,
                merchant_pattern TEXT NOT NULL UNIQUE,
                category_id TEXT NOT NULL,
                confidence REAL NOT NULL DEFAULT 0.9,
                source TEXT NOT NULL,
                sample_count INTEGER NOT NULL DEFAULT 1,
                last_used_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
        """)
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_learned_merchants_pattern ON learned_merchants(merchant_pattern)")

        // Recurring patterns table
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS recurring_patterns (
                id TEXT PRIMARY KEY NOT NULL,
                merchant_pattern TEXT,
                expected_amount INTEGER NOT NULL,
                amount_variance REAL NOT NULL DEFAULT 0.1,
                frequency_days INTEGER NOT NULL,
                frequency_variance INTEGER NOT NULL DEFAULT 3,
                next_expected_date INTEGER,
                category_id TEXT,
                is_subscription INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'active',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL
            )
        """)
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_recurring_status ON recurring_patterns(status)")
    }
}
```

---

## 4. Domain Layer

### 4.1 New Entities

```kotlin
// shared/src/commonMain/kotlin/com/finuts/domain/entity/

@Serializable
data class CategoryCorrection(
    val id: String,
    val transactionId: String,
    val originalCategoryId: String?,
    val correctedCategoryId: String,
    val merchantName: String?,
    val merchantNormalized: String?,
    val createdAt: Instant
)

@Serializable
data class LearnedMerchant(
    val id: String,
    val merchantPattern: String,
    val categoryId: String,
    val confidence: Float,
    val source: LearnedMerchantSource,
    val sampleCount: Int,
    val lastUsedAt: Instant,
    val createdAt: Instant
)

@Serializable
enum class LearnedMerchantSource {
    USER,           // From user corrections
    ML,             // From on-device ML
    COLLABORATIVE   // From similar users (future)
}

@Serializable
data class RecurringPattern(
    val id: String,
    val merchantPattern: String?,
    val expectedAmount: Long,
    val amountVariance: Float,
    val frequencyDays: Int,
    val frequencyVariance: Int,
    val nextExpectedDate: LocalDate?,
    val categoryId: String?,
    val isSubscription: Boolean,
    val status: RecurringStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class RecurringStatus {
    ACTIVE,
    PAUSED,
    CANCELLED
}
```

### 4.2 New Use Cases

```kotlin
// LearnFromCorrectionUseCase.kt
class LearnFromCorrectionUseCase(
    private val correctionRepository: CategoryCorrectionRepository,
    private val learnedMerchantRepository: LearnedMerchantRepository,
    private val merchantNormalizer: MerchantNormalizer
) {
    suspend fun learn(
        transactionId: String,
        originalCategoryId: String?,
        correctedCategoryId: String,
        merchantName: String?
    ): Result<Unit> {
        // 1. Save correction
        val normalized = merchantName?.let { merchantNormalizer.normalize(it) }
        val correction = CategoryCorrection(
            id = Uuid.random().toString(),
            transactionId = transactionId,
            originalCategoryId = originalCategoryId,
            correctedCategoryId = correctedCategoryId,
            merchantName = merchantName,
            merchantNormalized = normalized,
            createdAt = Clock.System.now()
        )
        correctionRepository.save(correction)

        // 2. Check if we should create/update learned merchant
        if (normalized != null) {
            val existingCorrections = correctionRepository
                .findByNormalizedMerchant(normalized)
                .filter { it.correctedCategoryId == correctedCategoryId }

            if (existingCorrections.size >= 2) {
                // Create or update learned merchant
                val existing = learnedMerchantRepository.findByPattern(normalized)
                if (existing != null) {
                    learnedMerchantRepository.update(
                        existing.copy(
                            sampleCount = existing.sampleCount + 1,
                            confidence = calculateConfidence(existingCorrections.size),
                            lastUsedAt = Clock.System.now()
                        )
                    )
                } else {
                    learnedMerchantRepository.save(
                        LearnedMerchant(
                            id = Uuid.random().toString(),
                            merchantPattern = normalized,
                            categoryId = correctedCategoryId,
                            confidence = calculateConfidence(existingCorrections.size),
                            source = LearnedMerchantSource.USER,
                            sampleCount = existingCorrections.size,
                            lastUsedAt = Clock.System.now(),
                            createdAt = Clock.System.now()
                        )
                    )
                }
            }
        }

        return Result.success(Unit)
    }

    private fun calculateConfidence(sampleCount: Int): Float {
        return when {
            sampleCount >= 5 -> 0.95f
            sampleCount >= 3 -> 0.90f
            else -> 0.85f
        }
    }
}

// DetectRecurringUseCase.kt
class DetectRecurringUseCase(
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringPatternRepository
) {
    suspend fun detect(): List<RecurringPattern> {
        val transactions = transactionRepository.getAllTransactions().first()
        val grouped = groupByMerchantAndAmount(transactions)

        return grouped.mapNotNull { (key, txList) ->
            if (txList.size >= 3) {
                val frequency = detectFrequency(txList)
                if (frequency != null) {
                    RecurringPattern(
                        id = Uuid.random().toString(),
                        merchantPattern = key.merchant,
                        expectedAmount = key.amount,
                        amountVariance = calculateVariance(txList),
                        frequencyDays = frequency,
                        frequencyVariance = 3,
                        nextExpectedDate = predictNextDate(txList, frequency),
                        categoryId = txList.first().categoryId,
                        isSubscription = isLikelySubscription(txList),
                        status = RecurringStatus.ACTIVE,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )
                } else null
            } else null
        }
    }

    private fun detectFrequency(transactions: List<Transaction>): Int? {
        // Detect ~30 days (monthly), ~7 days (weekly), ~365 days (yearly)
        val sorted = transactions.sortedBy { it.date }
        val gaps = sorted.zipWithNext { a, b ->
            (b.date.toEpochMilliseconds() - a.date.toEpochMilliseconds()) / (24 * 60 * 60 * 1000)
        }

        val avgGap = gaps.average()
        return when {
            avgGap in 28.0..32.0 -> 30  // Monthly
            avgGap in 6.0..8.0 -> 7     // Weekly
            avgGap in 360.0..370.0 -> 365 // Yearly
            else -> null
        }
    }
}
```

---

## 5. On-Device ML (Tier 1.5)

### 5.1 Architecture

```kotlin
// shared/src/commonMain/kotlin/com/finuts/data/categorization/
expect class OnDeviceMLCategorizer {
    fun categorize(description: String): CategorizationResult?
    fun isModelLoaded(): Boolean
    suspend fun loadModel()
}

// shared/src/androidMain/kotlin/com/finuts/data/categorization/
actual class OnDeviceMLCategorizer(
    private val context: Context
) {
    private var interpreter: Interpreter? = null

    actual suspend fun loadModel() {
        val modelBuffer = context.assets.open("categorizer.tflite").use {
            it.readBytes().let { bytes ->
                ByteBuffer.allocateDirect(bytes.size).apply {
                    put(bytes)
                    rewind()
                }
            }
        }
        interpreter = Interpreter(modelBuffer)
    }

    actual fun categorize(description: String): CategorizationResult? {
        val input = tokenize(description)
        val output = Array(1) { FloatArray(15) } // 15 categories
        interpreter?.run(input, output) ?: return null

        val maxIdx = output[0].indices.maxByOrNull { output[0][it] } ?: return null
        val confidence = output[0][maxIdx]

        return if (confidence >= 0.7f) {
            CategorizationResult(
                transactionId = "",
                categoryId = CATEGORY_IDS[maxIdx],
                confidence = confidence,
                source = CategorizationSource.ON_DEVICE_ML
            )
        } else null
    }

    actual fun isModelLoaded(): Boolean = interpreter != null
}

// shared/src/iosMain/kotlin/com/finuts/data/categorization/
actual class OnDeviceMLCategorizer {
    private var model: MLModel? = null

    actual suspend fun loadModel() {
        val config = MLModelConfiguration()
        val url = NSBundle.mainBundle.URLForResource("categorizer", withExtension = "mlmodelc")
            ?: throw IllegalStateException("Model not found")
        model = MLModel.modelWithContentsOfURL(url, configuration = config, error = null)
    }

    actual fun categorize(description: String): CategorizationResult? {
        // CoreML implementation
        // ...
    }

    actual fun isModelLoaded(): Boolean = model != null
}
```

### 5.2 Model Training Pipeline

```
1. Data Collection:
   - MerchantDatabase patterns → synthetic training examples
   - User corrections (anonymized) → real examples
   - Public banking datasets (if available)

2. Model Architecture:
   - Base: DistilBERT (66M params)
   - Fine-tune on transaction descriptions
   - Output: 15 categories (matching Finuts)

3. Quantization:
   - FP32 → INT8 (TFLite Converter)
   - Size: ~60MB → ~15MB
   - Latency: <10ms on mobile

4. Export:
   - Android: TFLite (.tflite)
   - iOS: CoreML (.mlmodelc)

5. Integration:
   - Bundled with app (no download)
   - Lazy loading on first categorization
```

---

## 6. PII Anonymization

### 6.1 Implementation

```kotlin
// shared/src/commonMain/kotlin/com/finuts/data/privacy/
class PIIAnonymizer {
    private val patterns = listOf(
        // Kazakhstan phone numbers
        Regex("\\+?7[0-9]{10}") to PIIType.PHONE,
        // Card numbers (masked)
        Regex("\\*{4}\\s?\\d{4}") to PIIType.CARD,
        // IIN (Individual Identification Number)
        Regex("\\b\\d{12}\\b") to PIIType.IIN,
        // Russian/Kazakh names (Иванов А.С.)
        Regex("[А-ЯЁ][а-яё]+\\s[А-ЯЁ]\\.?[А-ЯЁ]?\\.?") to PIIType.NAME,
        // Email addresses
        Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") to PIIType.EMAIL
    )

    fun anonymize(text: String): AnonymizedText {
        var result = text
        val mapping = mutableMapOf<String, String>()

        patterns.forEach { (pattern, type) ->
            pattern.findAll(text).forEach { match ->
                val token = "[${type.name}_${mapping.size + 1}]"
                mapping[token] = match.value
                result = result.replace(match.value, token)
            }
        }

        return AnonymizedText(
            text = result,
            mapping = mapping.toMap()
        )
    }

    fun deanonymize(text: String, mapping: Map<String, String>): String {
        var result = text
        mapping.forEach { (token, original) ->
            result = result.replace(token, original)
        }
        return result
    }
}

enum class PIIType { PHONE, CARD, IIN, NAME, EMAIL }

data class AnonymizedText(
    val text: String,
    val mapping: Map<String, String>
)
```

### 6.2 Usage in LLM Calls

```kotlin
class LLMCategorizationService(
    private val anonymizer: PIIAnonymizer,
    private val openAIClient: OpenAIClient
) {
    suspend fun categorize(transactions: List<Transaction>): List<CategorizationResult> {
        // 1. Anonymize descriptions
        val anonymizedBatch = transactions.map { tx ->
            tx.id to anonymizer.anonymize(tx.description ?: "")
        }

        // 2. Build prompt with anonymized data
        val prompt = buildPrompt(anonymizedBatch.map { it.second.text })

        // 3. Call LLM
        val response = openAIClient.chat(prompt)

        // 4. Parse response (no deanonymization needed for categories)
        return parseResponse(response, transactions)
    }
}
```

---

## 7. UI/UX Design Patterns

### 7.1 Category Correction Flow

```
User sees transaction with AI category
       ↓
Tap on category chip (shows ✨ sparkle if AI)
       ↓
Category picker opens
       ↓
User selects correct category
       ↓
Toast: "Запомним для будущих транзакций"
       ↓
LearnFromCorrectionUseCase.learn() called
       ↓
Next time same merchant → Tier 0 matches
```

### 7.2 Confidence Indicator Rules

| Confidence | Visual | Label | Action |
|------------|--------|-------|--------|
| ≥ 85% | None | — | Auto-accept |
| 70-84% | 🟡 Badge | "Вероятно" | Show, easy edit |
| < 70% | 🔴 Badge | "Проверьте" | Highlight for review |

### 7.3 Subscription Card Design

```
┌──────────────────────────────────────────┐
│ [Logo] Netflix                    -₸3,990│
│        Ежемесячно • 15 янв              │
│        ⚠️ Пробный заканчивается         │
│                          [Заблокировать]│
└──────────────────────────────────────────┘
```

### 7.4 Natural Language Query Examples

| Query | Response |
|-------|----------|
| "Сколько на еду?" | "В этом месяце ₸45,320 на еду. Это на 12% больше, чем обычно." |
| "Мои подписки" | Shows subscription list |
| "Могу позволить iPhone?" | "При текущих тратах накопите за 4 месяца если откладывать ₸50K/мес" |

---

## 8. Implementation Roadmap

### Phase 1: User Learning (Iteration 21)

**Files to create:**
- `shared/src/commonMain/kotlin/com/finuts/data/local/entity/CategoryCorrectionEntity.kt`
- `shared/src/commonMain/kotlin/com/finuts/data/local/entity/LearnedMerchantEntity.kt`
- `shared/src/commonMain/kotlin/com/finuts/data/local/dao/CategoryCorrectionDao.kt`
- `shared/src/commonMain/kotlin/com/finuts/data/local/dao/LearnedMerchantDao.kt`
- `shared/src/commonMain/kotlin/com/finuts/domain/entity/CategoryCorrection.kt`
- `shared/src/commonMain/kotlin/com/finuts/domain/entity/LearnedMerchant.kt`
- `shared/src/commonMain/kotlin/com/finuts/domain/repository/CategoryCorrectionRepository.kt`
- `shared/src/commonMain/kotlin/com/finuts/domain/repository/LearnedMerchantRepository.kt`
- `shared/src/commonMain/kotlin/com/finuts/domain/usecase/LearnFromCorrectionUseCase.kt`
- `shared/src/commonMain/kotlin/com/finuts/data/categorization/MerchantNormalizer.kt`

**Files to modify:**
- `shared/src/commonMain/kotlin/com/finuts/data/local/FinutsDatabase.kt` - Add entities
- `shared/src/commonMain/kotlin/com/finuts/data/local/Migrations.kt` - Add Migration 3→4
- `shared/src/commonMain/kotlin/com/finuts/data/categorization/RuleBasedCategorizer.kt` - Add Tier 0 check
- `shared/src/commonMain/kotlin/com/finuts/core/di/CoreModule.kt` - Register new components
- `composeApp/src/commonMain/kotlin/com/finuts/app/feature/transactions/TransactionDetailViewModel.kt` - Add correction handler

**Tests:**
- `LearnFromCorrectionUseCaseTest.kt`
- `MerchantNormalizerTest.kt`
- `CategoryCorrectionRepositoryTest.kt`

### Phase 2: Recurring Detection (Iteration 22)

**Files to create:**
- `shared/src/commonMain/kotlin/com/finuts/data/local/entity/RecurringPatternEntity.kt`
- `shared/src/commonMain/kotlin/com/finuts/data/local/dao/RecurringPatternDao.kt`
- `shared/src/commonMain/kotlin/com/finuts/domain/entity/RecurringPattern.kt`
- `shared/src/commonMain/kotlin/com/finuts/domain/repository/RecurringPatternRepository.kt`
- `shared/src/commonMain/kotlin/com/finuts/domain/usecase/DetectRecurringUseCase.kt`
- `composeApp/src/commonMain/kotlin/com/finuts/app/feature/subscriptions/SubscriptionsScreen.kt`
- `composeApp/src/commonMain/kotlin/com/finuts/app/feature/subscriptions/SubscriptionsViewModel.kt`

### Phase 3: On-Device ML (Iteration 23-24)

**Files to create:**
- `shared/src/commonMain/kotlin/com/finuts/data/categorization/OnDeviceMLCategorizer.kt` (expect)
- `shared/src/androidMain/kotlin/com/finuts/data/categorization/OnDeviceMLCategorizer.kt` (actual)
- `shared/src/iosMain/kotlin/com/finuts/data/categorization/OnDeviceMLCategorizer.kt` (actual)
- Model files: `categorizer.tflite`, `categorizer.mlmodelc`

### Phase 4: PII Anonymization + Cloud AI (Iteration 25-26)

**Files to create:**
- `shared/src/commonMain/kotlin/com/finuts/data/privacy/PIIAnonymizer.kt`
- `shared/src/commonMain/kotlin/com/finuts/data/privacy/PIIPatterns.kt`
- `shared/src/commonMain/kotlin/com/finuts/data/categorization/LLMCategorizationService.kt`

**Files to modify:**
- `shared/src/commonMain/kotlin/com/finuts/core/di/CoreModule.kt` - Enable AI categorizer

---

## 9. Cost Analysis

### Per 1000 Active Users/Month

| Component | Transactions | Cost |
|-----------|--------------|------|
| Tier 0-1.5 (local) | 85% (~170K tx) | $0 |
| Tier 2 (GPT-4o-mini) | 12% (~24K tx) | $0.72 |
| Tier 3 (GPT-4o) | 3% (~6K tx) | $1.80 |
| NL Queries (2/user) | 2K queries | $0.06 |
| Weekly Insights | 1K digests | $0.15 |
| **Total** | — | **$2.73** |
| **Per User** | — | **$0.003** |

✅ Below PRD target of $0.01/user

---

## 10. Sources

### Industry Best Practices
- [Plaid Enrich API](https://plaid.com/products/enrich/) - 98% accuracy, 16+104 categories
- [Copilot Money](https://help.copilot.money/en/articles/8182433-copilot-intelligence-for-spending) - Personal ML per user
- [Subaio](https://subaio.com/subaio-explained/how-does-subaio-detect-recurring-payments) - 98.7% recurring detection
- [Revolut Subscriptions](https://www.revolut.com/news/revolut_launches_smart_subscriptions_feature/)
- [Cleo AI](https://kaopiz.com/en/articles/finance-ai-chatbots/) - 20x engagement

### Technical References
- [BERT for Transaction Categorization](https://towardsdatascience.com/categorize-free-text-bank-transaction-descriptions-using-bert-44c9cc87735b/)
- [TFLite on Mobile](https://www.bitcot.com/litert-on-device-ai-for-mobile-apps/)
- [PII Anonymization](https://tsh.io/blog/pii-anonymization-in-llm-projects/)

### UI/UX Research
- [Banking App UX 2025](https://www.wavespace.agency/blog/banking-app-ux)
- [Fintech UX Practices](https://procreator.design/blog/best-fintech-ux-practices-for-mobile-apps/)
- [Push Notification UX](https://uxcam.com/blog/push-notification-guide/)

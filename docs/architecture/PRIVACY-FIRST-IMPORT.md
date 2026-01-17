# Privacy-First Import Architecture

**Version:** 1.0
**Date:** 2026-01-11
**Status:** Implemented

---

## 1. Executive Summary

Finuts реализует **Privacy-First подход** к импорту финансовых документов:
- **100% локальный OCR** (Vision Framework на iOS, ML Kit на Android)
- **Обязательная анонимизация** перед отправкой в Cloud LLM
- **Reversible токенизация** для восстановления PII после обработки
- **Cloud LLM только для enhancement** (merchant extraction, categorization hints)

### Privacy Guarantees

| Гарантия | Реализация |
|----------|------------|
| Сырой OCR текст никогда не покидает устройство | Steps 1-4 полностью локальные |
| PII не отправляется в Cloud | Обязательная анонимизация перед cloud calls |
| Mapping хранится только в памяти | Не персистируется |
| GDPR-ready | By design |

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                 PRIVACY-FIRST PDF PARSING PIPELINE                           │
└─────────────────────────────────────────────────────────────────────────────┘

PDF/Image Document
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ STEP 1: TEXT EXTRACTION (100% Local)                                        │
│                                                                             │
│   iOS: Vision Framework (VNRecognizeTextRequest)                            │
│   Android: ML Kit Text Recognition                                          │
│                                                                             │
│   Files:                                                                    │
│   - shared/src/iosMain/.../OcrService.ios.kt                                │
│   - shared/src/iosMain/.../PdfTextExtractor.ios.kt                          │
│   - shared/src/androidMain/.../OcrService.android.kt                        │
│   - shared/src/androidMain/.../PdfTextExtractor.android.kt                  │
│                                                                             │
│   Output: raw_text                                                          │
│   Privacy: ✅ Nothing leaves device                                         │
└─────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ STEP 2: PREPROCESSING (100% Local)                                          │
│                                                                             │
│   DocumentPreprocessor:                                                     │
│   - Filter lines with dates/amounts                                         │
│   - Remove headers, footers, page numbers                                   │
│   - Detect document type (BANK_STATEMENT, RECEIPT, INVOICE)                 │
│   - Detect language (ru, kk, en)                                            │
│                                                                             │
│   File: shared/src/commonMain/.../DocumentPreprocessor.kt                   │
│                                                                             │
│   Token reduction: 50-70%                                                   │
│   Output: cleaned_text, DocumentHints                                       │
│   Privacy: ✅ Nothing leaves device                                         │
└─────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ STEP 3: LOCAL EXTRACTION (100% Local)                                       │
│                                                                             │
│   LocalTransactionExtractor:                                                │
│   - Extract dates (multiple formats)                                        │
│   - Extract amounts (signed, with decimals)                                 │
│   - Detect currencies (₸, $, €, ₽, £, ¥)                                   │
│   - Capture raw descriptions                                                │
│                                                                             │
│   File: shared/src/commonMain/.../LocalTransactionExtractor.kt              │
│                                                                             │
│   Accuracy: 80-85% (dates/amounts), 0% (merchants)                          │
│   Output: List<PartialTransaction>                                          │
│   Privacy: ✅ Nothing leaves device                                         │
└─────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ STEP 4: PII ANONYMIZATION (100% Local, MANDATORY)                           │
│                                                                             │
│   RegexPIIAnonymizer detects and replaces:                                  │
│   - Person names: Иванов А.С. → [PERSON_NAME_1]                             │
│   - IBAN: KZ123... → [IBAN_1]                                               │
│   - Phone: +7 777... → [PHONE_1]                                            │
│   - Email: test@mail.ru → [EMAIL_1]                                         │
│   - Card: 4532 1234... → [CARD_NUMBER_1]                                    │
│   - IIN: 123456789012 → [IIN_1]                                             │
│                                                                             │
│   File: shared/src/commonMain/.../ai/privacy/PIIAnonymizer.kt               │
│                                                                             │
│   PRESERVES (needed for parsing):                                           │
│   ✅ Dates                                                                  │
│   ✅ Amounts                                                                │
│   ✅ Currency symbols                                                       │
│   ✅ Merchant names (public entities)                                       │
│                                                                             │
│   Output: (anonymized_text, pii_mapping)                                    │
│   Privacy: ✅ PII stored only in memory                                     │
└─────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ STEP 5: CLOUD LLM ENHANCEMENT (Anonymized Data Only)                        │
│                                                                             │
│   CloudTransactionEnhancer:                                                 │
│   - Uses Claude Haiku or GPT-4o-mini                                        │
│   - Structured JSON output                                                  │
│   - Extracts: merchant, category hint, transaction type                     │
│   - Preserves placeholders in output                                        │
│                                                                             │
│   File: shared/src/commonMain/.../CloudTransactionEnhancer.kt               │
│                                                                             │
│   What LLM sees:                                                            │
│   "15.01.2026 -5000 KZT [PERSON_NAME_1] перевод на [IBAN_1]"               │
│                                                                             │
│   What LLM returns:                                                         │
│   {index: 0, counterpartyName: "[PERSON_NAME_1]", categoryHint: "transfers"}│
│                                                                             │
│   Cost: ~$0.005/document                                                    │
│   Privacy: ✅ Only anonymized data sent                                     │
└─────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ STEP 6: DE-ANONYMIZATION (100% Local)                                       │
│                                                                             │
│   RegexPIIAnonymizer.deanonymize():                                         │
│   - [PERSON_NAME_1] → Иванов А.С.                                           │
│   - [IBAN_1] → KZ123456789012345678                                         │
│                                                                             │
│   Output: List<ImportedTransaction> with full data restored                 │
│   Privacy: ✅ PII restored only on device                                   │
└─────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ FINAL OUTPUT: ImportedTransaction                                           │
│                                                                             │
│   ├── date: LocalDate                                                       │
│   ├── amount: Long (kopecks/cents)                                          │
│   ├── description: String (full, de-anonymized)                             │
│   ├── merchant: String? (extracted by LLM)                                  │
│   ├── category: String? (hint from LLM)                                     │
│   ├── confidence: Float (0.9 for LLM-enhanced)                              │
│   └── source: ImportSource.LLM_ENHANCED or DOCUMENT_AI                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Component Details

### 3.1 DocumentPreprocessor

**Purpose:** Reduce token count for Cloud LLM, detect document metadata.

**Location:** `shared/src/commonMain/kotlin/com/finuts/data/import/ocr/DocumentPreprocessor.kt`

```kotlin
class DocumentPreprocessor {
    fun process(rawText: String): PreprocessResult
}

data class PreprocessResult(
    val cleanedText: String,
    val hints: DocumentHints
)

data class DocumentHints(
    val type: DocumentType,  // BANK_STATEMENT, RECEIPT, INVOICE, UNKNOWN
    val language: String     // "ru", "kk", "en"
)
```

**Filters:**
- Lines with dates (DD.MM.YYYY, YYYY-MM-DD, etc.)
- Lines with amounts (signed numbers, currency symbols)
- Transaction keywords (перевод, оплата, payment, etc.)

**Removes:**
- Page numbers (- 1 -, Page 1 of 3)
- Headers/footers (www., confidential, etc.)

---

### 3.2 LocalTransactionExtractor

**Purpose:** Extract structured data from OCR text without cloud calls.

**Location:** `shared/src/commonMain/kotlin/com/finuts/data/import/ocr/LocalTransactionExtractor.kt`

```kotlin
class LocalTransactionExtractor {
    fun extract(text: String): List<PartialTransaction>
}

data class PartialTransaction(
    val rawDate: String,
    val amountMinorUnits: Long,
    val currency: String?,
    val rawDescription: String,
    val isCredit: Boolean,
    val isDebit: Boolean
)
```

**Supported Formats:**
- Kaspi: `- 3 700,00 ₸`
- Standard: `+5000.00`
- Currency: `$100` or `100$`

**Date Formats:**
- DD.MM.YYYY
- DD/MM/YY
- YYYY-MM-DD

---

### 3.3 RegexPIIAnonymizer

**Purpose:** Detect and replace PII with reversible placeholders.

**Location:** `shared/src/commonMain/kotlin/com/finuts/ai/privacy/PIIAnonymizer.kt`

```kotlin
interface PIIAnonymizer {
    fun anonymize(text: String): AnonymizationResult
    fun deanonymize(text: String, mapping: Map<String, String>): String
    fun detectPII(text: String): List<DetectedPII>
}

data class AnonymizationResult(
    val anonymizedText: String,
    val mapping: Map<String, String>,  // [PERSON_NAME_1] → "Иванов"
    val detectedPII: List<DetectedPII>,
    val wasModified: Boolean
)
```

**PII Patterns:**
| Type | Pattern Example | Placeholder |
|------|----------------|-------------|
| PERSON_NAME | Иванов А.С., John Smith | [PERSON_NAME_1] |
| IBAN | KZ12345..., RU12345... | [IBAN_1] |
| PHONE | +7 777 123 45 67, 87771234567 | [PHONE_1] |
| EMAIL | test@example.com | [EMAIL_1] |
| CARD_NUMBER | 4532 1234 5678 9012 | [CARD_NUMBER_1] |
| IIN | 123456789012 (12 digits) | [IIN_1] |

---

### 3.4 CloudTransactionEnhancer

**Purpose:** Enhance transactions with merchant/category info using Cloud LLM.

**Location:** `shared/src/commonMain/kotlin/com/finuts/data/import/ocr/CloudTransactionEnhancer.kt`

```kotlin
class CloudTransactionEnhancer(
    private val llmProvider: LLMProvider
) {
    suspend fun enhance(transactions: List<PartialTransaction>): List<EnhancedTransaction>
}

data class EnhancedTransaction(
    val rawDate: String,
    val amountMinorUnits: Long,
    val currency: String?,
    val rawDescription: String,
    val isCredit: Boolean,
    val isDebit: Boolean,
    val merchant: String?,
    val counterpartyName: String?,
    val categoryHint: String?,
    val transactionType: String?
)
```

**LLM Prompt Highlights:**
- Preserve placeholders like [PERSON_NAME_1]
- For P2P transfers: merchant is null, counterpartyName is the person
- For business payments: merchant is the business

**JSON Schema:**
```json
{
    "type": "array",
    "items": {
        "type": "object",
        "properties": {
            "index": {"type": "integer"},
            "merchant": {"type": ["string", "null"]},
            "counterpartyName": {"type": ["string", "null"]},
            "categoryHint": {"type": ["string", "null"]},
            "transactionType": {
                "enum": ["DEBIT", "CREDIT", "TRANSFER", "FEE", "INTEREST", "REFUND", null]
            }
        },
        "required": ["index"]
    }
}
```

---

### 3.5 PrivacyFirstPdfParser

**Purpose:** Orchestrate the complete privacy-first pipeline.

**Location:** `shared/src/commonMain/kotlin/com/finuts/data/import/ocr/PrivacyFirstPdfParser.kt`

```kotlin
class PrivacyFirstPdfParser(
    private val pdfExtractor: PdfTextExtractor,
    private val ocrService: OcrService,
    private val preprocessor: DocumentPreprocessor,
    private val localExtractor: LocalTransactionExtractor,
    private val anonymizer: PIIAnonymizer,
    private val cloudEnhancer: CloudTransactionEnhancer?
) {
    suspend fun parsePdf(pdfData: ByteArray, documentType: DocumentType.Pdf): ImportResult
    suspend fun parseImage(imageData: ByteArray, documentType: DocumentType.Image): ImportResult
}
```

---

## 4. Data Flow Example

### Input
```
Kaspi Bank
Выписка за период январь 2026

15.01.2026 - 3 700,00 ₸ Glovo оплата заказа
16.01.2026 - 5 000,00 ₸ Перевод Иванов А.С.
```

### After Preprocessing
```
15.01.2026 - 3 700,00 ₸ Glovo оплата заказа
16.01.2026 - 5 000,00 ₸ Перевод Иванов А.С.
```
Hints: type=BANK_STATEMENT, language=ru

### After Local Extraction
```kotlin
[
    PartialTransaction(rawDate="15.01.2026", amount=-370000, currency="KZT", description="Glovo оплата заказа"),
    PartialTransaction(rawDate="16.01.2026", amount=-500000, currency="KZT", description="Перевод Иванов А.С.")
]
```

### After Anonymization
```
15.01.2026 - 3 700,00 ₸ Glovo оплата заказа
16.01.2026 - 5 000,00 ₸ Перевод [PERSON_NAME_1]
```
Mapping: `[PERSON_NAME_1] → "Иванов А.С."`

### What LLM Sees
```
0: 15.01.2026 | -3700.00 KZT | Glovo оплата заказа
1: 16.01.2026 | -5000.00 KZT | Перевод [PERSON_NAME_1]
```

### LLM Response
```json
[
    {"index": 0, "merchant": "Glovo", "categoryHint": "food_delivery", "transactionType": "DEBIT"},
    {"index": 1, "counterpartyName": "[PERSON_NAME_1]", "categoryHint": "transfers", "transactionType": "TRANSFER"}
]
```

### After De-anonymization
```kotlin
[
    ImportedTransaction(date=2026-01-15, amount=-370000, merchant="Glovo", category="food_delivery"),
    ImportedTransaction(date=2026-01-16, amount=-500000, description="Перевод: Иванов А.С.", category="transfers")
]
```

---

## 5. Test Coverage

### Unit Tests

| Test Class | Coverage |
|------------|----------|
| `DocumentPreprocessorTest` | Token reduction, document type detection, language detection |
| `LocalTransactionExtractorTest` | Date parsing, amount parsing, currency detection |
| `CloudTransactionEnhancerTest` | LLM response parsing, error handling, batch processing |
| `PIIAnonymizerTest` | PII detection, anonymization, de-anonymization |
| `PrivacyFirstPdfParserTest` | Pipeline integration, PII handling, LLM fallback |
| `ImportWorkflowTest` | Full workflow, multi-currency, edge cases |

### Test Files Location
```
shared/src/commonTest/kotlin/com/finuts/data/import/
├── ImportWorkflowTest.kt
├── ocr/
│   ├── DocumentPreprocessorTest.kt
│   ├── LocalTransactionExtractorTest.kt
│   ├── CloudTransactionEnhancerTest.kt
│   └── PrivacyFirstPdfParserTest.kt
```

---

## 6. Cost Analysis

| Scenario | Tokens | Cost (Claude Haiku) |
|----------|--------|---------------------|
| 1-page statement | ~800 | ~$0.005 |
| 5-page statement | ~3000 | ~$0.02 |
| Receipt | ~200 | ~$0.002 |

**Per-user estimate:** $0.01-0.05/month (assuming 5-15 documents/month)

---

## 7. Future Improvements

### Phase 2: On-Device LLM (Optional)
- Use Cactus SDK or Apple Foundation Models for merchant extraction
- Fallback to Cloud only for complex cases
- Zero cloud cost for most documents

### Phase 3: Template Learning
- Learn from user-confirmed templates (Kaspi, Halyk, etc.)
- Pattern matching for known formats
- Reduce LLM calls by 80%

---

## 8. Related Documents

- [AI-ARCHITECTURE.md](./AI-ARCHITECTURE.md) — AI Categorization Pipeline
- [2026-01-05-universal-bank-import.md](../research/2026-01-05-universal-bank-import.md) — Research
- [OCR-RESEARCH-INDEX.md](../research/OCR-RESEARCH-INDEX.md) — OCR Research

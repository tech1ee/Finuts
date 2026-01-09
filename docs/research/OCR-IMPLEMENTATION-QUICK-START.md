# OCR Implementation Quick Start Guide

## Summary Decision

**Primary:** Tesseract4Android 4.9.0
**Alternative:** PaddleOCR v3.0.3 (ONNX Runtime)
**Not Suitable:** Google ML Kit v2 (no Cyrillic on-device)

---

## Why Tesseract4Android?

| Factor | Assessment |
|--------|-----------|
| Cyrillic Support | ✅ Full (Russian, Kazakh, Bulgarian, etc.) |
| On-Device Privacy | ✅ 100% (no external calls) |
| KMP Integration | ✅ Simple (expect/actual pattern) |
| Performance | ✅ Acceptable (100-220ms) |
| Bundle Size | ✅ Managed (download models on-demand) |
| Maintenance | ✅ Active (2025 releases) |
| Community | ✅ Large & established |
| Risk Level | ✅ LOW (proven technology) |

---

## Quick Facts

```
Latest Version: 4.9.0 (June 2025)
Distribution: JitPack.io (NOT Maven Central)
Maven ID: cz.adaptech.tesseract4android:tesseract4android-openmp:4.9.0
Minimum Android: API 21
Language Models: 4-20MB each (downloaded separately)
Processing Time: 100-220ms per image
Accuracy (Russian): 83%+ on bank statements
```

---

## Minimal Setup

### 1. Add JitPack Repository
```gradle
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add Dependency
```gradle
// shared/build.gradle.kts or build.gradle.kts
androidImplementation("cz.adaptech.tesseract4android:tesseract4android-openmp:4.9.0")
```

### 3. Create Expect/Actual Interface

**In commonMain:**
```kotlin
// domain/usecase/OcrEngine.kt
expect class OcrEngine {
    suspend fun recognizeText(imageBytes: ByteArray, language: String = "rus"): String
}
```

**In androidMain:**
```kotlin
// data/ocr/TesseractOcrEngine.kt
actual class OcrEngine(context: Context) {
    private var api: TessBaseAPI? = null

    init {
        api = TessBaseAPI()
        api?.init(getTessDataPath(context), "eng")
    }

    actual suspend fun recognizeText(imageBytes: ByteArray, language: String): String {
        return withContext(Dispatchers.Default) {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            api?.apply {
                setImage(bitmap)
                setLanguage(language) // "rus" or "eng+rus"
                return@withContext utf8Text
            } ?: ""
        }
    }

    private fun getTessDataPath(context: Context): String {
        return File(context.filesDir, "tessdata").apply { mkdirs() }.absolutePath
    }
}
```

**In iosMain:**
```kotlin
// data/ocr/VisionOcrEngine.kt
actual class OcrEngine(context: Context) {
    actual suspend fun recognizeText(imageBytes: ByteArray, language: String): String {
        // Use existing Vision Framework code
        return performVisionRecognition(imageBytes)
    }
}
```

---

## Language Files Management

### Option A: Download on First Run
```kotlin
class TessLanguageManager(val context: Context) {
    suspend fun ensureLanguage(lang: String) {
        val file = File(context.filesDir, "tessdata/$lang.traineddata")
        if (!file.exists()) {
            // Download from: https://github.com/UB-Mannheim/tesseract/wiki
            downloadFile(
                url = "https://github.com/UB-Mannheim/tesseract/raw/master/tessdata/$lang.traineddata",
                destination = file
            )
        }
    }

    private suspend fun downloadFile(url: String, destination: File) {
        // Use Ktor HTTP client or standard URLConnection
    }
}
```

### Option B: Bundle Only English, Download Others
- Include `eng.traineddata` (~10MB) in initial APK
- Download `rus.traineddata` (~15MB) and `kaz.traineddata` (~12MB) on demand
- Store in app's private directory: `context.filesDir/tessdata/`

---

## Testing (TDD First!)

```kotlin
class OcrEngineTest {
    @Test
    fun recognizeRussianBankStatement() = runTest {
        val testImage = getTestAsset("bank_statement_russian.jpg")
        val result = ocrEngine.recognizeText(testImage, "rus")

        assertTrue(result.contains("рубль"))
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun handleMultilingualText() = runTest {
        val testImage = getTestAsset("mixed_lang.jpg")
        val result = ocrEngine.recognizeText(testImage, "eng+rus+kaz")

        assertTrue(result.isNotEmpty())
    }

    @Test
    fun performanceWithLargeImage() = runTest {
        val largeImage = generateTestImage(2000, 1500)
        val startTime = System.currentTimeMillis()

        ocrEngine.recognizeText(largeImage, "rus")

        val duration = System.currentTimeMillis() - startTime
        assertTrue(duration < 5000) // Should complete in < 5 seconds
    }
}
```

---

## Integration with Bank Statement Parser

```kotlin
class ParseBankStatementUseCase(
    private val ocrEngine: OcrEngine,
    private val analyzer: StatementAnalyzer
) {
    suspend operator fun invoke(imageBytes: ByteArray): BankStatement {
        // Step 1: OCR
        val rawText = ocrEngine.recognizeText(imageBytes, "rus+eng")

        // Step 2: Detect language
        val detectedLang = detectLanguage(rawText) // "ru", "en", "kk"

        // Step 3: Parse structure
        val statement = analyzer.parse(rawText, detectedLang)

        // Step 4: Extract transactions
        return statement.extractTransactions()
    }
}
```

---

## Performance Tips

1. **Create API once:** Don't create TessBaseAPI for each image
2. **Use OpenMP variant:** Multi-threading support
3. **Reduce image size:** 500KB instead of 3MB if possible
4. **Async processing:** Run on `Dispatchers.Default` (CPU-bound)
5. **Cache language files:** Download once, reuse

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| **JitPack not found** | Add JitPack repo before other repositories |
| **Language file missing** | Ensure tessdata/ directory exists in context.filesDir |
| **Poor accuracy** | Use language-specific trained data (eng, rus, kaz) |
| **Slow performance** | Use OpenMP variant, reduce image size |
| **Large APK** | Don't bundle traineddata files, download on-demand |

---

## Decision Points for Future

1. **If accuracy < 85%** → Evaluate PaddleOCR (ONNX Runtime)
2. **If performance > 500ms** → Switch to PaddleOCR
3. **If user feedback is positive** → Keep Tesseract4Android
4. **If KMP ecosystem adds OCR** → Reconsider multiplatform options

---

## Quick Reference: Library Comparison

```
┌─────────────────────────────────────────────────────────────┐
│ TESSERACT4ANDROID 4.9.0 (RECOMMENDED)                       │
├─────────────────────────────────────────────────────────────┤
│ ✅ Russian/Cyrillic support                                  │
│ ✅ On-device (full privacy)                                  │
│ ✅ Simple KMP integration                                    │
│ ✅ Proven & stable                                           │
│ ⚠️  Slower than ML-based (100-220ms)                         │
│ ⚠️  JitPack dependency                                       │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ PADDLEOCR v3.0.3 + ONNX (ALTERNATIVE)                       │
├─────────────────────────────────────────────────────────────┤
│ ✅ Better accuracy (90%+ Russian)                            │
│ ✅ Faster inference (100-150ms)                              │
│ ✅ Modern deep learning                                      │
│ ✅ On-device processing                                      │
│ ⚠️  More complex integration                                 │
│ ⚠️  Larger model files (20-50MB)                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ GOOGLE ML KIT v2 (NOT RECOMMENDED)                           │
├─────────────────────────────────────────────────────────────┤
│ ✅ Fast (140ms latency)                                      │
│ ✅ Easy integration                                          │
│ ✅ Google support                                            │
│ ❌ NO Cyrillic support (on-device)                           │
│ ❌ Would require cloud version (privacy issue)              │
└─────────────────────────────────────────────────────────────┘
```

---

## Next Steps

1. ✅ Research complete (this document)
2. ⬜ Write failing tests first (TDD)
3. ⬜ Implement OcrEngine expect/actual
4. ⬜ Setup language file manager
5. ⬜ Integrate with statement parser
6. ⬜ Performance benchmarking
7. ⬜ User testing with real bank statements
8. ⬜ If needed: Evaluate PaddleOCR alternative

**Estimated effort:** 50-60 hours (2-3 weeks with TDD)

---

## References

- [Tesseract4Android GitHub](https://github.com/adaptech-cz/Tesseract4Android)
- [Language Training Data Files](https://github.com/UB-Mannheim/tesseract/wiki)
- [CLAUDE.md - Project Requirements](../../CLAUDE.md)
- [Full Research Document](2026-01-09-android-ocr-cyrillic-research.md)

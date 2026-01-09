# OCR Technical Setup Guide

## Maven Coordinates & Dependency Configuration

### Primary Recommendation: Tesseract4Android 4.9.0

#### 1. Gradle Configuration

**Step 1: Add JitPack Repository (settings.gradle.kts)**
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Add JitPack as last resort to prevent conflicts
        maven {
            url = uri("https://jitpack.io")
            // Optional: Add authentication if private repositories
            // credentials {
            //     username = System.getenv("JITPACK_USER")
            //     password = System.getenv("JITPACK_TOKEN")
            // }
        }
    }
}
```

**Step 2: Add Dependency**

For **shared/build.gradle.kts** (KMP project):
```kotlin
kotlin {
    // ... existing KMP configuration

    sourceSets {
        androidMain {
            dependencies {
                // Tesseract4Android with OpenMP (recommended)
                implementation("cz.adaptech.tesseract4android:tesseract4android-openmp:4.9.0")

                // Alternative: Standard variant (single-threaded)
                // implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")

                // For image processing (if not already included)
                implementation("androidx.graphics:graphics-core:1.0.0-alpha03")
            }
        }
    }
}
```

Or for **build.gradle.kts** (Android-specific):
```kotlin
dependencies {
    // Tesseract4Android OpenMP variant
    implementation("cz.adaptech.tesseract4android:tesseract4android-openmp:4.9.0")

    // Standard variant alternative
    // implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")
}
```

**Step 3: Verify Resolution**
```bash
# List all Tesseract4Android dependencies
./gradlew dependencies --configuration androidRuntimeClasspath | grep -i tesseract
```

#### 2. Library Variants

Two variants are available via JitPack:

| Variant | Coordinate | Threading | Use Case |
|---------|-----------|-----------|----------|
| **OpenMP** | `cz.adaptech.tesseract4android:tesseract4android-openmp:4.9.0` | Multi-threaded | **RECOMMENDED** - Better performance |
| **Standard** | `cz.adaptech.tesseract4android:tesseract4android:4.9.0` | Single-threaded | Fallback - Smaller footprint |

**Recommendation:** Use OpenMP variant for better performance on modern devices.

#### 3. Version Matrix

| Tesseract4Android | Tesseract OCR | Release Date | Status |
|-------------------|---------------|------------|--------|
| 4.9.0 | 5.3.4 | June 2025 | ✅ Current |
| 4.8.0 | 5.3.2 | March 2025 | ✅ Stable |
| 4.7.0 | 5.2.0 | December 2024 | ✅ Stable |
| 4.6.0 | 5.1.0 | August 2024 | ⚠️ Older |

**Recommendation:** Always use 4.9.0 or later (Tesseract 5.3.4+)

---

## Language Training Data Files

### Data Source

All training data files are available from the official Tesseract project:
- **URL:** https://github.com/UB-Mannheim/tesseract/wiki
- **Format:** `.traineddata` files
- **License:** Apache 2.0

### Required Languages for Finuts

| Language | File Name | Size | Quality | Priority |
|----------|-----------|------|---------|----------|
| **Russian** | `rus.traineddata` | ~15MB | Excellent | HIGH |
| **Kazakh** | `kaz.traineddata` | ~12MB | Good | HIGH |
| **English** | `eng.traineddata` | ~12MB | Excellent | MEDIUM |
| **Bulgarian** | `bul.traineddata` | ~13MB | Good | LOW (fallback) |

### Download Strategy

**Option 1: Include Only in Runtime (Recommended)**
```kotlin
// Download URLs (direct from GitHub)
private val LANGUAGE_URLS = mapOf(
    "rus" to "https://github.com/UB-Mannheim/tesseract/raw/master/tessdata/rus.traineddata",
    "kaz" to "https://github.com/UB-Mannheim/tesseract/raw/master/tessdata/kaz.traineddata",
    "eng" to "https://github.com/UB-Mannheim/tesseract/raw/master/tessdata/eng.traineddata"
)

private val LANGUAGE_SIZES = mapOf(
    "rus" to 15728640L,  // 15MB
    "kaz" to 12582912L,  // 12MB
    "eng" to 12582912L   // 12MB
)
```

**Option 2: Include English in APK (Balanced)**
```gradle
// In build.gradle.kts
android {
    assets {
        srcDirs = ["assets/tessdata"]
    }
}
```

Then copy `eng.traineddata` to: `src/main/assets/tessdata/eng.traineddata`

**Directory Structure:**
```
composeApp/
├── src/
│   ├── main/
│   │   └── assets/
│   │       └── tessdata/
│   │           └── eng.traineddata  (optional, 12MB)
│   └── commonTest/
│       └── resources/
│           └── ocr_test_assets/
│               ├── bank_statement_rus.jpg
│               ├── bank_statement_eng.jpg
│               └── bank_statement_mixed.jpg
```

### Installation at Runtime

```kotlin
// androidMain/kotlin/com/finuts/data/ocr/TessLanguageManager.kt

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class TessLanguageManager(private val context: Context) {

    private val tessDataDir = File(context.filesDir, "tessdata")

    init {
        tessDataDir.mkdirs()
    }

    /**
     * Download training data file if not present
     */
    suspend fun ensureLanguage(language: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val file = File(tessDataDir, "$language.traineddata")

                // Return path if already exists
                if (file.exists() && file.length() > 0) {
                    return@withContext Result.success(file.absolutePath)
                }

                // Download from GitHub
                val url = LANGUAGE_URLS[language]
                    ?: return@withContext Result.failure(
                        Exception("Unknown language: $language")
                    )

                downloadFile(url, file, language)
                Result.success(file.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Download file with progress tracking
     */
    private suspend fun downloadFile(
        url: String,
        destination: File,
        language: String
    ): Unit = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection()
        val contentLength = connection.contentLength.toLong()

        var downloaded = 0L
        val buffer = ByteArray(8192)

        connection.getInputStream().use { input ->
            FileOutputStream(destination).use { output ->
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead

                    // Emit progress (could use Flow for UI updates)
                    val progress = (downloaded * 100) / contentLength
                    println("Downloading $language: $progress%")

                    bytesRead = input.read(buffer)
                }
            }
        }

        println("Downloaded $language.traineddata (${destination.length() / 1024 / 1024}MB)")
    }

    /**
     * Get tessdata directory path
     */
    fun getTessDataPath(): String = tessDataDir.absolutePath

    /**
     * Get all available languages
     */
    fun getAvailableLanguages(): List<String> {
        return tessDataDir.listFiles()
            ?.filter { it.name.endsWith(".traineddata") }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()
    }

    /**
     * Get language status
     */
    data class LanguageStatus(
        val language: String,
        val isDownloaded: Boolean,
        val sizeBytes: Long,
        val downloadUrl: String?
    )

    fun getLanguageStatus(): List<LanguageStatus> {
        return LANGUAGE_URLS.map { (lang, url) ->
            val file = File(tessDataDir, "$lang.traineddata")
            LanguageStatus(
                language = lang,
                isDownloaded = file.exists(),
                sizeBytes = if (file.exists()) file.length() else 0,
                downloadUrl = url
            )
        }
    }

    companion object {
        private val LANGUAGE_URLS = mapOf(
            "rus" to "https://github.com/UB-Mannheim/tesseract/raw/master/tessdata/rus.traineddata",
            "kaz" to "https://github.com/UB-Mannheim/tesseract/raw/master/tessdata/kaz.traineddata",
            "eng" to "https://github.com/UB-Mannheim/tesseract/raw/master/tessdata/eng.traineddata",
            "bul" to "https://github.com/UB-Mannheim/tesseract/raw/master/tessdata/bul.traineddata"
        )
    }
}
```

---

## Alternative: PaddleOCR Setup (If Needed Later)

### Maven Coordinates

```gradle
dependencies {
    // ONNX Runtime (required for PaddleOCR models)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.1")

    // Optional: RapidOCR wrapper (community-maintained)
    // implementation("com.github.RapidAI:RapidOCR:latest")
}
```

### Model Files (ONNX Format)

| Model | Size | URL |
|-------|------|-----|
| **Detection (v5)** | 50MB | https://huggingface.co/monkt/paddleocr-onnx |
| **Recognition (eslav)** | 45MB | https://huggingface.co/monkt/paddleocr-onnx |
| **Total** | 95MB | Download from HuggingFace |

### ONNX Runtime Initialization

```kotlin
// Only if switching to PaddleOCR later
private var ortSession: OrtSession? = null

private fun initializeOnnxRuntime(modelPath: String) {
    val env = OrtEnvironment.getEnvironment()
    val sessionOptions = OrtSession.SessionOptions()
    sessionOptions.setOptimizationModelType(GraphOptimizationLevel.ORT_ENABLE_EXTENDED)
    sessionOptions.addConfigEntry("execution_providers", "NNAPI")

    ortSession = env.createSession(modelPath, sessionOptions)
}
```

---

## Dependency Conflict Resolution

### Known Conflicts

| Dependency | Potential Conflict | Resolution |
|------------|------------------|------------|
| **Android Gradle Plugin** | JNI version mismatch | Use AGP 8.0+ |
| **NDK** | CMake version | Use NDK r24+ |
| **Java Version** | Bytecode compatibility | Use Java 11+ |

### Force Resolution

If dependency conflicts occur:
```gradle
configurations.all {
    resolutionStrategy {
        force("com.google.code.findbugs:jsr305:3.0.2")

        // If Tesseract4Android has issues:
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
}
```

---

## Runtime Permissions (Android)

Add to `AndroidManifest.xml`:
```xml
<!-- For file access to tessdata directory -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

<!-- For downloading language files -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Or use scoped storage (Android 10+):
```kotlin
// Use context.filesDir - no permissions needed
val tessDataDir = File(context.filesDir, "tessdata")
```

---

## Proguard/R8 Configuration

Add to `proguard-rules.pro`:
```proguard
# Tesseract4Android - Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve Tesseract classes
-keep class com.googlecode.tesseract.android.** { *; }
-keep class tesseract.** { *; }

# Keep Kotlin extensions
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
```

---

## CI/CD Considerations

### GitHub Actions Example

```yaml
name: Build with OCR

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'

      - name: Verify Tesseract4Android dependency
        run: |
          ./gradlew dependencies \
            --configuration androidRuntimeClasspath \
            | grep tesseract

      - name: Build with Gradle
        run: ./gradlew build

      - name: Run OCR tests
        run: ./gradlew testDebugUnitTest -k OcrTest
```

---

## Troubleshooting Checklist

### Dependency Issues

- [ ] JitPack repository is added BEFORE mavenCentral()
- [ ] Using correct variant (openmp vs standard)
- [ ] Version 4.9.0+ (not older versions)
- [ ] Run `./gradlew clean` after adding dependency

### Runtime Issues

- [ ] Tessdata directory exists: `context.filesDir/tessdata/`
- [ ] Language files are downloaded (check file size > 1MB)
- [ ] Image bitmap is valid (not null)
- [ ] Using correct language code: "rus", "eng", "kaz"

### Performance Issues

- [ ] Using OpenMP variant (not standard)
- [ ] Image size < 2MB (resize if needed)
- [ ] Running on Dispatchers.Default (not main thread)
- [ ] Creating TessBaseAPI once (not per image)

### Build Issues

- [ ] NDK installed (Android Studio setup)
- [ ] CMake available (gradle wrapper should handle)
- [ ] Java 11+ (check `./gradlew --version`)
- [ ] No conflicting JNI libraries

---

## Testing Dependencies

For OCR unit tests, add to test dependencies:

```gradle
testImplementation("junit:junit:4.13.2")
testImplementation("androidx.test:core:1.5.0")
testImplementation("org.robolectric:robolectric:4.11.1")

// Coroutine testing
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

// Truth assertions
testImplementation("com.google.truth:truth:1.1.5")
```

---

## Verification Checklist

After setup, verify with:

```bash
# 1. Check dependency is resolved
./gradlew dependencies | grep tesseract

# 2. Run lint
./gradlew lint

# 3. Build APK
./gradlew assembleDebug

# 4. Verify in APK
unzip -l build/outputs/apk/debug/*.apk | grep tesseract

# 5. Run tests
./gradlew testDebugUnitTest

# 6. Check ProGuard mappings (if minified)
cat build/outputs/mapping/release/mapping.txt | grep tesseract
```

---

## Summary

### For Finuts v1.0 Launch

```gradle
// Add to settings.gradle.kts
maven { url = uri("https://jitpack.io") }

// Add to shared/build.gradle.kts (androidMain)
implementation("cz.adaptech.tesseract4android:tesseract4android-openmp:4.9.0")
```

### Language Files
- Download on-demand from GitHub
- Store in: `context.filesDir/tessdata/`
- Total: 15MB Russian + 12MB Kazakh + 12MB English = 39MB (after first run)

### Minimal Viable Setup
- Tesseract4Android 4.9.0 (OpenMP)
- JitPack repository
- Language file manager
- Expect/actual interface for KMP

**Estimated setup time:** 2-4 hours

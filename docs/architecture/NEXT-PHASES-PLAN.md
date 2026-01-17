# Детальный план следующих фаз AI Auto-Categorization

**Дата:** 2026-01-11
**Статус:** Phase 1-2 завершены, Phase 3-5 спроектированы

---

## Статус: Что уже реализовано

| Компонент | Статус | Файл |
|-----------|--------|------|
| MIGRATION_6_7 | ✅ | `Migrations.kt` |
| IconRegistry (60 иконок, 15 цветов) | ✅ | `IconRegistry.kt` |
| CategoryResolver | ✅ | `CategoryResolver.kt` |
| ImportTransactionsUseCase integration | ✅ | `ImportTransactionsUseCase.kt` |
| DI configuration | ✅ | `CoreModule.kt` |

---

## Phase 3: Icon & Color Picker UI Components

### 3.1 Анализ текущей системы

**Существующие компоненты:**
- `EmojiPickerSheet.kt` — ModalBottomSheet с сеткой эмодзи (8 колонок)
- `CategoryColorPalette.kt` — FlowRow с 12 цветами
- `AddEditCategoryScreen.kt` — экран редактирования категории

**Паттерны в кодовой базе:**
- Bottom Sheet для picker'ов (`ModalBottomSheet`)
- FlowRow для цветовой палитры
- LazyVerticalGrid для сетки элементов
- FilterChip для выбора категорий

### 3.2 Новые компоненты

#### A. IconPickerSheet.kt

```kotlin
// composeApp/src/commonMain/kotlin/com/finuts/app/ui/components/pickers/IconPickerSheet.kt

/**
 * Bottom sheet для выбора иконки из IconRegistry.
 * Использует группы иконок с горизонтальными табами.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerSheet(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Структура:**
```
┌────────────────────────────────────┐
│ Select Icon                    [X] │  ← Header
├────────────────────────────────────┤
│ [Food] [Transport] [Shopping] ...  │  ← ScrollableTabRow (13 групп)
├────────────────────────────────────┤
│  🛒   ☕   🍕   🚚   🍳           │  ← LazyVerticalGrid (5 колонок)
│  ...                               │
│                                    │
│                     Height: 300dp  │
└────────────────────────────────────┘
```

**Реализация:**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerSheet(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconRegistry = remember { IconRegistry() }
    var selectedGroup by remember { mutableStateOf(iconRegistry.iconGroups.keys.first()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Icon",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(FinutsIcons.X, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Group tabs
            ScrollableTabRow(
                selectedTabIndex = iconRegistry.iconGroups.keys.indexOf(selectedGroup),
                edgePadding = 0.dp
            ) {
                iconRegistry.iconGroups.keys.forEachIndexed { index, group ->
                    Tab(
                        selected = selectedGroup == group,
                        onClick = { selectedGroup = group },
                        text = { Text(group.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Icon grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(300.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(iconRegistry.iconGroups[selectedGroup] ?: emptyList()) { iconKey ->
                    IconPickerItem(
                        iconKey = iconKey,
                        isSelected = iconKey == selectedIcon,
                        onClick = {
                            onIconSelected(iconKey)
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IconPickerItem(
    iconKey: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) FinutsColors.Accent.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) FinutsColors.Accent else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CategoryIcon(
            iconKey = iconKey,
            size = 24.dp,
            tint = if (isSelected) FinutsColors.Accent else FinutsColors.TextSecondary
        )
    }
}
```

#### B. ColorPickerPalette.kt

```kotlin
// composeApp/src/commonMain/kotlin/com/finuts/app/ui/components/pickers/ColorPickerPalette.kt

/**
 * Палитра из 15 цветов для выбора цвета категории.
 * Использует FlowRow с круглыми свотчами.
 */
@Composable
fun ColorPickerPalette(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

**Структура:**
```
┌────────────────────────────────────┐
│  ●  ●  ●  ●  ●  ●  ●  ●           │  ← 8 цветов в ряду
│  ●  ●  ●  ●  ●  ●  ●              │  ← 7 цветов во втором
└────────────────────────────────────┘
     ↑ 40dp круги с 2dp selection ring
```

**Реализация:**
```kotlin
@Composable
fun ColorPickerPalette(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val iconRegistry = remember { IconRegistry() }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        iconRegistry.colorPalette.forEach { colorHex ->
            ColorSwatch(
                colorHex = colorHex,
                isSelected = colorHex == selectedColor,
                onClick = { onColorSelected(colorHex) }
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = remember(colorHex) { Color(android.graphics.Color.parseColor(colorHex)) }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) FinutsColors.TextPrimary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = FinutsIcons.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
```

### 3.3 Интеграция в AddEditCategoryScreen

**Изменения:**

```kotlin
// Заменить EmojiPickerSheet на IconPickerSheet

var showIconPicker by remember { mutableStateOf(false) }

// Icon selector (вместо emoji)
Box(
    modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(selectedColor.copy(alpha = 0.15f))
        .clickable { showIconPicker = true },
    contentAlignment = Alignment.Center
) {
    CategoryIcon(
        iconKey = selectedIcon,
        size = 40.dp,
        tint = selectedColor
    )
}

// Sheet
if (showIconPicker) {
    IconPickerSheet(
        selectedIcon = selectedIcon,
        onIconSelected = { selectedIcon = it },
        onDismiss = { showIconPicker = false }
    )
}

// Color palette (уже есть, использовать IconRegistry.colorPalette)
ColorPickerPalette(
    selectedColor = selectedColor,
    onColorSelected = { selectedColor = it }
)
```

### 3.4 Тесты

```kotlin
// composeApp/src/commonTest/kotlin/com/finuts/app/ui/components/pickers/IconPickerSheetTest.kt

class IconPickerSheetTest {
    @Test
    fun `displays all icon groups as tabs`()

    @Test
    fun `shows icons for selected group`()

    @Test
    fun `calls onIconSelected when icon tapped`()

    @Test
    fun `highlights currently selected icon`()

    @Test
    fun `dismisses sheet after selection`()
}
```

---

## Phase 4: Claude Haiku Cloud Integration

### 4.1 Текущая архитектура AI

**Существующие компоненты:**
- `FinutsAIService` — unified interface
- `AIOrchestrator` — execution + fallback + cost tracking
- `PIIAnonymizer` — PII detection & anonymization
- `AICostTracker` — budget enforcement ($0.10/day, $2/month)
- `LLMProviderFactory` — provider routing

**Поддерживаемые провайдеры (в коде):**
- OpenAI (GPT-4o-mini, GPT-4o)
- Anthropic (Claude Haiku 4.5, Claude Sonnet 4)
- On-Device (placeholder)

### 4.2 anthropic-sdk-kotlin интеграция

**Зависимость (уже добавлена):**
```kotlin
implementation("com.xemantic.ai:anthropic-sdk-kotlin:0.25.2")
```

**Новый провайдер:**

```kotlin
// shared/src/commonMain/kotlin/com/finuts/ai/providers/AnthropicProvider.kt

class AnthropicProvider(
    private val apiKey: String
) : LLMProvider {

    private val anthropic = Anthropic {
        apiKey(apiKey)
    }

    override suspend fun complete(
        prompt: String,
        systemPrompt: String?,
        model: String,
        maxTokens: Int,
        temperature: Float
    ): CompletionResponse {
        val response = anthropic.messages.create {
            this.model = model
            this.maxTokens = maxTokens
            system(systemPrompt ?: "")
            +prompt
        }

        return CompletionResponse(
            content = response.content.first().text,
            inputTokens = response.usage.inputTokens,
            outputTokens = response.usage.outputTokens,
            model = model
        )
    }

    override fun isAvailable(): Boolean = apiKey.isNotBlank()

    companion object {
        const val MODEL_HAIKU = "claude-3-5-haiku-latest"
        const val MODEL_SONNET = "claude-sonnet-4-20250514"
    }
}
```

### 4.3 Categorization Prompt

```kotlin
// shared/src/commonMain/kotlin/com/finuts/ai/prompts/CategorizationPrompt.kt

object CategorizationPrompt {

    fun buildCategorizationPrompt(
        description: String,  // Already anonymized
        existingCategories: List<String>,
        language: String = "ru"
    ): String = """
You are a financial transaction categorizer for a personal finance app.

TASK: Categorize the following transaction description.

Transaction: "$description"

Available categories (use one of these if applicable):
${existingCategories.joinToString("\n") { "- $it" }}

RULES:
1. Use an existing category if it fits (prefer exact match)
2. If no existing category fits well, return categoryId: "other"
3. Return confidence 0.0-1.0 (how certain you are)
4. Response must be valid JSON only, no markdown

Response format:
{
  "categoryId": "groceries",
  "confidence": 0.92
}
""".trimIndent()

    const val SYSTEM_PROMPT = """
You are a precise financial transaction categorizer.
Always respond with valid JSON matching the requested schema.
Never include markdown formatting or explanations.
Analyze merchant names, transaction patterns, and amounts to determine the most likely category.
"""
}
```

### 4.4 AICategorizer обновление

```kotlin
// shared/src/commonMain/kotlin/com/finuts/data/categorization/AICategorizer.kt

class AICategorizer(
    private val orchestrator: AIOrchestrator,
    private val categoryRepository: CategoryRepository,
    private val anonymizer: PIIAnonymizer
) {
    private val log = Logger.withTag("AICategorizer")

    /**
     * Tier 2: Categorize using Claude Haiku.
     * Called when Tier 0-1 fails to categorize.
     *
     * @param transactionId Unique ID for tracking
     * @param description Raw transaction description
     * @return CategorizationResult or null if failed
     */
    suspend fun categorizeTier2(
        transactionId: String,
        description: String
    ): CategorizationResult? {
        log.d { "categorizeTier2: Processing '$description'" }

        // 1. Anonymize description
        val anonymized = anonymizer.anonymize(description)
        log.v { "categorizeTier2: Anonymized to '${anonymized.text}'" }

        // 2. Get existing categories
        val existingCategories = categoryRepository.getAllCategories()
            .first()
            .map { it.id }

        // 3. Build prompt
        val prompt = CategorizationPrompt.buildCategorizationPrompt(
            description = anonymized.text,
            existingCategories = existingCategories
        )

        // 4. Execute via orchestrator
        val task = AITask(
            prompt = prompt,
            systemPrompt = CategorizationPrompt.SYSTEM_PROMPT,
            preference = ProviderPreference.FAST_CHEAP, // Claude Haiku
            maxTokens = 100,
            temperature = 0.1f,
            requiresAnonymization = false, // Already done
            estimatedCost = 0.0001f
        )

        return when (val result = orchestrator.execute(task)) {
            is AIResult.Success -> parseResponse(transactionId, result.data.content)
            is AIResult.Error -> {
                log.e { "categorizeTier2: Error - ${result.message}" }
                null
            }
            is AIResult.CostLimitExceeded -> {
                log.w { "categorizeTier2: Cost limit exceeded" }
                null
            }
            is AIResult.ProviderUnavailable -> {
                log.w { "categorizeTier2: Provider unavailable" }
                null
            }
        }
    }

    private fun parseResponse(
        transactionId: String,
        jsonResponse: String
    ): CategorizationResult? {
        return try {
            val response = Json.decodeFromString<LLMCategoryResponse>(jsonResponse)

            CategorizationResult(
                transactionId = transactionId,
                categoryId = response.categoryId,
                confidence = response.confidence,
                source = CategorizationSource.LLM_TIER2
            )
        } catch (e: Exception) {
            log.e(e) { "categorizeTier2: Failed to parse response: $jsonResponse" }
            null
        }
    }
}

@Serializable
data class LLMCategoryResponse(
    val categoryId: String,
    val confidence: Float
)
```

### 4.5 TransactionCategorizer интеграция

```kotlin
// Обновить TransactionCategorizer для использования Tier 2

class TransactionCategorizer(
    private val ruleBasedCategorizer: RuleBasedCategorizer,
    private val learnedMerchantRepository: LearnedMerchantRepository,
    private val aiCategorizer: AICategorizer? = null  // Optional for tests
) {

    suspend fun categorize(transaction: Transaction): CategorizationResult {
        // Tier 0: User-learned patterns
        val tier0Result = checkLearnedPatterns(transaction)
        if (tier0Result != null && tier0Result.confidence >= 0.90) {
            return tier0Result
        }

        // Tier 1: Rule-based
        val tier1Result = ruleBasedCategorizer.categorize(transaction)
        if (tier1Result != null && tier1Result.confidence >= 0.85) {
            return tier1Result
        }

        // Tier 2: Cloud LLM (if available)
        if (aiCategorizer != null) {
            val tier2Result = aiCategorizer.categorizeTier2(
                transactionId = transaction.id,
                description = transaction.description ?: ""
            )
            if (tier2Result != null && tier2Result.confidence >= 0.75) {
                return tier2Result
            }
        }

        // Tier 3: Fallback to "other"
        return CategorizationResult(
            transactionId = transaction.id,
            categoryId = "other",
            confidence = 0.5f,
            source = CategorizationSource.FALLBACK
        )
    }
}
```

### 4.6 Тесты

```kotlin
class AICategorizerTest {
    @Test
    fun `categorizeTier2 anonymizes description before sending`()

    @Test
    fun `categorizeTier2 returns null on cost limit exceeded`()

    @Test
    fun `categorizeTier2 parses valid JSON response`()

    @Test
    fun `categorizeTier2 handles malformed JSON gracefully`()

    @Test
    fun `categorizeTier2 respects existing categories`()
}
```

---

## Phase 5: On-Device LLM (Gemma 3 270M)

### 5.1 Выбор технологии

**Рекомендация:** LiteRT-LM (Google) + MediaPipe LLM Inference

| Платформа | Framework | API |
|-----------|-----------|-----|
| Android | LiteRT-LM 0.7.0+ | Kotlin |
| iOS | MediaPipe LLM Inference | Swift |

**Модель:** Gemma 3 270M INT4 (~125MB)
- 270M параметров (170M embedding, 100M transformer)
- 0.75% батареи на 25 разговоров (Pixel 9 Pro)
- Поддержка GPU и NPU acceleration

### 5.2 Архитектура expect/actual

```kotlin
// shared/src/commonMain/kotlin/com/finuts/ai/ondevice/OnDeviceLLM.kt

/**
 * Interface for on-device LLM inference.
 * Platform-specific implementations use LiteRT (Android) or MediaPipe (iOS).
 */
interface OnDeviceLLM {
    /**
     * Check if on-device model is available and ready.
     */
    fun isAvailable(): Boolean

    /**
     * Download model if not present.
     * @return Flow of download progress (0-100)
     */
    suspend fun ensureModelReady(): Flow<DownloadProgress>

    /**
     * Generate response for the given prompt.
     * @param prompt User prompt
     * @param systemPrompt Optional system instructions
     * @param maxTokens Maximum tokens to generate
     * @return Generated text or null on error
     */
    suspend fun generate(
        prompt: String,
        systemPrompt: String? = null,
        maxTokens: Int = 256
    ): String?

    /**
     * Generate response with streaming.
     */
    fun generateStream(
        prompt: String,
        systemPrompt: String? = null,
        maxTokens: Int = 256
    ): Flow<String>
}

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isComplete: Boolean
) {
    val percentage: Int get() =
        if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
}

expect fun createOnDeviceLLM(): OnDeviceLLM
```

### 5.3 Android Implementation (LiteRT-LM)

```kotlin
// shared/src/androidMain/kotlin/com/finuts/ai/ondevice/OnDeviceLLM.android.kt

actual fun createOnDeviceLLM(): OnDeviceLLM = LiteRTLLM()

class LiteRTLLM : OnDeviceLLM {
    private var inferenceModel: InferenceModel? = null
    private val modelPath = "gemma-3-270m-it-int4.task"

    override fun isAvailable(): Boolean {
        // Check if model file exists
        return File(getModelDir(), modelPath).exists()
    }

    override suspend fun ensureModelReady(): Flow<DownloadProgress> = flow {
        val modelFile = File(getModelDir(), modelPath)

        if (modelFile.exists()) {
            emit(DownloadProgress(modelFile.length(), modelFile.length(), true))
            initializeModel(modelFile)
            return@flow
        }

        // Download from HuggingFace or bundled assets
        downloadModel(modelFile) { downloaded, total ->
            emit(DownloadProgress(downloaded, total, false))
        }

        emit(DownloadProgress(modelFile.length(), modelFile.length(), true))
        initializeModel(modelFile)
    }

    private fun initializeModel(modelFile: File) {
        val options = InferenceModelOptions.Builder()
            .setModelPath(modelFile.absolutePath)
            .setPreferredBackend(InferenceBackend.GPU)  // or NPU if available
            .build()

        inferenceModel = InferenceModel.create(options)
    }

    override suspend fun generate(
        prompt: String,
        systemPrompt: String?,
        maxTokens: Int
    ): String? {
        val model = inferenceModel ?: return null

        val fullPrompt = buildString {
            if (systemPrompt != null) {
                append("<start_of_turn>system\n$systemPrompt<end_of_turn>\n")
            }
            append("<start_of_turn>user\n$prompt<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }

        return try {
            model.generateContent(fullPrompt, maxTokens)
        } catch (e: Exception) {
            null
        }
    }

    override fun generateStream(
        prompt: String,
        systemPrompt: String?,
        maxTokens: Int
    ): Flow<String> = flow {
        val model = inferenceModel ?: return@flow

        val fullPrompt = buildPrompt(prompt, systemPrompt)

        model.generateContentAsync(fullPrompt, maxTokens).collect { token ->
            emit(token)
        }
    }
}
```

### 5.4 iOS Implementation (MediaPipe)

```swift
// iosApp/iosApp/OnDeviceLLM/LLMInferenceWrapper.swift

import MediaPipeTasksGenai

@objc public class LLMInferenceWrapper: NSObject {
    private var llmInference: LlmInference?
    private let modelName = "gemma-3-270m-it-int4"

    @objc public func isAvailable() -> Bool {
        let modelPath = Bundle.main.path(forResource: modelName, ofType: "task")
        return modelPath != nil
    }

    @objc public func initialize() throws {
        guard let modelPath = Bundle.main.path(forResource: modelName, ofType: "task") else {
            throw NSError(domain: "LLM", code: 1, userInfo: [NSLocalizedDescriptionKey: "Model not found"])
        }

        let options = LlmInferenceOptions()
        options.baseOptions.modelPath = modelPath
        options.maxTokens = 256
        options.topk = 40
        options.temperature = 0.1

        llmInference = try LlmInference(options: options)
    }

    @objc public func generate(prompt: String, systemPrompt: String?) async throws -> String {
        guard let inference = llmInference else {
            throw NSError(domain: "LLM", code: 2, userInfo: [NSLocalizedDescriptionKey: "Model not initialized"])
        }

        var fullPrompt = ""
        if let system = systemPrompt {
            fullPrompt += "<start_of_turn>system\n\(system)<end_of_turn>\n"
        }
        fullPrompt += "<start_of_turn>user\n\(prompt)<end_of_turn>\n"
        fullPrompt += "<start_of_turn>model\n"

        return try await inference.generateResponse(inputText: fullPrompt)
    }

    @objc public func generateStream(prompt: String, systemPrompt: String?) -> AsyncThrowingStream<String, Error> {
        return AsyncThrowingStream { continuation in
            guard let inference = llmInference else {
                continuation.finish(throwing: NSError(domain: "LLM", code: 2, userInfo: nil))
                return
            }

            let fullPrompt = buildPrompt(prompt: prompt, systemPrompt: systemPrompt)

            Task {
                do {
                    for try await token in inference.generateResponseAsync(inputText: fullPrompt) {
                        continuation.yield(token)
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
        }
    }
}
```

```kotlin
// shared/src/iosMain/kotlin/com/finuts/ai/ondevice/OnDeviceLLM.ios.kt

actual fun createOnDeviceLLM(): OnDeviceLLM = MediaPipeLLM()

class MediaPipeLLM : OnDeviceLLM {
    private val wrapper = LLMInferenceWrapper()

    override fun isAvailable(): Boolean = wrapper.isAvailable()

    override suspend fun ensureModelReady(): Flow<DownloadProgress> = flow {
        // Model is bundled in iOS, just initialize
        try {
            wrapper.initialize()
            emit(DownloadProgress(125_000_000, 125_000_000, true))
        } catch (e: Exception) {
            // Handle initialization error
        }
    }

    override suspend fun generate(
        prompt: String,
        systemPrompt: String?,
        maxTokens: Int
    ): String? {
        return try {
            wrapper.generate(prompt, systemPrompt)
        } catch (e: Exception) {
            null
        }
    }

    override fun generateStream(
        prompt: String,
        systemPrompt: String?,
        maxTokens: Int
    ): Flow<String> = wrapper.generateStream(prompt, systemPrompt).asFlow()
}
```

### 5.5 OnDeviceCategorizer

```kotlin
// shared/src/commonMain/kotlin/com/finuts/ai/ondevice/OnDeviceCategorizer.kt

class OnDeviceCategorizer(
    private val llm: OnDeviceLLM,
    private val categoryRepository: CategoryRepository
) {
    private val log = Logger.withTag("OnDeviceCategorizer")

    /**
     * Tier 1.5: Categorize using on-device LLM.
     * Called when Tier 0-1 fails but before cloud LLM.
     */
    suspend fun categorize(
        transactionId: String,
        description: String
    ): CategorizationResult? {
        if (!llm.isAvailable()) {
            log.d { "On-device LLM not available" }
            return null
        }

        val categories = categoryRepository.getAllCategories().first().map { it.id }

        val prompt = """
Categorize this transaction: "$description"
Choose from: ${categories.joinToString(", ")}
Reply with only the category name, nothing else.
""".trimIndent()

        val response = llm.generate(
            prompt = prompt,
            systemPrompt = "You are a transaction categorizer. Reply with only the category name.",
            maxTokens = 20
        ) ?: return null

        val categoryId = response.trim().lowercase()

        return if (categoryId in categories) {
            CategorizationResult(
                transactionId = transactionId,
                categoryId = categoryId,
                confidence = 0.85f,
                source = CategorizationSource.ON_DEVICE_LLM
            )
        } else {
            null
        }
    }
}
```

### 5.6 DI Configuration

```kotlin
// shared/src/commonMain/kotlin/com/finuts/ai/di/AIModule.kt

// Add to aiModule:
single<OnDeviceLLM> { createOnDeviceLLM() }
factory { OnDeviceCategorizer(get(), get()) }

// Update TransactionCategorizer factory:
factory {
    TransactionCategorizer(
        ruleBasedCategorizer = get(),
        learnedMerchantRepository = get(),
        onDeviceCategorizer = getOrNull(),  // Optional
        aiCategorizer = getOrNull()          // Optional
    )
}
```

---

## Приоритеты реализации

| # | Phase | Компонент | Effort | Зависимости |
|---|-------|-----------|--------|-------------|
| 1 | 3 | IconPickerSheet | Low | IconRegistry ✅ |
| 2 | 3 | ColorPickerPalette | Low | IconRegistry ✅ |
| 3 | 3 | AddEditCategoryScreen integration | Low | 1, 2 |
| 4 | 4 | AnthropicProvider | Medium | anthropic-sdk-kotlin ✅ |
| 5 | 4 | CategorizationPrompt | Low | - |
| 6 | 4 | AICategorizer | Medium | 4, 5 |
| 7 | 4 | TransactionCategorizer update | Low | 6 |
| 8 | 5 | OnDeviceLLM interface | Low | - |
| 9 | 5 | Android LiteRT implementation | High | Model download |
| 10 | 5 | iOS MediaPipe implementation | High | Model bundling |
| 11 | 5 | OnDeviceCategorizer | Medium | 8, 9, 10 |

---

## Источники

- [anthropic-sdk-kotlin](https://github.com/xemantic/anthropic-sdk-kotlin)
- [Claude Haiku 4.5](https://www.anthropic.com/claude/haiku)
- [LiteRT-LM GitHub](https://github.com/google-ai-edge/LiteRT-LM)
- [Gemma 3 270M](https://developers.googleblog.com/en/introducing-gemma-3-270m/)
- [MediaPipe LLM Inference iOS](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/ios)
- [Gemma on Mobile](https://ai.google.dev/gemma/docs/integrations/mobile)
- [DataCamp Gemma 3 270M Tutorial](https://www.datacamp.com/tutorial/gemma-3-270m)

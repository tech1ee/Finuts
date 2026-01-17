package com.finuts.ai.providers

import com.finuts.ai.inference.InferenceEngine
import com.finuts.ai.inference.InferenceResult
import com.finuts.domain.model.DownloadProgress
import com.finuts.domain.model.InstalledModel
import com.finuts.domain.model.ModelCapability
import com.finuts.domain.model.ModelConfig
import com.finuts.domain.model.ModelStatus
import com.finuts.domain.repository.ModelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for OnDeviceLLMProvider.
 * TDD: RED phase - these tests define expected behavior.
 */
class OnDeviceLLMProviderTest {

    // === Availability Tests ===

    @Test
    fun `isAvailable returns false when no model installed`() = runTest {
        val repository = FakeModelRepository(model = null)
        val engine = FakeInferenceEngine()
        val provider = OnDeviceLLMProvider(repository, engine)

        assertFalse(provider.isAvailable())
    }

    @Test
    fun `isAvailable returns false when model is corrupted`() = runTest {
        val model = createInstalledModel(status = ModelStatus.CORRUPTED)
        val repository = FakeModelRepository(model = model)
        val engine = FakeInferenceEngine()
        val provider = OnDeviceLLMProvider(repository, engine)

        assertFalse(provider.isAvailable())
    }

    @Test
    fun `isAvailable returns true when model is ready`() = runTest {
        val model = createInstalledModel(status = ModelStatus.READY)
        val repository = FakeModelRepository(model = model)
        val engine = FakeInferenceEngine(loadSuccess = true)
        val provider = OnDeviceLLMProvider(repository, engine)

        assertTrue(provider.isAvailable())
    }

    @Test
    fun `isAvailable returns false when engine fails to load model`() = runTest {
        val model = createInstalledModel(status = ModelStatus.READY)
        val repository = FakeModelRepository(model = model)
        val engine = FakeInferenceEngine(loadSuccess = false)
        val provider = OnDeviceLLMProvider(repository, engine)

        assertFalse(provider.isAvailable())
    }

    // === Completion Tests ===

    @Test
    fun `complete generates categorization response`() = runTest {
        val model = createInstalledModel()
        val repository = FakeModelRepository(model = model)
        val engine = FakeInferenceEngine(
            loadSuccess = true,
            responseText = """{"categoryId": "groceries", "confidence": 0.92}"""
        )
        val provider = OnDeviceLLMProvider(repository, engine)

        val response = provider.complete(
            CompletionRequest(
                prompt = "Categorize: МАГНУМ ТОО",
                maxTokens = 100,
                temperature = 0.1f
            )
        )

        assertTrue(response.content.contains("groceries"))
        assertTrue(response.inputTokens > 0)
        assertTrue(response.outputTokens > 0)
    }

    @Test
    fun `complete returns valid JSON for category`() = runTest {
        val model = createInstalledModel()
        val repository = FakeModelRepository(model = model)
        val engine = FakeInferenceEngine(
            loadSuccess = true,
            responseText = """{"categoryId": "shopping", "confidence": 0.85}"""
        )
        val provider = OnDeviceLLMProvider(repository, engine)

        val response = provider.complete(
            CompletionRequest(prompt = "Categorize: SOME SHOP")
        )

        assertTrue(response.content.startsWith("{"))
        assertTrue(response.content.endsWith("}"))
    }

    @Test
    fun `complete respects maxTokens limit`() = runTest {
        val model = createInstalledModel()
        val repository = FakeModelRepository(model = model)
        val engine = FakeInferenceEngine(loadSuccess = true)
        val provider = OnDeviceLLMProvider(repository, engine)

        provider.complete(
            CompletionRequest(
                prompt = "Test",
                maxTokens = 50
            )
        )

        assertEquals(50, engine.lastMaxTokens)
    }

    @Test
    fun `complete throws when model not loaded`() = runTest {
        val repository = FakeModelRepository(model = null)
        val engine = FakeInferenceEngine()
        val provider = OnDeviceLLMProvider(repository, engine)

        assertFailsWith<ProviderUnavailableException> {
            provider.complete(CompletionRequest(prompt = "Test"))
        }
    }

    // === Chat Tests ===

    @Test
    fun `chat uses last user message for prompt`() = runTest {
        // NOTE: System messages are now handled by Swift template layer (detectTemplate()),
        // so buildChatPrompt only includes user messages.
        val model = createInstalledModel()
        val repository = FakeModelRepository(model = model)
        val engine = FakeInferenceEngine(loadSuccess = true)
        val provider = OnDeviceLLMProvider(repository, engine)

        provider.chat(
            listOf(
                ChatMessage(ChatRole.SYSTEM, "You are a financial assistant."),
                ChatMessage(ChatRole.USER, "Categorize: STARBUCKS")
            )
        )

        // Only user message content is passed; system prompt is in Swift template
        assertTrue(engine.lastPrompt?.contains("STARBUCKS") == true)
        // System messages are filtered out (handled in Swift layer)
        assertFalse(engine.lastPrompt?.contains("financial assistant") == true)
    }

    // === Structured Output Tests ===

    @Test
    fun `structuredOutput includes schema in prompt`() = runTest {
        val model = createInstalledModel()
        val repository = FakeModelRepository(model = model)
        val engine = FakeInferenceEngine(loadSuccess = true)
        val provider = OnDeviceLLMProvider(repository, engine)

        val schema = """{"type": "object", "properties": {"categoryId": {"type": "string"}}}"""
        provider.structuredOutput("Categorize: SHOP", schema)

        assertTrue(engine.lastPrompt?.contains(schema) == true)
    }

    // === Provider Info Tests ===

    @Test
    fun `name returns on-device identifier`() {
        val repository = FakeModelRepository()
        val engine = FakeInferenceEngine()
        val provider = OnDeviceLLMProvider(repository, engine)

        assertEquals("on-device", provider.name)
    }

    @Test
    fun `availableModels returns empty list`() {
        val repository = FakeModelRepository()
        val engine = FakeInferenceEngine()
        val provider = OnDeviceLLMProvider(repository, engine)

        assertTrue(provider.availableModels.isEmpty())
    }

    // === Error Handling Tests ===

    @Test
    fun `complete handles engine exception gracefully`() = runTest {
        val model = createInstalledModel()
        val repository = FakeModelRepository(model = model)
        val engine = FakeInferenceEngine(
            loadSuccess = true,
            shouldThrowOnComplete = true
        )
        val provider = OnDeviceLLMProvider(repository, engine)

        assertFailsWith<ProviderUnavailableException> {
            provider.complete(CompletionRequest(prompt = "Test"))
        }
    }

    // === Helpers ===

    private fun createInstalledModel(
        status: ModelStatus = ModelStatus.READY
    ): InstalledModel = InstalledModel(
        config = ModelConfig(
            id = "compact",
            displayName = "SmolLM2 135M",
            description = "Fast categorization",
            sizeBytes = 105_000_000L,
            minRamBytes = 300_000_000L,
            capabilities = setOf(ModelCapability.CATEGORIZATION),
            downloadUrl = "https://example.com/model.gguf",
            checksumSha256 = "abc123",
            version = "1.0.0",
            minAppVersion = "1.0.0"
        ),
        filePath = "/path/to/model.gguf",
        installedAt = Instant.fromEpochMilliseconds(0),
        isSelected = true,
        status = status
    )

    // === Fake Implementations ===

    private class FakeModelRepository(
        model: InstalledModel? = null
    ) : ModelRepository {
        override val availableModels: List<ModelConfig> = emptyList()
        override val installedModels: StateFlow<List<InstalledModel>> =
            MutableStateFlow(listOfNotNull(model))
        override val currentModel: StateFlow<InstalledModel?> =
            MutableStateFlow(model)
        override val downloadProgress: StateFlow<DownloadProgress> =
            MutableStateFlow(DownloadProgress.Idle)

        override suspend fun getRecommendedModel(): ModelConfig =
            throw NotImplementedError()
        override suspend fun isDeviceCompatible(modelId: String): Boolean = true
        override suspend fun hasEnoughStorage(modelId: String): Boolean = true
        override suspend fun downloadModel(modelId: String): Result<InstalledModel> =
            throw NotImplementedError()
        override fun cancelDownload() {}
        override suspend fun deleteModel(modelId: String): Result<Unit> =
            throw NotImplementedError()
        override suspend fun selectModel(modelId: String): Result<Unit> =
            throw NotImplementedError()
        override suspend fun checkForUpdates(): List<ModelConfig> = emptyList()
    }

    private class FakeInferenceEngine(
        private val loadSuccess: Boolean = false,
        private val responseText: String = """{"categoryId": "other", "confidence": 0.5}""",
        private val shouldThrowOnComplete: Boolean = false
    ) : InferenceEngine {
        var lastPrompt: String? = null
        var lastMaxTokens: Int? = null
        private var modelLoaded = false

        override suspend fun loadModel(modelPath: String): Boolean {
            modelLoaded = loadSuccess
            return loadSuccess
        }

        override fun isModelLoaded(): Boolean = modelLoaded

        override suspend fun unloadModel() {
            modelLoaded = false
        }

        override suspend fun complete(
            prompt: String,
            maxTokens: Int,
            temperature: Float,
            stopSequences: List<String>
        ): InferenceResult {
            if (shouldThrowOnComplete) {
                throw RuntimeException("Inference failed")
            }
            lastPrompt = prompt
            lastMaxTokens = maxTokens
            return InferenceResult(
                text = responseText,
                inputTokens = prompt.length / 4,
                outputTokens = responseText.length / 4,
                durationMs = 100,
                tokensPerSecond = 10f
            )
        }

        override fun tokenCount(text: String): Int = text.length / 4
    }
}

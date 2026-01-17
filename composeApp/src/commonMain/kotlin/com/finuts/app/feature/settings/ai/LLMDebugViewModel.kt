package com.finuts.app.feature.settings.ai

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.finuts.ai.providers.CompletionRequest
import com.finuts.ai.providers.LLMProvider
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.model.InstalledModel
import com.finuts.domain.repository.ModelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * ViewModel for LLM Debug Screen.
 * Allows direct testing of on-device LLM without going through full import flow.
 */
class LLMDebugViewModel(
    private val modelRepository: ModelRepository,
    private val onDeviceLLMProvider: LLMProvider?
) : BaseViewModel() {

    private val log = Logger.withTag("LLMDebugViewModel")

    /** Currently selected model */
    val currentModel: StateFlow<InstalledModel?> = modelRepository.currentModel

    /** Test result state */
    private val _testState = MutableStateFlow<LLMTestState>(LLMTestState.Idle)
    val testState: StateFlow<LLMTestState> = _testState.asStateFlow()

    /** Test history */
    private val _testHistory = MutableStateFlow<List<LLMTestResult>>(emptyList())
    val testHistory: StateFlow<List<LLMTestResult>> = _testHistory.asStateFlow()

    /** Default test prompts for quick testing */
    val samplePrompts: List<String> = listOf(
        """Category for: "MAGNUM ALMATY"
Options: groceries, shopping, food, other
Answer:""",
        """{"transaction":"GLOVO ORDER 123","category":"food_delivery"}""",
        """Categorize: "KASPI GOLD PAYMENT" amount: $100.00
Categories: transfer, payment, salary, subscription
JSON: {"category":""",
        """Transaction: "STARBUCKS" $5.50
Is this: food, coffee, shopping?
Answer:"""
    )

    /**
     * Test the LLM with a custom prompt.
     */
    fun testPrompt(prompt: String) {
        if (prompt.isBlank()) return
        if (onDeviceLLMProvider == null) {
            _testState.value = LLMTestState.Error("On-device LLM provider not available")
            return
        }

        viewModelScope.launch {
            _testState.value = LLMTestState.Loading

            try {
                log.i { "Testing prompt: ${prompt.take(50)}..." }

                // Check if model is available
                if (!onDeviceLLMProvider.isAvailable()) {
                    _testState.value = LLMTestState.Error("Model not loaded. Please select a model in AI Features.")
                    return@launch
                }

                val startTime = Clock.System.now().toEpochMilliseconds()

                val response = onDeviceLLMProvider.complete(
                    CompletionRequest(
                        prompt = prompt,
                        maxTokens = 100,
                        temperature = 0.1f
                    )
                )

                val durationMs = Clock.System.now().toEpochMilliseconds() - startTime

                val result = LLMTestResult(
                    prompt = prompt,
                    response = response.content,
                    inputTokens = response.inputTokens,
                    outputTokens = response.outputTokens,
                    durationMs = durationMs,
                    model = response.model,
                    timestamp = Clock.System.now().toEpochMilliseconds()
                )

                log.i { "Test completed: ${result.outputTokens} tokens in ${result.durationMs}ms" }

                _testState.value = LLMTestState.Success(result)

                // Add to history (max 10 items)
                _testHistory.value = listOf(result) + _testHistory.value.take(9)

            } catch (e: Exception) {
                log.e(e) { "Test failed: ${e.message}" }
                _testState.value = LLMTestState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Clear the test state to idle.
     */
    fun clearState() {
        _testState.value = LLMTestState.Idle
    }

    /**
     * Clear test history.
     */
    fun clearHistory() {
        _testHistory.value = emptyList()
    }
}

/**
 * State for LLM testing.
 */
sealed class LLMTestState {
    data object Idle : LLMTestState()
    data object Loading : LLMTestState()
    data class Success(val result: LLMTestResult) : LLMTestState()
    data class Error(val message: String) : LLMTestState()
}

/**
 * Result of an LLM test.
 */
data class LLMTestResult(
    val prompt: String,
    val response: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val durationMs: Long,
    val model: String,
    val timestamp: Long
) {
    val tokensPerSecond: Float
        get() = if (durationMs > 0) outputTokens * 1000f / durationMs else 0f
}

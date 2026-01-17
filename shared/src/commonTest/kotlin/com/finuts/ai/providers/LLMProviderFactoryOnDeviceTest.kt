package com.finuts.ai.providers

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Tests for LLMProviderFactory on-device integration.
 * TDD: RED phase - defines expected on-device routing behavior.
 */
class LLMProviderFactoryOnDeviceTest {

    // === CHEAPEST Preference Tests ===

    @Test
    fun `CHEAPEST returns on-device when available`() = runTest {
        val onDevice = FakeLLMProvider("on-device", isAvailable = true)
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = null,
            onDeviceProvider = onDevice
        )

        val provider = factory.getProvider(ProviderPreference.CHEAPEST)

        assertEquals(onDevice, provider)
    }

    @Test
    fun `CHEAPEST returns cloud mini when on-device unavailable`() = runTest {
        val onDevice = FakeLLMProvider("on-device", isAvailable = false)
        val openAI = FakeLLMProvider("gpt-4o-mini", isAvailable = true)
        val factory = LLMProviderFactory(
            openAIProvider = openAI,
            anthropicProvider = null,
            onDeviceProvider = onDevice
        )

        val provider = factory.getProvider(ProviderPreference.CHEAPEST)

        assertEquals(openAI, provider)
    }

    @Test
    fun `CHEAPEST prefers on-device over cloud providers`() = runTest {
        val onDevice = FakeLLMProvider("on-device", isAvailable = true)
        val openAI = FakeLLMProvider("gpt-4o-mini", isAvailable = true)
        val anthropic = FakeLLMProvider("claude-haiku", isAvailable = true)
        val factory = LLMProviderFactory(
            openAIProvider = openAI,
            anthropicProvider = anthropic,
            onDeviceProvider = onDevice
        )

        val provider = factory.getProvider(ProviderPreference.CHEAPEST)

        assertEquals(onDevice, provider)
    }

    // === LOCAL_ONLY Preference Tests ===

    @Test
    fun `LOCAL_ONLY returns on-device when available`() = runTest {
        val onDevice = FakeLLMProvider("on-device", isAvailable = true)
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = null,
            onDeviceProvider = onDevice
        )

        val provider = factory.getProvider(ProviderPreference.LOCAL_ONLY)

        assertEquals(onDevice, provider)
    }

    @Test
    fun `LOCAL_ONLY throws when no on-device provider`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeLLMProvider("openai", isAvailable = true),
            anthropicProvider = null,
            onDeviceProvider = null
        )

        assertFailsWith<ProviderUnavailableException> {
            factory.getProvider(ProviderPreference.LOCAL_ONLY)
        }
    }

    @Test
    fun `LOCAL_ONLY does not fallback to cloud providers`() = runTest {
        val onDevice = FakeLLMProvider("on-device", isAvailable = false)
        val openAI = FakeLLMProvider("openai", isAvailable = true)
        val factory = LLMProviderFactory(
            openAIProvider = openAI,
            anthropicProvider = null,
            onDeviceProvider = onDevice
        )

        assertFailsWith<ProviderUnavailableException> {
            factory.getProvider(ProviderPreference.LOCAL_ONLY)
        }
    }

    // === Fallback Chain Tests ===

    @Test
    fun `fallback chain includes on-device before cloud`() = runTest {
        val onDevice = FakeLLMProvider("on-device", isAvailable = true)
        val openAI = FakeLLMProvider("openai", isAvailable = true)
        val anthropic = FakeLLMProvider("anthropic", isAvailable = true)
        val factory = LLMProviderFactory(
            openAIProvider = openAI,
            anthropicProvider = anthropic,
            onDeviceProvider = onDevice
        )

        val providers = factory.getProvidersWithFallback(ProviderPreference.CHEAPEST)

        // On-device should be first (as primary for CHEAPEST)
        assertEquals(onDevice, providers.first())
        // Cloud providers should be fallbacks
        assertTrue(providers.contains(openAI))
        assertTrue(providers.contains(anthropic))
    }

    @Test
    fun `fallback chain includes on-device even for BEST_QUALITY`() = runTest {
        val onDevice = FakeLLMProvider("on-device", isAvailable = true)
        val anthropic = FakeLLMProvider("claude-sonnet", isAvailable = true)
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = anthropic,
            onDeviceProvider = onDevice
        )

        val providers = factory.getProvidersWithFallback(ProviderPreference.BEST_QUALITY)

        // Anthropic should be primary for BEST_QUALITY
        assertEquals(anthropic, providers.first())
        // On-device should be in fallback chain
        assertTrue(providers.contains(onDevice))
        assertEquals(2, providers.size)
    }

    @Test
    fun `fallback chain handles null providers gracefully`() = runTest {
        val onDevice = FakeLLMProvider("on-device", isAvailable = true)
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = null,
            onDeviceProvider = onDevice
        )

        val providers = factory.getProvidersWithFallback(ProviderPreference.CHEAPEST)

        assertEquals(1, providers.size)
        assertEquals(onDevice, providers.first())
    }

    // === Availability Tests ===

    @Test
    fun `hasAnyProvider returns true when only on-device available`() = runTest {
        val onDevice = FakeLLMProvider("on-device", isAvailable = true)
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = null,
            onDeviceProvider = onDevice
        )

        assertTrue(factory.hasAnyProvider())
    }

    @Test
    fun `getAvailableProviders includes on-device`() = runTest {
        val onDevice = FakeLLMProvider("on-device", isAvailable = true)
        val openAI = FakeLLMProvider("openai", isAvailable = true)
        val factory = LLMProviderFactory(
            openAIProvider = openAI,
            anthropicProvider = null,
            onDeviceProvider = onDevice
        )

        val available = factory.getAvailableProviders()

        assertTrue(available.contains(onDevice))
        assertTrue(available.contains(openAI))
        assertEquals(2, available.size)
    }

    @Test
    fun `getAvailableProviders excludes unavailable on-device`() = runTest {
        val onDevice = FakeLLMProvider("on-device", isAvailable = false)
        val openAI = FakeLLMProvider("openai", isAvailable = true)
        val factory = LLMProviderFactory(
            openAIProvider = openAI,
            anthropicProvider = null,
            onDeviceProvider = onDevice
        )

        val available = factory.getAvailableProviders()

        assertEquals(1, available.size)
        assertEquals(openAI, available.first())
    }

    // === Fake Provider for Testing ===

    private class FakeLLMProvider(
        override val name: String,
        private val isAvailable: Boolean = true
    ) : LLMProvider {
        override val availableModels: List<ModelConfig> = emptyList()

        override suspend fun isAvailable(): Boolean = isAvailable

        override suspend fun complete(request: CompletionRequest): CompletionResponse {
            return CompletionResponse(
                content = "test response",
                inputTokens = 10,
                outputTokens = 10,
                model = name,
                finishReason = FinishReason.STOP
            )
        }

        override suspend fun chat(messages: List<ChatMessage>): CompletionResponse {
            return complete(CompletionRequest(prompt = "test"))
        }

        override suspend fun structuredOutput(
            prompt: String,
            schema: String
        ): CompletionResponse {
            return complete(CompletionRequest(prompt = prompt))
        }
    }
}

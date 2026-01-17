package com.finuts.ai.providers

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LLMProviderFactoryTest {

    // === Fake Providers for Testing ===

    private class FakeOpenAIProvider(override val name: String = "openai-mini") : LLMProvider {
        override val availableModels: List<ModelConfig> = emptyList()
        override suspend fun isAvailable(): Boolean = true
        override suspend fun complete(request: CompletionRequest): CompletionResponse {
            return CompletionResponse(
                content = "OpenAI response",
                inputTokens = 100,
                outputTokens = 50,
                model = name
            )
        }
        override suspend fun chat(messages: List<ChatMessage>): CompletionResponse = complete(CompletionRequest(""))
        override suspend fun structuredOutput(prompt: String, schema: String): CompletionResponse = complete(CompletionRequest(""))
    }

    private class FakeAnthropicProvider(override val name: String = "anthropic-sonnet") : LLMProvider {
        override val availableModels: List<ModelConfig> = emptyList()
        override suspend fun isAvailable(): Boolean = true
        override suspend fun complete(request: CompletionRequest): CompletionResponse {
            return CompletionResponse(
                content = "Anthropic response",
                inputTokens = 100,
                outputTokens = 50,
                model = name
            )
        }
        override suspend fun chat(messages: List<ChatMessage>): CompletionResponse = complete(CompletionRequest(""))
        override suspend fun structuredOutput(prompt: String, schema: String): CompletionResponse = complete(CompletionRequest(""))
    }

    private class FakeOnDeviceProvider(override val name: String = "on-device") : LLMProvider {
        override val availableModels: List<ModelConfig> = emptyList()
        override suspend fun isAvailable(): Boolean = true
        override suspend fun complete(request: CompletionRequest): CompletionResponse {
            return CompletionResponse(
                content = "On-device response",
                inputTokens = 100,
                outputTokens = 50,
                model = name
            )
        }
        override suspend fun chat(messages: List<ChatMessage>): CompletionResponse = complete(CompletionRequest(""))
        override suspend fun structuredOutput(prompt: String, schema: String): CompletionResponse = complete(CompletionRequest(""))
    }

    // === FAST_CHEAP Preference ===

    @Test
    fun `FAST_CHEAP returns mini provider when available`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider("gpt-4o-mini"),
            anthropicProvider = FakeAnthropicProvider("claude-sonnet"),
            onDeviceProvider = null
        )

        val provider = factory.getProvider(ProviderPreference.FAST_CHEAP)

        assertTrue(provider.name.contains("mini", ignoreCase = true))
    }

    @Test
    fun `FAST_CHEAP returns haiku when only anthropic available`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = FakeAnthropicProvider("claude-3-5-haiku"),
            onDeviceProvider = null
        )

        val provider = factory.getProvider(ProviderPreference.FAST_CHEAP)

        assertTrue(provider.name.contains("haiku", ignoreCase = true))
    }

    // === BEST_QUALITY Preference ===

    @Test
    fun `BEST_QUALITY prefers sonnet when available`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider("gpt-4o"),
            anthropicProvider = FakeAnthropicProvider("claude-sonnet-4"),
            onDeviceProvider = null
        )

        val provider = factory.getProvider(ProviderPreference.BEST_QUALITY)

        assertTrue(provider.name.contains("sonnet", ignoreCase = true))
    }

    @Test
    fun `BEST_QUALITY falls back to GPT-4o when no anthropic`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider("gpt-4o"),
            anthropicProvider = null,
            onDeviceProvider = null
        )

        val provider = factory.getProvider(ProviderPreference.BEST_QUALITY)

        assertEquals("gpt-4o", provider.name)
    }

    // === LOCAL_ONLY Preference ===

    @Test
    fun `LOCAL_ONLY returns on-device provider`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider(),
            anthropicProvider = FakeAnthropicProvider(),
            onDeviceProvider = FakeOnDeviceProvider()
        )

        val provider = factory.getProvider(ProviderPreference.LOCAL_ONLY)

        assertEquals("on-device", provider.name)
    }

    @Test
    fun `LOCAL_ONLY throws when no on-device provider`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider(),
            anthropicProvider = FakeAnthropicProvider(),
            onDeviceProvider = null
        )

        assertFailsWith<ProviderUnavailableException> {
            factory.getProvider(ProviderPreference.LOCAL_ONLY)
        }
    }

    // === CHEAPEST Preference ===

    @Test
    fun `CHEAPEST prefers on-device when available`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider(),
            anthropicProvider = FakeAnthropicProvider(),
            onDeviceProvider = FakeOnDeviceProvider()
        )

        val provider = factory.getProvider(ProviderPreference.CHEAPEST)

        assertEquals("on-device", provider.name)
    }

    @Test
    fun `CHEAPEST falls back to mini when no on-device`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider("gpt-4o-mini"),
            anthropicProvider = FakeAnthropicProvider("claude-sonnet"),
            onDeviceProvider = null
        )

        val provider = factory.getProvider(ProviderPreference.CHEAPEST)

        assertTrue(provider.name.contains("mini", ignoreCase = true))
    }

    // === STRUCTURED_OUTPUT Preference ===

    @Test
    fun `STRUCTURED_OUTPUT prefers anthropic`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider(),
            anthropicProvider = FakeAnthropicProvider("claude-sonnet"),
            onDeviceProvider = null
        )

        val provider = factory.getProvider(ProviderPreference.STRUCTURED_OUTPUT)

        assertTrue(provider.name.contains("anthropic") || provider.name.contains("claude", ignoreCase = true))
    }

    // === Fallback Chain ===

    @Test
    fun `getProvidersWithFallback returns all available providers`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider(),
            anthropicProvider = FakeAnthropicProvider(),
            onDeviceProvider = FakeOnDeviceProvider()
        )

        val providers = factory.getProvidersWithFallback(ProviderPreference.FAST_CHEAP)

        assertEquals(3, providers.size)
    }

    @Test
    fun `getProvidersWithFallback primary provider is first`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider("gpt-4o-mini"),
            anthropicProvider = FakeAnthropicProvider(),
            onDeviceProvider = null
        )

        val providers = factory.getProvidersWithFallback(ProviderPreference.FAST_CHEAP)

        assertTrue(providers.first().name.contains("mini", ignoreCase = true))
    }

    // === No Provider Available ===

    @Test
    fun `throws when no providers configured`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = null,
            onDeviceProvider = null
        )

        assertFailsWith<ProviderUnavailableException> {
            factory.getProvider(ProviderPreference.FAST_CHEAP)
        }
    }

    // === hasAnyProvider Tests ===

    @Test
    fun `hasAnyProvider returns true when at least one provider available`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider(),
            anthropicProvider = null,
            onDeviceProvider = null
        )

        val result = factory.hasAnyProvider()

        assertTrue(result)
    }

    @Test
    fun `hasAnyProvider returns false when no providers`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = null,
            onDeviceProvider = null
        )

        val result = factory.hasAnyProvider()

        assertEquals(false, result)
    }

    // === getAvailableProviders Tests ===

    @Test
    fun `getAvailableProviders returns all available`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider(),
            anthropicProvider = FakeAnthropicProvider(),
            onDeviceProvider = FakeOnDeviceProvider()
        )

        val providers = factory.getAvailableProviders()

        assertEquals(3, providers.size)
    }

    @Test
    fun `getAvailableProviders returns empty when none available`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = null,
            onDeviceProvider = null
        )

        val providers = factory.getAvailableProviders()

        assertTrue(providers.isEmpty())
    }

    @Test
    fun `getAvailableProviders filters unavailable providers`() = runTest {
        val unavailableProvider = object : LLMProvider {
            override val name: String = "unavailable"
            override val availableModels: List<ModelConfig> = emptyList()
            override suspend fun isAvailable(): Boolean = false
            override suspend fun complete(request: CompletionRequest): CompletionResponse =
                CompletionResponse("", 0, 0, "")
            override suspend fun chat(messages: List<ChatMessage>): CompletionResponse =
                complete(CompletionRequest(""))
            override suspend fun structuredOutput(prompt: String, schema: String): CompletionResponse =
                complete(CompletionRequest(""))
        }
        val factory = LLMProviderFactory(
            openAIProvider = unavailableProvider,
            anthropicProvider = FakeAnthropicProvider(),
            onDeviceProvider = null
        )

        val providers = factory.getAvailableProviders()

        assertEquals(1, providers.size)
        assertTrue(providers.first().name.contains("anthropic", ignoreCase = true))
    }

    // === LOCAL_ONLY with unavailable on-device ===

    @Test
    fun `LOCAL_ONLY throws when on-device not available`() = runTest {
        val unavailableOnDevice = object : LLMProvider {
            override val name: String = "on-device"
            override val availableModels: List<ModelConfig> = emptyList()
            override suspend fun isAvailable(): Boolean = false
            override suspend fun complete(request: CompletionRequest): CompletionResponse =
                CompletionResponse("", 0, 0, "")
            override suspend fun chat(messages: List<ChatMessage>): CompletionResponse =
                complete(CompletionRequest(""))
            override suspend fun structuredOutput(prompt: String, schema: String): CompletionResponse =
                complete(CompletionRequest(""))
        }
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = null,
            onDeviceProvider = unavailableOnDevice
        )

        assertFailsWith<ProviderUnavailableException> {
            factory.getProvider(ProviderPreference.LOCAL_ONLY)
        }
    }

    // === CHEAPEST with unavailable on-device ===

    @Test
    fun `CHEAPEST skips unavailable on-device and uses mini`() = runTest {
        val unavailableOnDevice = object : LLMProvider {
            override val name: String = "on-device"
            override val availableModels: List<ModelConfig> = emptyList()
            override suspend fun isAvailable(): Boolean = false
            override suspend fun complete(request: CompletionRequest): CompletionResponse =
                CompletionResponse("", 0, 0, "")
            override suspend fun chat(messages: List<ChatMessage>): CompletionResponse =
                complete(CompletionRequest(""))
            override suspend fun structuredOutput(prompt: String, schema: String): CompletionResponse =
                complete(CompletionRequest(""))
        }
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider("gpt-4o-mini"),
            anthropicProvider = null,
            onDeviceProvider = unavailableOnDevice
        )

        val provider = factory.getProvider(ProviderPreference.CHEAPEST)

        assertTrue(provider.name.contains("mini", ignoreCase = true))
    }

    // === getProvidersWithFallback edge cases ===

    @Test
    fun `getProvidersWithFallback handles no primary provider`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = FakeAnthropicProvider(),
            onDeviceProvider = null
        )

        // FAST_CHEAP with no mini providers would throw, but fallback should still work
        val providers = factory.getProvidersWithFallback(ProviderPreference.FAST_CHEAP)

        assertEquals(1, providers.size)
    }

    // === FAST_CHEAP fallback chain ===

    @Test
    fun `FAST_CHEAP falls back to openai when no mini and no haiku`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider("gpt-4o"),
            anthropicProvider = null,
            onDeviceProvider = null
        )

        val provider = factory.getProvider(ProviderPreference.FAST_CHEAP)

        assertEquals("gpt-4o", provider.name)
    }

    @Test
    fun `FAST_CHEAP falls back to anthropic when only anthropic available`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = FakeAnthropicProvider("claude-sonnet"),
            onDeviceProvider = null
        )

        val provider = factory.getProvider(ProviderPreference.FAST_CHEAP)

        assertTrue(provider.name.contains("sonnet", ignoreCase = true))
    }

    // === BEST_QUALITY fallback chain ===

    @Test
    fun `BEST_QUALITY falls back to anthropic when no sonnet but anthropic available`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = FakeAnthropicProvider("claude-haiku"),
            onDeviceProvider = null
        )

        val provider = factory.getProvider(ProviderPreference.BEST_QUALITY)

        assertTrue(provider.name.contains("haiku", ignoreCase = true))
    }

    @Test
    fun `BEST_QUALITY throws when no providers`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = null,
            onDeviceProvider = null
        )

        assertFailsWith<ProviderUnavailableException> {
            factory.getProvider(ProviderPreference.BEST_QUALITY)
        }
    }

    // === STRUCTURED_OUTPUT fallback ===

    @Test
    fun `STRUCTURED_OUTPUT falls back to openai when no anthropic`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider(),
            anthropicProvider = null,
            onDeviceProvider = null
        )

        val provider = factory.getProvider(ProviderPreference.STRUCTURED_OUTPUT)

        assertTrue(provider.name.contains("openai", ignoreCase = true) || provider.name.contains("mini"))
    }

    @Test
    fun `STRUCTURED_OUTPUT throws when no providers`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = null,
            onDeviceProvider = null
        )

        assertFailsWith<ProviderUnavailableException> {
            factory.getProvider(ProviderPreference.STRUCTURED_OUTPUT)
        }
    }

    // === CHEAPEST fallback chain ===

    @Test
    fun `CHEAPEST falls back to haiku when no mini`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = FakeOpenAIProvider("gpt-4o"),
            anthropicProvider = FakeAnthropicProvider("claude-haiku"),
            onDeviceProvider = null
        )

        val provider = factory.getProvider(ProviderPreference.CHEAPEST)

        assertTrue(provider.name.contains("haiku", ignoreCase = true))
    }

    @Test
    fun `CHEAPEST throws when no providers available`() = runTest {
        val factory = LLMProviderFactory(
            openAIProvider = null,
            anthropicProvider = null,
            onDeviceProvider = null
        )

        assertFailsWith<ProviderUnavailableException> {
            factory.getProvider(ProviderPreference.CHEAPEST)
        }
    }
}

package com.finuts.ai.providers

/**
 * Factory for selecting LLM providers based on task requirements.
 *
 * Implements routing logic:
 * - Simple tasks → cheapest provider (GPT-4o-mini / Haiku)
 * - Complex tasks → best quality (Claude Sonnet / GPT-4o)
 * - Document parsing → structured output (Claude Sonnet)
 * - Privacy-critical → local only
 */
class LLMProviderFactory(
    private val openAIProvider: LLMProvider?,
    private val anthropicProvider: LLMProvider?,
    private val onDeviceProvider: LLMProvider?
) {
    /**
     * Get the best provider for a given preference.
     * @throws ProviderUnavailableException if no suitable provider is available
     */
    suspend fun getProvider(preference: ProviderPreference): LLMProvider {
        return when (preference) {
            ProviderPreference.FAST_CHEAP -> {
                // Try GPT-4o-mini first (cheapest), then Haiku
                openAIProvider?.takeIf { it.name.contains("mini", ignoreCase = true) }
                    ?: anthropicProvider?.takeIf { it.name.contains("haiku", ignoreCase = true) }
                    ?: openAIProvider
                    ?: anthropicProvider
                    ?: throw ProviderUnavailableException("cheap", "No cheap provider available")
            }

            ProviderPreference.BEST_QUALITY -> {
                // Prefer Claude Sonnet for best reasoning, then GPT-4o
                anthropicProvider?.takeIf { it.name.contains("sonnet", ignoreCase = true) }
                    ?: openAIProvider?.takeIf { !it.name.contains("mini", ignoreCase = true) }
                    ?: anthropicProvider
                    ?: openAIProvider
                    ?: throw ProviderUnavailableException("quality", "No quality provider available")
            }

            ProviderPreference.STRUCTURED_OUTPUT -> {
                // Prefer Claude for structured output (100% schema compliance)
                anthropicProvider
                    ?: openAIProvider
                    ?: throw ProviderUnavailableException(
                        "structured",
                        "No provider with structured output available"
                    )
            }

            ProviderPreference.LOCAL_ONLY -> {
                val provider = onDeviceProvider
                    ?: throw ProviderUnavailableException(
                        "local",
                        "No on-device provider configured"
                    )
                // Check if actually available (model downloaded)
                if (!provider.isAvailable()) {
                    throw ProviderUnavailableException(
                        "local",
                        "On-device provider not available (model not downloaded)"
                    )
                }
                provider
            }

            ProviderPreference.CHEAPEST -> {
                // Order: on-device (free) → mini models → full models
                // Check on-device availability since model might not be downloaded
                val onDevice = onDeviceProvider?.takeIf { it.isAvailable() }
                onDevice
                    ?: openAIProvider?.takeIf { it.name.contains("mini", ignoreCase = true) }
                    ?: anthropicProvider?.takeIf { it.name.contains("haiku", ignoreCase = true) }
                    ?: openAIProvider
                    ?: anthropicProvider
                    ?: throw ProviderUnavailableException("any", "No provider available")
            }
        }
    }

    /**
     * Get provider with fallback chain.
     * Returns list of providers to try in order.
     */
    suspend fun getProvidersWithFallback(preference: ProviderPreference): List<LLMProvider> {
        val providers = mutableListOf<LLMProvider>()

        // Add primary provider
        try {
            providers.add(getProvider(preference))
        } catch (_: ProviderUnavailableException) {
            // No primary, continue
        }

        // Add fallbacks (all available providers not already added)
        listOfNotNull(anthropicProvider, openAIProvider, onDeviceProvider)
            .filter { it !in providers }
            .forEach { providers.add(it) }

        return providers
    }

    /**
     * Check if any provider is available.
     */
    suspend fun hasAnyProvider(): Boolean {
        return listOfNotNull(openAIProvider, anthropicProvider, onDeviceProvider)
            .any { it.isAvailable() }
    }

    /**
     * Get all available providers.
     */
    suspend fun getAvailableProviders(): List<LLMProvider> {
        return listOfNotNull(openAIProvider, anthropicProvider, onDeviceProvider)
            .filter { it.isAvailable() }
    }
}

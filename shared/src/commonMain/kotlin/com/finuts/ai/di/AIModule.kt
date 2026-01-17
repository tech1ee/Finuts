package com.finuts.ai.di

import com.finuts.ai.FinutsAIService
import com.finuts.ai.FinutsAIServiceImpl
import com.finuts.ai.context.UserContextManager
import com.finuts.ai.cost.AICostTracker
import com.finuts.ai.inference.InferenceEngine
import com.finuts.ai.orchestration.AIOrchestrator
import com.finuts.ai.privacy.PIIAnonymizer
import com.finuts.ai.privacy.RegexPIIAnonymizer
import com.finuts.ai.providers.LLMProviderFactory
import com.finuts.ai.providers.OnDeviceLLMProvider
import com.finuts.data.categorization.AICategorizer
import com.finuts.domain.registry.IconRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for AI components.
 *
 * Provides:
 * - FinutsAIService (unified entry point)
 * - AIOrchestrator (task routing)
 * - LLMProviderFactory (provider management)
 * - PIIAnonymizer (privacy)
 * - AICostTracker (cost management)
 * - UserContextManager (personalization)
 */
val aiModule: Module = module {

    // Privacy
    single<PIIAnonymizer> { RegexPIIAnonymizer() }

    // Cost tracking
    single {
        AICostTracker(
            dailyBudget = 0.10f, // $0.10/day
            monthlyBudget = 2.0f // $2.00/month
        )
    }

    // On-device LLM provider (Tier 1.5)
    // InferenceEngine is registered in platform modules (Android/iOS)
    // Creates OnDeviceLLMProvider only when InferenceEngine is available
    single<OnDeviceLLMProvider?> {
        val engine: InferenceEngine? = getOrNull()
        if (engine != null) {
            OnDeviceLLMProvider(
                modelRepository = get(),
                engine = engine
            )
        } else {
            null
        }
    }

    // Provider factory (providers injected from platform modules)
    single {
        LLMProviderFactory(
            openAIProvider = getOrNull(),
            anthropicProvider = getOrNull(),
            onDeviceProvider = getOrNull()
        )
    }

    // Orchestrator
    single {
        AIOrchestrator(
            providerFactory = get(),
            costTracker = get(),
            anonymizer = get()
        )
    }

    // AI Categorizer (Tier 2)
    single {
        // Provider is now selected at runtime via AIOrchestrator
        // since getProvider() is suspend and can't be called in DI
        AICategorizer(
            provider = null, // TODO: Refactor to use factory directly
            categoryRepository = get(),
            anonymizer = get(),
            costTracker = get(),
            iconRegistry = get()
        )
    }

    // User context
    single { UserContextManager(get()) }

    // Unified AI service
    single<FinutsAIService> {
        FinutsAIServiceImpl(
            orchestrator = get(),
            categorizer = get(),
            contextManager = get(),
            learnFromCorrection = get()
        )
    }
}

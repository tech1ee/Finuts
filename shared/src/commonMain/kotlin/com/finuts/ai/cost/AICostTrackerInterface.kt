package com.finuts.ai.cost

/**
 * Interface for AI cost tracking.
 *
 * Follows DIP (Dependency Inversion Principle) - high-level modules
 * should depend on abstractions, not concrete implementations.
 *
 * Used by LLMDocumentParser and other AI components to check budget
 * before making API calls.
 */
interface AICostTrackerInterface {

    /**
     * Check if an operation with estimated cost can be executed
     * within current budget limits.
     *
     * @param estimatedCost Estimated cost in USD
     * @return true if budget allows, false otherwise
     */
    fun canExecute(estimatedCost: Float): Boolean

    /**
     * Record usage after an API call.
     *
     * @param inputTokens Number of input tokens used
     * @param outputTokens Number of output tokens generated
     * @param model Model identifier for cost calculation
     */
    fun record(inputTokens: Int, outputTokens: Int, model: String)

    /**
     * Get current usage statistics.
     */
    fun getUsageStats(): UsageStats
}

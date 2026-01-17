package com.finuts.domain.registry

/**
 * Interface for accessing category metadata and icons.
 * Allows mocking in tests.
 */
interface IconRegistryProvider {
    /**
     * Get metadata for a known category.
     * @param id Category ID (e.g., "groceries", "transport")
     * @return CategoryMetadata if found, null otherwise
     */
    fun getCategoryMetadata(id: String): CategoryMetadata?

    /**
     * Get all known category IDs.
     */
    fun getAllKnownCategoryIds(): Set<String>

    /**
     * Find the best matching icon for a hint.
     * @param hint Icon hint from LLM suggestion
     * @return Icon name that best matches the hint
     */
    fun findBestMatch(hint: String): String
}

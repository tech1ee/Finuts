package com.finuts.data.categorization

/**
 * Interface for merchant name normalization.
 * Implementations normalize merchant names for consistent pattern matching,
 * enabling reusable merchant-to-category mappings.
 */
interface MerchantNormalizerInterface {

    /**
     * Normalize a merchant name for pattern matching.
     *
     * @param merchantName Raw merchant name from transaction
     * @return Normalized merchant pattern for storage and matching.
     *         Returns original uppercase trimmed name if normalization produces empty string.
     */
    fun normalize(merchantName: String): String

    /**
     * Extract key words from merchant name for fuzzy matching.
     * Returns the most significant words (filtering out common ones).
     *
     * @param merchantName Raw or normalized merchant name
     * @return List of key words for matching
     */
    fun extractKeywords(merchantName: String): List<String>

    /**
     * Check if two merchant names are similar enough to be considered the same.
     *
     * @param merchant1 First merchant name
     * @param merchant2 Second merchant name
     * @return True if merchants are likely the same
     */
    fun isSimilar(merchant1: String, merchant2: String): Boolean

    /**
     * Create a storable pattern from normalized merchant name.
     * Returns the primary keyword(s) for SQL LIKE matching.
     *
     * @param normalizedName Normalized merchant name
     * @return Pattern string for database storage (SQL LIKE compatible)
     */
    fun toPattern(normalizedName: String): String
}

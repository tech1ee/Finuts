package com.finuts.test.fakes

import com.finuts.data.categorization.MerchantNormalizerInterface

/**
 * Fake implementation of MerchantNormalizerInterface for testing.
 * Provides configurable responses for merchant normalization.
 */
class FakeMerchantNormalizer : MerchantNormalizerInterface {

    private var normalizeResponses = mutableMapOf<String, String>()
    private var keywordsResponses = mutableMapOf<String, List<String>>()
    private var similarityResponses = mutableMapOf<Pair<String, String>, Boolean>()
    private var patternResponses = mutableMapOf<String, String>()

    override fun normalize(merchantName: String): String {
        return normalizeResponses[merchantName]
            ?: normalizeDefault(merchantName)
    }

    override fun extractKeywords(merchantName: String): List<String> {
        return keywordsResponses[merchantName]
            ?: extractKeywordsDefault(merchantName)
    }

    override fun isSimilar(merchant1: String, merchant2: String): Boolean {
        return similarityResponses[Pair(merchant1, merchant2)]
            ?: similarityResponses[Pair(merchant2, merchant1)]
            ?: isSimilarDefault(merchant1, merchant2)
    }

    override fun toPattern(normalizedName: String): String {
        return patternResponses[normalizedName]
            ?: toPatternDefault(normalizedName)
    }

    // Test helpers

    fun setNormalizeResponse(input: String, result: String) {
        normalizeResponses[input] = result
    }

    fun setKeywordsResponse(input: String, keywords: List<String>) {
        keywordsResponses[input] = keywords
    }

    fun setSimilarityResponse(merchant1: String, merchant2: String, result: Boolean) {
        similarityResponses[Pair(merchant1, merchant2)] = result
    }

    fun setPatternResponse(normalizedName: String, pattern: String) {
        patternResponses[normalizedName] = pattern
    }

    fun reset() {
        normalizeResponses.clear()
        keywordsResponses.clear()
        similarityResponses.clear()
        patternResponses.clear()
    }

    // Default implementations for testing

    private fun normalizeDefault(merchantName: String): String {
        if (merchantName.isBlank()) return ""
        return merchantName.uppercase().trim()
    }

    private fun extractKeywordsDefault(merchantName: String): List<String> {
        val normalized = normalize(merchantName)
        if (normalized.isBlank()) return emptyList()
        return normalized.split(" ").filter { it.length >= 2 }.take(5)
    }

    private fun isSimilarDefault(merchant1: String, merchant2: String): Boolean {
        val norm1 = normalize(merchant1)
        val norm2 = normalize(merchant2)
        if (norm1.isBlank() || norm2.isBlank()) return false
        return norm1 == norm2 || norm1.contains(norm2) || norm2.contains(norm1)
    }

    private fun toPatternDefault(normalizedName: String): String {
        if (normalizedName.isBlank()) return ""
        val keywords = extractKeywords(normalizedName)
        return keywords.take(2).joinToString(" ")
    }
}

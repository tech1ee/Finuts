package com.finuts.data.categorization

/**
 * Normalizes merchant names for consistent pattern matching.
 * Used by the learning system to create reusable merchant mappings.
 *
 * Normalization steps:
 * 1. Convert to uppercase
 * 2. Remove common suffixes (TOO, ТОО, LLC, LTD, etc.)
 * 3. Remove location/branch prefixes (city names, branch numbers)
 * 4. Remove card/terminal identifiers
 * 5. Remove special characters except spaces
 * 6. Collapse multiple spaces
 * 7. Trim whitespace
 */
object MerchantNormalizer {

    // Common business suffixes to remove
    private val businessSuffixes = listOf(
        // Kazakhstan
        "ТОО", "АО", "ИП", "КХ", "ПК",
        // International
        "LLC", "LTD", "INC", "CORP", "CO", "PLC", "GMBH", "AG", "SA"
    )

    // Location prefixes to remove (cities, branches)
    private val locationPrefixes = listOf(
        "ALMATY", "АЛМАТЫ", "ASTANA", "АСТАНА", "NUR-SULTAN", "НУР-СУЛТАН",
        "SHYMKENT", "ШЫМКЕНТ", "КАРАГАНДА", "KARAGANDA", "АКТОБЕ", "AKTOBE",
        "BRANCH", "ФИЛИАЛ", "ОТДЕЛЕНИЕ"
    )

    // Patterns to remove (card terminals, transaction IDs, etc.)
    private val removePatterns = listOf(
        Regex("\\*+\\d+"), // Card mask like *1234
        Regex("\\d{6,}"), // Long numbers (terminal IDs)
        Regex("POS\\s*\\d*", RegexOption.IGNORE_CASE),
        Regex("TERMINAL\\s*\\d*", RegexOption.IGNORE_CASE),
        Regex("ТЕРМИНАЛ\\s*\\d*", RegexOption.IGNORE_CASE),
        Regex("\\d{2}[./]\\d{2}[./]\\d{2,4}"), // Dates
        Regex("\\d{2}:\\d{2}(:\\d{2})?"), // Times
        Regex("KZT|KZ|₸"), // Currency
        Regex("#\\d+") // Order numbers
    )

    /**
     * Normalize a merchant name for pattern matching.
     *
     * @param merchantName Raw merchant name from transaction
     * @return Normalized merchant pattern for storage and matching.
     *         Returns original uppercase trimmed name if normalization produces empty string.
     */
    fun normalize(merchantName: String): String {
        if (merchantName.isBlank()) return ""

        val original = merchantName.uppercase().trim()
        var result = original

        // Remove patterns (terminal IDs, dates, etc.)
        for (pattern in removePatterns) {
            result = pattern.replace(result, " ")
        }

        // Remove business suffixes
        for (suffix in businessSuffixes) {
            result = result.replace(Regex("\\b$suffix\\b"), " ")
        }

        // Remove location prefixes at start
        for (prefix in locationPrefixes) {
            result = result.replace(Regex("^$prefix\\s+"), "")
            result = result.replace(Regex("\\b$prefix\\b"), " ")
        }

        // Remove special characters except letters and spaces
        result = result.replace(Regex("[^A-ZА-ЯЁ\\s]"), " ")

        // Collapse multiple spaces
        result = result.replace(Regex("\\s+"), " ").trim()

        // Fallback: if normalization removed everything, use first word of original
        if (result.isBlank()) {
            // Extract first alphabetic word from original
            val firstWord = original.split(Regex("[^A-ZА-ЯЁ]+"))
                .firstOrNull { it.isNotBlank() && it.length >= 2 }
            return firstWord ?: original.take(20).trim()
        }

        return result
    }

    /**
     * Extract key words from merchant name for fuzzy matching.
     * Returns the most significant words (filtering out common ones).
     *
     * @param merchantName Raw or normalized merchant name
     * @return List of key words for matching
     */
    fun extractKeywords(merchantName: String): List<String> {
        val normalized = normalize(merchantName)
        if (normalized.isBlank()) return emptyList()

        val commonWords = setOf(
            "THE", "AND", "OF", "FOR", "IN", "AT", "TO", "BY",
            "И", "В", "НА", "ДЛЯ", "ИЗ", "ОТ", "ПО", "С"
        )

        return normalized.split(" ")
            .filter { it.length >= 2 && it !in commonWords }
            .take(5) // Keep max 5 keywords
    }

    /**
     * Check if two merchant names are similar enough to be considered the same.
     *
     * @param merchant1 First merchant name
     * @param merchant2 Second merchant name
     * @return True if merchants are likely the same
     */
    fun isSimilar(merchant1: String, merchant2: String): Boolean {
        val norm1 = normalize(merchant1)
        val norm2 = normalize(merchant2)

        if (norm1.isBlank() || norm2.isBlank()) return false

        // Exact match after normalization
        if (norm1 == norm2) return true

        // One contains the other
        if (norm1.contains(norm2) || norm2.contains(norm1)) return true

        // Check keyword overlap
        val keywords1 = extractKeywords(merchant1).toSet()
        val keywords2 = extractKeywords(merchant2).toSet()

        if (keywords1.isEmpty() || keywords2.isEmpty()) return false

        val intersection = keywords1.intersect(keywords2)
        val union = keywords1.union(keywords2)
        val jaccardSimilarity = intersection.size.toFloat() / union.size

        return jaccardSimilarity >= 0.5f
    }

    /**
     * Create a storable pattern from normalized merchant name.
     * Returns the primary keyword(s) for SQL LIKE matching.
     *
     * Strategy: Extract first 1-2 significant words that uniquely identify
     * the merchant. This enables efficient SQL LIKE queries.
     *
     * Examples:
     * - "MAGNUM SUPER MARKET" → "MAGNUM SUPER"
     * - "GLOVO ORDER" → "GLOVO"
     * - "KASPI GOLD" → "KASPI GOLD"
     *
     * @param normalizedName Normalized merchant name
     * @return Pattern string for database storage (SQL LIKE compatible)
     */
    fun toPattern(normalizedName: String): String {
        if (normalizedName.isBlank()) return ""

        val keywords = extractKeywords(normalizedName)
        if (keywords.isEmpty()) return normalizedName

        // Take first 2 keywords max for pattern (balance specificity vs matching)
        return keywords.take(2).joinToString(" ")
    }
}

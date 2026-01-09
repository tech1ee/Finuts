package com.finuts.data.import

import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.import.DuplicateStatus
import com.finuts.domain.entity.import.ImportedTransaction
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Fuzzy duplicate detector for imported transactions.
 * Uses Levenshtein distance for description similarity scoring.
 *
 * Algorithm:
 * 1. Compare date (exact or ±1 day tolerance)
 * 2. Compare amount (must be exact match)
 * 3. Normalize descriptions (uppercase, remove special chars, trim whitespace)
 * 4. Calculate Levenshtein distance for similarity score
 * 5. Thresholds: ≥0.95 = exact, ≥0.7 = probable, <0.7 = unique
 */
class FuzzyDuplicateDetector {

    companion object {
        private const val EXACT_MATCH_THRESHOLD = 0.95f
        private const val PROBABLE_MATCH_THRESHOLD = 0.5f
        private const val DATE_TOLERANCE_DAYS = 1
    }

    /**
     * Check if a single imported transaction is a duplicate.
     */
    fun checkDuplicate(
        imported: ImportedTransaction,
        existingTransactions: List<Transaction>
    ): DuplicateStatus {
        if (existingTransactions.isEmpty()) {
            return DuplicateStatus.Unique
        }

        var bestMatch: Pair<Transaction, Float>? = null

        for (existing in existingTransactions) {
            // Amount must match exactly
            if (imported.amount != existing.amount) continue

            // Date must be within tolerance
            if (!areDatesWithinTolerance(imported, existing)) continue

            // Calculate description similarity
            val normalizedImported = normalizeDescription(imported.description)
            val normalizedExisting = normalizeDescription(existing.description ?: "")

            val similarity = calculateSimilarity(normalizedImported, normalizedExisting)

            // Track best match
            if (bestMatch == null || similarity > bestMatch.second) {
                bestMatch = existing to similarity
            }
        }

        return when {
            bestMatch == null -> DuplicateStatus.Unique
            bestMatch.second >= EXACT_MATCH_THRESHOLD -> DuplicateStatus.ExactDuplicate(
                matchingTransactionId = bestMatch.first.id
            )
            bestMatch.second >= PROBABLE_MATCH_THRESHOLD -> DuplicateStatus.ProbableDuplicate(
                matchingTransactionId = bestMatch.first.id,
                similarity = bestMatch.second,
                reason = buildReason(bestMatch.second)
            )
            else -> DuplicateStatus.Unique
        }
    }

    /**
     * Check multiple imported transactions for duplicates.
     * Returns a map of index to duplicate status.
     */
    fun checkDuplicates(
        imported: List<ImportedTransaction>,
        existingTransactions: List<Transaction>
    ): Map<Int, DuplicateStatus> {
        return imported.mapIndexed { index, transaction ->
            index to checkDuplicate(transaction, existingTransactions)
        }.toMap()
    }

    private fun areDatesWithinTolerance(
        imported: ImportedTransaction,
        existing: Transaction
    ): Boolean {
        val existingDate = existing.date.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val daysDiff = abs(imported.date.toEpochDays() - existingDate.toEpochDays())
        return daysDiff <= DATE_TOLERANCE_DAYS
    }

    /**
     * Normalize description for comparison:
     * - Convert to uppercase
     * - Remove special characters (keep alphanumeric and spaces)
     * - Collapse multiple spaces to single space
     * - Trim whitespace
     */
    internal fun normalizeDescription(description: String): String {
        return description
            .uppercase()
            .replace(Regex("[^A-ZА-ЯЁ0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Calculate similarity between two strings using Levenshtein distance.
     * Returns a value between 0.0 (completely different) and 1.0 (identical).
     */
    internal fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f

        val distance = levenshteinDistance(s1, s2)
        val maxLength = max(s1.length, s2.length)

        return 1.0f - (distance.toFloat() / maxLength)
    }

    /**
     * Calculate Levenshtein edit distance between two strings.
     * Uses dynamic programming for O(m*n) time complexity.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length

        // Optimization: if one string is empty, distance is length of other
        if (m == 0) return n
        if (n == 0) return m

        // Use only two rows to save memory
        var previousRow = IntArray(n + 1) { it }
        var currentRow = IntArray(n + 1)

        for (i in 1..m) {
            currentRow[0] = i

            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                currentRow[j] = min(
                    min(
                        currentRow[j - 1] + 1,      // insertion
                        previousRow[j] + 1          // deletion
                    ),
                    previousRow[j - 1] + cost       // substitution
                )
            }

            // Swap rows
            val temp = previousRow
            previousRow = currentRow
            currentRow = temp
        }

        return previousRow[n]
    }

    private fun buildReason(similarity: Float): String {
        return when {
            similarity >= 0.9f -> "Same date and amount, similar description"
            similarity >= 0.8f -> "Same date and amount, partially matching description"
            else -> "Same date and amount"
        }
    }
}

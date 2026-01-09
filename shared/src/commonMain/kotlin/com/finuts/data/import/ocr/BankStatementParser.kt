package com.finuts.data.import.ocr

import com.finuts.data.import.utils.DateFormat
import com.finuts.data.import.utils.DateParser
import com.finuts.data.import.utils.NumberLocale
import com.finuts.data.import.utils.NumberParser
import com.finuts.domain.entity.import.ImportSource
import com.finuts.domain.entity.import.ImportedTransaction
import kotlinx.datetime.LocalDate

/**
 * Parser for converting OCR text output into transactions.
 *
 * Uses regex patterns to extract date, amount, and description
 * from various bank statement formats. Supports Russian and English text,
 * multiple date formats, and various number formats.
 */
class BankStatementParser {

    companion object {
        private const val BASE_CONFIDENCE = 0.7f
        private const val PAGE_BREAK_MARKER = "--- Page Break ---"
    }

    // Regex patterns for transaction detection
    private val transactionPatterns = listOf(
        // Pattern 1: DD.MM.YYYY Description Amount (with optional balance suffix)
        Regex("""(\d{1,2}[./]\d{1,2}[./]\d{2,4})\s+(.+?)\s+([+-]?\d[\d\s,.']*)\s*₸?(?:\s+[ОоOo]статок.*)?$"""),
        // Pattern 2: YYYY-MM-DD Description Amount (with optional balance suffix)
        Regex("""(\d{4}-\d{1,2}-\d{1,2})\s+(.+?)\s+([+-]?\d[\d\s,.']*)\s*₸?(?:\s+[ОоOo]статок.*)?$"""),
        // Pattern 3: Russian text date (15 января 2026)
        Regex("""(\d{1,2}\s+\p{L}+\s+\d{4})\s+(.+?)\s+([+-]?\d[\d\s,.']*)\s*₸?(?:\s+[ОоOo]статок.*)?$"""),
        // Pattern 4: Amount at start (for some formats)
        Regex("""([+-]?\d[\d\s,.']*)\s*₸?\s+(\d{1,2}[./]\d{1,2}[./]\d{2,4})\s+(.+)$""")
    )

    /**
     * Parse OCR text output into transactions.
     *
     * @param text Full OCR text from document
     * @param bankSignature Optional bank identifier for specialized parsing
     * @return List of parsed transactions
     */
    fun parseText(text: String, bankSignature: String?): List<ImportedTransaction> {
        val lines = text
            .lines()
            .filter { it.isNotBlank() }
            .filter { it != PAGE_BREAK_MARKER }

        val transactions = mutableListOf<ImportedTransaction>()

        for (line in lines) {
            val transaction = parseLine(line.trim())
            if (transaction != null) {
                transactions.add(transaction)
            }
        }

        return transactions
    }

    private fun parseLine(line: String): ImportedTransaction? {
        // Try each pattern
        for (pattern in transactionPatterns) {
            val match = pattern.find(line)
            if (match != null) {
                return parseMatch(match, line)
            }
        }
        return null
    }

    private fun parseMatch(match: MatchResult, originalLine: String): ImportedTransaction? {
        return try {
            val groups = match.groupValues

            // Determine which pattern matched based on group content
            val (dateStr, descriptionStr, amountStr) = extractGroups(groups)

            val date = parseDate(dateStr) ?: return null
            val amount = parseAmount(amountStr) ?: return null
            val description = cleanDescription(descriptionStr)
            val merchant = extractMerchant(description)

            ImportedTransaction(
                date = date,
                amount = amount,
                description = description,
                merchant = merchant,
                balance = null,
                category = null,
                confidence = calculateConfidence(match),
                source = ImportSource.DOCUMENT_AI,
                rawData = mapOf("line" to originalLine)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractGroups(groups: List<String>): Triple<String, String, String> {
        // Groups: [0] = full match, [1], [2], [3] = captured groups
        val g1 = groups.getOrElse(1) { "" }
        val g2 = groups.getOrElse(2) { "" }
        val g3 = groups.getOrElse(3) { "" }

        // Detect if g1 is amount (Pattern 4) or date
        return if (g1.startsWith('+') || g1.startsWith('-') || g1.first().isDigit() && g1.contains('.') && !g1.contains('/')) {
            // Pattern 4: Amount, Date, Description
            if (parseDate(g2) != null) {
                Triple(g2, g3, g1)
            } else {
                Triple(g1, g2, g3)
            }
        } else {
            // Standard: Date, Description, Amount
            Triple(g1, g2, g3)
        }
    }

    private fun parseDate(dateStr: String): LocalDate? {
        return try {
            DateParser.parseOrNull(dateStr.trim(), DateFormat.AUTO)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseAmount(amountStr: String): Long? {
        return try {
            val cleaned = amountStr
                .replace("₸", "")
                .replace(Regex("[\\s']"), " ")
                .trim()

            NumberParser.parse(cleaned, NumberLocale.AUTO)
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanDescription(description: String): String {
        return description
            .trim()
            .replace(Regex("\\s+"), " ")
            .removeSuffix("|")
            .removeSuffix("/")
            .trim()
    }

    private fun extractMerchant(description: String): String? {
        // Extract merchant from patterns like "Merchant | Details" or "Merchant / Details"
        val separators = listOf(" | ", " / ", " \\ ", ": ")

        for (separator in separators) {
            if (description.contains(separator)) {
                val parts = description.split(separator)
                if (parts.isNotEmpty()) {
                    return parts.first().trim().take(50)
                }
            }
        }

        // If no separator, try to extract first meaningful words
        val words = description.split(" ").take(3)
        if (words.isNotEmpty() && words.first().length >= 3) {
            return words.joinToString(" ").take(50)
        }

        return null
    }

    private fun calculateConfidence(match: MatchResult): Float {
        // Base confidence for OCR-parsed data
        var confidence = BASE_CONFIDENCE

        // Boost for full capture
        if (match.groupValues.all { it.isNotBlank() }) {
            confidence += 0.1f
        }

        // Boost for longer description (more context)
        val description = match.groupValues.getOrElse(2) { "" }
        if (description.length > 10) {
            confidence += 0.05f
        }

        return confidence.coerceIn(0f, 1f)
    }
}

package com.finuts.data.import.parsers

import com.finuts.data.import.utils.NumberLocale
import com.finuts.data.import.utils.NumberParserInterface
import com.finuts.domain.entity.import.DocumentType
import com.finuts.domain.entity.import.ImportResult
import com.finuts.domain.entity.import.ImportSource
import com.finuts.domain.entity.import.ImportedTransaction
import kotlinx.datetime.LocalDate

/**
 * Parser for QIF (Quicken Interchange Format) files.
 * Handles Bank, CCard, and other account types.
 */
class QifParser(
    private val numberParser: NumberParserInterface
) {

    /**
     * Parse QIF content and extract transactions.
     *
     * @param content QIF content as string
     * @param documentType Document type with account type info
     * @return ImportResult with parsed transactions
     */
    fun parse(content: String, documentType: DocumentType.Qif): ImportResult {
        if (content.isBlank()) {
            return ImportResult.Error("Empty QIF content", documentType, emptyList())
        }

        if (!isValidQif(content)) {
            return ImportResult.Error("Invalid QIF format - missing type header", documentType, emptyList())
        }

        val transactions = parseTransactions(content)

        if (transactions.isEmpty()) {
            return ImportResult.Error("No valid transactions found in QIF", documentType, emptyList())
        }

        return ImportResult.Success(
            transactions = transactions,
            documentType = documentType,
            totalConfidence = 0.92f
        )
    }

    private fun isValidQif(content: String): Boolean {
        return content.lines().any { it.startsWith("!Type:") }
    }

    private fun parseTransactions(content: String): List<ImportedTransaction> {
        val transactions = mutableListOf<ImportedTransaction>()
        var currentTransaction = TransactionBuilder()

        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            when {
                trimmed.startsWith("!") -> {
                    // Type header or other directive - skip
                }
                trimmed == "^" -> {
                    // End of transaction
                    currentTransaction.build()?.let { transactions.add(it) }
                    currentTransaction = TransactionBuilder()
                }
                trimmed.startsWith("D") -> {
                    currentTransaction.date = parseQifDate(trimmed.substring(1))
                }
                trimmed.startsWith("T") || trimmed.startsWith("U") -> {
                    currentTransaction.amount = parseAmount(trimmed.substring(1))
                }
                trimmed.startsWith("P") -> {
                    currentTransaction.payee = trimmed.substring(1).trim()
                }
                trimmed.startsWith("M") -> {
                    currentTransaction.memo = trimmed.substring(1).trim()
                }
                trimmed.startsWith("N") -> {
                    currentTransaction.number = trimmed.substring(1).trim()
                }
                trimmed.startsWith("L") -> {
                    currentTransaction.category = trimmed.substring(1).trim()
                }
            }
        }

        // Handle last transaction if not terminated with ^
        currentTransaction.build()?.let { transactions.add(it) }

        return transactions
    }

    private fun parseQifDate(dateStr: String): LocalDate? {
        val cleaned = dateStr.trim()
        if (cleaned.isEmpty()) return null

        // QIF dates can be in various formats:
        // MM/DD/YYYY, DD/MM/YYYY, MM/DD'YY, etc.
        val parts = cleaned.split('/', '-', '.')

        if (parts.size < 3) return null

        return try {
            val first = parts[0].toInt()
            val second = parts[1].toInt()
            val third = parseYear(parts[2])

            // Heuristic: if first > 12, it must be day (DD/MM/YYYY)
            // Otherwise assume MM/DD/YYYY (US format)
            val (month, day) = if (first > 12) {
                second to first
            } else if (second > 12) {
                first to second
            } else {
                // Ambiguous - default to US format
                first to second
            }

            LocalDate(third, month, day)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseYear(yearStr: String): Int {
        val year = yearStr.replace("'", "").toIntOrNull() ?: return 2000
        return when {
            year >= 100 -> year
            year >= 50 -> 1900 + year
            else -> 2000 + year
        }
    }

    private fun parseAmount(amountStr: String): Long? {
        return try {
            numberParser.parse(amountStr.trim(), NumberLocale.AUTO)
        } catch (_: Exception) {
            null
        }
    }

    private class TransactionBuilder {
        var date: LocalDate? = null
        var amount: Long? = null
        var payee: String? = null
        var memo: String? = null
        var number: String? = null
        var category: String? = null

        fun build(): ImportedTransaction? {
            val transactionDate = date ?: return null
            val transactionAmount = amount ?: return null

            val description = memo ?: payee ?: ""
            val merchant = payee

            val rawData = mutableMapOf<String, String>()
            payee?.let { rawData["P"] = it }
            memo?.let { rawData["M"] = it }
            number?.let { rawData["N"] = it }
            category?.let { rawData["L"] = it }

            return ImportedTransaction(
                date = transactionDate,
                amount = transactionAmount,
                description = description,
                merchant = merchant,
                balance = null,
                category = category,
                confidence = 0.92f,
                source = ImportSource.RULE_BASED,
                rawData = rawData
            )
        }
    }
}

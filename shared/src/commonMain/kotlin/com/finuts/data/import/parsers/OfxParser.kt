package com.finuts.data.import.parsers

import com.finuts.data.import.utils.NumberLocale
import com.finuts.data.import.utils.NumberParserInterface
import com.finuts.domain.entity.import.DocumentType
import com.finuts.domain.entity.import.ImportResult
import com.finuts.domain.entity.import.ImportSource
import com.finuts.domain.entity.import.ImportedTransaction
import kotlinx.datetime.LocalDate

/**
 * Parser for OFX (Open Financial Exchange) and QFX formats.
 * Supports both SGML (v1.x) and XML (v2.x) formats.
 */
class OfxParser(
    private val numberParser: NumberParserInterface
) {

    // Using [\s\S] instead of . with DOT_MATCHES_ALL for KMP compatibility
    private val transactionPattern = Regex(
        """<STMTTRN>([\s\S]*?)</STMTTRN>""",
        RegexOption.IGNORE_CASE
    )

    private val sgmlTagPattern = Regex("""<(\w+)>([^<\n]+)""")
    private val xmlTagPattern = Regex("""<(\w+)>([^<]*)</\1>""", RegexOption.IGNORE_CASE)

    /**
     * Parse OFX content and extract transactions.
     *
     * @param content OFX content as string
     * @param documentType Document type with version info
     * @return ImportResult with parsed transactions
     */
    fun parse(content: String, documentType: DocumentType.Ofx): ImportResult {
        if (content.isBlank()) {
            return ImportResult.Error("Empty OFX content", documentType, emptyList())
        }

        if (!isValidOfx(content)) {
            return ImportResult.Error("Invalid OFX format", documentType, emptyList())
        }

        val transactionMatches = transactionPattern.findAll(content)
        val transactions = mutableListOf<ImportedTransaction>()

        for (match in transactionMatches) {
            val transactionBlock = match.groupValues[1]
            try {
                val transaction = parseTransaction(transactionBlock)
                if (transaction != null) {
                    transactions.add(transaction)
                }
            } catch (_: Exception) {
                // Skip malformed transactions
            }
        }

        if (transactions.isEmpty()) {
            return ImportResult.Error("No valid transactions found in OFX", documentType, emptyList())
        }

        return ImportResult.Success(
            transactions = transactions,
            documentType = documentType,
            totalConfidence = 0.95f
        )
    }

    private fun isValidOfx(content: String): Boolean {
        val upper = content.uppercase()
        return upper.contains("<OFX>") || upper.contains("OFXHEADER")
    }

    private fun parseTransaction(block: String): ImportedTransaction? {
        val fields = extractFields(block)

        val dateStr = fields["DTPOSTED"] ?: return null
        val amountStr = fields["TRNAMT"] ?: return null

        val date = parseOfxDate(dateStr) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        val merchant = fields["NAME"]?.trim()
        val description = fields["MEMO"]?.trim() ?: merchant ?: ""

        val rawData = fields.mapValues { it.value }

        return ImportedTransaction(
            date = date,
            amount = amount,
            description = description,
            merchant = merchant,
            balance = null,
            category = null,
            confidence = 0.95f,
            source = ImportSource.RULE_BASED,
            rawData = rawData
        )
    }

    private fun extractFields(block: String): Map<String, String> {
        val fields = mutableMapOf<String, String>()

        // Try XML format first
        xmlTagPattern.findAll(block).forEach { match ->
            val tag = match.groupValues[1].uppercase()
            val value = match.groupValues[2]
            fields[tag] = value
        }

        // If no XML matches, try SGML format
        if (fields.isEmpty()) {
            sgmlTagPattern.findAll(block).forEach { match ->
                val tag = match.groupValues[1].uppercase()
                val value = match.groupValues[2].trim()
                fields[tag] = value
            }
        }

        return fields
    }

    private fun parseOfxDate(dateStr: String): LocalDate? {
        // OFX dates are in format: YYYYMMDD or YYYYMMDDHHMMSS
        val cleaned = dateStr.trim()
        if (cleaned.length < 8) return null

        return try {
            val year = cleaned.substring(0, 4).toInt()
            val month = cleaned.substring(4, 6).toInt()
            val day = cleaned.substring(6, 8).toInt()
            LocalDate(year, month, day)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAmount(amountStr: String): Long? {
        return try {
            numberParser.parse(amountStr.trim(), NumberLocale.US)
        } catch (_: Exception) {
            null
        }
    }
}

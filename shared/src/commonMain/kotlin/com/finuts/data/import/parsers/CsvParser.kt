package com.finuts.data.import.parsers

import com.finuts.data.import.utils.DateParser
import com.finuts.data.import.utils.NumberLocale
import com.finuts.data.import.utils.NumberParser
import com.finuts.domain.entity.import.DocumentType
import com.finuts.domain.entity.import.ImportResult
import com.finuts.domain.entity.import.ImportSource
import com.finuts.domain.entity.import.ImportedTransaction
import kotlinx.datetime.LocalDate

/**
 * CSV parser with automatic column detection.
 * Parses bank statements in CSV format and extracts transactions.
 */
class CsvParser {

    private val dateColumns = setOf(
        "date", "дата", "transaction date", "дата операции", "дата транзакции",
        "posting date", "value date", "дата проводки", "operation date"
    )

    private val amountColumns = setOf(
        "amount", "сумма", "sum", "value", "debit", "credit",
        "сумма операции", "сумма в валюте счета", "transaction amount"
    )

    private val descriptionColumns = setOf(
        "description", "описание", "details", "назначение", "memo",
        "narrative", "payment details", "описание операции", "детали"
    )

    private val balanceColumns = setOf(
        "balance", "остаток", "running balance", "баланс",
        "остаток после операции", "account balance"
    )

    private val merchantColumns = setOf(
        "merchant", "торговая точка", "payee", "получатель",
        "контрагент", "vendor", "store"
    )

    /**
     * Parse CSV content and extract transactions.
     *
     * @param content CSV content as string
     * @param documentType Document type with delimiter info
     * @return ImportResult with parsed transactions
     */
    fun parse(content: String, documentType: DocumentType.Csv): ImportResult {
        val cleaned = content.removePrefix("\uFEFF").trim()

        if (cleaned.isEmpty()) {
            return ImportResult.Error("Empty CSV content", documentType, emptyList())
        }

        val lines = cleaned.lines().filter { it.isNotBlank() }

        if (lines.size < 2) {
            return ImportResult.Error("CSV must have header and at least one data row", documentType, emptyList())
        }

        val header = parseLine(lines.first(), documentType.delimiter)
        val columnMap = detectColumns(header)

        if (columnMap.dateIndex == null || columnMap.amountIndex == null) {
            return ImportResult.NeedsUserInput(
                transactions = emptyList(),
                documentType = documentType,
                issues = listOf("Could not detect date or amount columns. Headers: ${header.joinToString(", ")}")
            )
        }

        val transactions = mutableListOf<ImportedTransaction>()
        val errors = mutableListOf<String>()

        for (i in 1 until lines.size) {
            val values = parseLine(lines[i], documentType.delimiter)
            try {
                val transaction = parseRow(values, columnMap, header)
                if (transaction != null) {
                    transactions.add(transaction)
                }
            } catch (e: Exception) {
                errors.add("Row ${i + 1}: ${e.message}")
            }
        }

        if (transactions.isEmpty()) {
            return ImportResult.Error(
                "No valid transactions found. Errors: ${errors.take(3).joinToString("; ")}",
                documentType,
                emptyList()
            )
        }

        val confidence = calculateConfidence(transactions.size, lines.size - 1, columnMap)

        return ImportResult.Success(
            transactions = transactions,
            documentType = documentType,
            totalConfidence = confidence
        )
    }

    private fun parseLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' && !inQuotes -> inQuotes = true
                char == '"' && inQuotes -> {
                    // Check for escaped quote
                    inQuotes = false
                }
                char == delimiter && !inQuotes -> {
                    result.add(current.toString().trim().removeSurrounding("\""))
                    current.clear()
                }
                else -> current.append(char)
            }
        }

        // Add last field
        result.add(current.toString().trim().removeSurrounding("\""))

        return result
    }

    private fun detectColumns(header: List<String>): ColumnMap {
        var dateIndex: Int? = null
        var amountIndex: Int? = null
        var descriptionIndex: Int? = null
        var balanceIndex: Int? = null
        var merchantIndex: Int? = null

        header.forEachIndexed { index, column ->
            val lower = column.lowercase().trim()

            when {
                dateIndex == null && dateColumns.any { lower.contains(it) } ->
                    dateIndex = index
                amountIndex == null && amountColumns.any { lower.contains(it) } ->
                    amountIndex = index
                descriptionIndex == null && descriptionColumns.any { lower.contains(it) } ->
                    descriptionIndex = index
                balanceIndex == null && balanceColumns.any { lower.contains(it) } ->
                    balanceIndex = index
                merchantIndex == null && merchantColumns.any { lower.contains(it) } ->
                    merchantIndex = index
            }
        }

        return ColumnMap(
            dateIndex = dateIndex,
            amountIndex = amountIndex,
            descriptionIndex = descriptionIndex,
            balanceIndex = balanceIndex,
            merchantIndex = merchantIndex
        )
    }

    private fun parseRow(
        values: List<String>,
        columnMap: ColumnMap,
        header: List<String>
    ): ImportedTransaction? {
        val dateStr = columnMap.dateIndex?.let { values.getOrNull(it) } ?: return null
        val amountStr = columnMap.amountIndex?.let { values.getOrNull(it) } ?: return null

        val date = try {
            DateParser.parse(dateStr.trim())
        } catch (_: Exception) {
            return null
        }

        val amount = try {
            NumberParser.parse(amountStr.trim(), NumberLocale.AUTO)
        } catch (_: Exception) {
            return null
        }

        val description = columnMap.descriptionIndex?.let {
            values.getOrNull(it)?.trim()
        } ?: ""

        val balance = columnMap.balanceIndex?.let { index ->
            values.getOrNull(index)?.trim()?.takeIf { it.isNotBlank() }?.let {
                try {
                    NumberParser.parse(it, NumberLocale.AUTO)
                } catch (_: Exception) {
                    null
                }
            }
        }

        val merchant = columnMap.merchantIndex?.let {
            values.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() }
        }

        val rawData = header.zip(values).toMap()

        return ImportedTransaction(
            date = date,
            amount = amount,
            description = description,
            merchant = merchant,
            balance = balance,
            category = null,
            confidence = 0.85f,
            source = ImportSource.RULE_BASED,
            rawData = rawData
        )
    }

    private fun calculateConfidence(
        successCount: Int,
        totalCount: Int,
        columnMap: ColumnMap
    ): Float {
        val parseRatio = successCount.toFloat() / totalCount
        val columnBonus = when {
            columnMap.balanceIndex != null && columnMap.merchantIndex != null -> 0.1f
            columnMap.balanceIndex != null || columnMap.merchantIndex != null -> 0.05f
            else -> 0f
        }

        return (parseRatio * 0.9f + columnBonus).coerceIn(0f, 1f)
    }

    private data class ColumnMap(
        val dateIndex: Int?,
        val amountIndex: Int?,
        val descriptionIndex: Int?,
        val balanceIndex: Int?,
        val merchantIndex: Int?
    )
}

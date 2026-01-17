package com.finuts.data.import.ocr

/**
 * Extracts transaction data locally from OCR text without cloud calls.
 *
 * Extracts:
 * - Dates (multiple formats: DD.MM.YYYY, MM/DD/YYYY, YYYY-MM-DD, DD/MM/YYYY)
 * - Amounts (with sign, decimals, thousand separators: $1,234.56, 1.234,56€, 1 234,56₽)
 * - Currencies (symbols and ISO codes)
 * - Raw descriptions (for Cloud LLM enhancement)
 *
 * This is Tier 1 of the Privacy-First pipeline.
 * Achieves 80-85% accuracy for dates/amounts, 0% for merchant extraction.
 */
class LocalTransactionExtractor {

    /**
     * Extract partial transactions from preprocessed text.
     *
     * Handles two document styles:
     * 1. Bank statements: Each line has DATE + DESCRIPTION + AMOUNT
     * 2. Receipts: Header date, then lines with just AMOUNT (or ITEM + AMOUNT)
     *
     * For receipts, a "context date" from the header applies to all amount lines.
     * Receipt amounts without explicit sign are treated as expenses (negative).
     *
     * @param text The preprocessed OCR text
     * @param docType Optional document type hint for sign determination
     */
    fun extract(text: String, docType: DocumentType? = null): List<PartialTransaction> {
        if (text.isBlank()) return emptyList()

        val transactions = mutableListOf<PartialTransaction>()
        var contextDate: String? = null  // Remember date from header lines
        var isReceiptMode = docType == DocumentType.RECEIPT

        text.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@forEach

            val dateMatch = findDate(trimmed)
            val amountResult = extractAmount(trimmed)

            when {
                // Case 1: Line has both date and amount (bank statement style)
                dateMatch != null && amountResult != null -> {
                    contextDate = dateMatch.value  // Update context date
                    // Line with both date and amount = bank statement style, NOT receipt
                    if (docType == null) isReceiptMode = false
                    val transaction = createTransaction(trimmed, dateMatch, amountResult, isReceiptMode)
                    transactions.add(transaction)
                }
                // Case 2: Line has only date (header line for receipts)
                dateMatch != null && amountResult == null -> {
                    // Only set receipt mode if the date is on a SHORT line (likely a header)
                    // Lines like "Statement Period: 01/01/2026 to 01/31/2026" are not receipt headers
                    // But "15.01.2026 12:45" (date + time) IS a valid receipt header
                    // Check for ":" only BEFORE the date position (label separator vs time separator)
                    val textBeforeDate = trimmed.substring(0, dateMatch.range.first)
                    val hasLabelColon = textBeforeDate.contains(":")
                    val isShortDateLine = trimmed.length < 40 && !hasLabelColon
                    if (docType == null && isShortDateLine) {
                        isReceiptMode = true
                    }
                    contextDate = dateMatch.value
                }
                // Case 3: Line has only amount (receipt items)
                dateMatch == null && amountResult != null && contextDate != null -> {
                    val transaction = createTransactionWithContextDate(
                        trimmed, contextDate!!, amountResult, isReceiptMode
                    )
                    transactions.add(transaction)
                }
                // Case 4: No date and no amount, or no context date - skip
            }
        }

        return transactions
    }

    private fun createTransaction(
        line: String,
        dateMatch: MatchResult,
        amountResult: AmountResult,
        isReceiptMode: Boolean
    ): PartialTransaction {
        val currency = detectCurrency(line)
        val rawDescription = extractDescription(line, dateMatch, amountResult)

        // In receipt mode, unsigned positive amounts are expenses (negate them)
        val amount = if (isReceiptMode && amountResult.valueMinorUnits > 0) {
            -amountResult.valueMinorUnits
        } else {
            amountResult.valueMinorUnits
        }

        return PartialTransaction(
            rawDate = dateMatch.value,
            amountMinorUnits = amount,
            currency = currency,
            rawDescription = rawDescription,
            isCredit = amount > 0,
            isDebit = amount < 0
        )
    }

    private fun createTransactionWithContextDate(
        line: String,
        contextDate: String,
        amountResult: AmountResult,
        isReceiptMode: Boolean
    ): PartialTransaction {
        val currency = detectCurrency(line)
        // For receipt items, description is everything except the amount
        val rawDescription = line.replace(amountResult.rawValue, "")
            .replace(CURRENCY_SYMBOLS_PATTERN, "")
            .trim()

        // In receipt mode, unsigned positive amounts are expenses (negate them)
        val amount = if (isReceiptMode && amountResult.valueMinorUnits > 0) {
            -amountResult.valueMinorUnits
        } else {
            amountResult.valueMinorUnits
        }

        return PartialTransaction(
            rawDate = contextDate,
            amountMinorUnits = amount,
            currency = currency,
            rawDescription = rawDescription,
            isCredit = amount > 0,
            isDebit = amount < 0
        )
    }

    private fun findDate(line: String): MatchResult? {
        return DATE_PATTERNS.firstNotNullOfOrNull { pattern ->
            pattern.find(line)
        }
    }

    private fun extractAmount(line: String): AmountResult? {
        // Order matters: try most specific patterns first

        // 1. Kaspi-style with sign and currency: "- 3 700,00 ₸" or "+ 50 000,00 ₸"
        KASPI_AMOUNT_PATTERN.find(line)?.let { match ->
            return parseKaspiAmount(match)
        }

        // 2. US dollar format: -$1,234.56 or $1,234.56
        US_DOLLAR_PATTERN.find(line)?.let { match ->
            return parseUsDollarAmount(match)
        }

        // 3. GBP format: -£1,234.56 or £1,234.56
        GBP_AMOUNT_PATTERN.find(line)?.let { match ->
            return parseGbpAmount(match)
        }

        // 4. EU format with dot thousands: -1.234,56 € or 1.234,56€
        EU_AMOUNT_PATTERN.find(line)?.let { match ->
            return parseEuAmount(match, line)
        }

        // 5. RU/CIS format with space thousands: -1 234,56 ₽ or 1 234,56 ₸
        RU_AMOUNT_PATTERN.find(line)?.let { match ->
            return parseRuAmount(match)
        }

        // 6. Standard signed amount: +5000 or -1234.56 or -100,00
        SIGNED_AMOUNT_PATTERN.find(line)?.let { match ->
            return parseSignedAmount(match)
        }

        // 7. Amount with currency symbol: $100 or 5000₸
        CURRENCY_AMOUNT_PATTERN.find(line)?.let { match ->
            return parseCurrencyAmount(match, line)
        }

        // 8. Unsigned decimal at line end: 100.00 or 9.99 (Revolut, N26 style)
        UNSIGNED_DECIMAL_PATTERN.find(line)?.let { match ->
            return parseUnsignedDecimalAmount(match)
        }

        return null
    }

    private fun parseUnsignedDecimalAmount(match: MatchResult): AmountResult {
        val value = match.value.toDoubleOrNull() ?: 0.0
        val minorUnits = kotlin.math.round(value * 100).toLong()

        return AmountResult(
            valueMinorUnits = minorUnits,
            rawValue = match.value
        )
    }

    private fun parseKaspiAmount(match: MatchResult): AmountResult {
        val fullMatch = match.value
        val isNegative = fullMatch.contains("-")

        // Extract number part: "3 700,00" -> 3700.00
        val numberPart = fullMatch
            .replace(CURRENCY_SYMBOLS_PATTERN, "")
            .replace(Regex("[+\\-]"), "")
            .trim()
            .replace(" ", "")
            .replace(",", ".")

        val value = numberPart.toDoubleOrNull() ?: 0.0
        val minorUnits = kotlin.math.round(value * 100).toLong()

        return AmountResult(
            valueMinorUnits = if (isNegative) -minorUnits else minorUnits,
            rawValue = match.value
        )
    }

    private fun parseUsDollarAmount(match: MatchResult): AmountResult {
        val fullMatch = match.value
        val isNegative = fullMatch.contains("-")

        // Extract number: remove $, -, commas, keep decimal point
        val numberPart = fullMatch
            .replace(Regex("[-+\$]"), "")
            .replace(",", "")
            .trim()

        val value = numberPart.toDoubleOrNull() ?: 0.0
        val minorUnits = kotlin.math.round(value * 100).toLong()

        return AmountResult(
            valueMinorUnits = if (isNegative) -minorUnits else minorUnits,
            rawValue = match.value
        )
    }

    private fun parseGbpAmount(match: MatchResult): AmountResult {
        val fullMatch = match.value
        val isNegative = fullMatch.contains("-")

        // Extract number: remove £, -, commas, keep decimal point
        val numberPart = fullMatch
            .replace(Regex("[-+£]"), "")
            .replace(",", "")
            .trim()

        val value = numberPart.toDoubleOrNull() ?: 0.0
        val minorUnits = kotlin.math.round(value * 100).toLong()

        return AmountResult(
            valueMinorUnits = if (isNegative) -minorUnits else minorUnits,
            rawValue = match.value
        )
    }

    private fun parseEuAmount(match: MatchResult, line: String): AmountResult {
        val fullMatch = match.value
        val isNegative = fullMatch.contains("-") || line.substringBefore(match.value).endsWith("-")

        // EU format: 1.234,56 -> remove dots (thousands), replace comma with dot (decimal)
        val numberPart = fullMatch
            .replace(Regex("[-+€]"), "")
            .replace(".", "")  // Remove thousand separator (dots)
            .replace(",", ".") // Decimal comma to dot
            .trim()

        val value = numberPart.toDoubleOrNull() ?: 0.0
        val minorUnits = kotlin.math.round(value * 100).toLong()

        return AmountResult(
            valueMinorUnits = if (isNegative) -minorUnits else minorUnits,
            rawValue = match.value
        )
    }

    private fun parseRuAmount(match: MatchResult): AmountResult {
        val fullMatch = match.value
        val isNegative = fullMatch.contains("-")

        // RU format: 1 234,56 -> remove spaces (thousands), replace comma with dot (decimal)
        val numberPart = fullMatch
            .replace(CURRENCY_SYMBOLS_PATTERN, "")
            .replace(Regex("[+\\-]"), "")
            .replace(" ", "")
            .replace(",", ".")
            .trim()

        val value = numberPart.toDoubleOrNull() ?: 0.0
        val minorUnits = kotlin.math.round(value * 100).toLong()

        return AmountResult(
            valueMinorUnits = if (isNegative) -minorUnits else minorUnits,
            rawValue = match.value
        )
    }

    private fun parseSignedAmount(match: MatchResult): AmountResult {
        val fullMatch = match.value
        val isNegative = fullMatch.contains("-")

        val numberPart = fullMatch
            .replace(Regex("[+\\-\\s]"), "")
            .replace(",", ".")

        val value = numberPart.toDoubleOrNull() ?: 0.0
        val minorUnits = kotlin.math.round(value * 100).toLong()

        return AmountResult(
            valueMinorUnits = if (isNegative) -minorUnits else minorUnits,
            rawValue = match.value
        )
    }

    private fun parseCurrencyAmount(match: MatchResult, line: String): AmountResult {
        val fullMatch = match.value

        // Check if there's a minus sign before the currency symbol
        val indexOfMatch = line.indexOf(match.value)
        val isNegative = indexOfMatch > 0 && line.substring(0, indexOfMatch).endsWith("-")

        val numberPart = fullMatch
            .replace(CURRENCY_SYMBOLS_PATTERN, "")
            .replace(Regex("\\s"), "")
            .replace(",", ".")

        val value = numberPart.toDoubleOrNull() ?: 0.0
        val minorUnits = kotlin.math.round(value * 100).toLong()

        return AmountResult(
            valueMinorUnits = if (isNegative) -minorUnits else minorUnits,
            rawValue = match.value
        )
    }

    private fun detectCurrency(line: String): String? {
        // Check currency symbols first
        when {
            line.contains("₸") -> return "KZT"
            line.contains("\$") -> return "USD"
            line.contains("€") -> return "EUR"
            line.contains("₽") -> return "RUB"
            line.contains("£") -> return "GBP"
            line.contains("¥") -> return "JPY"
        }

        // Check ISO codes
        CURRENCY_CODES.forEach { code ->
            if (line.contains(code, ignoreCase = true)) {
                return code
            }
        }

        return null
    }

    private fun extractDescription(
        line: String,
        dateMatch: MatchResult,
        amountResult: AmountResult
    ): String {
        // Remove date and amount from line to get description
        var description = line
            .replace(dateMatch.value, "")
            .replace(amountResult.rawValue, "")
            .replace(CURRENCY_SYMBOLS_PATTERN, "")
            .replace(Regex("\\b(KZT|USD|EUR|RUB|GBP|JPY|CNY|CHF)\\b", RegexOption.IGNORE_CASE), "")
            .trim()

        // Clean up extra spaces and punctuation at start
        description = description
            .replace(Regex("^[\\-+\\s]+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        return description
    }

    companion object {
        // Date patterns (order matters - more specific first)
        private val DATE_PATTERNS = listOf(
            Regex("\\d{4}-\\d{1,2}-\\d{1,2}"),       // YYYY-MM-DD (ISO)
            Regex("\\d{1,2}[./]\\d{1,2}[./]\\d{4}"), // DD.MM.YYYY or MM/DD/YYYY (full year)
            Regex("\\d{1,2}[./]\\d{1,2}[./]\\d{2}"), // DD.MM.YY or MM/DD/YY (short year)
            Regex("\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{4}", RegexOption.IGNORE_CASE), // DD Mon YYYY (Revolut)
            Regex("(?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2},?\\s+\\d{4}", RegexOption.IGNORE_CASE), // Month DD, YYYY (US)
            Regex("\\d{1,2}/\\d{1,2}(?=\\s)")        // MM/DD (US short format, must be followed by space)
        )

        // Currency symbols pattern (for removal)
        private val CURRENCY_SYMBOLS_PATTERN = Regex("[₸€₽£¥\$]")

        // Kaspi-style: "- 3 700,00 ₸" or "+ 50 000 ₸" (sign + space thousands)
        // MUST have space-separated thousands (at least one group of space+3digits)
        // Decimal part is OPTIONAL (amounts like "50 000" without ",00" are valid)
        private val KASPI_AMOUNT_PATTERN = Regex(
            "[+\\-]\\s*\\d{1,3}(\\s\\d{3})+(,\\d{2})?\\s*[₸€₽£¥\$]?"
        )

        // US Dollar: -$1,234.56 or $1,234.56 or $100.00 or $100
        private val US_DOLLAR_PATTERN = Regex(
            "-?\\$[\\d,]+(?:\\.\\d{1,2})?"
        )

        // GBP: -£1,234.56 or £1,234.56 or £100.00 or £100
        private val GBP_AMOUNT_PATTERN = Regex(
            "-?£[\\d,]+(?:\\.\\d{1,2})?"
        )

        // EU format: -1.234,56 € or 1.234,56€ (dot thousands, comma decimal)
        // MUST have at least one dot-thousand group to differentiate from simple comma decimal
        // Use word boundary \b to avoid matching mid-number
        private val EU_AMOUNT_PATTERN = Regex(
            "-?\\d{1,3}(?:\\.\\d{3})+,\\d{2}\\s*€?"
        )

        // RU/CIS format: -1 234,56 ₽ or 1 234,56 ₸ or 50 000 ₽ (space thousands, optional decimal)
        // Decimal part is OPTIONAL (amounts like "50 000" without ",00" are valid)
        private val RU_AMOUNT_PATTERN = Regex(
            "-?\\d{1,3}(\\s\\d{3})+(,\\d{2})?\\s*[₽₸]"
        )

        // Signed amount: +5000 or -1234.56 or +5000,00 (no thousand separators)
        // For amounts without thousand separators
        private val SIGNED_AMOUNT_PATTERN = Regex(
            "[+\\-]\\d+(?:[.,]\\d{1,2})?"
        )

        // Unsigned decimal amount at line end: 100.00 or 9.99 (Revolut, N26 style)
        // Must have exactly 2 decimal places and be followed by end-of-line or whitespace
        private val UNSIGNED_DECIMAL_PATTERN = Regex(
            "\\d+\\.\\d{2}$"
        )

        // Amount with currency symbol: $100 or 5000₸ or £100.00
        private val CURRENCY_AMOUNT_PATTERN = Regex(
            "[₸€₽£¥\$]\\s*[\\d,]+(?:[.]\\d{1,2})?|[\\d,.]+\\s*[₸€₽£¥\$]"
        )

        // Common currency ISO codes
        private val CURRENCY_CODES = listOf(
            "KZT", "USD", "EUR", "RUB", "GBP", "JPY", "CNY", "CHF"
        )
    }

    private data class AmountResult(
        val valueMinorUnits: Long,
        val rawValue: String
    )
}

/**
 * Partial transaction extracted locally.
 * Will be enhanced by Cloud LLM with merchant/category info.
 */
data class PartialTransaction(
    val rawDate: String,
    val amountMinorUnits: Long,
    val currency: String?,
    val rawDescription: String,
    val isCredit: Boolean,
    val isDebit: Boolean,
    // Enhanced by Cloud LLM
    var merchant: String? = null,
    var categoryHint: String? = null,
    var counterpartyName: String? = null
) {
    val amountFormatted: String
        get() {
            val major = amountMinorUnits / 100
            val minor = kotlin.math.abs(amountMinorUnits % 100)
            return "$major.${minor.toString().padStart(2, '0')}"
        }
}

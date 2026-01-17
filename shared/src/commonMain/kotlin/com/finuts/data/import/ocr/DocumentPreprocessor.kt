package com.finuts.data.import.ocr

/**
 * Preprocesses raw OCR text to reduce token count and detect document metadata.
 *
 * Functions:
 * - Filters lines with financial relevance (dates, amounts)
 * - Removes headers, footers, page numbers
 * - Detects document type (statement, receipt, invoice)
 * - Detects language (ru, kk, en)
 *
 * Typically achieves 50-70% token reduction.
 */
class DocumentPreprocessor {

    /**
     * Process raw OCR text and return cleaned text with hints.
     */
    fun process(rawText: String): PreprocessResult {
        if (rawText.isBlank()) {
            return PreprocessResult(
                cleanedText = "",
                hints = DocumentHints(
                    type = DocumentType.UNKNOWN,
                    language = "en"
                )
            )
        }

        val lines = rawText.lines()

        // Detect document type from full text
        val docType = detectDocumentType(rawText)

        // Detect language from full text
        val language = detectLanguage(rawText)

        // Filter and clean lines
        val cleanedLines = lines
            .filter { line -> isRelevantLine(line) }
            .filterNot { line -> isPageNumber(line) }
            .filterNot { line -> isHeaderOrFooter(line) }

        return PreprocessResult(
            cleanedText = cleanedLines.joinToString("\n"),
            hints = DocumentHints(
                type = docType,
                language = language
            )
        )
    }

    private fun isRelevantLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isBlank()) return false

        // Keep lines with dates
        if (containsDate(trimmed)) return true

        // Keep lines with amounts
        if (containsAmount(trimmed)) return true

        // Keep lines with transaction keywords
        if (containsTransactionKeyword(trimmed)) return true

        return false
    }

    private fun containsDate(line: String): Boolean {
        return DATE_PATTERNS.any { it.containsMatchIn(line) }
    }

    private fun containsAmount(line: String): Boolean {
        return AMOUNT_PATTERNS.any { it.containsMatchIn(line) }
    }

    private fun containsTransactionKeyword(line: String): Boolean {
        val lower = line.lowercase()
        return TRANSACTION_KEYWORDS.any { lower.contains(it) }
    }

    private fun isPageNumber(line: String): Boolean {
        val trimmed = line.trim()
        return PAGE_NUMBER_PATTERNS.any { it.matches(trimmed) }
    }

    private fun isHeaderOrFooter(line: String): Boolean {
        val lower = line.lowercase().trim()

        // Skip obvious headers/footers
        return HEADER_FOOTER_PATTERNS.any { lower.contains(it) }
    }

    private fun detectDocumentType(text: String): DocumentType {
        val lower = text.lowercase()

        // Prioritize by specificity: check explicit receipt terms first
        // "чек" is very specific to receipts (fiscal receipt number)
        if (lower.contains("чек") || lower.contains("касса") || lower.contains("фискальный")) {
            return DocumentType.RECEIPT
        }

        // Check for formal invoice terms
        if (lower.contains("счёт-фактура") || lower.contains("счет-фактура") ||
            lower.contains("invoice") || lower.contains("bill to") ||
            lower.contains("amount due")) {
            return DocumentType.INVOICE
        }

        // E-commerce order confirmations are invoices
        if (lower.contains("order #") || lower.contains("order date") ||
            lower.contains("order total") || lower.contains("заказ №") ||
            lower.contains("дата заказа")) {
            return DocumentType.INVOICE
        }

        // "итого к оплате" as a phrase is invoice-specific (formal Russian)
        // while "итого:" or "итого" alone is more receipt-like
        if (lower.contains("итого к оплате") || lower.contains("к оплате")) {
            return DocumentType.INVOICE
        }

        // Check for generic receipt terms
        if (lower.contains("итого") || lower.contains("total") || lower.contains("receipt")) {
            return DocumentType.RECEIPT
        }

        // Check for bank statement patterns
        if (STATEMENT_PATTERNS.any { lower.contains(it) }) {
            return DocumentType.BANK_STATEMENT
        }

        return DocumentType.UNKNOWN
    }

    private fun detectLanguage(text: String): String {
        val cyrillicCount = CYRILLIC_PATTERN.findAll(text).count()
        val latinCount = LATIN_PATTERN.findAll(text).count()
        val kazakhCount = KAZAKH_PATTERN.findAll(text).count()

        // Kazakh-specific characters (even 1 is enough to identify Kazakh)
        if (kazakhCount > 0) return "kk"

        // Cyrillic dominates (Russian)
        if (cyrillicCount > latinCount) return "ru"

        // Latin dominates
        return "en"
    }

    companion object {
        // Date patterns (various formats)
        private val DATE_PATTERNS = listOf(
            Regex("""\d{1,2}[./]\d{1,2}[./]\d{2,4}"""),  // DD.MM.YY(YY)
            Regex("""\d{4}-\d{1,2}-\d{1,2}"""),          // YYYY-MM-DD
            Regex("""\d{1,2}\s+\w+\s+\d{4}"""),          // 15 January 2026 (UK)
            Regex("""(?:January|February|March|April|May|June|July|August|September|October|November|December)\s+\d{1,2},?\s+\d{4}""", RegexOption.IGNORE_CASE) // January 15, 2026 (US)
        )

        // Amount patterns
        private val AMOUNT_PATTERNS = listOf(
            Regex("""[+-]?\s*\d[\d\s]*[.,]\d{2}"""),     // 1 234,50 or 1234.50
            Regex("""[+-]\s*\d+(?:\s*\d{3})*"""),        // +5000 or -1 000
            Regex("""\d+\s*[₸$€₽£¥]"""),                 // 5000₸
            Regex("""[₸$€₽£¥]\s*\d+""")                  // $5000
        )

        // Transaction-related keywords
        private val TRANSACTION_KEYWORDS = listOf(
            // Russian
            "перевод", "оплата", "покупка", "списание", "пополнение",
            "возврат", "комиссия", "остаток", "баланс",
            // Kazakh
            "аударым", "төлем", "сатып алу",
            // English
            "transfer", "payment", "purchase", "withdrawal", "deposit",
            "refund", "fee", "balance"
        )

        // Page number patterns
        private val PAGE_NUMBER_PATTERNS = listOf(
            Regex("""^-\s*\d+\s*-$"""),                  // - 1 -
            Regex("""^page\s+\d+.*$""", RegexOption.IGNORE_CASE),  // Page 1 of 3
            Regex("""^\d+\s*/\s*\d+$"""),                // 1/3
            Regex("""^стр\.?\s*\d+.*$""", RegexOption.IGNORE_CASE) // стр. 1
        )

        // Header/footer indicators (to remove)
        private val HEADER_FOOTER_PATTERNS = listOf(
            "generated", "confidential", "page", "дата формирования",
            "конфиденциально", "www.", "http", "tel:", "тел:"
        )

        // Document type detection
        private val RECEIPT_PATTERNS = listOf(
            "чек", "итого", "total", "receipt", "касса", "фискальный"
        )

        private val INVOICE_PATTERNS = listOf(
            "invoice", "счёт-фактура", "счет-фактура", "bill to",
            "amount due", "к оплате"
        )

        private val STATEMENT_PATTERNS = listOf(
            "statement", "выписка", "account", "счёт", "счет", "bank",
            "банк", "period", "период"
        )

        // Language detection
        private val CYRILLIC_PATTERN = Regex("""[а-яёА-ЯЁ]""")
        private val LATIN_PATTERN = Regex("""[a-zA-Z]""")
        private val KAZAKH_PATTERN = Regex("""[әіңғүұқөһӘІҢҒҮҰҚӨҺ]""")
    }
}

/**
 * Result of document preprocessing.
 */
data class PreprocessResult(
    val cleanedText: String,
    val hints: DocumentHints
)

/**
 * Hints about the document detected during preprocessing.
 */
data class DocumentHints(
    val type: DocumentType,
    val language: String
)

/**
 * Type of financial document.
 */
enum class DocumentType {
    BANK_STATEMENT,
    RECEIPT,
    INVOICE,
    UNKNOWN
}

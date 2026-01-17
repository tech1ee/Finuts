package com.finuts.data.import

import com.finuts.domain.entity.import.DocumentType

/**
 * Detects document types from file extensions and content.
 * Used to route documents to appropriate parsers.
 */
class FormatDetector : FormatDetectorInterface {

    private val EXTENSION_MAP = mapOf(
        "csv" to { DocumentType.Csv(',', "UTF-8") },
        "tsv" to { DocumentType.Csv('\t', "UTF-8") },
        "txt" to { DocumentType.Csv(',', "UTF-8") },
        "pdf" to { DocumentType.Pdf(null) },
        "ofx" to { DocumentType.Ofx("2.2") },
        "qfx" to { DocumentType.Ofx("2.2") },
        "qif" to { DocumentType.Qif("Bank") },
        "jpg" to { DocumentType.Image("JPEG") },
        "jpeg" to { DocumentType.Image("JPEG") },
        "png" to { DocumentType.Image("PNG") },
        "heic" to { DocumentType.Image("HEIC") },
        "heif" to { DocumentType.Image("HEIF") },
        "webp" to { DocumentType.Image("WebP") }
    )

    private val BANK_SIGNATURES = listOf(
        BankPattern("kaspi", listOf("kaspi", "каспи")),
        BankPattern("halyk", listOf("halyk", "народный банк", "халык")),
        BankPattern("jusan", listOf("jusan", "жусан")),
        BankPattern("forte", listOf("forte", "fortebank", "форте")),
        BankPattern("sberbank", listOf("сбербанк", "sberbank")),
        BankPattern("tinkoff", listOf("тинькофф", "tinkoff", "тинькоф")),
        BankPattern("alfa", listOf("альфа-банк", "alfa-bank", "альфабанк")),
        BankPattern("vtb", listOf("втб", "vtb")),
        BankPattern("raiffeisen", listOf("райффайзен", "raiffeisen")),
        BankPattern("centerkredit", listOf("центркредит", "centerkredit", "bcc"))
    )

    // Magic bytes for file type detection
    private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D) // %PDF-
    private val PNG_MAGIC = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )
    private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

    /**
     * Detect document type from filename and optional content.
     *
     * @param filename The filename including extension
     * @param content Optional file content for deeper analysis
     * @return Detected DocumentType
     */
    override fun detect(filename: String, content: ByteArray?): DocumentType {
        // First try content-based detection (more accurate)
        if (content != null && content.isNotEmpty()) {
            val contentType = detectFromContent(content)
            if (contentType !is DocumentType.Unknown) {
                return contentType
            }
        }

        // Fall back to extension-based detection
        return detectFromExtension(filename)
    }

    /**
     * Detect document type from file extension.
     *
     * @param filename The filename including extension
     * @return Detected DocumentType
     */
    override fun detectFromExtension(filename: String): DocumentType {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return EXTENSION_MAP[extension]?.invoke() ?: DocumentType.Unknown
    }

    /**
     * Detect document type from file content.
     *
     * @param content File content as bytes
     * @return Detected DocumentType
     */
    override fun detectFromContent(content: ByteArray): DocumentType {
        if (content.isEmpty()) {
            return DocumentType.Unknown
        }

        // Check binary signatures first
        if (content.startsWith(PDF_MAGIC)) {
            return DocumentType.Pdf(null)
        }

        if (content.startsWith(PNG_MAGIC)) {
            return DocumentType.Image("PNG")
        }

        if (content.startsWith(JPEG_MAGIC)) {
            return DocumentType.Image("JPEG")
        }

        // Try text-based detection
        val text = try {
            content.decodeToString()
        } catch (_: Exception) {
            return DocumentType.Unknown
        }

        return detectFromTextContent(text)
    }

    private fun detectFromTextContent(text: String): DocumentType {
        val trimmed = text.trim()

        // Check OFX format
        if (isOfxContent(trimmed)) {
            val version = extractOfxVersion(trimmed)
            return DocumentType.Ofx(version)
        }

        // Check QIF format
        if (isQifContent(trimmed)) {
            val accountType = extractQifAccountType(trimmed)
            return DocumentType.Qif(accountType)
        }

        // Check CSV format
        if (isCsvContent(trimmed)) {
            val delimiter = detectCsvDelimiter(trimmed)
            val encoding = "UTF-8" // Already decoded successfully
            return DocumentType.Csv(delimiter, encoding)
        }

        return DocumentType.Unknown
    }

    private fun isOfxContent(text: String): Boolean {
        val upper = text.uppercase()
        return upper.contains("OFXHEADER") ||
            upper.contains("<?OFX") ||
            (upper.contains("<OFX>") && upper.contains("</OFX>"))
    }

    private fun isQifContent(text: String): Boolean {
        val lines = text.lines().take(5)
        return lines.any { it.startsWith("!Type:") || it.startsWith("!Account") }
    }

    private fun isCsvContent(text: String): Boolean {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return false

        // Check if lines have consistent delimiter patterns
        val delimiters = listOf(',', ';', '\t', '|')
        for (delimiter in delimiters) {
            val counts = lines.take(5).map { countDelimiter(it, delimiter) }
            if (counts.all { it > 0 } && counts.distinct().size == 1) {
                return true
            }
        }

        return false
    }

    private fun countDelimiter(line: String, delimiter: Char): Int {
        var count = 0
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> count++
            }
        }

        return count
    }

    /**
     * Detect the most likely CSV delimiter from content.
     *
     * @param content CSV content as string
     * @return Detected delimiter character
     */
    override fun detectCsvDelimiter(content: String): Char {
        val delimiters = listOf(',', ';', '\t', '|')
        val lines = content.lines().filter { it.isNotBlank() }.take(10)

        if (lines.isEmpty()) return ','

        // Count delimiters per line, considering quotes
        val scores = delimiters.associateWith { delimiter ->
            lines.map { countDelimiter(it, delimiter) }
        }

        // Find delimiter with most consistent non-zero count
        val bestDelimiter = delimiters.maxByOrNull { delimiter ->
            val counts = scores[delimiter] ?: emptyList()
            if (counts.all { it > 0 }) {
                counts.sum() * 100 - counts.distinct().size
            } else {
                0
            }
        }

        return bestDelimiter ?: ','
    }

    private fun extractOfxVersion(text: String): String {
        val versionPattern = Regex("""VERSION[:\s=]+(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val match = versionPattern.find(text)
        return match?.groupValues?.get(1) ?: "2.2"
    }

    private fun extractQifAccountType(text: String): String {
        val typePattern = Regex("""!Type:(\w+)""")
        val match = typePattern.find(text)
        return match?.groupValues?.get(1) ?: "Bank"
    }

    /**
     * Detect bank signature from document content.
     *
     * @param content Document text content
     * @return Bank identifier or null if not recognized
     */
    override fun detectBankSignature(content: String): String? {
        val lower = content.lowercase()

        for (bank in BANK_SIGNATURES) {
            if (bank.patterns.any { lower.contains(it) }) {
                return bank.id
            }
        }

        return null
    }

    /**
     * Detect text encoding from BOM or content analysis.
     *
     * @param content File content as bytes
     * @return Detected encoding name
     */
    override fun detectEncoding(content: ByteArray): String {
        if (content.size < 2) return "UTF-8"

        // Check BOM
        if (content.size >= 3 &&
            content[0] == 0xEF.toByte() &&
            content[1] == 0xBB.toByte() &&
            content[2] == 0xBF.toByte()
        ) {
            return "UTF-8"
        }

        if (content[0] == 0xFF.toByte() && content[1] == 0xFE.toByte()) {
            return "UTF-16LE"
        }

        if (content[0] == 0xFE.toByte() && content[1] == 0xFF.toByte()) {
            return "UTF-16BE"
        }

        // Default to UTF-8
        return "UTF-8"
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (this.size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }

    private data class BankPattern(
        val id: String,
        val patterns: List<String>
    )
}

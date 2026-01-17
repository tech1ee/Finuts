package com.finuts.ai.privacy

/**
 * PII (Personally Identifiable Information) Anonymizer.
 *
 * Detects and replaces sensitive data before sending to cloud LLMs:
 * - Person names → [NAME_1], [NAME_2]
 * - IBANs → [IBAN_1]
 * - Account numbers → [ACCOUNT_1]
 * - Phone numbers → [PHONE_1]
 * - Email addresses → [EMAIL_1]
 *
 * Preserves non-PII data needed for parsing:
 * - Dates
 * - Amounts
 * - Merchant names (public entities)
 */
interface PIIAnonymizer {
    /**
     * Anonymize PII in text, returning anonymized text and mapping for reversal.
     */
    fun anonymize(text: String): AnonymizationResult

    /**
     * Restore original PII from anonymized text using stored mapping.
     */
    fun deanonymize(text: String, mapping: Map<String, String>): String

    /**
     * Detect PII in text without replacing.
     */
    fun detectPII(text: String): List<DetectedPII>
}

/**
 * Result of anonymization operation.
 */
data class AnonymizationResult(
    val anonymizedText: String,
    val mapping: Map<String, String>, // placeholder → original
    val detectedPII: List<DetectedPII>,
    val wasModified: Boolean
) {
    val piiCount: Int get() = detectedPII.size
}

/**
 * Information about detected PII.
 */
data class DetectedPII(
    val type: PIIType,
    val original: String,
    val placeholder: String,
    val startIndex: Int,
    val endIndex: Int
)

/**
 * Types of PII we detect.
 */
enum class PIIType {
    PERSON_NAME,
    IBAN,
    ACCOUNT_NUMBER,
    CARD_NUMBER,
    PHONE,
    EMAIL,
    ADDRESS,
    SSN,
    PASSPORT,
    IIN // Kazakhstan Individual Identification Number
}

/**
 * Regex-based implementation of PIIAnonymizer.
 * Thread-safe: counters are local to each anonymize() call.
 */
class RegexPIIAnonymizer : PIIAnonymizer {
    private val log = co.touchlab.kermit.Logger.withTag("PIIAnonymizer")

    override fun anonymize(text: String): AnonymizationResult {
        log.d { "anonymize: inputLen=${text.length}" }
        // Thread-safe: counters are local to this call
        val counters = mutableMapOf<PIIType, Int>()
        val detectedPII = mutableListOf<DetectedPII>()
        val mapping = mutableMapOf<String, String>()
        var result = text

        // Helper to generate placeholder with local counter
        fun generatePlaceholder(type: PIIType): String {
            val count = counters.getOrPut(type) { 0 } + 1
            counters[type] = count
            return "[${type.name}_$count]"
        }

        // Process each PII type
        piiPatterns.forEach { (type, patterns) ->
            patterns.forEach { pattern ->
                result = pattern.replace(result) { match ->
                    val original = match.value
                    // Skip if it looks like a date or amount
                    if (shouldSkip(original, type)) {
                        original
                    } else {
                        val placeholder = generatePlaceholder(type)
                        mapping[placeholder] = original
                        detectedPII.add(
                            DetectedPII(
                                type = type,
                                original = original,
                                placeholder = placeholder,
                                startIndex = match.range.first,
                                endIndex = match.range.last
                            )
                        )
                        placeholder
                    }
                }
            }
        }

        val wasModified = detectedPII.isNotEmpty()

        if (wasModified) {
            val typeCounts = detectedPII.groupBy { it.type }.mapValues { it.value.size }
            log.i { "anonymize: FOUND_PII count=${detectedPII.size}, types=$typeCounts" }
        } else {
            log.d { "anonymize: no PII detected" }
        }

        return AnonymizationResult(
            anonymizedText = result,
            mapping = mapping,
            detectedPII = detectedPII,
            wasModified = wasModified
        )
    }

    override fun deanonymize(text: String, mapping: Map<String, String>): String {
        log.d { "deanonymize: mappings=${mapping.size}" }
        var result = text
        mapping.forEach { (placeholder, original) ->
            result = result.replace(placeholder, original)
        }
        return result
    }

    override fun detectPII(text: String): List<DetectedPII> {
        return anonymize(text).detectedPII
    }

    private fun shouldSkip(value: String, type: PIIType): Boolean {
        // Skip dates mistakenly matched as phone numbers
        if (type == PIIType.PHONE && value.matches(datePattern)) return true

        // Skip amounts (must have decimal separator to be considered amount)
        if (type != PIIType.IIN && type != PIIType.ACCOUNT_NUMBER && value.matches(amountPattern)) {
            return true
        }

        // Skip common business/merchant words that look like names
        if (type == PIIType.PERSON_NAME && containsBusinessWord(value)) {
            return true
        }

        return false
    }

    private fun containsBusinessWord(value: String): Boolean {
        val words = value.split(Regex("\\s+"))
        return words.any { word -> businessWords.contains(word.lowercase()) }
    }

    companion object {
        // Date pattern to avoid false positives
        private val datePattern = Regex("""\d{1,2}[./]\d{1,2}[./]\d{2,4}""")
        // Amount must have decimal separator to avoid matching IINs/accounts
        private val amountPattern = Regex("""^[+-]?\d+[.,]\d+$""")

        // Business/merchant words that shouldn't be treated as person names
        private val businessWords = setOf(
            // Business types
            "bank", "банк", "store", "магазин", "shop", "market", "маркет",
            "restaurant", "ресторан", "cafe", "кафе", "hotel", "отель",
            "company", "компания", "corp", "corporation", "inc", "ltd", "llc",
            "gmbh", "ag", "sa", "ооо", "оао", "ао", "зао", "тоо", "ип",
            // Service types
            "service", "сервис", "services", "center", "центр", "clinic", "клиника",
            "pharmacy", "аптека", "studio", "студия", "agency", "агентство",
            // Financial
            "payment", "платёж", "transfer", "перевод", "exchange", "обмен",
            "insurance", "страхование", "credit", "кредит", "loan", "займ",
            // Common merchant words
            "express", "экспресс", "plus", "плюс", "pro", "premium", "gold",
            "mobile", "мобайл", "online", "онлайн", "digital", "smart",
            // Kazakhstan specific
            "kaspi", "halyk", "jusan", "forte", "bcc", "eurasian"
        )

        // PII detection patterns - ORDER MATTERS: more specific patterns first!
        private val piiPatterns: Map<PIIType, List<Regex>> = mapOf(
            // IBAN: starts with 2 letters, then digits (most specific format)
            PIIType.IBAN to listOf(
                Regex("""[A-Z]{2}\d{2}[A-Z0-9]{10,30}""", RegexOption.IGNORE_CASE)
            ),

            // Email addresses (highly specific format)
            PIIType.EMAIL to listOf(
                Regex("""[\w.+-]+@[\w.-]+\.\w{2,}""", RegexOption.IGNORE_CASE)
            ),

            // Card numbers: 16 digits with optional spaces/dashes
            PIIType.CARD_NUMBER to listOf(
                Regex("""\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b""")
            ),

            // Phone numbers (Kazakhstan/Russia format) - BEFORE account numbers!
            PIIType.PHONE to listOf(
                Regex("""\+?[78]\s*[\(\-]?\d{3}[\)\-]?\s*\d{3}[\-\s]?\d{2}[\-\s]?\d{2}\b"""),
                Regex("""\b87\d{9}\b""") // Kazakhstan mobile (11 digits starting with 87)
            ),

            // Kazakhstan IIN: exactly 12 digits - BEFORE account numbers!
            PIIType.IIN to listOf(
                Regex("""\b\d{12}\b""")
            ),

            // Account numbers: 10-20 digits (generic, should be last numeric pattern)
            PIIType.ACCOUNT_NUMBER to listOf(
                Regex("""\b\d{10,20}\b""")
            ),

            // Person names - Cyrillic (Russian/Kazakh with patronymic pattern)
            PIIType.PERSON_NAME to listOf(
                // Cyrillic: Фамилия И.О. (Surname + Initials)
                Regex("""[А-ЯЁ][а-яё]+\s+[А-ЯЁ]\.\s*[А-ЯЁ]\."""),
                // Cyrillic: Фамилия Имя Отчество (Full name with masculine patronymic -ич)
                Regex("""[А-ЯЁ][а-яё]+\s+[А-ЯЁ][а-яё]+\s+[А-ЯЁ][а-яё]+ич\b"""),
                // Cyrillic: Фамилия Имя Отчество (Full name with feminine patronymic -вна)
                Regex("""[А-ЯЁ][а-яё]+\s+[А-ЯЁ][а-яё]+\s+[А-ЯЁ][а-яё]+вна\b"""),
                // Cyrillic: Фамилия Имя (Surname + Firstname without patronymic)
                Regex("""[А-ЯЁ][а-яё]+\s+[А-ЯЁ][а-яё]+\b"""),
                // Latin: First Last (e.g., "John Smith", "Mary Jane")
                Regex("""[A-Z][a-z]+\s+[A-Z][a-z]+\b"""),
                // Latin: Last, First (e.g., "Smith, John")
                Regex("""[A-Z][a-z]+,\s*[A-Z][a-z]+\b"""),
                // Latin with initial: J. Smith, John S.
                Regex("""[A-Z]\.\s*[A-Z][a-z]+\b"""),
                Regex("""[A-Z][a-z]+\s+[A-Z]\.\b""")
            )
        )
    }
}

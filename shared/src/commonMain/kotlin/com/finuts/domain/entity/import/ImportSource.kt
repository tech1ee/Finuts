package com.finuts.domain.entity.import

import kotlinx.serialization.Serializable

/**
 * Indicates which tier of the import pipeline recognized the transaction.
 * Used for confidence scoring and debugging.
 */
@Serializable
enum class ImportSource {
    /**
     * Parsed using rule-based format detection (Tier 1).
     * Highest confidence for known formats like OFX, QIF, well-structured CSV.
     */
    RULE_BASED,

    /**
     * Parsed using Document AI - OCR and table extraction (Tier 2).
     * Used for PDF and image parsing.
     */
    DOCUMENT_AI,

    /**
     * Enhanced with LLM for complex/ambiguous data (Tier 3).
     * Used when rule-based and Document AI have low confidence.
     */
    LLM_ENHANCED,

    /**
     * Corrected by user during import confirmation (Tier 4).
     * Highest trust - user verified the data.
     */
    USER_CORRECTED,

    /**
     * Processed using native platform AI (Tier 0).
     * ML Kit GenAI on Android, Apple Intelligence on iOS.
     */
    NATIVE_AI
}

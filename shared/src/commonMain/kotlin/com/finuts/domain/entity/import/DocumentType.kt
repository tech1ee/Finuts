package com.finuts.domain.entity.import

import kotlinx.serialization.Serializable

/**
 * Represents the detected type of an imported document.
 * Used for routing to the appropriate parser.
 */
@Serializable
sealed interface DocumentType {

    /**
     * Comma-Separated Values file.
     * @param delimiter The character used to separate fields (comma, semicolon, tab)
     * @param encoding The character encoding of the file (UTF-8, windows-1251, etc.)
     */
    @Serializable
    data class Csv(
        val delimiter: Char,
        val encoding: String
    ) : DocumentType

    /**
     * Portable Document Format file.
     * @param bankSignature Optional identifier for the bank format (e.g., "kaspi", "halyk")
     */
    @Serializable
    data class Pdf(
        val bankSignature: String?
    ) : DocumentType

    /**
     * Open Financial Exchange format.
     * @param version The OFX specification version (e.g., "1.6", "2.2")
     */
    @Serializable
    data class Ofx(
        val version: String
    ) : DocumentType

    /**
     * Quicken Interchange Format.
     * @param accountType The type of account in the QIF file (Bank, CCard, etc.)
     */
    @Serializable
    data class Qif(
        val accountType: String
    ) : DocumentType

    /**
     * Image file (photo of statement).
     * @param format The image format (JPEG, PNG, HEIC, etc.)
     */
    @Serializable
    data class Image(
        val format: String
    ) : DocumentType

    /**
     * Unknown or unrecognized document type.
     */
    @Serializable
    data object Unknown : DocumentType
}

package com.finuts.data.import.ocr

/**
 * Represents a single page extracted from a PDF document.
 */
data class PdfPage(
    val index: Int,
    val width: Int,
    val height: Int,
    val imageData: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PdfPage
        return index == other.index &&
            width == other.width &&
            height == other.height &&
            imageData.contentEquals(other.imageData)
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + imageData.contentHashCode()
        return result
    }
}

/**
 * Result of OCR text recognition on an image.
 */
data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock>,
    val overallConfidence: Float
) {
    val isEmpty: Boolean get() = fullText.isBlank()
    val hasHighConfidence: Boolean get() = overallConfidence >= 0.8f
    val hasMediumConfidence: Boolean get() = overallConfidence in 0.5f..0.8f
}

/**
 * A single recognized text block with position and confidence.
 */
data class OcrBlock(
    val text: String,
    val confidence: Float,
    val boundingBox: BoundingBox
)

/**
 * Bounding box for a recognized text element.
 * Coordinates are normalized (0.0 to 1.0) relative to image dimensions.
 */
data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = x + width
    val bottom: Float get() = y + height
    val centerX: Float get() = x + width / 2
    val centerY: Float get() = y + height / 2
}

/**
 * Exception thrown when OCR processing fails.
 */
class OcrException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when PDF extraction fails.
 */
class PdfExtractionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

package com.finuts.data.categorization

/**
 * Pattern for merchant matching with category and confidence.
 */
data class MerchantPattern(
    val pattern: Regex,
    val categoryId: String,
    val confidence: Float,
    val merchantName: String? = null
) {
    constructor(
        patternString: String,
        categoryId: String,
        confidence: Float,
        merchantName: String? = null
    ) : this(
        pattern = Regex(patternString, RegexOption.IGNORE_CASE),
        categoryId = categoryId,
        confidence = confidence,
        merchantName = merchantName
    )
}

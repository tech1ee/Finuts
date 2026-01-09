package com.finuts.data.categorization.patterns

import com.finuts.data.categorization.MerchantPattern

/**
 * Merchant patterns for transfers and financial services.
 */
internal val transferPatterns = listOf(
    // Kaspi transfers
    MerchantPattern("KASPI.*PEREVOD", "transfer", 0.98f, "Kaspi Transfer"),
    MerchantPattern("КАСПИ.*ПЕРЕВОД", "transfer", 0.98f, "Kaspi Transfer"),
    MerchantPattern("KASPI.*TRANSFER", "transfer", 0.98f, "Kaspi Transfer"),
    MerchantPattern("ПЕРЕВОД.*KASPI", "transfer", 0.95f, "Kaspi Transfer"),
    MerchantPattern("ПЕРЕВОД.*КАРТ", "transfer", 0.90f),

    // Bank transfers
    MerchantPattern("HALYK.*PEREVOD", "transfer", 0.95f, "Halyk Transfer"),
    MerchantPattern("ХАЛЫК.*ПЕРЕВОД", "transfer", 0.95f, "Halyk Transfer"),
    MerchantPattern("JUSAN.*PEREVOD", "transfer", 0.95f, "Jusan Transfer"),
    MerchantPattern("FORTE.*PEREVOD", "transfer", 0.95f, "Forte Transfer"),
    MerchantPattern("ПЕРЕВОД", "transfer", 0.80f),
    MerchantPattern("TRANSFER", "transfer", 0.75f),

    // International transfers
    MerchantPattern("WESTERN\\s*UNION", "transfer", 0.95f, "Western Union"),
    MerchantPattern("MONEY\\s*GRAM", "transfer", 0.95f, "MoneyGram"),
    MerchantPattern("CONTACT", "transfer", 0.80f, "Contact"),
    MerchantPattern("ЗОЛОТАЯ\\s*КОРОНА", "transfer", 0.95f, "Zolotaya Korona"),
    MerchantPattern("GOLDEN\\s*CROWN", "transfer", 0.95f, "Golden Crown")
)

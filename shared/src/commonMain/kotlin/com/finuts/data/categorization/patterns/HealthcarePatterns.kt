package com.finuts.data.categorization.patterns

import com.finuts.data.categorization.MerchantPattern

/**
 * Merchant patterns for healthcare and pharmacies.
 */
internal val healthcarePatterns = listOf(
    // Pharmacies
    MerchantPattern("EUROPHARMA", "healthcare", 0.95f, "Europharma"),
    MerchantPattern("ЕВРОФАРМА", "healthcare", 0.95f, "Europharma"),
    MerchantPattern("БИОСФЕРА", "healthcare", 0.95f, "Biosfera"),
    MerchantPattern("BIOSFERA", "healthcare", 0.95f, "Biosfera"),
    MerchantPattern("ДОБРАЯ\\s*АПТЕКА", "healthcare", 0.90f, "Dobraya Apteka"),
    MerchantPattern("GIPPOKRAT", "healthcare", 0.90f, "Gippokrat"),
    MerchantPattern("АПТЕКА", "healthcare", 0.85f),
    MerchantPattern("PHARMACY", "healthcare", 0.85f),
    MerchantPattern("PHARMA", "healthcare", 0.80f),

    // Clinics
    MerchantPattern("INVIVO", "healthcare", 0.95f, "Invivo"),
    MerchantPattern("INTERTEACH", "healthcare", 0.95f, "Interteach"),
    MerchantPattern("ОЛИМП", "healthcare", 0.90f, "Olymp Clinic"),
    MerchantPattern("OLYMPIC", "healthcare", 0.90f),
    MerchantPattern("CLINIC", "healthcare", 0.75f),
    MerchantPattern("КЛИНИКА", "healthcare", 0.80f),
    MerchantPattern("MEDICAL", "healthcare", 0.75f),
    MerchantPattern("МЕДИЦИН", "healthcare", 0.80f),
    MerchantPattern("СТОМАТОЛОГ", "healthcare", 0.85f),
    MerchantPattern("DENTAL", "healthcare", 0.85f),
    MerchantPattern("DENT", "healthcare", 0.80f),

    // Labs
    MerchantPattern("KDLOLYMP", "healthcare", 0.95f, "KDL Olymp"),
    MerchantPattern("SYNEVO", "healthcare", 0.95f, "Synevo"),
    MerchantPattern("ЛАБОРАТОР", "healthcare", 0.85f)
)

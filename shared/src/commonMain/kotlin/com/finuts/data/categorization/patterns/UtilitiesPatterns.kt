package com.finuts.data.categorization.patterns

import com.finuts.data.categorization.MerchantPattern

/**
 * Merchant patterns for utilities and telecom.
 */
internal val utilitiesPatterns = listOf(
    // Energy
    MerchantPattern("АЛМАТЫЭНЕРГО", "utilities", 0.98f, "AlmatyEnergo"),
    MerchantPattern("ALMATY.*ENERG", "utilities", 0.98f, "AlmatyEnergo"),
    MerchantPattern("АСТАНАЭНЕРГО", "utilities", 0.98f, "AstanaEnergo"),
    MerchantPattern("КАРАГАНДА.*ЭНЕРГО", "utilities", 0.95f),
    MerchantPattern("KEGOC", "utilities", 0.95f, "KEGOC"),

    // Gas
    MerchantPattern("КАЗТРАНСГАЗ", "utilities", 0.95f, "KazTransGas"),
    MerchantPattern("АЛМАТЫГАЗ", "utilities", 0.95f, "AlmatyGas"),

    // Water
    MerchantPattern("АЛМАТЫ.*СУ", "utilities", 0.95f, "AlmatySu"),
    MerchantPattern("ASTANA.*SU", "utilities", 0.95f, "AstanaSu"),
    MerchantPattern("ВОДОКАНАЛ", "utilities", 0.90f),

    // Telecom
    MerchantPattern("КАЗАХТЕЛЕКОМ", "utilities", 0.98f, "Kazakhtelecom"),
    MerchantPattern("KAZAKHTELECOM", "utilities", 0.98f, "Kazakhtelecom"),
    MerchantPattern("BEELINE", "utilities", 0.95f, "Beeline"),
    MerchantPattern("БИЛАЙН", "utilities", 0.95f, "Beeline"),
    MerchantPattern("KCELL", "utilities", 0.95f, "Kcell"),
    MerchantPattern("ACTIV", "utilities", 0.95f, "Activ"),
    MerchantPattern("АКТИВ", "utilities", 0.95f, "Activ"),
    MerchantPattern("TELE2", "utilities", 0.95f, "Tele2"),
    MerchantPattern("ТЕЛЕ2", "utilities", 0.95f, "Tele2"),
    MerchantPattern("ALTEL", "utilities", 0.95f, "Altel"),
    MerchantPattern("АЛТЕЛ", "utilities", 0.95f, "Altel"),

    // Internet providers
    MerchantPattern("ALMA\\s*TV", "utilities", 0.90f, "Alma TV"),
    MerchantPattern("ID\\s*NET", "utilities", 0.90f, "ID Net"),
    MerchantPattern("IDNET", "utilities", 0.90f, "ID Net"),

    // Housing
    MerchantPattern("КСК", "utilities", 0.80f),
    MerchantPattern("ОСИ", "utilities", 0.80f),
    MerchantPattern("КОММ.*УСЛУГ", "utilities", 0.85f)
)

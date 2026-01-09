package com.finuts.data.categorization.patterns

import com.finuts.data.categorization.MerchantPattern

/**
 * Merchant patterns for shopping and retail.
 */
internal val shoppingPatterns = listOf(
    // Kaspi ecosystem
    MerchantPattern("KASPI\\s*MAGAZIN", "shopping", 0.95f, "Kaspi Magazin"),
    MerchantPattern("КАСПИ\\s*МАГАЗИН", "shopping", 0.95f, "Kaspi Magazin"),
    MerchantPattern("KASPI\\s*SHOP", "shopping", 0.95f, "Kaspi Shop"),

    // Electronics
    MerchantPattern("SULPAK", "shopping", 0.95f, "Sulpak"),
    MerchantPattern("СУЛПАК", "shopping", 0.95f, "Sulpak"),
    MerchantPattern("TECHNODOM", "shopping", 0.95f, "Technodom"),
    MerchantPattern("ТЕХНОДОМ", "shopping", 0.95f, "Technodom"),
    MerchantPattern("MECHTA", "shopping", 0.95f, "Mechta"),
    MerchantPattern("МЕЧТА", "shopping", 0.95f, "Mechta"),
    MerchantPattern("EVRIKA", "shopping", 0.90f, "Evrika"),
    MerchantPattern("ЭВРИКА", "shopping", 0.90f, "Evrika"),
    MerchantPattern("АЛСЕР", "shopping", 0.90f, "Alser"),
    MerchantPattern("ALSER", "shopping", 0.90f, "Alser"),

    // Online marketplaces
    MerchantPattern("WILDBERRIES", "shopping", 0.98f, "Wildberries"),
    MerchantPattern("OZON", "shopping", 0.98f, "Ozon"),
    MerchantPattern("ALIEXPRESS", "shopping", 0.95f, "AliExpress"),
    MerchantPattern("AMAZON", "shopping", 0.98f, "Amazon"),
    MerchantPattern("FLIP\\.KZ", "shopping", 0.90f, "Flip.kz"),

    // Fashion
    MerchantPattern("ZARA", "shopping", 0.95f, "Zara"),
    MerchantPattern("H&M", "shopping", 0.95f, "H&M"),
    MerchantPattern("MANGO", "shopping", 0.90f, "Mango"),
    MerchantPattern("BERSHKA", "shopping", 0.90f, "Bershka"),
    MerchantPattern("PULL.*BEAR", "shopping", 0.90f, "Pull&Bear"),
    MerchantPattern("MASSIMO.*DUTTI", "shopping", 0.90f, "Massimo Dutti"),
    MerchantPattern("STRADIVARIUS", "shopping", 0.90f, "Stradivarius"),
    MerchantPattern("LC\\s*WAIKIKI", "shopping", 0.90f, "LC Waikiki"),
    MerchantPattern("COLIN", "shopping", 0.85f, "Colin's"),
    MerchantPattern("DEFACTO", "shopping", 0.90f, "DeFacto"),

    // Home goods
    MerchantPattern("IKEA", "shopping", 0.95f, "IKEA"),
    MerchantPattern("JYSK", "shopping", 0.90f, "JYSK"),
    MerchantPattern("HOFF", "shopping", 0.90f, "Hoff"),
    MerchantPattern("ЛЕРУА\\s*МЕРЛЕН", "shopping", 0.95f, "Leroy Merlin"),
    MerchantPattern("LEROY\\s*MERLIN", "shopping", 0.95f, "Leroy Merlin"),

    // Malls
    MerchantPattern("MEGA.*CENTER", "shopping", 0.80f, "Mega Center"),
    MerchantPattern("DOSTYK\\s*PLAZA", "shopping", 0.80f, "Dostyk Plaza"),
    MerchantPattern("ESENTAI", "shopping", 0.80f, "Esentai Mall"),
    MerchantPattern("KERUEN", "shopping", 0.80f, "Keruen City")
)

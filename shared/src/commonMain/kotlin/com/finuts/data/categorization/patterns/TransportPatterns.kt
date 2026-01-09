package com.finuts.data.categorization.patterns

import com.finuts.data.categorization.MerchantPattern

/**
 * Merchant patterns for transport and taxi services.
 */
internal val transportPatterns = listOf(
    // Taxi services
    MerchantPattern("YANDEX.*TAXI", "transport", 0.98f, "Yandex Taxi"),
    MerchantPattern("ЯНДЕКС.*ТАКСИ", "transport", 0.98f, "Yandex Taxi"),
    MerchantPattern("INDRIVER", "transport", 0.95f, "InDriver"),
    MerchantPattern("DIDI", "transport", 0.95f, "DiDi"),
    MerchantPattern("UBER", "transport", 0.98f, "Uber"),
    MerchantPattern("МАКСИМ.*ТАКСИ", "transport", 0.90f, "Maxim Taxi"),
    MerchantPattern("MAXIM.*TAXI", "transport", 0.90f, "Maxim Taxi"),
    MerchantPattern("ТАКСИ", "transport", 0.75f),
    MerchantPattern("TAXI", "transport", 0.75f),

    // Public transit
    MerchantPattern("ONAY", "transport", 0.95f, "Onay Card"),
    MerchantPattern("ОНАЙ", "transport", 0.95f, "Onay Card"),
    MerchantPattern("МЕТРО\\s*АЛМАТЫ", "transport", 0.95f, "Almaty Metro"),
    MerchantPattern("ALMATY\\s*METRO", "transport", 0.95f, "Almaty Metro"),

    // Car services
    MerchantPattern("АВТОМОЙКА", "transport", 0.85f),
    MerchantPattern("CAR\\s*WASH", "transport", 0.85f),
    MerchantPattern("АЗС", "transport", 0.90f),
    MerchantPattern("PETROL", "transport", 0.85f),
    MerchantPattern("ГАЗПРОМНЕФТЬ", "transport", 0.95f, "Gazpromneft"),
    MerchantPattern("KMG", "transport", 0.90f, "KMG"),
    MerchantPattern("КАЗМУНАЙГАЗ", "transport", 0.90f, "KMG"),
    MerchantPattern("HELIOS", "transport", 0.90f, "Helios"),
    MerchantPattern("SHELL", "transport", 0.95f, "Shell"),

    // Parking
    MerchantPattern("PARKING", "transport", 0.85f),
    MerchantPattern("ПАРКОВКА", "transport", 0.85f)
)

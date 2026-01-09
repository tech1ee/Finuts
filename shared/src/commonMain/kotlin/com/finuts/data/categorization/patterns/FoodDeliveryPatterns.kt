package com.finuts.data.categorization.patterns

import com.finuts.data.categorization.MerchantPattern

/**
 * Merchant patterns for food delivery services.
 */
internal val foodDeliveryPatterns = listOf(
    // Major delivery apps
    MerchantPattern("WOLT", "food_delivery", 0.98f, "Wolt"),
    MerchantPattern("GLOVO", "food_delivery", 0.98f, "Glovo"),
    MerchantPattern("CHOCOFOOD", "food_delivery", 0.95f, "Chocofood"),
    MerchantPattern("YANDEX.*EDA", "food_delivery", 0.98f, "Yandex Eda"),
    MerchantPattern("ЯНДЕКС.*ЕДА", "food_delivery", 0.98f, "Yandex Eda"),
    MerchantPattern("DELIVERY\\s*CLUB", "food_delivery", 0.95f, "Delivery Club"),

    // Restaurant aggregators
    MerchantPattern("MENU\\.KZ", "food_delivery", 0.90f, "Menu.kz"),
    MerchantPattern("EDA\\.KZ", "food_delivery", 0.90f, "Eda.kz")
)

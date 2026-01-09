package com.finuts.data.categorization.patterns

import com.finuts.data.categorization.MerchantPattern

/**
 * Merchant patterns for grocery stores and supermarkets.
 */
internal val groceryPatterns = listOf(
    // Major supermarket chains
    MerchantPattern("MAGNUM", "groceries", 0.95f, "Magnum"),
    MerchantPattern("SMALL\\s*\\d*", "groceries", 0.90f, "Small"),
    MerchantPattern("METRO\\s*CASH", "groceries", 0.95f, "Metro"),
    MerchantPattern("ANVAR", "groceries", 0.90f, "Anvar"),
    MerchantPattern("RAMSTORE", "groceries", 0.92f, "Ramstore"),
    MerchantPattern("SKIDKA", "groceries", 0.88f, "Skidka"),
    MerchantPattern("GALMART", "groceries", 0.90f, "Galmart"),
    MerchantPattern("ФИКС\\s*ПРАЙС", "groceries", 0.88f, "Fix Price"),
    MerchantPattern("FIX\\s*PRICE", "groceries", 0.88f, "Fix Price"),
    MerchantPattern("ДИНА", "groceries", 0.85f, "Dina"),

    // Online grocery
    MerchantPattern("ARBUZ", "groceries", 0.95f, "Arbuz.kz"),
    MerchantPattern("KLEVER", "groceries", 0.90f, "Klever"),
    MerchantPattern("SPAR", "groceries", 0.90f, "Spar"),

    // Markets and bazaars
    MerchantPattern("ЗЕЛЕНЫЙ\\s*БАЗАР", "groceries", 0.85f, "Green Bazaar"),
    MerchantPattern("GREEN\\s*BAZAAR", "groceries", 0.85f, "Green Bazaar"),
    MerchantPattern("БАЗАР", "groceries", 0.70f),
    MerchantPattern("РЫНОК", "groceries", 0.70f),

    // Specialty stores
    MerchantPattern("МЯСНОЙ", "groceries", 0.80f),
    MerchantPattern("ОВОЩНОЙ", "groceries", 0.80f),
    MerchantPattern("МОЛОЧНЫЙ", "groceries", 0.80f),
    MerchantPattern("BAKERY", "groceries", 0.75f),
    MerchantPattern("ХЛЕБ", "groceries", 0.75f)
)

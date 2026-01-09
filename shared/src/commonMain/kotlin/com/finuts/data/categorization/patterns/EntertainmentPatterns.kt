package com.finuts.data.categorization.patterns

import com.finuts.data.categorization.MerchantPattern

/**
 * Merchant patterns for entertainment and subscriptions.
 */
internal val entertainmentPatterns = listOf(
    // Cinemas
    MerchantPattern("KINOPARK", "entertainment", 0.95f, "Kinopark"),
    MerchantPattern("КИНОПАРК", "entertainment", 0.95f, "Kinopark"),
    MerchantPattern("CHAPLIN", "entertainment", 0.95f, "Chaplin Cinemas"),
    MerchantPattern("ЧАПЛИН", "entertainment", 0.95f, "Chaplin Cinemas"),
    MerchantPattern("CINEMAX", "entertainment", 0.95f, "Cinemax"),
    MerchantPattern("ARMAN", "entertainment", 0.85f, "Arman Cinema"),

    // Streaming subscriptions
    MerchantPattern("NETFLIX", "entertainment", 0.98f, "Netflix"),
    MerchantPattern("SPOTIFY", "entertainment", 0.98f, "Spotify"),
    MerchantPattern("APPLE\\s*MUSIC", "entertainment", 0.98f, "Apple Music"),
    MerchantPattern("YOUTUBE\\s*PREMIUM", "entertainment", 0.98f, "YouTube Premium"),
    MerchantPattern("IVI", "entertainment", 0.95f, "IVI"),
    MerchantPattern("КИНОПОИСК", "entertainment", 0.95f, "Kinopoisk"),
    MerchantPattern("KINOPOISK", "entertainment", 0.95f, "Kinopoisk"),
    MerchantPattern("OKKO", "entertainment", 0.95f, "Okko"),
    MerchantPattern("MEGOGO", "entertainment", 0.95f, "Megogo"),
    MerchantPattern("YANDEX.*PLUS", "entertainment", 0.95f, "Yandex Plus"),
    MerchantPattern("ЯНДЕКС.*ПЛЮС", "entertainment", 0.95f, "Yandex Plus"),

    // Gaming
    MerchantPattern("STEAM", "entertainment", 0.95f, "Steam"),
    MerchantPattern("PLAYSTATION", "entertainment", 0.95f, "PlayStation"),
    MerchantPattern("XBOX", "entertainment", 0.95f, "Xbox"),
    MerchantPattern("NINTENDO", "entertainment", 0.95f, "Nintendo"),
    MerchantPattern("EPIC\\s*GAMES", "entertainment", 0.95f, "Epic Games"),

    // Amusement
    MerchantPattern("HAPPY.*LAND", "entertainment", 0.85f, "Happylon"),
    MerchantPattern("БОУЛИНГ", "entertainment", 0.85f),
    MerchantPattern("BOWLING", "entertainment", 0.85f),
    MerchantPattern("КАТОК", "entertainment", 0.85f),
    MerchantPattern("АКВАПАРК", "entertainment", 0.90f)
)

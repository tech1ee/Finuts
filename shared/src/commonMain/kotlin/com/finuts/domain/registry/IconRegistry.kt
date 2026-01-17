package com.finuts.domain.registry

import com.finuts.domain.entity.CategoryType

/**
 * Metadata for a known category including display names, icons, and colors.
 */
data class CategoryMetadata(
    val id: String,
    val displayName: String,
    val displayNameRu: String,
    val displayNameKk: String,
    val defaultIcon: String,
    val defaultColor: String,
    val type: CategoryType,
    val keywords: List<String> = emptyList()
)

/**
 * Registry of icons and colors for category customization.
 * Contains 60 curated icons in 13 semantic groups and 15 preset colors.
 */
class IconRegistry : IconRegistryProvider {

    /**
     * Pre-defined category metadata for known categories.
     * IDs must match MerchantDatabase patterns and default categories.
     */
    private val knownCategories = mapOf(
        // Expense categories
        "groceries" to CategoryMetadata(
            id = "groceries",
            displayName = "Groceries",
            displayNameRu = "Продукты",
            displayNameKk = "Азық-түлік",
            defaultIcon = "basket",
            defaultColor = "#4CAF50",
            type = CategoryType.EXPENSE,
            keywords = listOf("supermarket", "food", "магазин", "продукты", "магнум")
        ),
        "food_delivery" to CategoryMetadata(
            id = "food_delivery",
            displayName = "Food Delivery",
            displayNameRu = "Доставка еды",
            displayNameKk = "Тамақ жеткізу",
            defaultIcon = "truck_delivery",
            defaultColor = "#8BC34A",
            type = CategoryType.EXPENSE,
            keywords = listOf("glovo", "wolt", "yandex", "delivery", "доставка")
        ),
        "transport" to CategoryMetadata(
            id = "transport",
            displayName = "Transport",
            displayNameRu = "Транспорт",
            displayNameKk = "Көлік",
            defaultIcon = "car",
            defaultColor = "#2196F3",
            type = CategoryType.EXPENSE,
            keywords = listOf("taxi", "bus", "metro", "такси", "автобус")
        ),
        "shopping" to CategoryMetadata(
            id = "shopping",
            displayName = "Shopping",
            displayNameRu = "Покупки",
            displayNameKk = "Сауда",
            defaultIcon = "shopping_cart",
            defaultColor = "#9C27B0",
            type = CategoryType.EXPENSE,
            keywords = listOf("mall", "store", "магазин", "одежда")
        ),
        "utilities" to CategoryMetadata(
            id = "utilities",
            displayName = "Utilities",
            displayNameRu = "Коммунальные",
            displayNameKk = "Коммуналдық",
            defaultIcon = "bolt",
            defaultColor = "#FF9800",
            type = CategoryType.EXPENSE,
            keywords = listOf("electricity", "water", "газ", "вода", "свет")
        ),
        "healthcare" to CategoryMetadata(
            id = "healthcare",
            displayName = "Healthcare",
            displayNameRu = "Здоровье",
            displayNameKk = "Денсаулық",
            defaultIcon = "heart",
            defaultColor = "#F44336",
            type = CategoryType.EXPENSE,
            keywords = listOf("pharmacy", "doctor", "аптека", "врач")
        ),
        "entertainment" to CategoryMetadata(
            id = "entertainment",
            displayName = "Entertainment",
            displayNameRu = "Развлечения",
            displayNameKk = "Ойын-сауық",
            defaultIcon = "movie",
            defaultColor = "#E91E63",
            type = CategoryType.EXPENSE,
            keywords = listOf("cinema", "games", "кино", "игры")
        ),
        "education" to CategoryMetadata(
            id = "education",
            displayName = "Education",
            displayNameRu = "Образование",
            displayNameKk = "Білім",
            defaultIcon = "school",
            defaultColor = "#3F51B5",
            type = CategoryType.EXPENSE,
            keywords = listOf("school", "courses", "школа", "курсы")
        ),
        "housing" to CategoryMetadata(
            id = "housing",
            displayName = "Housing",
            displayNameRu = "Жильё",
            displayNameKk = "Тұрғын үй",
            defaultIcon = "home",
            defaultColor = "#795548",
            type = CategoryType.EXPENSE,
            keywords = listOf("rent", "mortgage", "аренда", "ипотека")
        ),
        "transfer" to CategoryMetadata(
            id = "transfer",
            displayName = "Transfers",
            displayNameRu = "Переводы",
            displayNameKk = "Аударымдар",
            defaultIcon = "arrows_left_right",
            defaultColor = "#607D8B",
            type = CategoryType.EXPENSE,
            keywords = listOf("transfer", "перевод")
        ),
        "other" to CategoryMetadata(
            id = "other",
            displayName = "Other",
            displayNameRu = "Другое",
            displayNameKk = "Басқа",
            defaultIcon = "package",
            defaultColor = "#9E9E9E",
            type = CategoryType.EXPENSE,
            keywords = listOf("other", "misc", "другое")
        ),

        // Income categories
        "salary" to CategoryMetadata(
            id = "salary",
            displayName = "Salary",
            displayNameRu = "Зарплата",
            displayNameKk = "Жалақы",
            defaultIcon = "briefcase",
            defaultColor = "#4CAF50",
            type = CategoryType.INCOME,
            keywords = listOf("salary", "wage", "зарплата", "оклад")
        ),
        "freelance" to CategoryMetadata(
            id = "freelance",
            displayName = "Freelance",
            displayNameRu = "Фриланс",
            displayNameKk = "Фриланс",
            defaultIcon = "laptop",
            defaultColor = "#00BCD4",
            type = CategoryType.INCOME,
            keywords = listOf("freelance", "contract", "фриланс")
        ),
        "investments" to CategoryMetadata(
            id = "investments",
            displayName = "Investments",
            displayNameRu = "Инвестиции",
            displayNameKk = "Инвестициялар",
            defaultIcon = "trending_up",
            defaultColor = "#8BC34A",
            type = CategoryType.INCOME,
            keywords = listOf("dividends", "stocks", "дивиденды", "акции")
        ),
        "gifts" to CategoryMetadata(
            id = "gifts",
            displayName = "Gifts",
            displayNameRu = "Подарки",
            displayNameKk = "Сыйлықтар",
            defaultIcon = "gift",
            defaultColor = "#FF5722",
            type = CategoryType.INCOME,
            keywords = listOf("gift", "present", "подарок")
        ),
        "other_income" to CategoryMetadata(
            id = "other_income",
            displayName = "Other Income",
            displayNameRu = "Другой доход",
            displayNameKk = "Басқа табыс",
            defaultIcon = "plus",
            defaultColor = "#607D8B",
            type = CategoryType.INCOME,
            keywords = listOf("other", "income", "доход")
        )
    )

    /**
     * Curated icon set for finance categories.
     * 60 icons total, 3-5 per semantic group.
     */
    val iconGroups: Map<String, List<String>> = mapOf(
        "food" to listOf("basket", "coffee", "pizza", "truck_delivery", "tools_kitchen"),
        "transport" to listOf("car", "bus", "gas_station", "taxi", "plane"),
        "shopping" to listOf("shopping_cart", "shirt", "device_mobile", "gift", "bag"),
        "utilities" to listOf("bolt", "droplet", "wifi", "phone", "flame"),
        "health" to listOf("heart", "pill", "stethoscope", "run", "activity"),
        "entertainment" to listOf("movie", "music", "gamepad", "ticket", "device_tv"),
        "housing" to listOf("home", "key", "tool", "building", "sofa"),
        "education" to listOf("school", "book", "certificate", "bulb", "pencil"),
        "personal" to listOf("scissors", "sparkles", "user"),
        "income" to listOf("briefcase", "laptop", "trending_up", "rotate_ccw", "plus"),
        "finance" to listOf("credit_card", "wallet", "coin", "arrows_left_right", "cash"),
        "subscriptions" to listOf("repeat", "calendar", "clock"),
        "other" to listOf("package", "tag", "star", "flag", "circle")
    )

    /**
     * Color palette for categories. 15 preset colors.
     */
    val colorPalette: List<String> = listOf(
        "#4CAF50", // Green (groceries, income)
        "#8BC34A", // Light Green (food delivery)
        "#2196F3", // Blue (transport)
        "#9C27B0", // Purple (shopping)
        "#FF9800", // Orange (utilities)
        "#F44336", // Red (health)
        "#E91E63", // Pink (entertainment)
        "#3F51B5", // Indigo (education)
        "#795548", // Brown (housing)
        "#607D8B", // Blue Grey (transfers)
        "#9E9E9E", // Grey (other)
        "#00BCD4", // Cyan (tech)
        "#FF5722", // Deep Orange (gifts)
        "#673AB7", // Deep Purple
        "#009688"  // Teal
    )

    /**
     * Get metadata for a known category.
     * @param id Category ID (e.g., "groceries", "transport")
     * @return CategoryMetadata if found, null otherwise
     */
    override fun getCategoryMetadata(id: String): CategoryMetadata? = knownCategories[id]

    /**
     * Get all known category IDs.
     */
    override fun getAllKnownCategoryIds(): Set<String> = knownCategories.keys

    /**
     * Find best matching icon for a keyword hint.
     * Used when LLM suggests an icon.
     * @param hint Icon keyword from LLM
     * @return Best matching icon key, or "package" as default
     */
    override fun findBestMatch(hint: String): String {
        val normalized = hint.lowercase()

        // Direct match in any group
        iconGroups.values.flatten().find { it == normalized }?.let { return it }

        // Check if hint matches a category
        knownCategories[normalized]?.let { return it.defaultIcon }

        // Keyword match in group names
        for ((group, icons) in iconGroups) {
            if (normalized.contains(group) || group.contains(normalized)) {
                return icons.first()
            }
        }

        return "package" // Default icon
    }
}

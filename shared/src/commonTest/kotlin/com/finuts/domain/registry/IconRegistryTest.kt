package com.finuts.domain.registry

import com.finuts.domain.entity.CategoryType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IconRegistryTest {

    private val registry = IconRegistry()

    @Test
    fun `getCategoryMetadata returns metadata for known category`() {
        val metadata = registry.getCategoryMetadata("groceries")

        assertNotNull(metadata)
        assertEquals("groceries", metadata.id)
        assertEquals("Groceries", metadata.displayName)
        assertEquals("Продукты", metadata.displayNameRu)
        assertEquals("basket", metadata.defaultIcon)
        assertEquals("#4CAF50", metadata.defaultColor)
        assertEquals(CategoryType.EXPENSE, metadata.type)
    }

    @Test
    fun `getCategoryMetadata returns null for unknown category`() {
        val metadata = registry.getCategoryMetadata("unknown_category")
        assertNull(metadata)
    }

    @Test
    fun `getAllKnownCategoryIds returns all category IDs`() {
        val ids = registry.getAllKnownCategoryIds()

        assertTrue(ids.contains("groceries"))
        assertTrue(ids.contains("food_delivery"))
        assertTrue(ids.contains("transport"))
        assertTrue(ids.contains("shopping"))
        assertTrue(ids.contains("other"))
        assertTrue(ids.contains("salary"))
        assertTrue(ids.size >= 16) // At least 16 default categories
    }

    @Test
    fun `iconGroups contains expected groups`() {
        val groups = registry.iconGroups

        assertTrue(groups.containsKey("food"))
        assertTrue(groups.containsKey("transport"))
        assertTrue(groups.containsKey("shopping"))
        assertTrue(groups.containsKey("finance"))
        assertTrue(groups.containsKey("other"))
    }

    @Test
    fun `each icon group has 3-5 icons`() {
        registry.iconGroups.forEach { (group, icons) ->
            assertTrue(
                icons.size in 3..6,
                "Group '$group' should have 3-6 icons, has ${icons.size}"
            )
        }
    }

    @Test
    fun `colorPalette has 15 colors`() {
        assertEquals(15, registry.colorPalette.size)
    }

    @Test
    fun `all colors are valid hex format`() {
        val hexPattern = Regex("^#[0-9A-Fa-f]{6}$")
        registry.colorPalette.forEach { color ->
            assertTrue(
                hexPattern.matches(color),
                "Color '$color' is not valid hex format"
            )
        }
    }

    @Test
    fun `findBestMatch returns icon for known keyword`() {
        assertEquals("basket", registry.findBestMatch("groceries"))
        assertEquals("car", registry.findBestMatch("transport"))
    }

    @Test
    fun `findBestMatch returns default icon for unknown keyword`() {
        assertEquals("package", registry.findBestMatch("unknown"))
    }

    @Test
    fun `total icon count is approximately 60`() {
        val totalIcons = registry.iconGroups.values.flatten().size
        assertTrue(totalIcons in 50..70, "Expected ~60 icons, got $totalIcons")
    }

    @Test
    fun `expense categories have correct type`() {
        val expenseCategories = listOf(
            "groceries", "food_delivery", "transport", "shopping",
            "utilities", "healthcare", "entertainment", "education",
            "housing", "transfer", "other"
        )

        expenseCategories.forEach { id ->
            val metadata = registry.getCategoryMetadata(id)
            assertNotNull(metadata, "Category '$id' should exist")
            assertEquals(CategoryType.EXPENSE, metadata!!.type)
        }
    }

    @Test
    fun `income categories have correct type`() {
        val incomeCategories = listOf(
            "salary", "freelance", "investments", "gifts", "other_income"
        )

        incomeCategories.forEach { id ->
            val metadata = registry.getCategoryMetadata(id)
            assertNotNull(metadata, "Category '$id' should exist")
            assertEquals(CategoryType.INCOME, metadata!!.type)
        }
    }
}

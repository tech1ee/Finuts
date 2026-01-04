package com.finuts.domain.entity

import com.finuts.test.TestData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Category entity and related types.
 */
class CategoryTest {

    @Test
    fun `Category can be created with all parameters`() {
        val category = Category(
            id = "cat-1",
            name = "Groceries",
            icon = "shopping_cart",
            color = "#4CAF50",
            type = CategoryType.EXPENSE,
            parentId = "parent-1",
            isDefault = true,
            sortOrder = 5
        )

        assertEquals("cat-1", category.id)
        assertEquals("Groceries", category.name)
        assertEquals("shopping_cart", category.icon)
        assertEquals("#4CAF50", category.color)
        assertEquals(CategoryType.EXPENSE, category.type)
        assertEquals("parent-1", category.parentId)
        assertTrue(category.isDefault)
        assertEquals(5, category.sortOrder)
    }

    @Test
    fun `Category defaults parentId to null`() {
        val category = TestData.category()
        assertNull(category.parentId)
    }

    @Test
    fun `Category defaults isDefault to false`() {
        val category = TestData.category()
        assertFalse(category.isDefault)
    }

    @Test
    fun `Category defaults sortOrder to 0`() {
        val category = TestData.category()
        assertEquals(0, category.sortOrder)
    }

    @Test
    fun `Category can be a subcategory with parentId`() {
        val parent = TestData.category(id = "parent", name = "Food")
        val child = TestData.category(id = "child", name = "Restaurants", parentId = "parent")

        assertNull(parent.parentId)
        assertEquals("parent", child.parentId)
    }

    @Test
    fun `Category copy works correctly`() {
        val original = TestData.category(name = "Original", sortOrder = 1)
        val modified = original.copy(name = "Modified", sortOrder = 2)

        assertEquals("Original", original.name)
        assertEquals(1, original.sortOrder)
        assertEquals("Modified", modified.name)
        assertEquals(2, modified.sortOrder)
        assertEquals(original.id, modified.id)
    }

    @Test
    fun `CategoryType has INCOME and EXPENSE values`() {
        val types = CategoryType.entries.map { it.name }
        assertTrue("INCOME" in types)
        assertTrue("EXPENSE" in types)
        assertEquals(2, CategoryType.entries.size)
    }

    @Test
    fun `all CategoryTypes can be used in Category`() {
        CategoryType.entries.forEach { type ->
            val category = TestData.category(type = type)
            assertEquals(type, category.type)
        }
    }

    @Test
    fun `Category with expense type`() {
        val category = TestData.category(type = CategoryType.EXPENSE)
        assertEquals(CategoryType.EXPENSE, category.type)
    }

    @Test
    fun `Category with income type`() {
        val category = TestData.category(type = CategoryType.INCOME)
        assertEquals(CategoryType.INCOME, category.type)
    }

    @Test
    fun `Category allows various sort orders`() {
        listOf(-1, 0, 1, 10, 100, Int.MAX_VALUE).forEach { order ->
            val category = TestData.category(sortOrder = order)
            assertEquals(order, category.sortOrder)
        }
    }

    @Test
    fun `Category with color hex values`() {
        val colors = listOf("#FF0000", "#00FF00", "#0000FF", "#FFFFFF", "#000000")
        colors.forEach { color ->
            val category = TestData.category(color = color)
            assertEquals(color, category.color)
        }
    }
}

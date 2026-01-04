package com.finuts.data.local.mapper

import com.finuts.domain.entity.CategoryType
import com.finuts.test.TestData
import com.finuts.test.TestEntityData
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for CategoryMapper extension functions.
 */
class CategoryMapperTest {

    @Test
    fun `toDomain maps entity to domain correctly`() {
        val entity = TestEntityData.categoryEntity(
            id = "cat-123",
            name = "Groceries",
            icon = "shopping_cart",
            color = "#4CAF50",
            type = "EXPENSE",
            parentId = null,
            isDefault = true,
            sortOrder = 1
        )

        val domain = entity.toDomain()

        assertEquals("cat-123", domain.id)
        assertEquals("Groceries", domain.name)
        assertEquals("shopping_cart", domain.icon)
        assertEquals("#4CAF50", domain.color)
        assertEquals(CategoryType.EXPENSE, domain.type)
        assertEquals(null, domain.parentId)
        assertEquals(true, domain.isDefault)
        assertEquals(1, domain.sortOrder)
    }

    @Test
    fun `toDomain maps all category types correctly`() {
        CategoryType.entries.forEach { categoryType ->
            val entity = TestEntityData.categoryEntity(type = categoryType.name)
            val domain = entity.toDomain()
            assertEquals(categoryType, domain.type)
        }
    }

    @Test
    fun `toDomain handles subcategory with parentId`() {
        val entity = TestEntityData.categoryEntity(
            id = "sub-cat-1",
            parentId = "parent-cat-1"
        )
        val domain = entity.toDomain()
        assertEquals("parent-cat-1", domain.parentId)
    }

    @Test
    fun `toDomain handles non-default category`() {
        val entity = TestEntityData.categoryEntity(isDefault = false)
        val domain = entity.toDomain()
        assertEquals(false, domain.isDefault)
    }

    @Test
    fun `toDomain handles different sort orders`() {
        listOf(0, 1, 10, 100, Int.MAX_VALUE).forEach { order ->
            val entity = TestEntityData.categoryEntity(sortOrder = order)
            val domain = entity.toDomain()
            assertEquals(order, domain.sortOrder)
        }
    }

    @Test
    fun `toEntity maps domain to entity correctly`() {
        val domain = TestData.category(
            id = "cat-456",
            name = "Salary",
            icon = "payments",
            color = "#2196F3",
            type = CategoryType.INCOME,
            parentId = null,
            isDefault = true,
            sortOrder = 0
        )

        val entity = domain.toEntity()

        assertEquals("cat-456", entity.id)
        assertEquals("Salary", entity.name)
        assertEquals("payments", entity.icon)
        assertEquals("#2196F3", entity.color)
        assertEquals("INCOME", entity.type)
        assertEquals(null, entity.parentId)
        assertEquals(true, entity.isDefault)
        assertEquals(0, entity.sortOrder)
    }

    @Test
    fun `toEntity maps all category types correctly`() {
        CategoryType.entries.forEach { categoryType ->
            val domain = TestData.category(type = categoryType)
            val entity = domain.toEntity()
            assertEquals(categoryType.name, entity.type)
        }
    }

    @Test
    fun `toEntity preserves parentId for subcategories`() {
        val domain = TestData.category(parentId = "parent-123")
        val entity = domain.toEntity()
        assertEquals("parent-123", entity.parentId)
    }

    @Test
    fun `roundtrip entity to domain to entity preserves data`() {
        val originalEntity = TestEntityData.categoryEntity(
            id = "roundtrip-cat",
            name = "Entertainment",
            icon = "movie",
            color = "#9C27B0",
            type = "EXPENSE",
            parentId = "leisure",
            isDefault = false,
            sortOrder = 5
        )

        val domain = originalEntity.toDomain()
        val resultEntity = domain.toEntity()

        assertEquals(originalEntity.id, resultEntity.id)
        assertEquals(originalEntity.name, resultEntity.name)
        assertEquals(originalEntity.icon, resultEntity.icon)
        assertEquals(originalEntity.color, resultEntity.color)
        assertEquals(originalEntity.type, resultEntity.type)
        assertEquals(originalEntity.parentId, resultEntity.parentId)
        assertEquals(originalEntity.isDefault, resultEntity.isDefault)
        assertEquals(originalEntity.sortOrder, resultEntity.sortOrder)
    }

    @Test
    fun `roundtrip domain to entity to domain preserves data`() {
        val originalDomain = TestData.category(
            id = "roundtrip-cat-2",
            name = "Transport",
            icon = "directions_bus",
            color = "#FF5722",
            type = CategoryType.EXPENSE,
            parentId = null,
            isDefault = true,
            sortOrder = 3
        )

        val entity = originalDomain.toEntity()
        val resultDomain = entity.toDomain()

        assertEquals(originalDomain.id, resultDomain.id)
        assertEquals(originalDomain.name, resultDomain.name)
        assertEquals(originalDomain.icon, resultDomain.icon)
        assertEquals(originalDomain.color, resultDomain.color)
        assertEquals(originalDomain.type, resultDomain.type)
        assertEquals(originalDomain.parentId, resultDomain.parentId)
        assertEquals(originalDomain.isDefault, resultDomain.isDefault)
        assertEquals(originalDomain.sortOrder, resultDomain.sortOrder)
    }

    @Test
    fun `toDomain handles expense type`() {
        val entity = TestEntityData.categoryEntity(type = "EXPENSE")
        val domain = entity.toDomain()
        assertEquals(CategoryType.EXPENSE, domain.type)
    }

    @Test
    fun `toDomain handles income type`() {
        val entity = TestEntityData.categoryEntity(type = "INCOME")
        val domain = entity.toDomain()
        assertEquals(CategoryType.INCOME, domain.type)
    }
}

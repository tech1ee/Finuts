package com.finuts.data.repository

import app.cash.turbine.test
import com.finuts.data.local.entity.CategoryEntity
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategoryType
import com.finuts.test.fakes.dao.FakeCategoryDao
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryRepositoryImplTest {

    private lateinit var fakeDao: FakeCategoryDao
    private lateinit var repository: CategoryRepositoryImpl

    @BeforeTest
    fun setup() {
        fakeDao = FakeCategoryDao()
        repository = CategoryRepositoryImpl(fakeDao)
    }

    // === getAllCategories Tests ===

    @Test
    fun `getAllCategories returns empty list when no categories`() = runTest {
        repository.getAllCategories().test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllCategories returns all categories mapped to domain`() = runTest {
        fakeDao.setCategories(listOf(
            createCategoryEntity("groceries", "Groceries", sortOrder = 1),
            createCategoryEntity("transport", "Transport", sortOrder = 2)
        ))

        repository.getAllCategories().test {
            val categories = awaitItem()
            assertEquals(2, categories.size)
            assertEquals("Groceries", categories[0].name)
            assertEquals("Transport", categories[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === getCategoryById Tests ===

    @Test
    fun `getCategoryById returns null when not found`() = runTest {
        repository.getCategoryById("non-existent").test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getCategoryById returns category when found`() = runTest {
        fakeDao.setCategories(listOf(createCategoryEntity("groceries", "Groceries")))

        repository.getCategoryById("groceries").test {
            val category = awaitItem()
            assertEquals("groceries", category?.id)
            assertEquals("Groceries", category?.name)
            awaitComplete()
        }
    }

    // === getCategoriesByType Tests ===

    @Test
    fun `getCategoriesByType filters by expense type`() = runTest {
        fakeDao.setCategories(listOf(
            createCategoryEntity("groceries", "Groceries", type = "EXPENSE"),
            createCategoryEntity("salary", "Salary", type = "INCOME")
        ))

        repository.getCategoriesByType(CategoryType.EXPENSE).test {
            val categories = awaitItem()
            assertEquals(1, categories.size)
            assertEquals("Groceries", categories[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getCategoriesByType filters by income type`() = runTest {
        fakeDao.setCategories(listOf(
            createCategoryEntity("groceries", "Groceries", type = "EXPENSE"),
            createCategoryEntity("salary", "Salary", type = "INCOME")
        ))

        repository.getCategoriesByType(CategoryType.INCOME).test {
            val categories = awaitItem()
            assertEquals(1, categories.size)
            assertEquals("Salary", categories[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === getDefaultCategories Tests ===

    @Test
    fun `getDefaultCategories returns only default categories`() = runTest {
        fakeDao.setCategories(listOf(
            createCategoryEntity("groceries", "Groceries", isDefault = true),
            createCategoryEntity("custom", "Custom", isDefault = false)
        ))

        repository.getDefaultCategories().test {
            val categories = awaitItem()
            assertEquals(1, categories.size)
            assertEquals("Groceries", categories[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === createCategory Tests ===

    @Test
    fun `createCategory inserts entity into dao`() = runTest {
        val category = createDomainCategory("new-cat", "New Category")

        repository.createCategory(category)

        val stored = fakeDao.getCategoryById("new-cat")
        assertEquals("New Category", stored?.name)
    }

    // === updateCategory Tests ===

    @Test
    fun `updateCategory modifies existing entity`() = runTest {
        fakeDao.setCategories(listOf(createCategoryEntity("cat1", "Old Name")))
        val updated = createDomainCategory("cat1", "New Name")

        repository.updateCategory(updated)

        val stored = fakeDao.getCategoryById("cat1")
        assertEquals("New Name", stored?.name)
    }

    // === deleteCategory Tests ===

    @Test
    fun `deleteCategory removes entity from dao`() = runTest {
        fakeDao.setCategories(listOf(createCategoryEntity("cat1", "ToDelete")))

        repository.deleteCategory("cat1")

        assertNull(fakeDao.getCategoryById("cat1"))
    }

    // === seedDefaultCategories Tests ===

    @Test
    fun `seedDefaultCategories does nothing when categories exist`() = runTest {
        fakeDao.setCategories(listOf(createCategoryEntity("existing", "Existing")))

        repository.seedDefaultCategories()

        // Should still have only the one category
        assertEquals(1, fakeDao.getAll().size)
    }

    @Test
    fun `seedDefaultCategories inserts default categories when empty`() = runTest {
        repository.seedDefaultCategories()

        val all = fakeDao.getAll()
        assertTrue(all.isNotEmpty())
        assertTrue(all.any { it.id == "groceries" })
        assertTrue(all.any { it.id == "salary" })
    }

    // === Helper Functions ===

    private fun createCategoryEntity(
        id: String,
        name: String,
        type: String = "EXPENSE",
        isDefault: Boolean = true,
        sortOrder: Int = 1
    ) = CategoryEntity(
        id = id,
        name = name,
        icon = "category",
        color = "#4CAF50",
        type = type,
        parentId = null,
        isDefault = isDefault,
        sortOrder = sortOrder
    )

    private fun createDomainCategory(
        id: String,
        name: String
    ) = Category(
        id = id,
        name = name,
        icon = "category",
        color = "#4CAF50",
        type = CategoryType.EXPENSE,
        parentId = null,
        isDefault = true,
        sortOrder = 1
    )
}

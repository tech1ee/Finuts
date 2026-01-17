package com.finuts.domain.usecase

import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategoryType
import com.finuts.domain.registry.CategoryMetadata
import com.finuts.domain.registry.IconRegistry
import com.finuts.domain.registry.IconRegistryProvider
import com.finuts.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryResolverTest {

    @Test
    fun `returns categoryId when category exists in database`() = runTest {
        val repository = FakeCategoryRepository(
            existingCategories = listOf(
                Category(
                    id = "groceries",
                    name = "Groceries",
                    icon = "basket",
                    color = "#4CAF50",
                    type = CategoryType.EXPENSE
                )
            )
        )
        val resolver = CategoryResolver(repository, IconRegistry())

        val result = resolver.ensureExists("groceries")

        assertEquals("groceries", result)
        assertTrue(repository.createdCategories.isEmpty())
    }

    @Test
    fun `creates category from IconRegistry when known but missing`() = runTest {
        val repository = FakeCategoryRepository()
        val resolver = CategoryResolver(repository, IconRegistry())

        val result = resolver.ensureExists("groceries")

        assertEquals("groceries", result)
        assertEquals(1, repository.createdCategories.size)
        assertEquals("groceries", repository.createdCategories[0].id)
        assertEquals("Groceries", repository.createdCategories[0].name)
    }

    @Test
    fun `returns other for unknown category and creates it`() = runTest {
        val repository = FakeCategoryRepository()
        val resolver = CategoryResolver(repository, IconRegistry())

        val result = resolver.ensureExists("completely_unknown_category")

        assertEquals("other", result)
        // Should create "other" category
        assertTrue(repository.createdCategories.any { it.id == "other" })
    }

    @Test
    fun `handles multiple calls for same category`() = runTest {
        val repository = FakeCategoryRepository()
        val resolver = CategoryResolver(repository, IconRegistry())

        val result1 = resolver.ensureExists("transport")
        val result2 = resolver.ensureExists("transport")

        assertEquals("transport", result1)
        assertEquals("transport", result2)
        // Second call should find existing category, not create duplicate
        assertEquals(1, repository.createdCategories.size)
    }

    @Test
    fun `creates all known expense categories correctly`() = runTest {
        val repository = FakeCategoryRepository()
        val resolver = CategoryResolver(repository, IconRegistry())
        val knownExpense = listOf(
            "groceries", "food_delivery", "transport", "shopping",
            "utilities", "healthcare", "entertainment", "education",
            "housing", "transfer", "other"
        )

        knownExpense.forEach { categoryId ->
            val result = resolver.ensureExists(categoryId)
            assertEquals(categoryId, result)
        }

        assertEquals(knownExpense.size, repository.createdCategories.size)
        repository.createdCategories.forEach { category ->
            assertEquals(CategoryType.EXPENSE, category.type)
        }
    }

    @Test
    fun `creates income categories with correct type`() = runTest {
        val repository = FakeCategoryRepository()
        val resolver = CategoryResolver(repository, IconRegistry())
        val knownIncome = listOf("salary", "freelance", "investments", "gifts", "other_income")

        knownIncome.forEach { categoryId ->
            val result = resolver.ensureExists(categoryId)
            assertEquals(categoryId, result)
        }

        assertEquals(knownIncome.size, repository.createdCategories.size)
        repository.createdCategories.forEach { category ->
            assertEquals(CategoryType.INCOME, category.type)
        }
    }

    // === Fallback Path Tests ===

    @Test
    fun `uses hardcoded fallback when other is not in IconRegistry`() = runTest {
        val repository = FakeCategoryRepository()
        // Use EmptyIconRegistry that returns null for all categories including "other"
        val emptyRegistry = EmptyIconRegistry()
        val resolver = CategoryResolver(repository, emptyRegistry)

        val result = resolver.ensureExists("unknown_category_xyz")

        assertEquals("other", result)
        // Should create "other" category with hardcoded values
        val createdOther = repository.createdCategories.find { it.id == "other" }
        assertEquals("Other", createdOther?.name)
        assertEquals("package", createdOther?.icon)
        assertEquals("#9E9E9E", createdOther?.color)
        assertEquals(999, createdOther?.sortOrder)
    }

    @Test
    fun `propagates exception when category creation fails`() = runTest {
        val repository = FailingCategoryRepository()
        val resolver = CategoryResolver(repository, IconRegistry())

        try {
            resolver.ensureExists("groceries")
            assertTrue(false, "Should have thrown exception")
        } catch (e: RuntimeException) {
            assertEquals("Database error", e.message)
        }
    }

    @Test
    fun `uses existing other category when already in database`() = runTest {
        val repository = FakeCategoryRepository(
            existingCategories = listOf(
                Category(
                    id = "other",
                    name = "Other",
                    icon = "package",
                    color = "#9E9E9E",
                    type = CategoryType.EXPENSE
                )
            )
        )
        val emptyRegistry = EmptyIconRegistry()
        val resolver = CategoryResolver(repository, emptyRegistry)

        val result = resolver.ensureExists("unknown_category")

        assertEquals("other", result)
        // Should NOT create another "other" category
        assertTrue(repository.createdCategories.isEmpty())
    }
}

/**
 * Fake implementation of CategoryRepository for testing.
 */
private class FakeCategoryRepository(
    private val existingCategories: List<Category> = emptyList()
) : CategoryRepository {

    val createdCategories = mutableListOf<Category>()

    override fun getAllCategories(): Flow<List<Category>> = flow {
        emit(existingCategories + createdCategories)
    }

    override fun getCategoryById(id: String): Flow<Category?> = flow {
        emit((existingCategories + createdCategories).find { it.id == id })
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flow {
        emit((existingCategories + createdCategories).filter { it.type == type })
    }

    override fun getDefaultCategories(): Flow<List<Category>> = flow {
        emit((existingCategories + createdCategories).filter { it.isDefault })
    }

    override suspend fun createCategory(category: Category) {
        createdCategories.add(category)
    }

    override suspend fun updateCategory(category: Category) {
        // Not needed for these tests
    }

    override suspend fun deleteCategory(id: String) {
        // Not needed for these tests
    }

    override suspend fun seedDefaultCategories() {
        // Not needed for these tests
    }
}

/**
 * IconRegistry that returns null for all categories.
 */
private class EmptyIconRegistry : IconRegistryProvider {
    override fun getCategoryMetadata(id: String): CategoryMetadata? = null
    override fun getAllKnownCategoryIds(): Set<String> = emptySet()
    override fun findBestMatch(hint: String): String = "package"
}

/**
 * Repository that throws on createCategory.
 */
private class FailingCategoryRepository : CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> = flow { emit(emptyList()) }
    override fun getCategoryById(id: String): Flow<Category?> = flow { emit(null) }
    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flow { emit(emptyList()) }
    override fun getDefaultCategories(): Flow<List<Category>> = flow { emit(emptyList()) }
    override suspend fun createCategory(category: Category) {
        throw RuntimeException("Database error")
    }
    override suspend fun updateCategory(category: Category) {}
    override suspend fun deleteCategory(id: String) {}
    override suspend fun seedDefaultCategories() {}
}

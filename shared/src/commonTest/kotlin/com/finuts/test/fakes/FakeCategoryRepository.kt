package com.finuts.test.fakes

import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategoryType
import com.finuts.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of CategoryRepository for testing.
 * Stores categories in memory and provides full control for test scenarios.
 */
class FakeCategoryRepository : CategoryRepository {

    private val categories = MutableStateFlow<List<Category>>(emptyList())

    override fun getAllCategories(): Flow<List<Category>> = categories

    override fun getCategoryById(id: String): Flow<Category?> =
        categories.map { list -> list.find { it.id == id } }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> =
        categories.map { list -> list.filter { it.type == type } }

    override fun getDefaultCategories(): Flow<List<Category>> =
        categories.map { list -> list.filter { it.isDefault } }

    override suspend fun createCategory(category: Category) {
        categories.update { it + category }
    }

    override suspend fun updateCategory(category: Category) {
        categories.update { list ->
            list.map { if (it.id == category.id) category else it }
        }
    }

    override suspend fun deleteCategory(id: String) {
        categories.update { list -> list.filter { it.id != id } }
    }

    override suspend fun seedDefaultCategories() {
        // No-op for tests, or can be customized per test
    }

    // Test helpers
    fun setCategories(newCategories: List<Category>) {
        categories.value = newCategories
    }

    fun clear() {
        categories.value = emptyList()
    }
}

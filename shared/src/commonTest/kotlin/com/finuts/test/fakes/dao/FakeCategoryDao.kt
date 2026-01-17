package com.finuts.test.fakes.dao

import com.finuts.data.local.dao.CategoryDao
import com.finuts.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of CategoryDao for unit testing.
 * Stores entities in memory with full CRUD support.
 */
class FakeCategoryDao : CategoryDao {

    private val categories = MutableStateFlow<List<CategoryEntity>>(emptyList())

    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categories.map { it.sortedBy { c -> c.sortOrder } }

    override suspend fun getCategoryById(id: String): CategoryEntity? =
        categories.value.find { it.id == id }

    override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
        categories.map { list ->
            list.filter { it.type == type }.sortedBy { it.sortOrder }
        }

    override fun getDefaultCategories(): Flow<List<CategoryEntity>> =
        categories.map { list ->
            list.filter { it.isDefault }.sortedBy { it.sortOrder }
        }

    override fun getSubcategories(parentId: String): Flow<List<CategoryEntity>> =
        categories.map { list ->
            list.filter { it.parentId == parentId }.sortedBy { it.sortOrder }
        }

    override suspend fun insert(category: CategoryEntity) {
        categories.update { list ->
            list.filterNot { it.id == category.id } + category
        }
    }

    override suspend fun insertAll(categories: List<CategoryEntity>) {
        categories.forEach { insert(it) }
    }

    override suspend fun update(category: CategoryEntity) {
        categories.update { list ->
            list.map { if (it.id == category.id) category else it }
        }
    }

    override suspend fun deleteById(id: String) {
        categories.update { list -> list.filter { it.id != id } }
    }

    override suspend fun getCategoryCount(): Int = categories.value.size

    // Test helpers
    fun setCategories(newCategories: List<CategoryEntity>) {
        categories.value = newCategories
    }

    fun clear() {
        categories.value = emptyList()
    }

    fun getAll(): List<CategoryEntity> = categories.value
}

package com.finuts.domain.repository

import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoryById(id: String): Flow<Category?>
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>
    fun getDefaultCategories(): Flow<List<Category>>
    suspend fun createCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: String)
    suspend fun seedDefaultCategories()
}

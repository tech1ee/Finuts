package com.finuts.data.repository

import com.finuts.data.local.dao.CategoryDao
import com.finuts.data.local.entity.CategoryEntity
import com.finuts.data.local.mapper.toDomain
import com.finuts.data.local.mapper.toEntity
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategoryType
import com.finuts.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getCategoryById(id: String): Flow<Category?> = flow {
        emit(categoryDao.getCategoryById(id)?.toDomain())
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type.name).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getDefaultCategories(): Flow<List<Category>> =
        categoryDao.getDefaultCategories().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createCategory(category: Category) {
        categoryDao.insert(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.update(category.toEntity())
    }

    override suspend fun deleteCategory(id: String) {
        categoryDao.deleteById(id)
    }

    override suspend fun seedDefaultCategories() {
        if (categoryDao.getCategoryCount() > 0) return
        categoryDao.insertAll(getDefaultCategoriesList())
    }

    private fun getDefaultCategoriesList(): List<CategoryEntity> = listOf(
        // Expense categories - IDs must match MerchantDatabase patterns
        CategoryEntity("groceries", "Groceries", "shopping_cart", "#4CAF50", "EXPENSE", null, true, 1),
        CategoryEntity("food_delivery", "Food Delivery", "delivery_dining", "#8BC34A", "EXPENSE", null, true, 2),
        CategoryEntity("transport", "Transport", "directions_car", "#2196F3", "EXPENSE", null, true, 3),
        CategoryEntity("shopping", "Shopping", "shopping_bag", "#9C27B0", "EXPENSE", null, true, 4),
        CategoryEntity("utilities", "Utilities", "power", "#FF9800", "EXPENSE", null, true, 5),
        CategoryEntity("healthcare", "Healthcare", "medical_services", "#F44336", "EXPENSE", null, true, 6),
        CategoryEntity("entertainment", "Entertainment", "sports_esports", "#E91E63", "EXPENSE", null, true, 7),
        CategoryEntity("education", "Education", "school", "#3F51B5", "EXPENSE", null, true, 8),
        CategoryEntity("housing", "Housing", "home", "#795548", "EXPENSE", null, true, 9),
        CategoryEntity("transfer", "Transfers", "swap_horiz", "#607D8B", "EXPENSE", null, true, 10),
        CategoryEntity("other", "Other", "more_horiz", "#9E9E9E", "EXPENSE", null, true, 11),
        // Income categories
        CategoryEntity("salary", "Salary", "work", "#4CAF50", "INCOME", null, true, 12),
        CategoryEntity("freelance", "Freelance", "computer", "#00BCD4", "INCOME", null, true, 13),
        CategoryEntity("investments", "Investments", "trending_up", "#8BC34A", "INCOME", null, true, 14),
        CategoryEntity("gifts", "Gifts", "card_giftcard", "#FF5722", "INCOME", null, true, 15),
        CategoryEntity("other_income", "Other Income", "attach_money", "#607D8B", "INCOME", null, true, 16)
    )
}

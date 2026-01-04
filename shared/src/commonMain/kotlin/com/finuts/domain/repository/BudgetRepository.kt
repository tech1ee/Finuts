package com.finuts.domain.repository

import com.finuts.domain.entity.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getAllBudgets(): Flow<List<Budget>>
    fun getActiveBudgets(): Flow<List<Budget>>
    fun getBudgetById(id: String): Flow<Budget?>
    fun getBudgetByCategory(categoryId: String): Flow<Budget?>
    suspend fun createBudget(budget: Budget)
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(id: String)
    suspend fun deactivateBudget(id: String)
}

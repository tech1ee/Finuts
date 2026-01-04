package com.finuts.data.repository

import com.finuts.data.local.dao.BudgetDao
import com.finuts.data.local.mapper.toDomain
import com.finuts.data.local.mapper.toEntity
import com.finuts.domain.entity.Budget
import com.finuts.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import com.finuts.core.util.currentTimeMillis

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getAllBudgets(): Flow<List<Budget>> =
        budgetDao.getAllBudgets().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getActiveBudgets(): Flow<List<Budget>> =
        budgetDao.getActiveBudgets().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getBudgetById(id: String): Flow<Budget?> = flow {
        emit(budgetDao.getBudgetById(id)?.toDomain())
    }

    override fun getBudgetByCategory(categoryId: String): Flow<Budget?> = flow {
        emit(budgetDao.getBudgetByCategory(categoryId)?.toDomain())
    }

    override suspend fun createBudget(budget: Budget) {
        budgetDao.insert(budget.toEntity())
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetDao.update(budget.toEntity())
    }

    override suspend fun deleteBudget(id: String) {
        budgetDao.deleteById(id)
    }

    override suspend fun deactivateBudget(id: String) {
        budgetDao.deactivate(id, currentTimeMillis())
    }
}

package com.finuts.test.fakes.dao

import com.finuts.data.local.dao.BudgetDao
import com.finuts.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of BudgetDao for unit testing.
 */
class FakeBudgetDao : BudgetDao {

    private val budgets = MutableStateFlow<List<BudgetEntity>>(emptyList())

    override fun getAllBudgets(): Flow<List<BudgetEntity>> =
        budgets.map { it.sortedBy { b -> b.name } }

    override fun getActiveBudgets(): Flow<List<BudgetEntity>> =
        budgets.map { list ->
            list.filter { it.isActive }.sortedBy { it.name }
        }

    override suspend fun getBudgetById(id: String): BudgetEntity? =
        budgets.value.find { it.id == id }

    override suspend fun getBudgetByCategory(categoryId: String): BudgetEntity? =
        budgets.value.find { it.categoryId == categoryId && it.isActive }

    override fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>> =
        budgets.map { list ->
            list.filter { it.period == period && it.isActive }
        }

    override suspend fun insert(budget: BudgetEntity) {
        budgets.update { list ->
            list.filterNot { it.id == budget.id } + budget
        }
    }

    override suspend fun update(budget: BudgetEntity) {
        budgets.update { list ->
            list.map { if (it.id == budget.id) budget else it }
        }
    }

    override suspend fun deleteById(id: String) {
        budgets.update { list -> list.filter { it.id != id } }
    }

    override suspend fun deactivate(id: String, timestamp: Long) {
        budgets.update { list ->
            list.map {
                if (it.id == id) it.copy(isActive = false, updatedAt = timestamp)
                else it
            }
        }
    }

    // Test helpers
    fun setBudgets(newBudgets: List<BudgetEntity>) {
        budgets.value = newBudgets
    }

    fun clear() {
        budgets.value = emptyList()
    }

    fun getAll(): List<BudgetEntity> = budgets.value
}

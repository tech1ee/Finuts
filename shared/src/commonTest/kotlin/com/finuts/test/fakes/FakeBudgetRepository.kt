package com.finuts.test.fakes

import com.finuts.domain.entity.Budget
import com.finuts.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of BudgetRepository for testing.
 * Stores budgets in memory and provides full control for test scenarios.
 */
class FakeBudgetRepository : BudgetRepository {

    private val budgets = MutableStateFlow<List<Budget>>(emptyList())

    override fun getAllBudgets(): Flow<List<Budget>> = budgets

    override fun getActiveBudgets(): Flow<List<Budget>> =
        budgets.map { list -> list.filter { it.isActive } }

    override fun getBudgetById(id: String): Flow<Budget?> =
        budgets.map { list -> list.find { it.id == id } }

    override fun getBudgetByCategory(categoryId: String): Flow<Budget?> =
        budgets.map { list -> list.find { it.categoryId == categoryId } }

    override suspend fun createBudget(budget: Budget) {
        budgets.update { it + budget }
    }

    override suspend fun updateBudget(budget: Budget) {
        budgets.update { list ->
            list.map { if (it.id == budget.id) budget else it }
        }
    }

    override suspend fun deleteBudget(id: String) {
        budgets.update { list -> list.filter { it.id != id } }
    }

    override suspend fun deactivateBudget(id: String) {
        budgets.update { list ->
            list.map { if (it.id == id) it.copy(isActive = false) else it }
        }
    }

    // Test helpers
    fun setBudgets(newBudgets: List<Budget>) {
        budgets.value = newBudgets
    }

    fun clear() {
        budgets.value = emptyList()
    }
}

package com.finuts.test.fakes

import app.cash.turbine.test
import com.finuts.domain.entity.BudgetPeriod
import com.finuts.test.BaseTest
import com.finuts.test.TestData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for FakeBudgetRepository to ensure our test fakes work correctly.
 */
class FakeBudgetRepositoryTest : BaseTest() {

    private val repository = FakeBudgetRepository()

    @Test
    fun `getAllBudgets returns empty list initially`() = runTest {
        repository.getAllBudgets().test {
            assertEquals(emptyList(), awaitItem())
        }
    }

    @Test
    fun `createBudget adds budget to list`() = runTest {
        val budget = TestData.budget(id = "budget-1", name = "Food Budget")

        repository.getAllBudgets().test {
            assertEquals(emptyList(), awaitItem())

            repository.createBudget(budget)

            val budgets = awaitItem()
            assertEquals(1, budgets.size)
            assertEquals("Food Budget", budgets.first().name)
        }
    }

    @Test
    fun `getBudgetById returns budget when exists`() = runTest {
        val budget = TestData.budget(id = "budget-1", name = "Monthly Budget")
        repository.createBudget(budget)

        repository.getBudgetById("budget-1").test {
            assertEquals("Monthly Budget", awaitItem()?.name)
        }
    }

    @Test
    fun `getBudgetById returns null when not exists`() = runTest {
        repository.getBudgetById("non-existent").test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `getActiveBudgets excludes inactive budgets`() = runTest {
        val activeBudget = TestData.budget(id = "1", name = "Active", isActive = true)
        val inactiveBudget = TestData.budget(id = "2", name = "Inactive", isActive = false)

        repository.setBudgets(listOf(activeBudget, inactiveBudget))

        repository.getActiveBudgets().test {
            val active = awaitItem()
            assertEquals(1, active.size)
            assertEquals("Active", active.first().name)
        }
    }

    @Test
    fun `getBudgetByCategory returns budget for category`() = runTest {
        val foodBudget = TestData.budget(id = "1", categoryId = "food-cat", name = "Food")
        val transportBudget = TestData.budget(id = "2", categoryId = "transport-cat", name = "Transport")

        repository.setBudgets(listOf(foodBudget, transportBudget))

        repository.getBudgetByCategory("food-cat").test {
            assertEquals("Food", awaitItem()?.name)
        }
    }

    @Test
    fun `getBudgetByCategory returns null when no budget for category`() = runTest {
        val budget = TestData.budget(categoryId = "food-cat")
        repository.createBudget(budget)

        repository.getBudgetByCategory("non-existent-cat").test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `updateBudget modifies existing budget`() = runTest {
        val budget = TestData.budget(id = "1", name = "Original", amount = 10000L)
        repository.createBudget(budget)

        val updatedBudget = budget.copy(name = "Updated", amount = 20000L)
        repository.updateBudget(updatedBudget)

        repository.getBudgetById("1").test {
            val result = awaitItem()
            assertEquals("Updated", result?.name)
            assertEquals(20000L, result?.amount)
        }
    }

    @Test
    fun `deleteBudget removes budget from list`() = runTest {
        val budget = TestData.budget(id = "1")
        repository.createBudget(budget)

        repository.deleteBudget("1")

        repository.getAllBudgets().test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `deactivateBudget sets isActive to false`() = runTest {
        val budget = TestData.budget(id = "1", isActive = true)
        repository.createBudget(budget)

        repository.deactivateBudget("1")

        repository.getBudgetById("1").test {
            assertEquals(false, awaitItem()?.isActive)
        }
    }

    @Test
    fun `clear removes all budgets`() = runTest {
        repository.setBudgets(
            listOf(
                TestData.budget(id = "1"),
                TestData.budget(id = "2"),
                TestData.budget(id = "3")
            )
        )

        repository.clear()

        repository.getAllBudgets().test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `getAllBudgets returns budgets with different periods`() = runTest {
        val dailyBudget = TestData.budget(id = "1", period = BudgetPeriod.DAILY)
        val weeklyBudget = TestData.budget(id = "2", period = BudgetPeriod.WEEKLY)
        val monthlyBudget = TestData.budget(id = "3", period = BudgetPeriod.MONTHLY)

        repository.setBudgets(listOf(dailyBudget, weeklyBudget, monthlyBudget))

        repository.getAllBudgets().test {
            val budgets = awaitItem()
            assertEquals(3, budgets.size)
            assertEquals(BudgetPeriod.DAILY, budgets[0].period)
            assertEquals(BudgetPeriod.WEEKLY, budgets[1].period)
            assertEquals(BudgetPeriod.MONTHLY, budgets[2].period)
        }
    }

    @Test
    fun `getBudgetByCategory handles null categoryId budget`() = runTest {
        val budgetWithoutCategory = TestData.budget(id = "1", categoryId = null, name = "Overall")
        repository.createBudget(budgetWithoutCategory)

        // Searching for null category returns the budget
        repository.getBudgetByCategory("some-category").test {
            assertNull(awaitItem())
        }
    }
}

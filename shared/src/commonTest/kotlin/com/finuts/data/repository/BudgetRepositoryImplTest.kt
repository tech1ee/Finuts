package com.finuts.data.repository

import app.cash.turbine.test
import com.finuts.data.local.entity.BudgetEntity
import com.finuts.domain.entity.Budget
import com.finuts.domain.entity.BudgetPeriod
import com.finuts.domain.entity.Currency
import com.finuts.test.fakes.dao.FakeBudgetDao
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BudgetRepositoryImplTest {

    private lateinit var fakeDao: FakeBudgetDao
    private lateinit var repository: BudgetRepositoryImpl

    @BeforeTest
    fun setup() {
        fakeDao = FakeBudgetDao()
        repository = BudgetRepositoryImpl(fakeDao)
    }

    // === getAllBudgets Tests ===

    @Test
    fun `getAllBudgets returns empty list when no budgets`() = runTest {
        repository.getAllBudgets().test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllBudgets returns all budgets mapped to domain`() = runTest {
        fakeDao.setBudgets(listOf(
            createBudgetEntity("1", "Food Budget"),
            createBudgetEntity("2", "Transport Budget")
        ))

        repository.getAllBudgets().test {
            val budgets = awaitItem()
            assertEquals(2, budgets.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === getActiveBudgets Tests ===

    @Test
    fun `getActiveBudgets excludes inactive budgets`() = runTest {
        fakeDao.setBudgets(listOf(
            createBudgetEntity("1", "Active", isActive = true),
            createBudgetEntity("2", "Inactive", isActive = false)
        ))

        repository.getActiveBudgets().test {
            val budgets = awaitItem()
            assertEquals(1, budgets.size)
            assertEquals("Active", budgets[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === getBudgetById Tests ===

    @Test
    fun `getBudgetById returns null when not found`() = runTest {
        repository.getBudgetById("non-existent").test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getBudgetById returns budget when found`() = runTest {
        fakeDao.setBudgets(listOf(createBudgetEntity("1", "My Budget")))

        repository.getBudgetById("1").test {
            val budget = awaitItem()
            assertEquals("1", budget?.id)
            assertEquals("My Budget", budget?.name)
            awaitComplete()
        }
    }

    // === getBudgetByCategory Tests ===

    @Test
    fun `getBudgetByCategory returns null when no matching category`() = runTest {
        fakeDao.setBudgets(listOf(createBudgetEntity("1", "Budget", categoryId = "food")))

        repository.getBudgetByCategory("transport").test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getBudgetByCategory returns active budget for category`() = runTest {
        fakeDao.setBudgets(listOf(
            createBudgetEntity("1", "Food Budget", categoryId = "food", isActive = true),
            createBudgetEntity("2", "Old Budget", categoryId = "food", isActive = false)
        ))

        repository.getBudgetByCategory("food").test {
            val budget = awaitItem()
            assertEquals("Food Budget", budget?.name)
            awaitComplete()
        }
    }

    // === createBudget Tests ===

    @Test
    fun `createBudget inserts entity into dao`() = runTest {
        val budget = createDomainBudget("new-id", "New Budget")

        repository.createBudget(budget)

        val stored = fakeDao.getBudgetById("new-id")
        assertEquals("New Budget", stored?.name)
    }

    // === updateBudget Tests ===

    @Test
    fun `updateBudget modifies existing entity`() = runTest {
        fakeDao.setBudgets(listOf(createBudgetEntity("1", "Old Name")))
        val updated = createDomainBudget("1", "New Name")

        repository.updateBudget(updated)

        val stored = fakeDao.getBudgetById("1")
        assertEquals("New Name", stored?.name)
    }

    // === deleteBudget Tests ===

    @Test
    fun `deleteBudget removes entity from dao`() = runTest {
        fakeDao.setBudgets(listOf(createBudgetEntity("1", "ToDelete")))

        repository.deleteBudget("1")

        assertNull(fakeDao.getBudgetById("1"))
    }

    // === deactivateBudget Tests ===

    @Test
    fun `deactivateBudget sets isActive to false`() = runTest {
        fakeDao.setBudgets(listOf(createBudgetEntity("1", "Active", isActive = true)))

        repository.deactivateBudget("1")

        val budget = fakeDao.getBudgetById("1")
        assertTrue(budget?.isActive == false)
    }

    // === Helper Functions ===

    private fun createBudgetEntity(
        id: String,
        name: String,
        categoryId: String = "groceries",
        isActive: Boolean = true
    ) = BudgetEntity(
        id = id,
        categoryId = categoryId,
        name = name,
        amount = 50000L,
        currencyCode = "USD",
        currencySymbol = "$",
        currencyName = "US Dollar",
        period = "MONTHLY",
        startDate = 1704067200000L,
        endDate = null,
        isActive = isActive,
        createdAt = 1704067200000L,
        updatedAt = 1704067200000L
    )

    private fun createDomainBudget(
        id: String,
        name: String
    ) = Budget(
        id = id,
        categoryId = "groceries",
        name = name,
        amount = 50000L,
        currency = Currency("USD", "$", "US Dollar"),
        period = BudgetPeriod.MONTHLY,
        startDate = Instant.fromEpochMilliseconds(1704067200000L),
        endDate = null,
        isActive = true,
        createdAt = Instant.fromEpochMilliseconds(1704067200000L),
        updatedAt = Instant.fromEpochMilliseconds(1704067200000L)
    )
}

package com.finuts.app.feature.budgets

import app.cash.turbine.test
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.test.TestData
import com.finuts.app.test.fakes.FakeBudgetRepository
import com.finuts.app.test.fakes.FakeTransactionRepository
import com.finuts.domain.entity.BudgetPeriod
import com.finuts.domain.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for BudgetsViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var budgetRepository: FakeBudgetRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var viewModel: BudgetsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        budgetRepository = FakeBudgetRepository()
        transactionRepository = FakeTransactionRepository()
        viewModel = BudgetsViewModel(budgetRepository, transactionRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // === UI State Tests ===

    @Test
    fun `uiState starts with Loading`() = runTest {
        viewModel.uiState.test {
            assertIs<BudgetsUiState.Loading>(awaitItem())
        }
    }

    @Test
    fun `uiState emits Success with empty budgets`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetsUiState.Success>(state)
            assertTrue(state.budgets.isEmpty())
        }
    }

    @Test
    fun `uiState shows active budgets only`() = runTest {
        val activeBudget = TestData.budget(id = "1", name = "Food", isActive = true)
        val inactiveBudget = TestData.budget(id = "2", name = "Old", isActive = false)
        budgetRepository.setBudgets(listOf(activeBudget, inactiveBudget))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetsUiState.Success>(state)
            assertEquals(1, state.budgets.size)
            assertEquals("Food", state.budgets.first().budget.name)
        }
    }

    @Test
    fun `uiState calculates budget progress`() = runTest {
        val budget = TestData.budget(
            id = "1",
            categoryId = "food-cat",
            amount = 100_000_00L
        )
        val expense = TestData.transaction(
            id = "tx-1",
            categoryId = "food-cat",
            amount = 30_000_00L,
            type = TransactionType.EXPENSE
        )
        budgetRepository.setBudgets(listOf(budget))
        transactionRepository.setTransactions(listOf(expense))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetsUiState.Success>(state)
            val progress = state.budgets.first()
            assertEquals(30_000_00L, progress.spent)
            assertEquals(30f, progress.percentUsed, 0.1f)
        }
    }

    @Test
    fun `isEmpty returns true when no budgets`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetsUiState.Success>(state)
            assertTrue(state.isEmpty)
        }
    }

    @Test
    fun `isEmpty returns false when has budgets`() = runTest {
        budgetRepository.setBudgets(listOf(TestData.budget()))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetsUiState.Success>(state)
            assertFalse(state.isEmpty)
        }
    }

    @Test
    fun `totalBudgeted sums all active budget amounts`() = runTest {
        val budget1 = TestData.budget(id = "1", amount = 50_000_00L)
        val budget2 = TestData.budget(id = "2", amount = 30_000_00L)
        budgetRepository.setBudgets(listOf(budget1, budget2))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetsUiState.Success>(state)
            assertEquals(80_000_00L, state.totalBudgeted)
        }
    }

    @Test
    fun `totalSpent sums all expenses for budgeted categories`() = runTest {
        val budget = TestData.budget(id = "1", categoryId = "food", amount = 100_000_00L)
        val tx1 = TestData.transaction(id = "1", categoryId = "food", amount = 20_000_00L, type = TransactionType.EXPENSE)
        val tx2 = TestData.transaction(id = "2", categoryId = "food", amount = 15_000_00L, type = TransactionType.EXPENSE)
        val tx3 = TestData.transaction(id = "3", categoryId = "other", amount = 50_000_00L, type = TransactionType.EXPENSE)

        budgetRepository.setBudgets(listOf(budget))
        transactionRepository.setTransactions(listOf(tx1, tx2, tx3))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetsUiState.Success>(state)
            assertEquals(35_000_00L, state.totalSpent)
        }
    }

    // === Navigation Tests ===

    @Test
    fun `onBudgetClick navigates to BudgetDetail`() = runTest {
        viewModel.navigationEvents.test {
            viewModel.onBudgetClick("budget-123")
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.BudgetDetail("budget-123"), event.route)
        }
    }

    @Test
    fun `onAddBudgetClick navigates to AddBudget`() = runTest {
        viewModel.navigationEvents.test {
            viewModel.onAddBudgetClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.AddBudget, event.route)
        }
    }

    // === Action Tests ===

    @Test
    fun `onDeleteBudget removes the budget`() = runTest {
        val budget = TestData.budget(id = "1")
        budgetRepository.setBudgets(listOf(budget))

        viewModel.onDeleteBudget("1")
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetsUiState.Success>(state)
            assertTrue(state.isEmpty)
        }
    }

    @Test
    fun `onDeactivateBudget sets budget inactive`() = runTest {
        val budget = TestData.budget(id = "1", isActive = true)
        budgetRepository.setBudgets(listOf(budget))

        viewModel.onDeactivateBudget("1")
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetsUiState.Success>(state)
            assertTrue(state.isEmpty) // Active filter excludes deactivated
        }
    }

    @Test
    fun `refresh sets isRefreshing to true then false`() = runTest {
        viewModel.isRefreshing.test {
            assertFalse(awaitItem()) // Initial false

            viewModel.refresh()
            advanceUntilIdle()

            assertTrue(awaitItem()) // Refreshing
            assertFalse(awaitItem()) // Done
        }
    }

    // === Period Filter Tests ===

    @Test
    fun `budgets can be filtered by period`() = runTest {
        val monthlyBudget = TestData.budget(id = "1", period = BudgetPeriod.MONTHLY)
        val weeklyBudget = TestData.budget(id = "2", period = BudgetPeriod.WEEKLY)
        budgetRepository.setBudgets(listOf(monthlyBudget, weeklyBudget))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetsUiState.Success>(state)
            assertEquals(2, state.budgets.size)
        }
    }

    // === Sorting Tests ===

    @Test
    fun `budgets are sorted by percentage used descending`() = runTest {
        val lowSpend = TestData.budget(id = "1", categoryId = "cat-1", amount = 100_000_00L)
        val highSpend = TestData.budget(id = "2", categoryId = "cat-2", amount = 100_000_00L)

        val tx1 = TestData.transaction(id = "1", categoryId = "cat-1", amount = 10_000_00L, type = TransactionType.EXPENSE)
        val tx2 = TestData.transaction(id = "2", categoryId = "cat-2", amount = 80_000_00L, type = TransactionType.EXPENSE)

        budgetRepository.setBudgets(listOf(lowSpend, highSpend))
        transactionRepository.setTransactions(listOf(tx1, tx2))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetsUiState.Success>(state)
            // High spend budget should be first (80% vs 10%)
            assertEquals("2", state.budgets.first().budget.id)
        }
    }
}

package com.finuts.app.feature.budgets

import app.cash.turbine.test
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.test.TestData
import com.finuts.app.test.fakes.FakeBudgetRepository
import com.finuts.app.test.fakes.FakeTransactionRepository
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
 * Tests for BudgetDetailViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var budgetRepository: FakeBudgetRepository
    private lateinit var transactionRepository: FakeTransactionRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        budgetRepository = FakeBudgetRepository()
        transactionRepository = FakeTransactionRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(budgetId: String) = BudgetDetailViewModel(
        budgetId = budgetId,
        budgetRepository = budgetRepository,
        transactionRepository = transactionRepository
    )

    // === UI State Tests ===

    @Test
    fun `uiState starts with Loading`() = runTest {
        val viewModel = createViewModel("budget-1")

        viewModel.uiState.test {
            assertIs<BudgetDetailUiState.Loading>(awaitItem())
        }
    }

    @Test
    fun `uiState emits Error when budget not found`() = runTest {
        val viewModel = createViewModel("non-existent")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetDetailUiState.Error>(state)
            assertEquals("Budget not found", state.message)
        }
    }

    @Test
    fun `uiState emits Success when budget exists`() = runTest {
        val budget = TestData.budget(id = "budget-1", name = "Food Budget")
        budgetRepository.setBudgets(listOf(budget))

        val viewModel = createViewModel("budget-1")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetDetailUiState.Success>(state)
            assertEquals("Food Budget", state.budget.name)
        }
    }

    @Test
    fun `uiState includes transactions for budget category`() = runTest {
        val budget = TestData.budget(id = "budget-1", categoryId = "food-cat")
        val tx1 = TestData.transaction(id = "tx-1", categoryId = "food-cat", type = TransactionType.EXPENSE)
        val tx2 = TestData.transaction(id = "tx-2", categoryId = "food-cat", type = TransactionType.EXPENSE)
        val otherTx = TestData.transaction(id = "tx-3", categoryId = "transport-cat", type = TransactionType.EXPENSE)

        budgetRepository.setBudgets(listOf(budget))
        transactionRepository.setTransactions(listOf(tx1, tx2, otherTx))

        val viewModel = createViewModel("budget-1")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetDetailUiState.Success>(state)
            assertEquals(2, state.transactions.size)
        }
    }

    @Test
    fun `uiState calculates progress correctly`() = runTest {
        val budget = TestData.budget(id = "budget-1", categoryId = "food-cat", amount = 100_000_00L)
        val tx = TestData.transaction(id = "tx-1", categoryId = "food-cat", amount = 45_000_00L, type = TransactionType.EXPENSE)

        budgetRepository.setBudgets(listOf(budget))
        transactionRepository.setTransactions(listOf(tx))

        val viewModel = createViewModel("budget-1")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetDetailUiState.Success>(state)
            assertEquals(45_000_00L, state.spent)
            assertEquals(55_000_00L, state.remaining)
            assertEquals(45f, state.percentUsed, 0.1f)
        }
    }

    @Test
    fun `hasTransactions returns true when transactions exist`() = runTest {
        val budget = TestData.budget(id = "budget-1", categoryId = "food-cat")
        val tx = TestData.transaction(categoryId = "food-cat")

        budgetRepository.setBudgets(listOf(budget))
        transactionRepository.setTransactions(listOf(tx))

        val viewModel = createViewModel("budget-1")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetDetailUiState.Success>(state)
            assertTrue(state.hasTransactions)
        }
    }

    @Test
    fun `hasTransactions returns false when no transactions`() = runTest {
        val budget = TestData.budget(id = "budget-1", categoryId = "food-cat")
        budgetRepository.setBudgets(listOf(budget))

        val viewModel = createViewModel("budget-1")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetDetailUiState.Success>(state)
            assertFalse(state.hasTransactions)
        }
    }

    // === Navigation Tests ===

    @Test
    fun `onEditClick navigates to EditBudget`() = runTest {
        val viewModel = createViewModel("budget-123")

        viewModel.navigationEvents.test {
            viewModel.onEditClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.EditBudget("budget-123"), event.route)
        }
    }

    @Test
    fun `onTransactionClick navigates to TransactionDetail`() = runTest {
        val viewModel = createViewModel("budget-1")

        viewModel.navigationEvents.test {
            viewModel.onTransactionClick("tx-456")
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.TransactionDetail("tx-456"), event.route)
        }
    }

    @Test
    fun `onBackClick navigates back`() = runTest {
        val viewModel = createViewModel("budget-1")

        viewModel.navigationEvents.test {
            viewModel.onBackClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }
    }

    // === Action Tests ===

    @Test
    fun `onDeleteClick deletes budget and navigates back`() = runTest {
        val budget = TestData.budget(id = "budget-1")
        budgetRepository.setBudgets(listOf(budget))

        val viewModel = createViewModel("budget-1")

        viewModel.navigationEvents.test {
            viewModel.onDeleteClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }

        // Verify budget is deleted
        budgetRepository.getBudgetById("budget-1").test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `onDeactivateClick deactivates budget and navigates back`() = runTest {
        val budget = TestData.budget(id = "budget-1", isActive = true)
        budgetRepository.setBudgets(listOf(budget))

        val viewModel = createViewModel("budget-1")

        viewModel.navigationEvents.test {
            viewModel.onDeactivateClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }

        // Verify budget is deactivated
        budgetRepository.getBudgetById("budget-1").test {
            val deactivated = awaitItem()
            assertEquals(false, deactivated?.isActive)
        }
    }

    // === Income Transactions Excluded ===

    @Test
    fun `only expense transactions count towards budget spending`() = runTest {
        val budget = TestData.budget(id = "budget-1", categoryId = "food-cat", amount = 100_000_00L)
        val expense = TestData.transaction(id = "1", categoryId = "food-cat", amount = 30_000_00L, type = TransactionType.EXPENSE)
        val income = TestData.transaction(id = "2", categoryId = "food-cat", amount = 50_000_00L, type = TransactionType.INCOME)

        budgetRepository.setBudgets(listOf(budget))
        transactionRepository.setTransactions(listOf(expense, income))

        val viewModel = createViewModel("budget-1")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<BudgetDetailUiState.Success>(state)
            assertEquals(30_000_00L, state.spent) // Only expense counted
        }
    }
}

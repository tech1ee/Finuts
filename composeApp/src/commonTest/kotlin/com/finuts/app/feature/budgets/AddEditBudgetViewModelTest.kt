package com.finuts.app.feature.budgets

import app.cash.turbine.test
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.test.TestData
import com.finuts.app.test.fakes.FakeBudgetRepository
import com.finuts.app.test.fakes.FakeCategoryRepository
import com.finuts.domain.entity.BudgetPeriod
import com.finuts.domain.entity.CategoryType
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for AddEditBudgetViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditBudgetViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var budgetRepository: FakeBudgetRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        budgetRepository = FakeBudgetRepository()
        categoryRepository = FakeCategoryRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(budgetId: String? = null) = AddEditBudgetViewModel(
        budgetId = budgetId,
        budgetRepository = budgetRepository,
        categoryRepository = categoryRepository
    )

    // === Mode Detection ===

    @Test
    fun `isEditMode is false when budgetId is null`() {
        val viewModel = createViewModel(null)
        assertFalse(viewModel.isEditMode)
    }

    @Test
    fun `isEditMode is true when budgetId is provided`() {
        val viewModel = createViewModel("budget-1")
        assertTrue(viewModel.isEditMode)
    }

    // === Default Form State ===

    @Test
    fun `formState starts with default values in add mode`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            val state = awaitItem()
            assertEquals("", state.name)
            assertEquals("", state.amount)
            assertNull(state.categoryId)
            assertEquals(BudgetPeriod.MONTHLY, state.period)
            assertEquals("KZT", state.currencyCode)
            assertNull(state.nameError)
            assertNull(state.amountError)
        }
    }

    // === Loading Existing Budget ===

    @Test
    fun `formState loads existing budget in edit mode`() = runTest {
        val budget = TestData.budget(
            id = "budget-1",
            name = "Food Budget",
            amount = 50000_00L,
            categoryId = "food-cat",
            period = BudgetPeriod.WEEKLY
        )
        budgetRepository.setBudgets(listOf(budget))

        val viewModel = createViewModel("budget-1")

        viewModel.formState.test {
            val initial = awaitItem()
            if (initial.name.isEmpty()) {
                advanceUntilIdle()
                val state = awaitItem()
                assertEquals("Food Budget", state.name)
                assertEquals("50000.0", state.amount)
                assertEquals("food-cat", state.categoryId)
                assertEquals(BudgetPeriod.WEEKLY, state.period)
            } else {
                assertEquals("Food Budget", initial.name)
                assertEquals(BudgetPeriod.WEEKLY, initial.period)
            }
        }
    }

    // === Form Field Changes ===

    @Test
    fun `onNameChange updates name`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onNameChange("Groceries")

            val state = awaitItem()
            assertEquals("Groceries", state.name)
        }
    }

    @Test
    fun `onNameChange clears nameError`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            // Trigger error first
            viewModel.save()
            advanceUntilIdle()
            awaitItem() // Error state

            viewModel.onNameChange("Valid Name")

            val state = awaitItem()
            assertNull(state.nameError)
        }
    }

    @Test
    fun `onAmountChange updates amount`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onAmountChange("10000.50")

            val state = awaitItem()
            assertEquals("10000.50", state.amount)
        }
    }

    @Test
    fun `onAmountChange clears amountError`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onNameChange("Budget")
            awaitItem()

            // Trigger error first
            viewModel.save()
            advanceUntilIdle()
            awaitItem() // Error state

            viewModel.onAmountChange("5000")

            val state = awaitItem()
            assertNull(state.amountError)
        }
    }

    @Test
    fun `onAmountChange rejects invalid format`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            val initial = awaitItem()
            assertEquals("", initial.amount)

            viewModel.onAmountChange("abc") // Invalid

            // Valid input
            viewModel.onAmountChange("123.45")
            val state = awaitItem()
            assertEquals("123.45", state.amount)
        }
    }

    @Test
    fun `onCategoryChange updates categoryId`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onCategoryChange("food-cat")

            val state = awaitItem()
            assertEquals("food-cat", state.categoryId)
        }
    }

    @Test
    fun `onPeriodChange updates period`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onPeriodChange(BudgetPeriod.WEEKLY)

            val state = awaitItem()
            assertEquals(BudgetPeriod.WEEKLY, state.period)
        }
    }

    @Test
    fun `onCurrencyChange updates currencyCode`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onCurrencyChange("USD")

            val state = awaitItem()
            assertEquals("USD", state.currencyCode)
        }
    }

    // === Categories Loading ===

    @Test
    fun `categories loads expense categories from repository`() = runTest {
        val expenseCategory = TestData.category(id = "food", name = "Food", type = CategoryType.EXPENSE)
        val incomeCategory = TestData.category(id = "salary", name = "Salary", type = CategoryType.INCOME)
        categoryRepository.setCategories(listOf(expenseCategory, incomeCategory))

        val viewModel = createViewModel(null)

        viewModel.categories.test {
            awaitItem() // May be empty initially
            advanceUntilIdle()

            val categories = expectMostRecentItem()
            assertEquals(1, categories.size)
            assertEquals("Food", categories.first().name)
        }
    }

    // === Validation ===

    @Test
    fun `save fails when name is blank`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.save()
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Name is required", state.nameError)
        }
    }

    @Test
    fun `save fails when amount is empty`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Initial state

            viewModel.onNameChange("Test Budget")
            awaitItem() // After name change

            viewModel.save()
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Amount is required", state.amountError)
        }
    }

    @Test
    fun `save fails when amount is zero`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Initial

            viewModel.onNameChange("Test Budget")
            awaitItem()

            viewModel.onAmountChange("0")
            awaitItem()

            viewModel.save()
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Amount must be greater than zero", state.amountError)
        }
    }

    // === Save Operations ===

    @Test
    fun `save creates budget and navigates back`() = runTest {
        val category = TestData.category(id = "food-cat", type = CategoryType.EXPENSE)
        categoryRepository.setCategories(listOf(category))

        val viewModel = createViewModel(null)

        viewModel.onNameChange("Food Budget")
        viewModel.onAmountChange("50000")
        viewModel.onCategoryChange("food-cat")
        viewModel.onPeriodChange(BudgetPeriod.MONTHLY)

        viewModel.navigationEvents.test {
            viewModel.save()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }

        // Verify budget was created
        budgetRepository.getAllBudgets().test {
            val budgets = awaitItem()
            assertEquals(1, budgets.size)
            assertEquals("Food Budget", budgets.first().name)
            assertEquals(50000_00L, budgets.first().amount) // 50000 * 100
            assertEquals("food-cat", budgets.first().categoryId)
            assertEquals(BudgetPeriod.MONTHLY, budgets.first().period)
        }
    }

    @Test
    fun `save updates budget in edit mode`() = runTest {
        val budget = TestData.budget(id = "budget-1", name = "Old Name", amount = 30000_00L)
        budgetRepository.setBudgets(listOf(budget))

        val viewModel = createViewModel("budget-1")
        advanceUntilIdle()

        // Wait for form state to be populated
        viewModel.formState.test {
            var state = awaitItem()
            if (state.name.isEmpty()) {
                advanceUntilIdle()
                state = awaitItem()
            }
            assertEquals("Old Name", state.name)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.onNameChange("New Name")
        viewModel.onAmountChange("60000")
        advanceUntilIdle()

        viewModel.navigationEvents.test {
            viewModel.save()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }

        // Verify budget was updated
        budgetRepository.getBudgetById("budget-1").test {
            val updated = awaitItem()
            assertEquals("New Name", updated?.name)
            assertEquals(60000_00L, updated?.amount)
        }
    }

    @Test
    fun `isSaving is false initially`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.isSaving.test {
            assertFalse(awaitItem())
        }
    }

    // === Navigation ===

    @Test
    fun `onBackClick navigates back`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.navigationEvents.test {
            viewModel.onBackClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }
    }

    // === Static Values ===

    @Test
    fun `SUPPORTED_CURRENCIES has expected values`() {
        val currencies = AddEditBudgetViewModel.SUPPORTED_CURRENCIES
        assertEquals(4, currencies.size)
        assertTrue(currencies.any { it.code == "KZT" })
        assertTrue(currencies.any { it.code == "USD" })
        assertTrue(currencies.any { it.code == "EUR" })
        assertTrue(currencies.any { it.code == "RUB" })
    }

    @Test
    fun `BUDGET_PERIODS has all periods`() {
        val periods = AddEditBudgetViewModel.BUDGET_PERIODS
        assertEquals(BudgetPeriod.entries.size, periods.size)
    }

    // === Form State Defaults ===

    @Test
    fun `BudgetFormState defaults are correct`() {
        val state = BudgetFormState()
        assertEquals("", state.name)
        assertEquals("", state.amount)
        assertNull(state.categoryId)
        assertEquals(BudgetPeriod.MONTHLY, state.period)
        assertEquals("KZT", state.currencyCode)
        assertNull(state.nameError)
        assertNull(state.amountError)
    }
}

package com.finuts.app.feature.categories

import app.cash.turbine.test
import com.finuts.app.test.TestData
import com.finuts.app.test.fakes.FakeCategoryRepository
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for CategoryManagementViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var viewModel: CategoryManagementViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryRepository = FakeCategoryRepository()
        viewModel = CategoryManagementViewModel(categoryRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // === Initial State ===

    @Test
    fun `uiState starts with Loading`() = runTest {
        viewModel.uiState.test {
            assertIs<CategoryManagementUiState.Loading>(awaitItem())
        }
    }

    @Test
    fun `uiState emits Success after loading categories`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<CategoryManagementUiState.Success>(state)
        }
    }

    // === Type Selection ===

    @Test
    fun `selectedType defaults to EXPENSE`() = runTest {
        viewModel.selectedType.test {
            assertEquals(CategoryType.EXPENSE, awaitItem())
        }
    }

    @Test
    fun `selectType changes selectedType to INCOME`() = runTest {
        viewModel.selectedType.test {
            assertEquals(CategoryType.EXPENSE, awaitItem())

            viewModel.selectType(CategoryType.INCOME)
            advanceUntilIdle()

            assertEquals(CategoryType.INCOME, awaitItem())
        }
    }

    @Test
    fun `selectType filters categories by type`() = runTest {
        val expenseCategory = TestData.category(
            id = "expense-1",
            name = "Food",
            type = CategoryType.EXPENSE
        )
        val incomeCategory = TestData.category(
            id = "income-1",
            name = "Salary",
            type = CategoryType.INCOME
        )
        categoryRepository.setCategories(listOf(expenseCategory, incomeCategory))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            var state = awaitItem()
            assertIs<CategoryManagementUiState.Success>(state)
            assertEquals(1, state.defaultCategories.size + state.customCategories.size)
            assertEquals("Food", (state.defaultCategories + state.customCategories).first().name)

            viewModel.selectType(CategoryType.INCOME)
            advanceUntilIdle()

            state = awaitItem()
            assertIs<CategoryManagementUiState.Success>(state)
            assertEquals(1, state.defaultCategories.size + state.customCategories.size)
            assertEquals("Salary", (state.defaultCategories + state.customCategories).first().name)
        }
    }

    // === Category Separation ===

    @Test
    fun `separates default and custom categories`() = runTest {
        val defaultCategory = TestData.category(
            id = "default-1",
            name = "Food",
            isDefault = true,
            type = CategoryType.EXPENSE
        )
        val customCategory = TestData.category(
            id = "custom-1",
            name = "Gaming",
            isDefault = false,
            type = CategoryType.EXPENSE
        )
        categoryRepository.setCategories(listOf(defaultCategory, customCategory))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<CategoryManagementUiState.Success>(state)
            assertEquals(1, state.defaultCategories.size)
            assertEquals(1, state.customCategories.size)
            assertEquals("Food", state.defaultCategories.first().name)
            assertEquals("Gaming", state.customCategories.first().name)
        }
    }

    @Test
    fun `sorts categories by sortOrder`() = runTest {
        val category1 = TestData.category(
            id = "cat-1",
            name = "Zebra",
            sortOrder = 2,
            isDefault = true,
            type = CategoryType.EXPENSE
        )
        val category2 = TestData.category(
            id = "cat-2",
            name = "Apple",
            sortOrder = 0,
            isDefault = true,
            type = CategoryType.EXPENSE
        )
        val category3 = TestData.category(
            id = "cat-3",
            name = "Mango",
            sortOrder = 1,
            isDefault = true,
            type = CategoryType.EXPENSE
        )
        categoryRepository.setCategories(listOf(category1, category2, category3))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<CategoryManagementUiState.Success>(state)
            assertEquals(3, state.defaultCategories.size)
            assertEquals("Apple", state.defaultCategories[0].name)
            assertEquals("Mango", state.defaultCategories[1].name)
            assertEquals("Zebra", state.defaultCategories[2].name)
        }
    }

    @Test
    fun `shows empty lists when no categories of type`() = runTest {
        val incomeCategory = TestData.category(
            id = "income-1",
            name = "Salary",
            type = CategoryType.INCOME
        )
        categoryRepository.setCategories(listOf(incomeCategory))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<CategoryManagementUiState.Success>(state)
            assertTrue(state.defaultCategories.isEmpty())
            assertTrue(state.customCategories.isEmpty())
        }
    }

    // === Delete Operations ===

    @Test
    fun `deleteCategory removes category from list`() = runTest {
        val category = TestData.category(
            id = "cat-1",
            name = "Food",
            type = CategoryType.EXPENSE
        )
        categoryRepository.setCategories(listOf(category))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            var state = awaitItem()
            assertIs<CategoryManagementUiState.Success>(state)
            assertEquals(1, state.defaultCategories.size + state.customCategories.size)

            viewModel.deleteCategory("cat-1")
            advanceUntilIdle()

            state = awaitItem()
            assertIs<CategoryManagementUiState.Success>(state)
            assertTrue(state.defaultCategories.isEmpty())
            assertTrue(state.customCategories.isEmpty())
        }
    }

    @Test
    fun `deleteCategory calls repository deleteCategory`() = runTest {
        val category = TestData.category(id = "cat-1", name = "Food")
        categoryRepository.setCategories(listOf(category))

        viewModel.deleteCategory("cat-1")
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<CategoryManagementUiState.Success>(state)
            assertTrue(state.defaultCategories.isEmpty())
            assertTrue(state.customCategories.isEmpty())
        }
    }

    // === Flow Updates ===

    @Test
    fun `uiState updates when repository emits new data`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            var state = awaitItem()
            assertIs<CategoryManagementUiState.Success>(state)
            assertTrue(state.defaultCategories.isEmpty())
            assertTrue(state.customCategories.isEmpty())

            // Add category via repository
            val newCategory = TestData.category(
                id = "new-1",
                name = "New Category",
                type = CategoryType.EXPENSE
            )
            categoryRepository.setCategories(listOf(newCategory))
            advanceUntilIdle()

            state = awaitItem()
            assertIs<CategoryManagementUiState.Success>(state)
            assertEquals(1, state.defaultCategories.size + state.customCategories.size)
        }
    }
}

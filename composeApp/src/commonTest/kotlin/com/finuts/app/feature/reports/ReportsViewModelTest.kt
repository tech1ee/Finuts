package com.finuts.app.feature.reports

import app.cash.turbine.test
import com.finuts.app.test.TestData
import com.finuts.app.test.fakes.FakeCategoryRepository
import com.finuts.app.test.fakes.FakeTransactionRepository
import com.finuts.domain.entity.CategoryType
import com.finuts.domain.entity.ReportPeriod
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.usecase.GetSpendingReportUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for ReportsViewModel.
 * Covers report loading, period selection, and state management.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var viewModel: ReportsViewModel

    private val now = Instant.parse("2024-01-15T12:00:00Z")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transactionRepository = FakeTransactionRepository()
        categoryRepository = FakeCategoryRepository()

        val useCase = GetSpendingReportUseCase(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            clock = { now }
        )
        viewModel = ReportsViewModel(useCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // === UI State Tests ===

    @Test
    fun `uiState starts with Loading`() = runTest {
        viewModel.uiState.test {
            assertIs<ReportsUiState.Loading>(awaitItem())
        }
    }

    @Test
    fun `uiState emits Success with empty data when no transactions`() = runTest {
        categoryRepository.setCategories(listOf(TestData.category()))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<ReportsUiState.Success>(state)
            assertFalse(state.hasData)
            assertEquals(0L, state.totalIncome)
            assertEquals(0L, state.totalExpense)
        }
    }

    @Test
    fun `uiState shows income and expense totals`() = runTest {
        val category = TestData.category(type = CategoryType.EXPENSE)
        categoryRepository.setCategories(listOf(category))

        transactionRepository.setTransactions(listOf(
            TestData.transaction(
                id = "t1",
                type = TransactionType.INCOME,
                amount = 500_000_00L,
                date = now
            ),
            TestData.transaction(
                id = "t2",
                type = TransactionType.EXPENSE,
                amount = 150_000_00L,
                categoryId = category.id,
                date = now
            )
        ))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<ReportsUiState.Success>(state)
            assertTrue(state.hasData)
            assertEquals(500_000_00L, state.totalIncome)
            assertEquals(150_000_00L, state.totalExpense)
        }
    }

    @Test
    fun `uiState includes category breakdown`() = runTest {
        val foodCategory = TestData.category(id = "food", name = "Food", type = CategoryType.EXPENSE)
        val transportCategory = TestData.category(id = "transport", name = "Transport", type = CategoryType.EXPENSE)
        categoryRepository.setCategories(listOf(foodCategory, transportCategory))

        transactionRepository.setTransactions(listOf(
            TestData.transaction(
                id = "t1",
                type = TransactionType.EXPENSE,
                amount = 100_00L,
                categoryId = "food",
                date = now
            ),
            TestData.transaction(
                id = "t2",
                type = TransactionType.EXPENSE,
                amount = 50_00L,
                categoryId = "transport",
                date = now
            )
        ))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<ReportsUiState.Success>(state)
            assertEquals(2, state.categoryBreakdown.size)
        }
    }

    @Test
    fun `uiState calculates net change correctly`() = runTest {
        val category = TestData.category(type = CategoryType.EXPENSE)
        categoryRepository.setCategories(listOf(category))

        transactionRepository.setTransactions(listOf(
            TestData.transaction(id = "t1", type = TransactionType.INCOME, amount = 100_00L, date = now),
            TestData.transaction(id = "t2", type = TransactionType.EXPENSE, amount = 40_00L, categoryId = category.id, date = now)
        ))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<ReportsUiState.Success>(state)
            assertEquals(60_00L, state.netChange)
        }
    }

    // === Period Selection Tests ===

    @Test
    fun `initial period is THIS_MONTH`() = runTest {
        categoryRepository.setCategories(listOf(TestData.category()))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<ReportsUiState.Success>(state)
            assertEquals(ReportPeriod.THIS_MONTH, state.period)
        }
    }

    @Test
    fun `onPeriodSelected updates period`() = runTest {
        categoryRepository.setCategories(listOf(TestData.category()))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()
            awaitItem() // Skip first Success

            viewModel.onPeriodSelected(ReportPeriod.THIS_WEEK)
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<ReportsUiState.Success>(state)
            assertEquals(ReportPeriod.THIS_WEEK, state.period)
        }
    }

    @Test
    fun `onPeriodSelected reloads data for new period`() = runTest {
        categoryRepository.setCategories(listOf(TestData.category()))

        viewModel.onPeriodSelected(ReportPeriod.THIS_YEAR)

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<ReportsUiState.Success>(state)
            assertEquals(ReportPeriod.THIS_YEAR, state.period)
        }
    }

    // === Refresh Tests ===

    @Test
    fun `isRefreshing starts as false`() = runTest {
        viewModel.isRefreshing.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `refresh sets isRefreshing to true then false`() = runTest {
        viewModel.isRefreshing.test {
            assertFalse(awaitItem())

            viewModel.refresh()
            assertTrue(awaitItem())

            advanceUntilIdle()
            assertFalse(awaitItem())
        }
    }

    // === Computed Properties Tests ===

    @Test
    fun `hasData is false when no transactions`() = runTest {
        categoryRepository.setCategories(listOf(TestData.category()))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<ReportsUiState.Success>(state)
            assertFalse(state.hasData)
        }
    }

    @Test
    fun `hasData is true when has income`() = runTest {
        categoryRepository.setCategories(listOf(TestData.category()))
        transactionRepository.setTransactions(listOf(
            TestData.transaction(id = "t1", type = TransactionType.INCOME, amount = 100_00L, date = now)
        ))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<ReportsUiState.Success>(state)
            assertTrue(state.hasData)
        }
    }

    @Test
    fun `hasData is true when has expense`() = runTest {
        val category = TestData.category(type = CategoryType.EXPENSE)
        categoryRepository.setCategories(listOf(category))
        transactionRepository.setTransactions(listOf(
            TestData.transaction(
                id = "t1",
                type = TransactionType.EXPENSE,
                amount = 50_00L,
                categoryId = category.id,
                date = now
            )
        ))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<ReportsUiState.Success>(state)
            assertTrue(state.hasData)
        }
    }
}

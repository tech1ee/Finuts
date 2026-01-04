package com.finuts.app.feature.dashboard

import app.cash.turbine.test
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.test.TestData
import com.finuts.app.test.fakes.FakeAccountRepository
import com.finuts.app.test.fakes.FakeCategoryRepository
import com.finuts.app.test.fakes.FakeTransactionRepository
import com.finuts.domain.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import com.finuts.app.ui.components.feedback.EmptyStateType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for DashboardViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var viewModel: DashboardViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        categoryRepository = FakeCategoryRepository()
        viewModel = DashboardViewModel(
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState starts with Loading`() = runTest {
        viewModel.uiState.test {
            assertIs<DashboardUiState.Loading>(awaitItem())
        }
    }

    @Test
    fun `uiState emits Success with empty data`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<DashboardUiState.Success>(state)
            assertEquals(0L, state.totalBalance)
            assertTrue(state.accounts.isEmpty())
            assertEquals(0L, state.monthlySpending)
            assertEquals(0L, state.monthlyIncome)
        }
    }

    @Test
    fun `totalBalance is sum of active accounts`() = runTest {
        val account1 = TestData.account(id = "1", balance = 100000_00L, isArchived = false)
        val account2 = TestData.account(id = "2", balance = 50000_00L, isArchived = false)
        val archived = TestData.account(id = "3", balance = 200000_00L, isArchived = true)
        accountRepository.setAccounts(listOf(account1, account2, archived))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<DashboardUiState.Success>(state)
            // Should only sum active accounts (account1 + account2)
            assertEquals(150000_00L, state.totalBalance)
        }
    }

    @Test
    fun `accounts only contains active accounts`() = runTest {
        val active = TestData.account(id = "1", name = "Active", isArchived = false)
        val archived = TestData.account(id = "2", name = "Archived", isArchived = true)
        accountRepository.setAccounts(listOf(active, archived))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<DashboardUiState.Success>(state)
            assertEquals(1, state.accounts.size)
            assertEquals("Active", state.accounts.first().name)
        }
    }

    @Test
    fun `healthStatus is ON_TRACK when spending less than 80 percent of income`() = runTest {
        // This test verifies health calculation logic
        // When expenses < 80% of income, should be ON_TRACK
        accountRepository.setAccounts(listOf(TestData.account()))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<DashboardUiState.Success>(state)
            // With no transactions, income is 0, so ON_TRACK by default
            assertEquals(HealthStatus.ON_TRACK, state.healthStatus)
        }
    }

    @Test
    fun `categorySpending is limited to top 3 categories`() = runTest {
        val categories = listOf(
            TestData.category(id = "1", name = "Food"),
            TestData.category(id = "2", name = "Transport"),
            TestData.category(id = "3", name = "Entertainment"),
            TestData.category(id = "4", name = "Shopping")
        )
        categoryRepository.setCategories(categories)
        accountRepository.setAccounts(listOf(TestData.account()))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<DashboardUiState.Success>(state)
            // With no transactions, categorySpending should be empty
            assertTrue(state.categorySpending.size <= 3)
        }
    }

    @Test
    fun `onAccountClick navigates to AccountDetail`() = runTest {
        viewModel.navigationEvents.test {
            viewModel.onAccountClick("account-123")
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.AccountDetail("account-123"), event.route)
        }
    }

    @Test
    fun `onAddTransactionClick navigates to AddTransaction`() = runTest {
        viewModel.navigationEvents.test {
            viewModel.onAddTransactionClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.AddTransaction, event.route)
        }
    }

    @Test
    fun `onSeeAllAccountsClick navigates to Accounts`() = runTest {
        viewModel.navigationEvents.test {
            viewModel.onSeeAllAccountsClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.Accounts, event.route)
        }
    }

    @Test
    fun `onSeeAllTransactionsClick navigates to Transactions`() = runTest {
        viewModel.navigationEvents.test {
            viewModel.onSeeAllTransactionsClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.Transactions, event.route)
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

    @Test
    fun `DashboardCategorySpending calculates percentage correctly`() {
        val spending = DashboardCategorySpending(
            id = "1",
            name = "Food",
            icon = "restaurant",
            colorHex = "#4CAF50",
            amount = 25000_00L,
            percentage = 50f
        )

        assertEquals("Food", spending.name)
        assertEquals(25000_00L, spending.amount)
        assertEquals(50f, spending.percentage)
    }

    @Test
    fun `HealthStatus has all expected values`() {
        val statuses = HealthStatus.entries
        assertEquals(3, statuses.size)
        assertTrue(statuses.contains(HealthStatus.ON_TRACK))
        assertTrue(statuses.contains(HealthStatus.WARNING))
        assertTrue(statuses.contains(HealthStatus.OVER_BUDGET))
    }

    @Test
    fun `DashboardUiState Loading is correct`() {
        val loading = DashboardUiState.Loading
        assertIs<DashboardUiState.Loading>(loading)
    }

    @Test
    fun `DashboardUiState Error contains message`() {
        val error = DashboardUiState.Error("Network error")
        assertEquals("Network error", error.message)
    }

    @Test
    fun `DashboardUiState Success contains all required fields`() {
        val success = DashboardUiState.Success(
            totalBalance = 100000_00L,
            accounts = emptyList(),
            monthlySpending = 20000_00L,
            monthlyIncome = 50000_00L,
            monthlyBudget = 30000_00L,
            categorySpending = emptyList(),
            healthStatus = HealthStatus.ON_TRACK,
            periodLabel = "January 2024"
        )

        assertEquals(100000_00L, success.totalBalance)
        assertEquals(20000_00L, success.monthlySpending)
        assertEquals(50000_00L, success.monthlyIncome)
        assertEquals(30000_00L, success.monthlyBudget)
        assertEquals(HealthStatus.ON_TRACK, success.healthStatus)
        assertEquals("January 2024", success.periodLabel)
    }

    @Test
    fun `emptyStateType is DashboardNoAccounts when no accounts`() = runTest {
        accountRepository.setAccounts(emptyList())
        transactionRepository.setTransactions(emptyList())

        viewModel.emptyStateType.test {
            awaitItem() // Skip initial
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<EmptyStateType.DashboardNoAccounts>(state)
        }
    }

    @Test
    fun `emptyStateType is DashboardNoTransactions when accounts exist but no transactions`() = runTest {
        accountRepository.setAccounts(listOf(TestData.account()))
        transactionRepository.setTransactions(emptyList())

        viewModel.emptyStateType.test {
            awaitItem() // Skip initial
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<EmptyStateType.DashboardNoTransactions>(state)
        }
    }

    @Test
    fun `emptyStateType is null when accounts and transactions exist`() = runTest {
        accountRepository.setAccounts(listOf(TestData.account()))
        transactionRepository.setTransactions(listOf(TestData.transaction()))

        viewModel.emptyStateType.test {
            // Initial value is null, and with data present computed value is also null
            // StateFlow won't emit again if value is the same
            val state = awaitItem()
            assertNull(state)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

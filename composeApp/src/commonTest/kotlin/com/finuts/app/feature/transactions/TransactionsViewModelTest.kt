package com.finuts.app.feature.transactions

import app.cash.turbine.test
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.test.TestData
import com.finuts.app.test.fakes.FakeAccountRepository
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
 * Tests for TransactionsViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var viewModel: TransactionsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        viewModel = TransactionsViewModel(transactionRepository, accountRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState starts with Loading`() = runTest {
        viewModel.uiState.test {
            assertIs<TransactionsUiState.Loading>(awaitItem())
        }
    }

    @Test
    fun `uiState emits Success with empty transactions`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<TransactionsUiState.Success>(state)
            assertTrue(state.groupedTransactions.isEmpty())
            assertEquals(0, state.totalCount)
        }
    }

    @Test
    fun `filter starts with ALL`() = runTest {
        viewModel.filter.test {
            assertEquals(TransactionFilter.ALL, awaitItem())
        }
    }

    @Test
    fun `onFilterChange updates filter to INCOME`() = runTest {
        viewModel.filter.test {
            awaitItem() // Skip initial ALL

            viewModel.onFilterChange(TransactionFilter.INCOME)

            assertEquals(TransactionFilter.INCOME, awaitItem())
        }
    }

    @Test
    fun `onFilterChange updates filter to EXPENSE`() = runTest {
        viewModel.filter.test {
            awaitItem() // Skip initial

            viewModel.onFilterChange(TransactionFilter.EXPENSE)

            assertEquals(TransactionFilter.EXPENSE, awaitItem())
        }
    }

    @Test
    fun `onFilterChange updates filter to TRANSFER`() = runTest {
        viewModel.filter.test {
            awaitItem() // Skip initial

            viewModel.onFilterChange(TransactionFilter.TRANSFER)

            assertEquals(TransactionFilter.TRANSFER, awaitItem())
        }
    }

    @Test
    fun `filter INCOME shows only income transactions`() = runTest {
        val income = TestData.transaction(id = "1", type = TransactionType.INCOME)
        val expense = TestData.transaction(id = "2", type = TransactionType.EXPENSE)
        transactionRepository.setTransactions(listOf(income, expense))

        viewModel.onFilterChange(TransactionFilter.INCOME)

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<TransactionsUiState.Success>(state)
            assertEquals(1, state.totalCount)
            assertEquals(TransactionFilter.INCOME, state.activeFilter)
        }
    }

    @Test
    fun `filter EXPENSE shows only expense transactions`() = runTest {
        val income = TestData.transaction(id = "1", type = TransactionType.INCOME)
        val expense = TestData.transaction(id = "2", type = TransactionType.EXPENSE)
        transactionRepository.setTransactions(listOf(income, expense))

        viewModel.onFilterChange(TransactionFilter.EXPENSE)

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<TransactionsUiState.Success>(state)
            assertEquals(1, state.totalCount)
        }
    }

    @Test
    fun `isEmpty returns true when no transactions`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<TransactionsUiState.Success>(state)
            assertTrue(state.isEmpty)
        }
    }

    @Test
    fun `isEmpty returns false when has transactions`() = runTest {
        transactionRepository.setTransactions(listOf(TestData.transaction()))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<TransactionsUiState.Success>(state)
            assertFalse(state.isEmpty)
        }
    }

    @Test
    fun `onTransactionClick navigates to TransactionDetail`() = runTest {
        viewModel.navigationEvents.test {
            viewModel.onTransactionClick("tx-123")
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.TransactionDetail("tx-123"), event.route)
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
    fun `onDeleteTransaction removes transaction`() = runTest {
        val transaction = TestData.transaction(id = "tx-1")
        transactionRepository.setTransactions(listOf(transaction))

        viewModel.onDeleteTransaction("tx-1")
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<TransactionsUiState.Success>(state)
            assertTrue(state.isEmpty)
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
    fun `TransactionFilter has all expected values`() {
        val filters = TransactionFilter.entries
        assertEquals(4, filters.size)
        assertTrue(filters.contains(TransactionFilter.ALL))
        assertTrue(filters.contains(TransactionFilter.INCOME))
        assertTrue(filters.contains(TransactionFilter.EXPENSE))
        assertTrue(filters.contains(TransactionFilter.TRANSFER))
    }

    @Test
    fun `TransactionGroup contains label and transactions`() {
        val transactions = listOf(TestData.transaction())
        val group = TransactionGroup("Today", transactions)

        assertEquals("Today", group.label)
        assertEquals(1, group.transactions.size)
    }

    @Test
    fun `TransactionsUiState Error contains message`() {
        val error = TransactionsUiState.Error("Failed to load")
        assertEquals("Failed to load", error.message)
    }

    @Test
    fun `hasAccounts returns true by default`() = runTest {
        viewModel.hasAccounts.test {
            // Initial value is true (optimistic)
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `hasAccounts returns false when no accounts`() = runTest {
        accountRepository.setAccounts(emptyList())

        viewModel.hasAccounts.test {
            awaitItem() // Skip initial
            advanceUntilIdle()

            assertFalse(awaitItem())
        }
    }

    @Test
    fun `hasAccounts returns true when accounts exist`() = runTest {
        accountRepository.setAccounts(listOf(TestData.account()))

        viewModel.hasAccounts.test {
            // Initial value is true (optimistic), and with accounts it's also true
            // StateFlow won't emit again if value is the same
            val value = awaitItem()
            assertTrue(value)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

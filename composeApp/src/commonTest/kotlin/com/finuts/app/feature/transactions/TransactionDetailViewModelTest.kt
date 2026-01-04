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
 * Tests for TransactionDetailViewModel.
 *
 * TDD: These tests are written BEFORE implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var accountRepository: FakeAccountRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transactionRepository = FakeTransactionRepository()
        accountRepository = FakeAccountRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(transactionId: String) = TransactionDetailViewModel(
        transactionId = transactionId,
        transactionRepository = transactionRepository,
        accountRepository = accountRepository
    )

    // === UI State Tests ===

    @Test
    fun `uiState starts with Loading`() = runTest {
        val viewModel = createViewModel("tx-1")

        viewModel.uiState.test {
            assertIs<TransactionDetailUiState.Loading>(awaitItem())
        }
    }

    @Test
    fun `uiState emits Error when transaction not found`() = runTest {
        val viewModel = createViewModel("non-existent")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<TransactionDetailUiState.Error>(state)
            assertEquals("Transaction not found", state.message)
        }
    }

    @Test
    fun `uiState emits Success when transaction exists`() = runTest {
        val account = TestData.account(id = "acc-1", name = "Kaspi")
        val transaction = TestData.transaction(
            id = "tx-1",
            accountId = "acc-1",
            amount = 50_000_00L,
            type = TransactionType.EXPENSE,
            merchant = "Magnum"
        )

        accountRepository.setAccounts(listOf(account))
        transactionRepository.setTransactions(listOf(transaction))

        val viewModel = createViewModel("tx-1")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<TransactionDetailUiState.Success>(state)
            assertEquals("tx-1", state.transaction.id)
            assertEquals("Magnum", state.transaction.merchant)
        }
    }

    @Test
    fun `uiState includes account name`() = runTest {
        val account = TestData.account(id = "acc-1", name = "My Savings")
        val transaction = TestData.transaction(id = "tx-1", accountId = "acc-1")

        accountRepository.setAccounts(listOf(account))
        transactionRepository.setTransactions(listOf(transaction))

        val viewModel = createViewModel("tx-1")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<TransactionDetailUiState.Success>(state)
            assertEquals("My Savings", state.accountName)
        }
    }

    @Test
    fun `uiState includes currency symbol from account`() = runTest {
        val account = TestData.account(id = "acc-1", currency = TestData.KZT)
        val transaction = TestData.transaction(id = "tx-1", accountId = "acc-1")

        accountRepository.setAccounts(listOf(account))
        transactionRepository.setTransactions(listOf(transaction))

        val viewModel = createViewModel("tx-1")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<TransactionDetailUiState.Success>(state)
            assertEquals("₸", state.currencySymbol)
        }
    }

    // === Navigation Tests ===

    @Test
    fun `onBackClick navigates back`() = runTest {
        val viewModel = createViewModel("tx-1")

        viewModel.navigationEvents.test {
            viewModel.onBackClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }
    }

    @Test
    fun `onEditClick navigates to EditTransaction`() = runTest {
        val viewModel = createViewModel("tx-123")

        viewModel.navigationEvents.test {
            viewModel.onEditClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.EditTransaction("tx-123"), event.route)
        }
    }

    // === Delete Dialog Tests ===

    @Test
    fun `showDeleteDialog is false initially`() = runTest {
        val viewModel = createViewModel("tx-1")

        viewModel.showDeleteDialog.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `onDeleteClick shows confirmation dialog`() = runTest {
        val viewModel = createViewModel("tx-1")

        viewModel.showDeleteDialog.test {
            assertFalse(awaitItem()) // Initially false

            viewModel.onDeleteClick()

            assertTrue(awaitItem()) // Now true
        }
    }

    @Test
    fun `onDismissDeleteDialog hides confirmation dialog`() = runTest {
        val viewModel = createViewModel("tx-1")

        viewModel.showDeleteDialog.test {
            awaitItem() // Initial false

            viewModel.onDeleteClick()
            assertTrue(awaitItem()) // Shows dialog

            viewModel.onDismissDeleteDialog()
            assertFalse(awaitItem()) // Hides dialog
        }
    }

    @Test
    fun `onConfirmDelete deletes transaction and navigates back`() = runTest {
        val transaction = TestData.transaction(id = "tx-1")
        transactionRepository.setTransactions(listOf(transaction))

        val viewModel = createViewModel("tx-1")

        viewModel.navigationEvents.test {
            viewModel.onConfirmDelete()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }

        // Verify transaction is deleted
        transactionRepository.getTransactionById("tx-1").test {
            assertEquals(null, awaitItem())
        }
    }
}

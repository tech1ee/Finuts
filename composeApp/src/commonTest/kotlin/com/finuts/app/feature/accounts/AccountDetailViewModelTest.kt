package com.finuts.app.feature.accounts

import app.cash.turbine.test
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.test.TestData
import com.finuts.app.test.fakes.FakeAccountRepository
import com.finuts.app.test.fakes.FakeTransactionRepository
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
 * Tests for AccountDetailViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(accountId: String) = AccountDetailViewModel(
        accountId = accountId,
        accountRepository = accountRepository,
        transactionRepository = transactionRepository
    )

    @Test
    fun `uiState starts with Loading`() = runTest {
        val viewModel = createViewModel("account-1")

        viewModel.uiState.test {
            assertIs<AccountDetailUiState.Loading>(awaitItem())
        }
    }

    @Test
    fun `uiState emits Error when account not found`() = runTest {
        val viewModel = createViewModel("non-existent")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountDetailUiState.Error>(state)
            assertEquals("Account not found", state.message)
        }
    }

    @Test
    fun `uiState emits Success when account exists`() = runTest {
        val account = TestData.account(id = "account-1", name = "Cash")
        accountRepository.setAccounts(listOf(account))

        val viewModel = createViewModel("account-1")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountDetailUiState.Success>(state)
            assertEquals("Cash", state.account.name)
        }
    }

    @Test
    fun `uiState includes transactions for account`() = runTest {
        val account = TestData.account(id = "account-1")
        val tx1 = TestData.transaction(id = "tx-1", accountId = "account-1")
        val tx2 = TestData.transaction(id = "tx-2", accountId = "account-1")
        val otherTx = TestData.transaction(id = "tx-3", accountId = "other-account")

        accountRepository.setAccounts(listOf(account))
        transactionRepository.setTransactions(listOf(tx1, tx2, otherTx))

        val viewModel = createViewModel("account-1")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountDetailUiState.Success>(state)
            assertEquals(2, state.transactions.size)
        }
    }

    @Test
    fun `hasTransactions returns true when transactions exist`() = runTest {
        val account = TestData.account(id = "account-1")
        val tx = TestData.transaction(accountId = "account-1")

        accountRepository.setAccounts(listOf(account))
        transactionRepository.setTransactions(listOf(tx))

        val viewModel = createViewModel("account-1")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountDetailUiState.Success>(state)
            assertTrue(state.hasTransactions)
        }
    }

    @Test
    fun `hasTransactions returns false when no transactions`() = runTest {
        val account = TestData.account(id = "account-1")
        accountRepository.setAccounts(listOf(account))

        val viewModel = createViewModel("account-1")

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountDetailUiState.Success>(state)
            assertFalse(state.hasTransactions)
        }
    }

    @Test
    fun `onEditClick navigates to EditAccount`() = runTest {
        val viewModel = createViewModel("account-123")

        viewModel.navigationEvents.test {
            viewModel.onEditClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.EditAccount("account-123"), event.route)
        }
    }

    @Test
    fun `onTransactionClick navigates to TransactionDetail`() = runTest {
        val viewModel = createViewModel("account-1")

        viewModel.navigationEvents.test {
            viewModel.onTransactionClick("tx-456")
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.TransactionDetail("tx-456"), event.route)
        }
    }

    @Test
    fun `onAddTransactionClick navigates to AddTransaction`() = runTest {
        val viewModel = createViewModel("account-1")

        viewModel.navigationEvents.test {
            viewModel.onAddTransactionClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.AddTransaction, event.route)
        }
    }

    @Test
    fun `onArchiveClick archives account and navigates back`() = runTest {
        val account = TestData.account(id = "account-1", isArchived = false)
        accountRepository.setAccounts(listOf(account))

        val viewModel = createViewModel("account-1")

        viewModel.navigationEvents.test {
            viewModel.onArchiveClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }

        // Verify account is archived
        accountRepository.getAccountById("account-1").test {
            val archived = awaitItem()
            assertTrue(archived?.isArchived == true)
        }
    }

    @Test
    fun `onDeleteClick deletes account and navigates back`() = runTest {
        val account = TestData.account(id = "account-1")
        accountRepository.setAccounts(listOf(account))

        val viewModel = createViewModel("account-1")

        viewModel.navigationEvents.test {
            viewModel.onDeleteClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }

        // Verify account is deleted
        accountRepository.getAccountById("account-1").test {
            val deleted = awaitItem()
            assertEquals(null, deleted)
        }
    }

    @Test
    fun `onBackClick navigates back`() = runTest {
        val viewModel = createViewModel("account-1")

        viewModel.navigationEvents.test {
            viewModel.onBackClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }
    }
}

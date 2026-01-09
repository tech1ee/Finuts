package com.finuts.app.feature.accounts

import app.cash.turbine.test
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.test.TestData
import com.finuts.app.test.fakes.FakeAccountRepository
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
 * Tests for AccountsViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var viewModel: AccountsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        viewModel = AccountsViewModel(accountRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState starts with Loading`() = runTest {
        viewModel.uiState.test {
            assertIs<AccountsUiState.Loading>(awaitItem())
        }
    }

    @Test
    fun `uiState emits Success with empty accounts`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertTrue(state.activeAccounts.isEmpty())
            assertTrue(state.archivedAccounts.isEmpty())
            assertEquals(0L, state.totalBalance)
        }
    }

    @Test
    fun `uiState shows active accounts`() = runTest {
        val account1 = TestData.account(id = "1", name = "Cash", balance = 10000_00L)
        val account2 = TestData.account(id = "2", name = "Card", balance = 50000_00L)
        accountRepository.setAccounts(listOf(account1, account2))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertEquals(2, state.activeAccounts.size)
            assertEquals(60000_00L, state.totalBalance)
        }
    }

    @Test
    fun `uiState separates active and archived accounts`() = runTest {
        val active = TestData.account(id = "1", name = "Active", isArchived = false)
        val archived = TestData.account(id = "2", name = "Archived", isArchived = true)
        accountRepository.setAccounts(listOf(active, archived))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertEquals(1, state.activeAccounts.size)
            assertEquals(1, state.archivedAccounts.size)
            assertEquals("Active", state.activeAccounts.first().name)
            assertEquals("Archived", state.archivedAccounts.first().name)
        }
    }

    @Test
    fun `totalBalance only counts active accounts`() = runTest {
        val active = TestData.account(id = "1", balance = 30000_00L, isArchived = false)
        val archived = TestData.account(id = "2", balance = 50000_00L, isArchived = true)
        accountRepository.setAccounts(listOf(active, archived))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertEquals(30000_00L, state.totalBalance)
        }
    }

    @Test
    fun `isEmpty returns true when no active accounts`() = runTest {
        val archived = TestData.account(id = "1", isArchived = true)
        accountRepository.setAccounts(listOf(archived))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertTrue(state.isEmpty)
        }
    }

    @Test
    fun `isEmpty returns false when has active accounts`() = runTest {
        val active = TestData.account(id = "1", isArchived = false)
        accountRepository.setAccounts(listOf(active))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertFalse(state.isEmpty)
        }
    }

    @Test
    fun `hasArchived returns true when has archived accounts`() = runTest {
        val archived = TestData.account(id = "1", isArchived = true)
        accountRepository.setAccounts(listOf(archived))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertTrue(state.hasArchived)
        }
    }

    @Test
    fun `hasArchived returns false when no archived accounts`() = runTest {
        val active = TestData.account(id = "1", isArchived = false)
        accountRepository.setAccounts(listOf(active))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertFalse(state.hasArchived)
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
    fun `onAddAccountClick navigates to AddAccount`() = runTest {
        viewModel.navigationEvents.test {
            viewModel.onAddAccountClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.AddAccount, event.route)
        }
    }

    @Test
    fun `onEditAccountClick navigates to EditAccount`() = runTest {
        viewModel.navigationEvents.test {
            viewModel.onEditAccountClick("account-456")
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.NavigateTo>(event)
            assertEquals(Route.EditAccount("account-456"), event.route)
        }
    }

    @Test
    fun `onArchiveAccount archives the account`() = runTest {
        val account = TestData.account(id = "1", isArchived = false)
        accountRepository.setAccounts(listOf(account))

        viewModel.onArchiveAccount("1")
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertTrue(state.activeAccounts.isEmpty())
            assertEquals(1, state.archivedAccounts.size)
        }
    }

    @Test
    fun `onDeleteAccount removes the account`() = runTest {
        val account = TestData.account(id = "1")
        accountRepository.setAccounts(listOf(account))

        viewModel.onDeleteAccount("1")
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertTrue(state.activeAccounts.isEmpty())
            assertTrue(state.archivedAccounts.isEmpty())
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

    // ============ Soft Archive / Undo Tests ============

    @Test
    fun `softArchiveAccount hides account from active list`() = runTest {
        val account = TestData.account(id = "1", name = "Cash", isArchived = false)
        accountRepository.setAccounts(listOf(account))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            var state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertEquals(1, state.activeAccounts.size)

            viewModel.softArchiveAccount("1")
            advanceUntilIdle()

            state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertEquals(0, state.activeAccounts.size)
            // Not yet in archived (soft archive)
            assertEquals(0, state.archivedAccounts.size)
        }
    }

    @Test
    fun `restoreAccount brings back soft-archived account`() = runTest {
        val account = TestData.account(id = "1", name = "Cash", isArchived = false)
        accountRepository.setAccounts(listOf(account))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            awaitItem() // Initial success

            viewModel.softArchiveAccount("1")
            advanceUntilIdle()

            var state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertEquals(0, state.activeAccounts.size)

            viewModel.restoreAccount("1")
            advanceUntilIdle()

            state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertEquals(1, state.activeAccounts.size)
            assertEquals("Cash", state.activeAccounts.first().name)
        }
    }

    @Test
    fun `commitArchive persists soft-archived account to database`() = runTest {
        val account = TestData.account(id = "1", name = "Cash", isArchived = false)
        accountRepository.setAccounts(listOf(account))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            awaitItem() // Initial success

            viewModel.softArchiveAccount("1")
            advanceUntilIdle()
            awaitItem() // Account hidden

            viewModel.commitArchive("1")
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountsUiState.Success>(state)
            assertEquals(0, state.activeAccounts.size)
            assertEquals(1, state.archivedAccounts.size)
            assertEquals("Cash", state.archivedAccounts.first().name)
        }
    }

    @Test
    fun `commitArchive does nothing if account not soft-archived`() = runTest {
        val account = TestData.account(id = "1", name = "Cash", isArchived = false)
        accountRepository.setAccounts(listOf(account))

        viewModel.uiState.test {
            awaitItem() // Skip Loading
            advanceUntilIdle()

            val initialState = awaitItem()
            assertIs<AccountsUiState.Success>(initialState)
            assertEquals(1, initialState.activeAccounts.size)

            // Try to commit without soft archive
            viewModel.commitArchive("1")
            advanceUntilIdle()

            // Should still be active (no emission expected since nothing changed)
            assertEquals(1, initialState.activeAccounts.size)
            assertEquals(0, initialState.archivedAccounts.size)
        }
    }
}

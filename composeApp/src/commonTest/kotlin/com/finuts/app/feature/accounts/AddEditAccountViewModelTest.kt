package com.finuts.app.feature.accounts

import app.cash.turbine.test
import com.finuts.app.presentation.base.NavigationEvent
import com.finuts.app.test.TestData
import com.finuts.app.test.fakes.FakeAccountRepository
import com.finuts.domain.entity.AccountType
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
 * Tests for AddEditAccountViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditAccountViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(accountId: String? = null) =
        AddEditAccountViewModel(accountId, accountRepository)

    @Test
    fun `isEditMode is false when accountId is null`() {
        val viewModel = createViewModel(null)
        assertFalse(viewModel.isEditMode)
    }

    @Test
    fun `isEditMode is true when accountId is provided`() {
        val viewModel = createViewModel("account-1")
        assertTrue(viewModel.isEditMode)
    }

    @Test
    fun `formState starts with default values in add mode`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            val state = awaitItem()
            assertEquals("", state.name)
            assertEquals(AccountType.BANK_ACCOUNT, state.type)
            assertEquals("KZT", state.currencyCode)
            assertEquals("0", state.balance)
            assertEquals("", state.icon)
            assertEquals("", state.color)
            assertNull(state.nameError)
            assertNull(state.balanceError)
        }
    }

    @Test
    fun `formState loads existing account in edit mode`() = runTest {
        val account = TestData.account(
            id = "account-1",
            name = "Savings",
            type = AccountType.SAVINGS,
            balance = 10000_00L
        )
        accountRepository.setAccounts(listOf(account))

        val viewModel = createViewModel("account-1")

        viewModel.formState.test {
            // First emission is default, wait for load
            val initial = awaitItem()
            if (initial.name.isEmpty()) {
                // Account loading hasn't completed yet, wait for it
                advanceUntilIdle()
                val state = awaitItem()
                assertEquals("Savings", state.name)
                assertEquals(AccountType.SAVINGS, state.type)
                assertEquals("10000.0", state.balance)
            } else {
                // Account already loaded
                assertEquals("Savings", initial.name)
                assertEquals(AccountType.SAVINGS, initial.type)
                assertEquals("10000.0", initial.balance)
            }
        }
    }

    @Test
    fun `onNameChange updates name`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onNameChange("My Cash")

            val state = awaitItem()
            assertEquals("My Cash", state.name)
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
    fun `onTypeChange updates type`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onTypeChange(AccountType.CASH)

            val state = awaitItem()
            assertEquals(AccountType.CASH, state.type)
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

    @Test
    fun `onBalanceChange updates balance`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onBalanceChange("5000.50")

            val state = awaitItem()
            assertEquals("5000.50", state.balance)
        }
    }

    @Test
    fun `onBalanceChange rejects invalid format`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            val initial = awaitItem()
            assertEquals("0", initial.balance)

            viewModel.onBalanceChange("abc") // Invalid

            // Valid input
            viewModel.onBalanceChange("123.45")
            val state = awaitItem()
            assertEquals("123.45", state.balance)
        }
    }

    @Test
    fun `onIconChange updates icon`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onIconChange("wallet")

            val state = awaitItem()
            assertEquals("wallet", state.icon)
        }
    }

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
    fun `save fails when balance is invalid`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Initial state

            viewModel.onNameChange("Test Account")
            awaitItem() // After name change

            viewModel.onBalanceChange("")
            awaitItem() // After balance change

            viewModel.save()
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Invalid balance", state.balanceError)
        }
    }

    @Test
    fun `save creates account and navigates back`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.onNameChange("New Account")
        viewModel.onBalanceChange("1000")
        viewModel.onTypeChange(AccountType.CASH)

        viewModel.navigationEvents.test {
            viewModel.save()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }

        // Verify account was created
        accountRepository.getAllAccounts().test {
            val accounts = awaitItem()
            assertEquals(1, accounts.size)
            assertEquals("New Account", accounts.first().name)
            assertEquals(AccountType.CASH, accounts.first().type)
            assertEquals(100000L, accounts.first().balance) // 1000 * 100
        }
    }

    @Test
    fun `save updates account in edit mode`() = runTest {
        val account = TestData.account(id = "account-1", name = "Old Name", balance = 5000_00L)
        accountRepository.setAccounts(listOf(account))

        val viewModel = createViewModel("account-1")
        // Wait for account to load
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
        advanceUntilIdle()

        viewModel.navigationEvents.test {
            viewModel.save()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }

        // Verify account was updated
        accountRepository.getAccountById("account-1").test {
            val updated = awaitItem()
            assertEquals("New Name", updated?.name)
        }
    }

    @Test
    fun `isSaving is false initially`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.isSaving.test {
            assertFalse(awaitItem())
        }
    }

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

    @Test
    fun `SUPPORTED_CURRENCIES has expected values`() {
        val currencies = AddEditAccountViewModel.SUPPORTED_CURRENCIES
        assertEquals(4, currencies.size)
        assertTrue(currencies.any { it.code == "KZT" })
        assertTrue(currencies.any { it.code == "USD" })
        assertTrue(currencies.any { it.code == "EUR" })
        assertTrue(currencies.any { it.code == "RUB" })
    }

    @Test
    fun `ACCOUNT_TYPES has all account types`() {
        val types = AddEditAccountViewModel.ACCOUNT_TYPES
        assertEquals(AccountType.entries.size, types.size)
    }

    @Test
    fun `AccountFormState defaults are correct`() {
        val state = AccountFormState()
        assertEquals("", state.name)
        assertEquals(AccountType.BANK_ACCOUNT, state.type)
        assertEquals("KZT", state.currencyCode)
        assertEquals("0", state.balance)
        assertEquals("", state.icon)
        assertEquals("", state.color)
        assertNull(state.nameError)
        assertNull(state.balanceError)
    }
}

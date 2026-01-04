package com.finuts.app.feature.transfers

import app.cash.turbine.test
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.AccountType
import com.finuts.domain.entity.Currency
import com.finuts.domain.entity.Transfer
import com.finuts.domain.usecase.CreateTransferUseCase
import com.finuts.app.test.fakes.FakeAccountRepository
import com.finuts.app.test.fakes.FakeTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransferViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var createTransferUseCase: CreateTransferUseCase
    private lateinit var viewModel: AddTransferViewModel

    private val now = Instant.fromEpochMilliseconds(1704067200000)
    private val kzt = Currency(code = "KZT", symbol = "₸", name = "Tenge")

    private val account1 = Account(
        id = "acc-1",
        name = "Wallet",
        type = AccountType.CASH,
        currency = kzt,
        balance = 100000L,
        color = "#4CAF50",
        icon = "wallet",
        isArchived = false,
        createdAt = now,
        updatedAt = now
    )

    private val account2 = Account(
        id = "acc-2",
        name = "Savings",
        type = AccountType.SAVINGS,
        currency = kzt,
        balance = 500000L,
        color = "#2196F3",
        icon = "savings",
        isArchived = false,
        createdAt = now,
        updatedAt = now
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        accountRepository.setAccounts(listOf(account1, account2))
        createTransferUseCase = CreateTransferUseCase(transactionRepository, clock = { now })
        viewModel = AddTransferViewModel(accountRepository, createTransferUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should load accounts`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.accounts.size)
        }
    }

    @Test
    fun `onFromAccountSelected should update state`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onFromAccountSelected(account1)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(account1, state.fromAccount)
        }
    }

    @Test
    fun `onToAccountSelected should update state`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onToAccountSelected(account2)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(account2, state.toAccount)
        }
    }

    @Test
    fun `onAmountChanged should update state`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAmountChanged("50000")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("50000", state.amount)
        }
    }

    @Test
    fun `onNoteChanged should update state`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onNoteChanged("Monthly savings")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Monthly savings", state.note)
        }
    }

    @Test
    fun `isValid should be false when from account not selected`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onToAccountSelected(account2)
        viewModel.onAmountChanged("50000")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isValid)
        }
    }

    @Test
    fun `isValid should be false when to account not selected`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onFromAccountSelected(account1)
        viewModel.onAmountChanged("50000")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isValid)
        }
    }

    @Test
    fun `isValid should be false when amount is empty`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onFromAccountSelected(account1)
        viewModel.onToAccountSelected(account2)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isValid)
        }
    }

    @Test
    fun `isValid should be false when from and to accounts are same`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onFromAccountSelected(account1)
        viewModel.onToAccountSelected(account1)
        viewModel.onAmountChanged("50000")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isValid)
        }
    }

    @Test
    fun `isValid should be true when all fields valid`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onFromAccountSelected(account1)
        viewModel.onToAccountSelected(account2)
        viewModel.onAmountChanged("50000")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isValid)
        }
    }

    @Test
    fun `submitTransfer should create transfer and emit success`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onFromAccountSelected(account1)
        viewModel.onToAccountSelected(account2)
        viewModel.onAmountChanged("50000")

        viewModel.events.test {
            viewModel.submitTransfer()
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is AddTransferEvent.Success)
        }
    }

    @Test
    fun `submitTransfer should show error when invalid`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        // Don't fill required fields

        viewModel.events.test {
            viewModel.submitTransfer()
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is AddTransferEvent.Error)
        }
    }

    @Test
    fun `submitTransfer should convert amount to cents by multiplying by 100`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        // User enters 100.50 (representing 100.50 KZT)
        viewModel.onFromAccountSelected(account1)
        viewModel.onToAccountSelected(account2)
        viewModel.onAmountChanged("100.50")

        viewModel.events.test {
            viewModel.submitTransfer()
            testDispatcher.scheduler.advanceUntilIdle()

            awaitItem() // Wait for success

            // Verify stored transactions have amount in cents (100.50 * 100 = 10050)
            val storedTransactions = transactionRepository.getStoredTransactions()
            assertEquals(2, storedTransactions.size) // outgoing + incoming
            assertTrue(storedTransactions.all { it.amount == 10050L })
        }
    }

    @Test
    fun `submitTransfer should handle whole number amount correctly`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        // User enters 500 (representing 500 KZT)
        viewModel.onFromAccountSelected(account1)
        viewModel.onToAccountSelected(account2)
        viewModel.onAmountChanged("500")

        viewModel.events.test {
            viewModel.submitTransfer()
            testDispatcher.scheduler.advanceUntilIdle()

            awaitItem() // Wait for success

            // Verify stored transactions have amount in cents (500 * 100 = 50000)
            val storedTransactions = transactionRepository.getStoredTransactions()
            assertEquals(2, storedTransactions.size)
            assertTrue(storedTransactions.all { it.amount == 50000L })
        }
    }

    @Test
    fun `isValid should accept decimal amounts`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onFromAccountSelected(account1)
        viewModel.onToAccountSelected(account2)
        viewModel.onAmountChanged("99.99")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isValid)
        }
    }
}

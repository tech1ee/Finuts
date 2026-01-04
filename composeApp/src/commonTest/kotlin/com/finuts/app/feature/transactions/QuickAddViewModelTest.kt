package com.finuts.app.feature.transactions

import app.cash.turbine.test
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for QuickAddViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuickAddViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var viewModel: QuickAddViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        viewModel = QuickAddViewModel(accountRepository, transactionRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `accounts starts with empty list`() = runTest {
        viewModel.accounts.test {
            assertEquals(emptyList(), awaitItem())
        }
    }

    @Test
    fun `accounts shows active accounts`() = runTest {
        val account = TestData.account(id = "1", name = "Cash")
        accountRepository.setAccounts(listOf(account))

        viewModel.accounts.test {
            awaitItem() // Skip initial empty
            advanceUntilIdle()

            val accounts = awaitItem()
            assertEquals(1, accounts.size)
            assertEquals("Cash", accounts.first().name)
        }
    }

    @Test
    fun `formState starts with default values`() = runTest {
        viewModel.formState.test {
            val state = awaitItem()
            assertEquals(TransactionType.EXPENSE, state.type)
            assertEquals("", state.accountId)
            assertEquals("", state.amount)
            assertEquals("", state.categoryId)
            assertEquals("", state.note)
            assertNull(state.amountError)
        }
    }

    @Test
    fun `onTypeChange updates type to INCOME`() = runTest {
        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onTypeChange(TransactionType.INCOME)

            val state = awaitItem()
            assertEquals(TransactionType.INCOME, state.type)
        }
    }

    @Test
    fun `onTypeChange updates type to TRANSFER`() = runTest {
        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onTypeChange(TransactionType.TRANSFER)

            val state = awaitItem()
            assertEquals(TransactionType.TRANSFER, state.type)
        }
    }

    @Test
    fun `onAccountChange updates accountId`() = runTest {
        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onAccountChange("account-123")

            val state = awaitItem()
            assertEquals("account-123", state.accountId)
        }
    }

    @Test
    fun `onCategoryChange updates categoryId`() = runTest {
        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onCategoryChange("food")

            val state = awaitItem()
            assertEquals("food", state.categoryId)
        }
    }

    @Test
    fun `onNoteChange updates note`() = runTest {
        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onNoteChange("Coffee with friends")

            val state = awaitItem()
            assertEquals("Coffee with friends", state.note)
        }
    }

    @Test
    fun `onAmountChange updates amount`() = runTest {
        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onAmountChange("123.45")

            val state = awaitItem()
            assertEquals("123.45", state.amount)
        }
    }

    @Test
    fun `onAmountChange clears amount error`() = runTest {
        viewModel.formState.test {
            awaitItem() // Skip initial

            // Trigger error first by saving with no data
            viewModel.save()
            advanceUntilIdle()
            awaitItem() // Error state

            // Now enter valid amount
            viewModel.onAmountChange("50")

            val state = awaitItem()
            assertNull(state.amountError)
        }
    }

    @Test
    fun `onAmountChange rejects invalid format`() = runTest {
        viewModel.formState.test {
            val initial = awaitItem()
            assertEquals("", initial.amount)

            viewModel.onAmountChange("abc")
            // No new emission since invalid input is rejected

            // Valid input should work
            viewModel.onAmountChange("123")
            val state = awaitItem()
            assertEquals("123", state.amount)
        }
    }

    @Test
    fun `onAmountChange allows decimal with two places`() = runTest {
        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onAmountChange("99.99")

            val state = awaitItem()
            assertEquals("99.99", state.amount)
        }
    }

    @Test
    fun `save fails when accountId is blank`() = runTest {
        viewModel.onAmountChange("100")

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.save()
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Select an account", state.amountError)
        }
    }

    @Test
    fun `save fails when amount is zero`() = runTest {
        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onAccountChange("account-1")
            awaitItem() // With account

            viewModel.onAmountChange("0")
            awaitItem() // With zero amount

            viewModel.save()
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Enter valid amount", state.amountError)
        }
    }

    @Test
    fun `save fails when amount is empty`() = runTest {
        viewModel.onAccountChange("account-1")

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.save()
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Enter valid amount", state.amountError)
        }
    }

    @Test
    fun `save succeeds with valid input`() = runTest {
        viewModel.onAccountChange("account-1")
        viewModel.onAmountChange("100")

        viewModel.saveSuccess.test {
            assertFalse(awaitItem()) // Initial false

            viewModel.save()
            advanceUntilIdle()

            assertTrue(awaitItem()) // Success
        }
    }

    @Test
    fun `save creates transaction in repository`() = runTest {
        viewModel.onAccountChange("account-1")
        viewModel.onAmountChange("100")
        viewModel.onCategoryChange("food")
        viewModel.onNoteChange("Lunch")
        viewModel.onTypeChange(TransactionType.EXPENSE)

        viewModel.save()
        advanceUntilIdle()

        transactionRepository.getAllTransactions().test {
            val transactions = awaitItem()
            assertEquals(1, transactions.size)
            assertEquals("account-1", transactions.first().accountId)
            assertEquals("food", transactions.first().categoryId)
            assertEquals("Lunch", transactions.first().note)
        }
    }

    @Test
    fun `isSaving is false after save completes`() = runTest {
        viewModel.onAccountChange("account-1")
        viewModel.onAmountChange("100")

        viewModel.isSaving.test {
            assertFalse(awaitItem()) // Initial false

            viewModel.save()
            advanceUntilIdle()

            // After save completes, isSaving should be false
            // The state might emit true during save and then false
            // We just verify the final state is false
            cancelAndIgnoreRemainingEvents()
        }

        // Verify save completed and isSaving is false
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun `resetForm clears all fields`() = runTest {
        viewModel.onAccountChange("account-1")
        viewModel.onAmountChange("100")
        viewModel.onCategoryChange("food")
        viewModel.onNoteChange("Test note")
        viewModel.onTypeChange(TransactionType.INCOME)

        viewModel.resetForm()

        viewModel.formState.test {
            val state = awaitItem()
            assertEquals(TransactionType.EXPENSE, state.type)
            assertEquals("", state.accountId)
            assertEquals("", state.amount)
            assertEquals("", state.categoryId)
            assertEquals("", state.note)
        }
    }

    @Test
    fun `resetForm resets saveSuccess`() = runTest {
        viewModel.onAccountChange("account-1")
        viewModel.onAmountChange("100")
        viewModel.save()
        advanceUntilIdle()

        viewModel.saveSuccess.test {
            awaitItem() // Initial true after save

            viewModel.resetForm()

            assertFalse(awaitItem())
        }
    }

    @Test
    fun `QUICK_CATEGORIES contains expected categories`() {
        val categories = QuickAddViewModel.QUICK_CATEGORIES
        assertEquals(5, categories.size)
        assertTrue(categories.any { it.first == "food" })
        assertTrue(categories.any { it.first == "transport" })
        assertTrue(categories.any { it.first == "shopping" })
        assertTrue(categories.any { it.first == "entertainment" })
        assertTrue(categories.any { it.first == "utilities" })
    }

    @Test
    fun `QuickAddFormState defaults are correct`() {
        val state = QuickAddFormState()
        assertEquals(TransactionType.EXPENSE, state.type)
        assertEquals("", state.accountId)
        assertEquals("", state.amount)
        assertEquals("", state.categoryId)
        assertEquals("", state.note)
        assertNull(state.amountError)
    }
}

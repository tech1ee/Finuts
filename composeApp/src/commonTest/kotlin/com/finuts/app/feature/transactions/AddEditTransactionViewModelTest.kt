package com.finuts.app.feature.transactions

import app.cash.turbine.test
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for AddEditTransactionViewModel.
 *
 * TDD: These tests are written BEFORE implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transactionRepository = FakeTransactionRepository()
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(transactionId: String? = null) = AddEditTransactionViewModel(
        transactionId = transactionId,
        transactionRepository = transactionRepository,
        accountRepository = accountRepository,
        categoryRepository = categoryRepository
    )

    // === Mode Detection Tests ===

    @Test
    fun `isEditMode is false when transactionId is null`() {
        val viewModel = createViewModel(null)
        assertFalse(viewModel.isEditMode)
    }

    @Test
    fun `isEditMode is true when transactionId is provided`() {
        val viewModel = createViewModel("tx-1")
        assertTrue(viewModel.isEditMode)
    }

    @Test
    fun `formState loads existing transaction in edit mode`() = runTest {
        val transaction = TestData.transaction(
            id = "tx-1",
            amount = 25_000_00L, // 2,500,000 cents = 25,000.00
            type = TransactionType.EXPENSE,
            accountId = "acc-1",
            categoryId = "cat-1",
            merchant = "Starbucks",
            note = "Coffee"
        )
        transactionRepository.setTransactions(listOf(transaction))

        val viewModel = createViewModel("tx-1")

        viewModel.formState.test {
            val initial = awaitItem()
            if (initial.merchant.isEmpty()) {
                advanceUntilIdle()
                val state = awaitItem()
                assertEquals("25000.0", state.amount)
                assertEquals(TransactionType.EXPENSE, state.type)
                assertEquals("acc-1", state.accountId)
                assertEquals("cat-1", state.categoryId)
                assertEquals("Starbucks", state.merchant)
                assertEquals("Coffee", state.note)
            } else {
                assertEquals("Starbucks", initial.merchant)
            }
        }
    }

    // === Form State Tests ===

    @Test
    fun `formState starts with default values in add mode`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            val state = awaitItem()
            assertEquals("", state.amount)
            assertEquals(TransactionType.EXPENSE, state.type)
            assertEquals("", state.accountId)
            assertNull(state.categoryId)
            assertEquals("", state.merchant)
            assertEquals("", state.description)
            assertEquals("", state.note)
            assertNull(state.amountError)
            assertNull(state.accountError)
        }
    }

    @Test
    fun `onAmountChange updates amount`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onAmountChange("150.50")

            val state = awaitItem()
            assertEquals("150.50", state.amount)
        }
    }

    @Test
    fun `onAmountChange rejects invalid format`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Initial state

            viewModel.onAmountChange("abc") // Invalid - should be rejected

            // Valid input
            viewModel.onAmountChange("123.45")
            val state = awaitItem()
            assertEquals("123.45", state.amount)
        }
    }

    @Test
    fun `onTypeChange updates type`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onTypeChange(TransactionType.INCOME)

            val state = awaitItem()
            assertEquals(TransactionType.INCOME, state.type)
        }
    }

    @Test
    fun `onAccountChange updates accountId`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onAccountChange("acc-123")

            val state = awaitItem()
            assertEquals("acc-123", state.accountId)
        }
    }

    @Test
    fun `onAccountChange clears accountError`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            // Trigger error first
            viewModel.onAmountChange("100")
            awaitItem()

            viewModel.save()
            advanceUntilIdle()
            awaitItem() // Error state

            viewModel.onAccountChange("acc-1")

            val state = awaitItem()
            assertNull(state.accountError)
        }
    }

    @Test
    fun `onCategoryChange updates categoryId`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onCategoryChange("cat-food")

            val state = awaitItem()
            assertEquals("cat-food", state.categoryId)
        }
    }

    @Test
    fun `onMerchantChange updates merchant`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onMerchantChange("Amazon")

            val state = awaitItem()
            assertEquals("Amazon", state.merchant)
        }
    }

    @Test
    fun `onNoteChange updates note`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Skip initial

            viewModel.onNoteChange("Birthday gift")

            val state = awaitItem()
            assertEquals("Birthday gift", state.note)
        }
    }

    // === Validation Tests ===

    @Test
    fun `save fails when amount is empty`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Initial state

            viewModel.onAccountChange("acc-1")
            awaitItem()

            viewModel.save()
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Enter valid amount", state.amountError)
        }
    }

    @Test
    fun `save fails when account not selected`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Initial state

            viewModel.onAmountChange("100")
            awaitItem()

            viewModel.save()
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Select account", state.accountError)
        }
    }

    @Test
    fun `onAmountChange clears amountError`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.formState.test {
            awaitItem() // Initial state

            viewModel.onAccountChange("acc-1")
            awaitItem()

            viewModel.save()
            advanceUntilIdle()
            awaitItem() // Error state with amountError

            viewModel.onAmountChange("50")

            val state = awaitItem()
            assertNull(state.amountError)
        }
    }

    // === Save Tests ===

    @Test
    fun `save creates new transaction in add mode`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.onAmountChange("1000")
        viewModel.onTypeChange(TransactionType.EXPENSE)
        viewModel.onAccountChange("acc-1")
        viewModel.onMerchantChange("Test Shop")

        viewModel.navigationEvents.test {
            viewModel.save()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }

        // Verify transaction was created
        transactionRepository.getAllTransactions().test {
            val transactions = awaitItem()
            assertEquals(1, transactions.size)
            assertEquals(-100000L, transactions.first().amount) // Expense is negative
            assertEquals("Test Shop", transactions.first().merchant)
        }
    }

    @Test
    fun `save updates existing transaction in edit mode`() = runTest {
        val transaction = TestData.transaction(
            id = "tx-1",
            merchant = "Old Merchant",
            amount = 50_000_00L
        )
        transactionRepository.setTransactions(listOf(transaction))

        val viewModel = createViewModel("tx-1")
        advanceUntilIdle()

        viewModel.formState.test {
            var state = awaitItem()
            if (state.merchant.isEmpty()) {
                advanceUntilIdle()
                state = awaitItem()
            }
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.onMerchantChange("New Merchant")
        advanceUntilIdle()

        viewModel.navigationEvents.test {
            viewModel.save()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<NavigationEvent.PopBackStack>(event)
        }

        // Verify transaction was updated
        transactionRepository.getTransactionById("tx-1").test {
            val updated = awaitItem()
            assertEquals("New Merchant", updated?.merchant)
        }
    }

    @Test
    fun `isSaving is false initially`() = runTest {
        val viewModel = createViewModel(null)

        viewModel.isSaving.test {
            assertFalse(awaitItem())
        }
    }

    // === Navigation Tests ===

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
}

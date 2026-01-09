package com.finuts.app.feature.`import`

import app.cash.turbine.test
import com.finuts.data.categorization.MerchantDatabase
import com.finuts.data.categorization.RuleBasedCategorizer
import com.finuts.data.import.FuzzyDuplicateDetector
import com.finuts.data.import.ImportFileProcessor
import com.finuts.data.import.ImportValidator
import com.finuts.data.import.parsers.CsvParser
import com.finuts.data.import.parsers.OfxParser
import com.finuts.data.import.parsers.QifParser
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.AccountType
import com.finuts.domain.entity.Currency
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.entity.import.DocumentType
import com.finuts.domain.entity.import.ImportProgress
import com.finuts.domain.entity.import.ImportResult
import com.finuts.domain.entity.import.ImportSource
import com.finuts.domain.entity.import.ImportedTransaction
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.repository.TransactionRepository
import com.finuts.domain.usecase.CategorizePendingTransactionsUseCase
import com.finuts.domain.usecase.ImportTransactionsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for ImportViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTransactionRepository: FakeTransactionRepository
    private lateinit var fakeAccountRepository: FakeAccountRepository
    private lateinit var importUseCase: ImportTransactionsUseCase
    private lateinit var fileProcessor: ImportFileProcessor
    private lateinit var viewModel: ImportViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTransactionRepository = FakeTransactionRepository()
        fakeAccountRepository = FakeAccountRepository()

        // Categorization dependencies (Tier 1 only)
        val merchantDatabase = MerchantDatabase()
        val ruleBasedCategorizer = RuleBasedCategorizer(merchantDatabase)
        val categorizationUseCase = CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = ruleBasedCategorizer,
            aiCategorizer = null
        )

        importUseCase = ImportTransactionsUseCase(
            transactionRepository = fakeTransactionRepository,
            duplicateDetector = FuzzyDuplicateDetector(),
            validator = ImportValidator(),
            categorizationUseCase = categorizationUseCase
        )
        fileProcessor = ImportFileProcessor(
            csvParser = CsvParser(),
            ofxParser = OfxParser(),
            qifParser = QifParser()
        )
        viewModel = ImportViewModel(
            importTransactionsUseCase = importUseCase,
            accountRepository = fakeAccountRepository,
            fileProcessor = fileProcessor
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // === Initial State ===

    @Test
    fun `uiState starts with ENTRY step`() = runTest {
        assertEquals(ImportStep.ENTRY, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `uiState starts with Idle progress`() = runTest {
        assertIs<ImportProgress.Idle>(viewModel.uiState.value.progress)
    }

    @Test
    fun `accounts are loaded on init`() = runTest {
        fakeAccountRepository.setAccounts(listOf(createTestAccount()))

        // Create new ViewModel to trigger init
        val newViewModel = ImportViewModel(
            importTransactionsUseCase = importUseCase,
            accountRepository = fakeAccountRepository,
            fileProcessor = fileProcessor
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, newViewModel.accounts.value.size)
    }

    // === File Selection ===

    @Test
    fun `onFileSelected updates filename in state`() = runTest {
        val parseResult = createSuccessParseResult()

        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("test.csv", viewModel.uiState.value.filename)
    }

    @Test
    fun `onFileSelected transitions to PROCESSING step`() = runTest {
        val parseResult = createSuccessParseResult()

        viewModel.onFileSelected("test.csv", parseResult)

        assertEquals(ImportStep.PROCESSING, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `successful parsing transitions to REVIEW step`() = runTest {
        val parseResult = createSuccessParseResult()

        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ImportStep.REVIEW, viewModel.uiState.value.currentStep)
    }

    // === Transaction Selection ===

    @Test
    fun `onTransactionToggle updates selectedIndices`() = runTest {
        val parseResult = createSuccessParseResult()
        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onTransactionToggle(0, true)

        assertTrue(viewModel.uiState.value.selectedIndices.contains(0))
    }

    @Test
    fun `onTransactionToggle can deselect transaction`() = runTest {
        val parseResult = createSuccessParseResult()
        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        // First select, then deselect
        viewModel.onTransactionToggle(0, true)
        viewModel.onTransactionToggle(0, false)

        assertFalse(viewModel.uiState.value.selectedIndices.contains(0))
    }

    // === Bulk Selection ===

    @Test
    fun `onSelectAll selects all transactions`() = runTest {
        val parseResult = createSuccessParseResultWithMultipleTransactions()
        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSelectAll()

        assertEquals(3, viewModel.uiState.value.selectedIndices.size)
        assertTrue(viewModel.uiState.value.selectedIndices.containsAll(setOf(0, 1, 2)))
    }

    @Test
    fun `onDeselectAll clears selection`() = runTest {
        val parseResult = createSuccessParseResultWithMultipleTransactions()
        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSelectAll()
        viewModel.onDeselectAll()

        assertTrue(viewModel.uiState.value.selectedIndices.isEmpty())
    }

    @Test
    fun `onDeselectDuplicates removes duplicate indices`() = runTest {
        val parseResult = createSuccessParseResultWithMultipleTransactions()
        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        // Select all first
        viewModel.onSelectAll()
        // Then deselect duplicates
        viewModel.onDeselectDuplicates()

        // Only unique transactions should remain selected
        val selectedIndices = viewModel.uiState.value.selectedIndices
        assertTrue(selectedIndices.isNotEmpty())
    }

    // === Step Counter Properties ===

    @Test
    fun `stepNumber returns 1 for ENTRY step`() = runTest {
        assertEquals(1, viewModel.uiState.value.stepNumber)
    }

    @Test
    fun `stepNumber returns 3 for REVIEW step`() = runTest {
        val parseResult = createSuccessParseResult()
        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.stepNumber)
    }

    @Test
    fun `stepNumber returns 4 for CONFIRM step`() = runTest {
        val parseResult = createSuccessParseResult()
        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onContinueToConfirm()

        assertEquals(4, viewModel.uiState.value.stepNumber)
    }

    @Test
    fun `progressFraction is calculated correctly`() = runTest {
        // ENTRY step = 1/5 = 0.2
        assertEquals(0.2f, viewModel.uiState.value.progressFraction)

        val parseResult = createSuccessParseResult()
        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        // REVIEW step = 3/5 = 0.6
        assertEquals(0.6f, viewModel.uiState.value.progressFraction)
    }

    @Test
    fun `stepCounterText formats correctly`() = runTest {
        assertEquals("1 из 5", viewModel.uiState.value.stepCounterText)

        val parseResult = createSuccessParseResult()
        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("3 из 5", viewModel.uiState.value.stepCounterText)
    }

    // === Account Selection ===

    @Test
    fun `onAccountSelect updates selectedAccountId`() = runTest {
        viewModel.onAccountSelect("acc-1")

        assertEquals("acc-1", viewModel.uiState.value.selectedAccountId)
    }

    // === Category Override ===

    @Test
    fun `onCategoryOverride updates categoryOverrides map`() = runTest {
        viewModel.onCategoryOverride(0, "entertainment")

        assertEquals("entertainment", viewModel.uiState.value.categoryOverrides[0])
    }

    // === Navigation ===

    @Test
    fun `onContinueToConfirm transitions to CONFIRM step`() = runTest {
        val parseResult = createSuccessParseResult()
        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onContinueToConfirm()

        assertEquals(ImportStep.CONFIRM, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `onConfirmImport transitions to RESULT step on success`() = runTest {
        val parseResult = createSuccessParseResult()
        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAccountSelect("acc-1")
        viewModel.onTransactionToggle(0, true)
        viewModel.onContinueToConfirm()
        viewModel.onConfirmImport()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ImportStep.RESULT, viewModel.uiState.value.currentStep)
    }

    // === Cancel/Reset ===

    @Test
    fun `onCancel resets to ENTRY step`() = runTest {
        val parseResult = createSuccessParseResult()
        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onCancel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ImportStep.ENTRY, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `onReset clears all state`() = runTest {
        val parseResult = createSuccessParseResult()
        viewModel.onFileSelected("test.csv", parseResult)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAccountSelect("acc-1")
        viewModel.onTransactionToggle(0, true)
        viewModel.onReset()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ImportStep.ENTRY, state.currentStep)
        assertEquals(null, state.selectedAccountId)
        assertTrue(state.selectedIndices.isEmpty())
    }

    // === Helper Functions ===

    private fun createSuccessParseResult(): ImportResult.Success {
        return ImportResult.Success(
            transactions = listOf(
                ImportedTransaction(
                    date = LocalDate(2026, 1, 8),
                    amount = -5000L,
                    description = "Test Transaction",
                    confidence = 0.9f,
                    source = ImportSource.RULE_BASED
                )
            ),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )
    }

    private fun createSuccessParseResultWithMultipleTransactions(): ImportResult.Success {
        return ImportResult.Success(
            transactions = listOf(
                ImportedTransaction(
                    date = LocalDate(2026, 1, 8),
                    amount = -5000L,
                    description = "Transaction 1",
                    confidence = 0.9f,
                    source = ImportSource.RULE_BASED
                ),
                ImportedTransaction(
                    date = LocalDate(2026, 1, 9),
                    amount = -3000L,
                    description = "Transaction 2",
                    confidence = 0.85f,
                    source = ImportSource.RULE_BASED
                ),
                ImportedTransaction(
                    date = LocalDate(2026, 1, 10),
                    amount = +10000L,
                    description = "Transaction 3",
                    confidence = 0.95f,
                    source = ImportSource.RULE_BASED
                )
            ),
            documentType = DocumentType.Csv(',', "UTF-8"),
            totalConfidence = 0.9f
        )
    }

    private fun createTestAccount(): Account {
        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        return Account(
            id = "acc-1",
            name = "Kaspi Gold",
            type = AccountType.DEBIT_CARD,
            currency = Currency(code = "KZT", symbol = "₸", name = "Kazakhstani Tenge"),
            balance = 100000L,
            color = "#10B981",
            icon = "💳",
            isArchived = false,
            createdAt = now,
            updatedAt = now
        )
    }

    // === Fake Repositories ===

    private class FakeTransactionRepository : TransactionRepository {
        val savedTransactions = mutableListOf<Transaction>()

        override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> {
            return flowOf(emptyList())
        }

        override fun getTransactionById(id: String): Flow<Transaction?> = flowOf(null)

        override suspend fun createTransaction(transaction: Transaction) {
            savedTransactions.add(transaction)
        }

        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(id: String) {}
        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByDateRange(start: Instant, end: Instant): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun insertTransfer(outgoing: Transaction, incoming: Transaction) {}
    }

    private class FakeAccountRepository : AccountRepository {
        private val accountsFlow = MutableStateFlow<List<Account>>(emptyList())

        fun setAccounts(accounts: List<Account>) {
            accountsFlow.value = accounts
        }

        override fun getAllAccounts(): Flow<List<Account>> = accountsFlow
        override fun getAccountById(id: String): Flow<Account?> = flowOf(accountsFlow.value.find { it.id == id })
        override fun getActiveAccounts(): Flow<List<Account>> = flowOf(accountsFlow.value.filter { !it.isArchived })
        override suspend fun createAccount(account: Account) {}
        override suspend fun updateAccount(account: Account) {}
        override suspend fun deleteAccount(id: String) {}
        override suspend fun archiveAccount(id: String) {}
    }
}

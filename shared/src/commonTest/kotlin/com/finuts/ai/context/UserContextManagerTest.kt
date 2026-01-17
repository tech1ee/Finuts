package com.finuts.ai.context

import com.finuts.domain.entity.TransactionType
import com.finuts.test.TestData
import com.finuts.test.fakes.FakeTransactionRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class UserContextManagerTest {

    private val repository = FakeTransactionRepository()
    private val manager = UserContextManager(repository)

    // === Short-term Memory Tests ===

    @Test
    fun `getContext returns recent transactions within 7 days`() = runTest {
        // Given: transactions at different dates
        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        val recentTx = TestData.transaction(
            id = "recent",
            date = now.minus(3.days),
            amount = -5000L
        )
        val oldTx = TestData.transaction(
            id = "old",
            date = now.minus(14.days),
            amount = -10000L
        )
        repository.setTransactions(listOf(recentTx, oldTx))

        // When
        val context = manager.getContext()

        // Then: only recent transaction included
        assertTrue(context.shortTerm.recentTransactions.any { it.id == "recent" })
    }

    @Test
    fun `getContext limits recent transactions to 20`() = runTest {
        // Given: 30 recent transactions
        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        val transactions = (1..30).map { i ->
            TestData.transaction(
                id = "tx-$i",
                date = now.minus(1.days),
                amount = -1000L * i
            )
        }
        repository.setTransactions(transactions)

        // When
        val context = manager.getContext()

        // Then: max 20 transactions
        assertTrue(context.shortTerm.recentTransactions.size <= 20)
    }

    // === Medium-term Memory Tests ===

    @Test
    fun `getContext calculates category distribution`() = runTest {
        // Given: expenses in different categories
        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        val transactions = listOf(
            TestData.transaction(
                id = "food-1",
                categoryId = "food",
                amount = -10000L,
                type = TransactionType.EXPENSE,
                date = now.minus(5.days)
            ),
            TestData.transaction(
                id = "food-2",
                categoryId = "food",
                amount = -5000L,
                type = TransactionType.EXPENSE,
                date = now.minus(3.days)
            ),
            TestData.transaction(
                id = "transport",
                categoryId = "transport",
                amount = -3000L,
                type = TransactionType.EXPENSE,
                date = now.minus(2.days)
            )
        )
        repository.setTransactions(transactions)

        // When
        val context = manager.getContext()

        // Then: category distribution calculated
        val distribution = context.mediumTerm.categoryDistribution
        assertTrue(distribution.isNotEmpty())

        // Food should have higher amount (15000 vs 3000)
        val foodShare = distribution.find { it.categoryId == "food" }
        assertNotNull(foodShare)
        assertEquals(15000L, foodShare.amount)
    }

    @Test
    fun `getContext calculates monthly spending`() = runTest {
        // Given: expenses within 30 days
        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        val transactions = listOf(
            TestData.transaction(
                id = "tx-1",
                amount = -10000L,
                type = TransactionType.EXPENSE,
                date = now.minus(5.days)
            ),
            TestData.transaction(
                id = "tx-2",
                amount = -20000L,
                type = TransactionType.EXPENSE,
                date = now.minus(10.days)
            )
        )
        repository.setTransactions(transactions)

        // When
        val context = manager.getContext()

        // Then: total spending = 30000
        assertEquals(30000L, context.mediumTerm.monthlySpending)
        assertEquals(2, context.mediumTerm.transactionCount)
    }

    @Test
    fun `getContext identifies unusual transactions`() = runTest {
        // Given: one large transaction among small ones
        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        val transactions = listOf(
            TestData.transaction(id = "small-1", amount = -1000L, date = now.minus(1.days)),
            TestData.transaction(id = "small-2", amount = -1200L, date = now.minus(2.days)),
            TestData.transaction(id = "small-3", amount = -800L, date = now.minus(3.days)),
            TestData.transaction(id = "large", amount = -50000L, date = now.minus(4.days))
        )
        repository.setTransactions(transactions)

        // When
        val context = manager.getContext()

        // Then: large transaction identified as unusual (> 3x average)
        assertTrue(context.mediumTerm.unusualTransactions.any { it.id == "large" })
    }

    // === Session Memory Tests ===

    @Test
    fun `recordAction adds action to session`() = runTest {
        // Given
        val action = UserAction(
            type = UserAction.ActionType.VIEW_TRANSACTION,
            details = mapOf("transactionId" to "tx-123")
        )

        // When
        manager.recordAction(action)
        val context = manager.getContext()

        // Then
        assertTrue(context.shortTerm.sessionActions.isNotEmpty())
        assertEquals(UserAction.ActionType.VIEW_TRANSACTION, context.shortTerm.sessionActions.first().type)
    }

    @Test
    fun `recordAction limits session actions to 50`() = runTest {
        // Given: record 60 actions
        repeat(60) { i ->
            manager.recordAction(
                UserAction(
                    type = UserAction.ActionType.VIEW_TRANSACTION,
                    details = mapOf("index" to "$i")
                )
            )
        }

        // When
        val context = manager.getContext()

        // Then: max 50 actions kept
        assertEquals(50, context.shortTerm.sessionActions.size)
        // Oldest actions removed (FIFO)
        assertTrue(context.shortTerm.sessionActions.none { it.details["index"] == "0" })
    }

    @Test
    fun `startSession clears session memory`() = runTest {
        // Given: existing session actions
        manager.recordAction(
            UserAction(type = UserAction.ActionType.CHAT_MESSAGE)
        )
        val contextBefore = manager.getContext()
        assertTrue(contextBefore.shortTerm.sessionActions.isNotEmpty())

        // When
        manager.startSession()
        val contextAfter = manager.getContext()

        // Then: session cleared
        assertTrue(contextAfter.shortTerm.sessionActions.isEmpty())
    }

    // === Long-term Memory Tests ===

    @Test
    fun `getContext returns default long-term memory`() = runTest {
        // When
        val context = manager.getContext()

        // Then: defaults returned
        assertEquals("KZT", context.longTerm.preferredCurrency)
        assertEquals("ru", context.longTerm.preferredLanguage)
    }

    // === Context Formatting Tests ===

    @Test
    fun `getContextForPrompt returns formatted string`() = runTest {
        // Given: some transactions
        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        repository.setTransactions(
            listOf(
                TestData.transaction(
                    id = "tx-1",
                    description = "Coffee Shop",
                    amount = -500L,
                    date = now.minus(1.days)
                )
            )
        )

        // When
        val prompt = manager.getContextForPrompt()

        // Then: contains expected sections
        assertTrue(prompt.contains("User Context"))
        assertTrue(prompt.contains("Recent Activity"))
        assertTrue(prompt.contains("Monthly Patterns"))
        assertTrue(prompt.contains("Preferences"))
        assertTrue(prompt.contains("KZT"))
    }

    // === Edge Cases ===

    @Test
    fun `getContext handles empty transaction list`() = runTest {
        // Given: no transactions
        repository.clear()

        // When
        val context = manager.getContext()

        // Then: returns valid context with empty data
        assertTrue(context.shortTerm.recentTransactions.isEmpty())
        assertEquals(0L, context.mediumTerm.monthlySpending)
        assertEquals(0, context.mediumTerm.transactionCount)
        assertTrue(context.mediumTerm.categoryDistribution.isEmpty())
    }

    @Test
    fun `getContext handles income transactions correctly`() = runTest {
        // Given: only income (positive amounts)
        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        repository.setTransactions(
            listOf(
                TestData.transaction(
                    id = "income",
                    amount = 100000L, // positive = income
                    type = TransactionType.INCOME,
                    date = now.minus(1.days)
                )
            )
        )

        // When
        val context = manager.getContext()

        // Then: monthly spending = 0 (income excluded)
        assertEquals(0L, context.mediumTerm.monthlySpending)
        // Category distribution empty (only expenses counted)
        assertTrue(context.mediumTerm.categoryDistribution.isEmpty())
    }

    // === Data Class Tests ===

    @Test
    fun `UserContext toPromptString includes all sections`() = runTest {
        // Given
        val context = UserContext(
            shortTerm = ShortTermMemory(
                recentTransactions = emptyList(),
                sessionActions = emptyList(),
                lastInteraction = null
            ),
            mediumTerm = MediumTermMemory(
                monthlySpending = 50000L,
                categoryDistribution = listOf(
                    CategoryShare("groceries", 30000L, 60f)
                ),
                transactionCount = 10,
                averageTransaction = 5000L,
                unusualTransactions = emptyList()
            ),
            longTerm = LongTermMemory(
                preferredCurrency = "USD",
                preferredLanguage = "en",
                goals = emptyList(),
                learnedPatterns = emptyList()
            )
        )

        // When
        val promptString = context.toPromptString()

        // Then
        assertTrue(promptString.contains("User Context"))
        assertTrue(promptString.contains("Monthly Patterns"))
        assertTrue(promptString.contains("groceries"))
        assertTrue(promptString.contains("USD"))
        assertTrue(promptString.contains("en"))
    }

    @Test
    fun `CategoryShare stores all fields`() {
        val share = CategoryShare(
            categoryId = "groceries",
            amount = 50000L,
            percentage = 45.5f
        )

        assertEquals("groceries", share.categoryId)
        assertEquals(50000L, share.amount)
        assertEquals(45.5f, share.percentage)
    }

    @Test
    fun `FinancialGoal stores all fields`() {
        val goal = FinancialGoal(
            name = "Vacation Fund",
            targetAmount = 500000L,
            currentAmount = 250000L
        )

        assertEquals("Vacation Fund", goal.name)
        assertEquals(500000L, goal.targetAmount)
        assertEquals(250000L, goal.currentAmount)
    }

    @Test
    fun `LearnedPattern stores all fields`() {
        val pattern = LearnedPattern(
            description = "MAGNUM",
            categoryId = "groceries",
            confidence = 0.95f
        )

        assertEquals("MAGNUM", pattern.description)
        assertEquals("groceries", pattern.categoryId)
        assertEquals(0.95f, pattern.confidence)
    }

    @Test
    fun `SessionMemory default values are correct`() {
        val session = SessionMemory()

        assertTrue(session.actions.isEmpty())
        assertEquals(null, session.lastInteraction)
    }

    @Test
    fun `UserAction stores all fields with defaults`() {
        val action = UserAction(
            type = UserAction.ActionType.CATEGORIZE,
            details = mapOf("categoryId" to "food")
        )

        assertEquals(UserAction.ActionType.CATEGORIZE, action.type)
        assertEquals("food", action.details["categoryId"])
        assertTrue(action.timestamp > 0)
    }

    @Test
    fun `UserAction ActionType enum has all values`() {
        val types = UserAction.ActionType.entries

        assertEquals(6, types.size)
        assertTrue(UserAction.ActionType.VIEW_TRANSACTION in types)
        assertTrue(UserAction.ActionType.CATEGORIZE in types)
        assertTrue(UserAction.ActionType.CREATE_BUDGET in types)
        assertTrue(UserAction.ActionType.VIEW_REPORT in types)
        assertTrue(UserAction.ActionType.CHAT_MESSAGE in types)
        assertTrue(UserAction.ActionType.IMPORT_FILE in types)
    }

    @Test
    fun `ShortTermMemory stores all fields`() {
        val memory = ShortTermMemory(
            recentTransactions = emptyList(),
            sessionActions = listOf(UserAction(type = UserAction.ActionType.VIEW_REPORT)),
            lastInteraction = 1234567890L
        )

        assertTrue(memory.recentTransactions.isEmpty())
        assertEquals(1, memory.sessionActions.size)
        assertEquals(1234567890L, memory.lastInteraction)
    }

    @Test
    fun `MediumTermMemory stores all fields`() {
        val memory = MediumTermMemory(
            monthlySpending = 100000L,
            categoryDistribution = emptyList(),
            transactionCount = 50,
            averageTransaction = 2000L,
            unusualTransactions = emptyList()
        )

        assertEquals(100000L, memory.monthlySpending)
        assertEquals(50, memory.transactionCount)
        assertEquals(2000L, memory.averageTransaction)
    }

    @Test
    fun `LongTermMemory stores all fields`() {
        val memory = LongTermMemory(
            preferredCurrency = "EUR",
            preferredLanguage = "de",
            goals = listOf(FinancialGoal("Save", 10000L, 5000L)),
            learnedPatterns = listOf(LearnedPattern("test", "cat", 0.9f))
        )

        assertEquals("EUR", memory.preferredCurrency)
        assertEquals("de", memory.preferredLanguage)
        assertEquals(1, memory.goals.size)
        assertEquals(1, memory.learnedPatterns.size)
    }
}

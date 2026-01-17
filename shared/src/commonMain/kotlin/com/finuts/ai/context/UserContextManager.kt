package com.finuts.ai.context

import com.finuts.domain.entity.Transaction
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

/**
 * Manages hierarchical user context for AI personalization.
 *
 * Memory tiers:
 * - Short-term: Current session, recent actions (volatile)
 * - Medium-term: Last 30 days patterns, corrections (persisted)
 * - Long-term: User profile, preferences, learned patterns (persisted)
 */
class UserContextManager(
    private val transactionRepository: TransactionRepository
) {
    // In-memory session data
    private var currentSession = SessionMemory()

    /**
     * Get full user context for LLM prompts.
     */
    suspend fun getContext(): UserContext {
        return UserContext(
            shortTerm = getShortTermMemory(),
            mediumTerm = getMediumTermMemory(),
            longTerm = getLongTermMemory()
        )
    }

    /**
     * Get context as a string for LLM prompts.
     */
    suspend fun getContextForPrompt(): String {
        val context = getContext()
        return context.toPromptString()
    }

    /**
     * Record user action in session memory.
     */
    fun recordAction(action: UserAction) {
        currentSession.actions.add(action)
        if (currentSession.actions.size > MAX_SESSION_ACTIONS) {
            currentSession.actions.removeAt(0)
        }
    }

    /**
     * Start a new session.
     */
    fun startSession() {
        currentSession = SessionMemory()
    }

    private suspend fun getShortTermMemory(): ShortTermMemory {
        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        val weekAgo = now.minus(7.days)

        val recentTransactions = transactionRepository
            .getTransactionsByDateRange(weekAgo, now)
            .first()
            .take(20)

        return ShortTermMemory(
            recentTransactions = recentTransactions,
            sessionActions = currentSession.actions.toList(),
            lastInteraction = currentSession.lastInteraction
        )
    }

    private suspend fun getMediumTermMemory(): MediumTermMemory {
        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        val monthAgo = now.minus(30.days)

        val transactions = transactionRepository
            .getTransactionsByDateRange(monthAgo, now)
            .first()

        // Calculate spending patterns
        val categoryTotals = transactions
            .filter { it.amount < 0 } // expenses only
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { -it.amount } }

        val totalSpending = categoryTotals.values.sum()

        val categoryDistribution = categoryTotals.map { (catId, amount) ->
            CategoryShare(
                categoryId = catId ?: "other",
                amount = amount,
                percentage = if (totalSpending > 0) (amount * 100f / totalSpending) else 0f
            )
        }.sortedByDescending { it.amount }

        // Detect unusual transactions
        val avgTransaction = if (transactions.isNotEmpty()) {
            transactions.map { kotlin.math.abs(it.amount) }.average()
        } else 0.0

        val unusualTransactions = transactions.filter {
            kotlin.math.abs(it.amount) > avgTransaction * 3
        }.take(5)

        return MediumTermMemory(
            monthlySpending = totalSpending,
            categoryDistribution = categoryDistribution,
            transactionCount = transactions.size,
            averageTransaction = avgTransaction.toLong(),
            unusualTransactions = unusualTransactions
        )
    }

    private suspend fun getLongTermMemory(): LongTermMemory {
        // For now, basic implementation - can be extended with persistence
        return LongTermMemory(
            preferredCurrency = "KZT",
            preferredLanguage = "ru",
            goals = emptyList(),
            learnedPatterns = emptyList()
        )
    }

    companion object {
        private const val MAX_SESSION_ACTIONS = 50
    }
}

/**
 * Full user context.
 */
data class UserContext(
    val shortTerm: ShortTermMemory,
    val mediumTerm: MediumTermMemory,
    val longTerm: LongTermMemory
) {
    fun toPromptString(): String {
        return buildString {
            appendLine("=== User Context ===")
            appendLine()
            appendLine("Recent Activity (7 days):")
            appendLine("- Transactions: ${shortTerm.recentTransactions.size}")
            shortTerm.recentTransactions.take(5).forEach { tx ->
                appendLine("  • ${tx.description}: ${tx.amount / 100.0}")
            }
            appendLine()
            appendLine("Monthly Patterns (30 days):")
            appendLine("- Total spending: ${mediumTerm.monthlySpending / 100.0}")
            appendLine("- Transactions: ${mediumTerm.transactionCount}")
            appendLine("- Top categories:")
            mediumTerm.categoryDistribution.take(5).forEach { cat ->
                appendLine("  • ${cat.categoryId}: ${cat.percentage.toInt()}%")
            }
            appendLine()
            appendLine("Preferences:")
            appendLine("- Currency: ${longTerm.preferredCurrency}")
            appendLine("- Language: ${longTerm.preferredLanguage}")
        }
    }
}

/**
 * Short-term memory (session).
 */
data class ShortTermMemory(
    val recentTransactions: List<Transaction>,
    val sessionActions: List<UserAction>,
    val lastInteraction: Long?
)

/**
 * Medium-term memory (30 days).
 */
data class MediumTermMemory(
    val monthlySpending: Long,
    val categoryDistribution: List<CategoryShare>,
    val transactionCount: Int,
    val averageTransaction: Long,
    val unusualTransactions: List<Transaction>
)

/**
 * Long-term memory (profile).
 */
data class LongTermMemory(
    val preferredCurrency: String,
    val preferredLanguage: String,
    val goals: List<FinancialGoal>,
    val learnedPatterns: List<LearnedPattern>
)

data class CategoryShare(
    val categoryId: String,
    val amount: Long,
    val percentage: Float
)

data class FinancialGoal(
    val name: String,
    val targetAmount: Long,
    val currentAmount: Long
)

data class LearnedPattern(
    val description: String,
    val categoryId: String,
    val confidence: Float
)

/**
 * Session memory (volatile).
 */
data class SessionMemory(
    val actions: MutableList<UserAction> = mutableListOf(),
    var lastInteraction: Long? = null
)

/**
 * User action for session tracking.
 */
data class UserAction(
    val type: ActionType,
    val timestamp: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    val details: Map<String, String> = emptyMap()
) {
    enum class ActionType {
        VIEW_TRANSACTION,
        CATEGORIZE,
        CREATE_BUDGET,
        VIEW_REPORT,
        CHAT_MESSAGE,
        IMPORT_FILE
    }
}

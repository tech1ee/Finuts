package com.finuts.app.feature.dashboard

import androidx.lifecycle.viewModelScope
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.repository.CategoryRepository
import com.finuts.domain.repository.TransactionRepository
import com.finuts.app.ui.components.feedback.EmptyStateType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : BaseViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val uiState: StateFlow<DashboardUiState> = combine(
        accountRepository.getActiveAccounts(),
        transactionRepository.getAllTransactions(),
        categoryRepository.getAllCategories()
    ) { accounts, transactions, categories ->
        try {
            val now = Instant.fromEpochMilliseconds(
                kotlin.time.Clock.System.now().toEpochMilliseconds()
            )
            val tz = TimeZone.currentSystemDefault()
            val localNow = now.toLocalDateTime(tz)

            // Calculate month period
            val monthStart = getMonthStart(localNow.year, localNow.monthNumber)
            val monthEnd = getMonthEnd(localNow.year, localNow.monthNumber)

            // Filter transactions for current month
            val monthTransactions = transactions.filter { tx ->
                tx.date >= monthStart && tx.date <= monthEnd
            }

            // Calculate spending and income
            val monthExpenses = monthTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { kotlin.math.abs(it.amount) }
            val monthIncome = monthTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { kotlin.math.abs(it.amount) }

            // Calculate spending by category (top 3)
            val categorySpending = calculateCategorySpending(
                transactions = monthTransactions.filter { it.type == TransactionType.EXPENSE },
                categories = categories,
                totalSpending = monthExpenses
            )

            // Calculate financial health
            val healthStatus = calculateHealthStatus(monthExpenses, monthIncome)

            DashboardUiState.Success(
                totalBalance = accounts.sumOf { it.balance },
                accounts = accounts,
                monthlySpending = monthExpenses,
                monthlyIncome = monthIncome,
                monthlyBudget = null, // TODO: Fetch from BudgetRepository when implemented
                categorySpending = categorySpending,
                healthStatus = healthStatus,
                periodLabel = formatPeriodLabel(localNow.monthNumber, localNow.year)
            )
        } catch (e: Exception) {
            DashboardUiState.Error(e.message ?: "Unknown error")
        }
    }.catch { e ->
        emit(DashboardUiState.Error(e.message ?: "Unknown error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState.Loading
    )

    val emptyStateType: StateFlow<EmptyStateType?> = accountRepository.getActiveAccounts()
        .map { accounts ->
            if (accounts.isEmpty()) EmptyStateType.DashboardNoAccounts else null
        }
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val showFirstTransactionPrompt: StateFlow<Boolean> = combine(
        accountRepository.getActiveAccounts(),
        transactionRepository.getAllTransactions()
    ) { accounts, transactions ->
        accounts.isNotEmpty() && transactions.isEmpty()
    }.catch { emit(false) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    private fun calculateCategorySpending(
        transactions: List<Transaction>,
        categories: List<Category>,
        totalSpending: Long
    ): List<DashboardCategorySpending> {
        val categoryMap = categories.associateBy { it.id }

        return transactions
            .groupBy { it.categoryId ?: "uncategorized" }
            .map { (categoryId, txs) ->
                val amount = txs.sumOf { kotlin.math.abs(it.amount) }
                val category = categoryMap[categoryId]
                DashboardCategorySpending(
                    id = categoryId,
                    name = category?.name ?: "Other",
                    icon = category?.icon ?: "📦",
                    colorHex = category?.color ?: "#9CA3AF",
                    amount = amount,
                    percentage = if (totalSpending > 0) {
                        (amount.toFloat() / totalSpending * 100)
                    } else 0f
                )
            }
            .sortedByDescending { it.amount }
            .take(TOP_CATEGORIES_LIMIT)
    }

    private fun calculateHealthStatus(expenses: Long, income: Long): HealthStatus {
        if (income == 0L) return HealthStatus.ON_TRACK
        val spendingRatio = expenses.toFloat() / income
        return when {
            spendingRatio > 1.0f -> HealthStatus.OVER_BUDGET
            spendingRatio > 0.8f -> HealthStatus.WARNING
            else -> HealthStatus.ON_TRACK
        }
    }

    private fun getMonthStart(year: Int, month: Int): Instant {
        val startStr = "$year-${month.toString().padStart(2, '0')}-01T00:00:00Z"
        return Instant.parse(startStr)
    }

    private fun getMonthEnd(year: Int, month: Int): Instant {
        val daysInMonth = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            else -> 31
        }
        val endStr = "$year-${month.toString().padStart(2, '0')}-${daysInMonth}T23:59:59Z"
        return Instant.parse(endStr)
    }

    private fun formatPeriodLabel(month: Int, year: Int): String {
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return "${monthNames[month - 1]} $year"
    }

    fun onAccountClick(accountId: String) {
        navigateTo(Route.AccountDetail(accountId))
    }

    fun onAddTransactionClick() {
        navigateTo(Route.AddTransaction)
    }

    fun onSeeAllAccountsClick() {
        navigateTo(Route.Accounts)
    }

    fun onSeeAllTransactionsClick() {
        navigateTo(Route.Transactions)
    }

    fun refresh() {
        safeScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(300)
            _isRefreshing.value = false
        }
    }

    companion object {
        private const val TOP_CATEGORIES_LIMIT = 3
    }
}

data class DashboardCategorySpending(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val amount: Long,
    val percentage: Float
)

enum class HealthStatus { ON_TRACK, WARNING, OVER_BUDGET }

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    data class Success(
        val totalBalance: Long,
        val accounts: List<Account>,
        val monthlySpending: Long,
        val monthlyIncome: Long,
        val monthlyBudget: Long?,
        val categorySpending: List<DashboardCategorySpending>,
        val healthStatus: HealthStatus,
        val periodLabel: String
    ) : DashboardUiState

    data class Error(val message: String) : DashboardUiState
}

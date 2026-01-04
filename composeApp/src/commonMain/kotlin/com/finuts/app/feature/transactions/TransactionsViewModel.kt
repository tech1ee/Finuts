package com.finuts.app.feature.transactions

import androidx.lifecycle.viewModelScope
import com.finuts.app.navigation.Route
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.filter_all
import finuts.composeapp.generated.resources.filter_expense
import finuts.composeapp.generated.resources.filter_income
import finuts.composeapp.generated.resources.filter_transfer
import org.jetbrains.compose.resources.StringResource

/**
 * ViewModel for Transactions list screen.
 * Handles filtering, date grouping, and navigation.
 */
class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : BaseViewModel() {

    private val _filter = MutableStateFlow<TransactionFilter>(TransactionFilter.ALL)
    val filter: StateFlow<TransactionFilter> = _filter

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val hasAccounts: StateFlow<Boolean> = accountRepository.getActiveAccounts()
        .map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.getAllTransactions(),
        _filter
    ) { transactions, filter ->
        val now = Instant.fromEpochMilliseconds(
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        val tz = TimeZone.currentSystemDefault()
        val localNow = now.toLocalDateTime(tz)

        // Calculate month period
        val monthStart = getMonthStart(localNow.year, localNow.monthNumber)
        val monthEnd = getMonthEnd(localNow.year, localNow.monthNumber)

        // Filter transactions for current month summary
        val monthTransactions = transactions.filter { tx ->
            tx.date >= monthStart && tx.date <= monthEnd
        }

        // Calculate summary
        val monthIncome = monthTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { kotlin.math.abs(it.amount) }
        val monthExpense = monthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { kotlin.math.abs(it.amount) }

        val periodLabel = formatPeriodLabel(localNow.monthNumber, localNow.year)

        val filtered = when (filter) {
            TransactionFilter.ALL -> transactions
            TransactionFilter.INCOME -> transactions.filter { it.type == TransactionType.INCOME }
            TransactionFilter.EXPENSE -> transactions.filter { it.type == TransactionType.EXPENSE }
            TransactionFilter.TRANSFER -> transactions.filter { it.type == TransactionType.TRANSFER }
        }

        val grouped = groupTransactionsByDate(filtered.sortedByDescending { it.date })
        TransactionsUiState.Success(
            groupedTransactions = grouped,
            activeFilter = filter,
            totalCount = filtered.size,
            monthlyIncome = monthIncome,
            monthlyExpense = monthExpense,
            periodLabel = periodLabel
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState.Loading
    )

    fun onFilterChange(newFilter: TransactionFilter) {
        _filter.value = newFilter
    }

    fun onTransactionClick(transactionId: String) {
        navigateTo(Route.TransactionDetail(transactionId))
    }

    fun onAddTransactionClick() {
        navigateTo(Route.AddTransaction)
    }

    fun onDeleteTransaction(transactionId: String) {
        launchSafe { transactionRepository.deleteTransaction(transactionId) }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(300)
            _isRefreshing.value = false
        }
    }

    private fun groupTransactionsByDate(transactions: List<Transaction>): List<TransactionGroup> {
        val tz = TimeZone.currentSystemDefault()
        val now = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
        val today = now.toLocalDateTime(tz).date
        val yesterday = now.minus(1, DateTimeUnit.DAY, tz).toLocalDateTime(tz).date
        val weekAgo = now.minus(7, DateTimeUnit.DAY, tz).toLocalDateTime(tz).date

        return transactions
            .groupBy { tx ->
                val txDate = tx.date.toLocalDateTime(tz).date
                when {
                    txDate == today -> "Today"
                    txDate == yesterday -> "Yesterday"
                    txDate > weekAgo -> "This Week"
                    else -> "${txDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${txDate.year}"
                }
            }
            .map { (label, txs) -> TransactionGroup(label, txs) }
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
}

enum class TransactionFilter(val labelRes: StringResource) {
    ALL(Res.string.filter_all),
    INCOME(Res.string.filter_income),
    EXPENSE(Res.string.filter_expense),
    TRANSFER(Res.string.filter_transfer)
}

data class TransactionGroup(val label: String, val transactions: List<Transaction>)

sealed interface TransactionsUiState {
    data object Loading : TransactionsUiState

    data class Success(
        val groupedTransactions: List<TransactionGroup>,
        val activeFilter: TransactionFilter,
        val totalCount: Int,
        val monthlyIncome: Long,
        val monthlyExpense: Long,
        val periodLabel: String
    ) : TransactionsUiState {
        val isEmpty: Boolean get() = groupedTransactions.isEmpty()
    }

    data class Error(val message: String) : TransactionsUiState
}

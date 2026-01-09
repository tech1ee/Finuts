package com.finuts.app.feature.reports

import androidx.lifecycle.viewModelScope
import com.finuts.app.presentation.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.finuts.domain.entity.CategorySpending
import com.finuts.domain.entity.ReportPeriod
import com.finuts.domain.usecase.GetSpendingReportUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Reports screen.
 * Displays spending reports with period selection and category breakdown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val getSpendingReportUseCase: GetSpendingReportUseCase
) : BaseViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)

    val uiState: StateFlow<ReportsUiState> = _selectedPeriod
        .flatMapLatest { period ->
            getSpendingReportUseCase.execute(period)
                .map<_, ReportsUiState> { report ->
                    ReportsUiState.Success(
                        period = report.period,
                        totalIncome = report.totalIncome,
                        totalExpense = report.totalExpense,
                        netChange = report.netChange,
                        categoryBreakdown = report.categoryBreakdown,
                        savingsRate = report.savingsRate
                    )
                }
        }
        .catch { e ->
            emit(ReportsUiState.Error(e.message ?: "Unknown error"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReportsUiState.Loading
        )

    fun onPeriodSelected(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun refresh() {
        safeScope.launch {
            _isRefreshing.value = true
            delay(300)
            _isRefreshing.value = false
        }
    }
}

/**
 * UI State for Reports screen.
 */
sealed interface ReportsUiState {
    data object Loading : ReportsUiState

    data class Success(
        val period: ReportPeriod,
        val totalIncome: Long,
        val totalExpense: Long,
        val netChange: Long,
        val categoryBreakdown: List<CategorySpending>,
        val savingsRate: Float
    ) : ReportsUiState {
        val hasData: Boolean get() = totalIncome > 0 || totalExpense > 0
        val topCategories: List<CategorySpending>
            get() = categoryBreakdown.sortedByDescending { it.amount }.take(5)
    }

    data class Error(val message: String) : ReportsUiState
}

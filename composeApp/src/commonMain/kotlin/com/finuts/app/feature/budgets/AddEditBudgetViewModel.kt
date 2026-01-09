package com.finuts.app.feature.budgets

import androidx.lifecycle.viewModelScope
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Budget
import com.finuts.domain.entity.BudgetPeriod
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategoryType
import com.finuts.domain.entity.Currency
import com.finuts.domain.repository.BudgetRepository
import com.finuts.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

/**
 * ViewModel for Add/Edit Budget screen.
 * Handles form state, validation, and save operations.
 */
class AddEditBudgetViewModel(
    private val budgetId: String?,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) : BaseViewModel() {

    private val _formState = MutableStateFlow(BudgetFormState())
    val formState: StateFlow<BudgetFormState> = _formState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val isEditMode: Boolean = budgetId != null

    val categories: StateFlow<List<Category>> = categoryRepository
        .getCategoriesByType(CategoryType.EXPENSE)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (budgetId != null) loadExistingBudget()
    }

    private fun loadExistingBudget() {
        safeScope.launch {
            val budget = budgetRepository.getBudgetById(budgetId!!).first()
            if (budget != null) {
                _formState.update {
                    BudgetFormState(
                        name = budget.name,
                        amount = (budget.amount / 100.0).toString(),
                        categoryId = budget.categoryId,
                        period = budget.period,
                        currencyCode = budget.currency.code
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) = _formState.update { it.copy(name = name, nameError = null) }

    fun onAmountChange(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _formState.update { it.copy(amount = amount, amountError = null) }
        }
    }

    fun onCategoryChange(categoryId: String?) = _formState.update { it.copy(categoryId = categoryId) }

    fun onPeriodChange(period: BudgetPeriod) = _formState.update { it.copy(period = period) }

    fun onCurrencyChange(code: String) = _formState.update { it.copy(currencyCode = code) }

    fun save() {
        val state = _formState.value
        var nameError: String? = null
        var amountError: String? = null

        if (state.name.isBlank()) {
            nameError = "Name is required"
        }

        val amountValue = state.amount.toDoubleOrNull()
        if (amountValue == null || state.amount.isEmpty()) {
            amountError = "Amount is required"
        } else if (amountValue <= 0) {
            amountError = "Amount must be greater than zero"
        }

        if (nameError != null || amountError != null) {
            _formState.update { it.copy(nameError = nameError, amountError = amountError) }
            return
        }

        _isSaving.value = true
        launchSafe(
            onError = { _isSaving.value = false; sendError(it.message ?: "Save failed") }
        ) {
            val now = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
            val currency = SUPPORTED_CURRENCIES.find { it.code == state.currencyCode }
                ?: SUPPORTED_CURRENCIES.first()

            val budget = Budget(
                id = budgetId ?: generateId(),
                categoryId = state.categoryId,
                name = state.name.trim(),
                amount = ((amountValue ?: 0.0) * 100).toLong(),
                currency = currency,
                period = state.period,
                startDate = now,
                endDate = null,
                isActive = true,
                createdAt = now,
                updatedAt = now
            )

            if (isEditMode) budgetRepository.updateBudget(budget)
            else budgetRepository.createBudget(budget)

            _isSaving.value = false
            navigateBack()
        }
    }

    fun onBackClick() = navigateBack()

    private fun generateId() = "budget_${kotlin.random.Random.nextLong().toString(16)}"

    companion object {
        val SUPPORTED_CURRENCIES = listOf(
            Currency("KZT", "₸", "Kazakhstani Tenge"),
            Currency("USD", "$", "US Dollar"),
            Currency("EUR", "€", "Euro"),
            Currency("RUB", "₽", "Russian Ruble")
        )

        val BUDGET_PERIODS = BudgetPeriod.entries.toList()
    }
}

data class BudgetFormState(
    val name: String = "",
    val amount: String = "",
    val categoryId: String? = null,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val currencyCode: String = "KZT",
    val nameError: String? = null,
    val amountError: String? = null
)

package com.finuts.app.feature.categories

import androidx.lifecycle.viewModelScope
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategoryType
import com.finuts.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryManagementViewModel(
    private val categoryRepository: CategoryRepository
) : BaseViewModel() {

    private val _selectedType = MutableStateFlow(CategoryType.EXPENSE)
    val selectedType: StateFlow<CategoryType> = _selectedType

    val uiState: StateFlow<CategoryManagementUiState> = combine(
        categoryRepository.getAllCategories(),
        _selectedType
    ) { categories, selectedType ->
        val filtered = categories.filter { it.type == selectedType }
        val defaultCategories = filtered.filter { it.isDefault }.sortedBy { it.sortOrder }
        val customCategories = filtered.filter { !it.isDefault }.sortedBy { it.sortOrder }

        val result: CategoryManagementUiState = CategoryManagementUiState.Success(
            selectedType = selectedType,
            defaultCategories = defaultCategories,
            customCategories = customCategories
        )
        result
    }.catch { e ->
        emit(CategoryManagementUiState.Error(e.message ?: "Unknown error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryManagementUiState.Loading
    )

    fun selectType(type: CategoryType) {
        _selectedType.value = type
    }

    fun deleteCategory(categoryId: String) {
        safeScope.launch {
            categoryRepository.deleteCategory(categoryId)
        }
    }
}

sealed interface CategoryManagementUiState {
    data object Loading : CategoryManagementUiState

    data class Success(
        val selectedType: CategoryType,
        val defaultCategories: List<Category>,
        val customCategories: List<Category>
    ) : CategoryManagementUiState

    data class Error(val message: String) : CategoryManagementUiState
}

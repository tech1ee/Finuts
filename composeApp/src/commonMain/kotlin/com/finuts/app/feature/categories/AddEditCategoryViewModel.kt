package com.finuts.app.feature.categories

import androidx.lifecycle.viewModelScope
import com.finuts.domain.registry.IconRegistry
import com.finuts.app.presentation.base.BaseViewModel
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategoryType
import com.finuts.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AddEditCategoryViewModel(
    private val categoryRepository: CategoryRepository,
    private val categoryId: String?,
    private val defaultType: CategoryType
) : BaseViewModel() {

    private val _formState = MutableStateFlow(CategoryFormState(type = defaultType))
    val formState: StateFlow<CategoryFormState> = _formState.asStateFlow()

    private val _events = MutableSharedFlow<CategoryFormEvent>()
    val events: SharedFlow<CategoryFormEvent> = _events.asSharedFlow()

    val isEditMode: Boolean = categoryId != null

    init {
        if (categoryId != null) {
            loadCategory(categoryId)
        }
    }

    private fun loadCategory(id: String) {
        safeScope.launch {
            val category = categoryRepository.getCategoryById(id).first()
            category?.let {
                _formState.value = CategoryFormState(
                    id = it.id,
                    name = it.name,
                    icon = it.icon,
                    color = it.color,
                    type = it.type,
                    isDefault = it.isDefault
                )
            }
        }
    }

    fun updateName(name: String) {
        _formState.value = _formState.value.copy(
            name = name,
            nameError = null
        )
    }

    fun updateIcon(icon: String) {
        _formState.value = _formState.value.copy(icon = icon)
    }

    fun updateColor(color: String) {
        _formState.value = _formState.value.copy(color = color)
    }

    fun updateType(type: CategoryType) {
        if (!isEditMode) {
            _formState.value = _formState.value.copy(type = type)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val state = _formState.value

        // Validation
        if (state.name.isBlank()) {
            _formState.value = state.copy(nameError = "Name is required")
            return
        }

        if (state.name.length > 50) {
            _formState.value = state.copy(nameError = "Name must be 50 characters or less")
            return
        }

        _formState.value = state.copy(isSaving = true)

        safeScope.launch {
            val category = Category(
                id = state.id ?: Uuid.random().toString(),
                name = state.name.trim(),
                icon = state.icon,
                color = state.color,
                type = state.type,
                isDefault = state.isDefault,
                sortOrder = 0
            )

            if (isEditMode) {
                categoryRepository.updateCategory(category)
            } else {
                categoryRepository.createCategory(category)
            }

            _formState.value = _formState.value.copy(isSaving = false)
            _events.emit(CategoryFormEvent.SaveSuccess)
        }
    }

    fun delete() {
        val state = _formState.value

        // Cannot delete default categories
        if (state.isDefault) {
            safeScope.launch {
                _events.emit(CategoryFormEvent.CannotDeleteDefault)
            }
            return
        }

        val id = state.id ?: return

        safeScope.launch {
            categoryRepository.deleteCategory(id)
            _events.emit(CategoryFormEvent.DeleteSuccess)
        }
    }
}

data class CategoryFormState(
    val id: String? = null,
    val name: String = "",
    val icon: String = "package",
    val color: String = IconRegistry().colorPalette.first(),
    val type: CategoryType = CategoryType.EXPENSE,
    val isDefault: Boolean = false,
    val nameError: String? = null,
    val isSaving: Boolean = false
)

sealed interface CategoryFormEvent {
    data object SaveSuccess : CategoryFormEvent
    data object DeleteSuccess : CategoryFormEvent
    data object CannotDeleteDefault : CategoryFormEvent
}

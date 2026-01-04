package com.finuts.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val type: CategoryType,
    val parentId: String? = null,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

@Serializable
enum class CategoryType {
    INCOME,
    EXPENSE
}

package com.finuts.data.local.mapper

import com.finuts.data.local.entity.CategoryEntity
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategoryType

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = CategoryType.valueOf(type),
    parentId = parentId,
    isDefault = isDefault,
    sortOrder = sortOrder
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = type.name,
    parentId = parentId,
    isDefault = isDefault,
    sortOrder = sortOrder
)

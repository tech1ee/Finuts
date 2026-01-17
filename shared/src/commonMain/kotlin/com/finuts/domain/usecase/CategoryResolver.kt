package com.finuts.domain.usecase

import co.touchlab.kermit.Logger
import com.finuts.domain.entity.Category
import com.finuts.domain.registry.CategoryMetadata
import com.finuts.domain.registry.IconRegistryProvider
import com.finuts.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first

/**
 * Ensures a category exists in the database before use.
 *
 * Used during transaction import and categorization to prevent
 * FOREIGN KEY constraint violations. If a category doesn't exist,
 * it creates it from IconRegistry metadata or falls back to "other".
 */
class CategoryResolver(
    private val categoryRepository: CategoryRepository,
    private val iconRegistry: IconRegistryProvider
) {
    private val log = Logger.withTag("CategoryResolver")

    /**
     * Ensures the specified category exists in the database.
     *
     * @param categoryId The category ID to check/create (e.g., "groceries")
     * @return The valid category ID (same as input if known, or "other" as fallback)
     */
    suspend fun ensureExists(categoryId: String): String {
        log.d { "ensureExists: checking categoryId='$categoryId'" }

        // Check if category already exists
        val existing = categoryRepository.getCategoryById(categoryId).first()
        if (existing != null) {
            log.d { "ensureExists: FOUND in DB categoryId='$categoryId'" }
            return categoryId
        }
        log.d { "ensureExists: NOT_FOUND in DB categoryId='$categoryId'" }

        // Try to create from IconRegistry
        val metadata = iconRegistry.getCategoryMetadata(categoryId)
        if (metadata != null) {
            log.i { "ensureExists: CREATING from IconRegistry categoryId='$categoryId', icon=${metadata.defaultIcon}" }
            createCategoryFromMetadata(metadata)
            return categoryId
        }
        log.w { "ensureExists: NOT_IN_REGISTRY categoryId='$categoryId' → fallback to 'other'" }

        // Unknown category - fallback to "other"
        return ensureOtherExists()
    }

    /**
     * Creates a category from IconRegistry metadata.
     */
    private suspend fun createCategoryFromMetadata(metadata: CategoryMetadata) {
        log.d { "createCategoryFromMetadata: id=${metadata.id}, name=${metadata.displayName}" }
        try {
            val category = Category(
                id = metadata.id,
                name = metadata.displayName,
                icon = metadata.defaultIcon,
                color = metadata.defaultColor,
                type = metadata.type,
                parentId = null,
                isDefault = true,
                sortOrder = 0
            )
            categoryRepository.createCategory(category)
            log.i { "createCategoryFromMetadata: SUCCESS created category id=${metadata.id}" }
        } catch (e: Exception) {
            log.e(e) { "createCategoryFromMetadata: FAILED to create category id=${metadata.id} - ${e.message}" }
            throw e
        }
    }

    /**
     * Ensures the "other" category exists.
     * @return "other" category ID
     */
    private suspend fun ensureOtherExists(): String {
        val otherId = "other"
        log.d { "ensureOtherExists: checking '$otherId'" }
        val existing = categoryRepository.getCategoryById(otherId).first()

        if (existing == null) {
            log.d { "ensureOtherExists: 'other' not found, creating..." }
            val metadata = iconRegistry.getCategoryMetadata(otherId)
            if (metadata != null) {
                createCategoryFromMetadata(metadata)
            } else {
                log.w { "ensureOtherExists: 'other' not in registry, using hardcoded fallback" }
                // Fallback if even "other" is not in registry (shouldn't happen)
                val fallbackCategory = Category(
                    id = otherId,
                    name = "Other",
                    icon = "package",
                    color = "#9E9E9E",
                    type = com.finuts.domain.entity.CategoryType.EXPENSE,
                    parentId = null,
                    isDefault = true,
                    sortOrder = 999
                )
                categoryRepository.createCategory(fallbackCategory)
                log.i { "ensureOtherExists: created hardcoded 'other' category" }
            }
        } else {
            log.d { "ensureOtherExists: 'other' already exists" }
        }

        return otherId
    }
}

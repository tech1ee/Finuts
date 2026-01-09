package com.finuts.domain.repository

import com.finuts.domain.entity.CategoryCorrection
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for category corrections.
 */
interface CategoryCorrectionRepository {

    /**
     * Save a category correction.
     */
    suspend fun save(correction: CategoryCorrection)

    /**
     * Get correction by transaction ID.
     */
    suspend fun getByTransactionId(transactionId: String): CategoryCorrection?

    /**
     * Get all corrections for a normalized merchant.
     */
    suspend fun getByNormalizedMerchant(merchantNormalized: String): List<CategoryCorrection>

    /**
     * Get corrections for a merchant that were corrected to a specific category.
     */
    suspend fun getByMerchantAndCategory(
        merchantNormalized: String,
        categoryId: String
    ): List<CategoryCorrection>

    /**
     * Get all corrections as a Flow.
     */
    fun getAllCorrections(): Flow<List<CategoryCorrection>>

    /**
     * Count corrections for a specific merchant.
     */
    suspend fun countByMerchant(merchantNormalized: String): Int

    /**
     * Delete a correction by ID.
     */
    suspend fun deleteById(id: String)
}

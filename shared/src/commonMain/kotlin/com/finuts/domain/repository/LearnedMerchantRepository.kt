package com.finuts.domain.repository

import com.finuts.domain.entity.LearnedMerchant
import com.finuts.domain.entity.LearnedMerchantSource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for learned merchant mappings.
 */
interface LearnedMerchantRepository {

    /**
     * Save a learned merchant mapping.
     */
    suspend fun save(merchant: LearnedMerchant)

    /**
     * Update an existing learned merchant mapping.
     */
    suspend fun update(merchant: LearnedMerchant)

    /**
     * Get a learned merchant by exact pattern match.
     */
    suspend fun getByPattern(pattern: String): LearnedMerchant?

    /**
     * Find a matching merchant for a transaction description.
     * Returns the best match based on confidence and sample count.
     */
    suspend fun findMatch(description: String): LearnedMerchant?

    /**
     * Get all learned merchants as a Flow.
     */
    fun getAllMerchants(): Flow<List<LearnedMerchant>>

    /**
     * Get high confidence merchants (confidence >= threshold).
     */
    suspend fun getHighConfidenceMerchants(minConfidence: Float = 0.85f): List<LearnedMerchant>

    /**
     * Get merchants by source.
     */
    suspend fun getBySource(source: LearnedMerchantSource): List<LearnedMerchant>

    /**
     * Delete a learned merchant by ID.
     */
    suspend fun deleteById(id: String)
}

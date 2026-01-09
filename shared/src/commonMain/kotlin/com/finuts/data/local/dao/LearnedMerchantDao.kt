package com.finuts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finuts.data.local.entity.LearnedMerchantEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for learned merchant mappings.
 */
@Dao
interface LearnedMerchantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(merchant: LearnedMerchantEntity)

    @Update
    suspend fun update(merchant: LearnedMerchantEntity)

    @Query("SELECT * FROM learned_merchants WHERE merchantPattern = :pattern")
    suspend fun getByPattern(pattern: String): LearnedMerchantEntity?

    /**
     * Find a learned merchant that matches the given normalized description.
     * The description should be pre-normalized using MerchantNormalizer.normalize().
     *
     * Matching logic: Check if merchantPattern is contained within the description.
     * Uses UPPER() for case-insensitive matching as a safety measure.
     *
     * @param normalizedDescription Pre-normalized transaction description
     * @return Best matching merchant (highest confidence), or null
     */
    @Query("""
        SELECT * FROM learned_merchants
        WHERE UPPER(:normalizedDescription) LIKE '%' || UPPER(merchantPattern) || '%'
        ORDER BY confidence DESC, sampleCount DESC
        LIMIT 1
    """)
    suspend fun findMatchingMerchant(normalizedDescription: String): LearnedMerchantEntity?

    @Query("SELECT * FROM learned_merchants ORDER BY lastUsedAt DESC")
    fun getAllMerchants(): Flow<List<LearnedMerchantEntity>>

    @Query("SELECT * FROM learned_merchants WHERE confidence >= :minConfidence ORDER BY confidence DESC")
    suspend fun getHighConfidenceMerchants(minConfidence: Float = 0.85f): List<LearnedMerchantEntity>

    @Query("SELECT * FROM learned_merchants WHERE source = :source ORDER BY lastUsedAt DESC")
    suspend fun getBySource(source: String): List<LearnedMerchantEntity>

    @Query("DELETE FROM learned_merchants WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM learned_merchants")
    suspend fun deleteAll()
}

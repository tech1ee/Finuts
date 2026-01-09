package com.finuts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.finuts.data.local.entity.CategoryCorrectionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for category corrections.
 */
@Dao
interface CategoryCorrectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(correction: CategoryCorrectionEntity)

    @Query("SELECT * FROM category_corrections WHERE transactionId = :transactionId")
    suspend fun getByTransactionId(transactionId: String): CategoryCorrectionEntity?

    @Query("""
        SELECT * FROM category_corrections
        WHERE merchantNormalized = :merchantNormalized
        ORDER BY createdAt DESC
    """)
    suspend fun getByNormalizedMerchant(merchantNormalized: String): List<CategoryCorrectionEntity>

    @Query("""
        SELECT * FROM category_corrections
        WHERE merchantNormalized = :merchantNormalized
        AND correctedCategoryId = :categoryId
        ORDER BY createdAt DESC
    """)
    suspend fun getByMerchantAndCategory(
        merchantNormalized: String,
        categoryId: String
    ): List<CategoryCorrectionEntity>

    @Query("SELECT * FROM category_corrections ORDER BY createdAt DESC")
    fun getAllCorrections(): Flow<List<CategoryCorrectionEntity>>

    @Query("SELECT COUNT(*) FROM category_corrections WHERE merchantNormalized = :merchantNormalized")
    suspend fun countByMerchant(merchantNormalized: String): Int

    @Query("DELETE FROM category_corrections WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM category_corrections")
    suspend fun deleteAll()
}

package com.finuts.test.fakes.dao

import com.finuts.data.local.dao.CategoryCorrectionDao
import com.finuts.data.local.entity.CategoryCorrectionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of CategoryCorrectionDao for unit testing.
 */
class FakeCategoryCorrectionDao : CategoryCorrectionDao {

    private val corrections = MutableStateFlow<List<CategoryCorrectionEntity>>(emptyList())

    override suspend fun insert(correction: CategoryCorrectionEntity) {
        corrections.update { list ->
            list.filterNot { it.id == correction.id } + correction
        }
    }

    override suspend fun getByTransactionId(
        transactionId: String
    ): CategoryCorrectionEntity? =
        corrections.value.find { it.transactionId == transactionId }

    override suspend fun getByNormalizedMerchant(
        merchantNormalized: String
    ): List<CategoryCorrectionEntity> =
        corrections.value
            .filter { it.merchantNormalized == merchantNormalized }
            .sortedByDescending { it.createdAt }

    override suspend fun getByMerchantAndCategory(
        merchantNormalized: String,
        categoryId: String
    ): List<CategoryCorrectionEntity> =
        corrections.value
            .filter {
                it.merchantNormalized == merchantNormalized &&
                    it.correctedCategoryId == categoryId
            }
            .sortedByDescending { it.createdAt }

    override fun getAllCorrections(): Flow<List<CategoryCorrectionEntity>> =
        corrections.map { it.sortedByDescending { c -> c.createdAt } }

    override suspend fun countByMerchant(merchantNormalized: String): Int =
        corrections.value.count { it.merchantNormalized == merchantNormalized }

    override suspend fun deleteById(id: String) {
        corrections.update { list -> list.filter { it.id != id } }
    }

    override suspend fun deleteAll() {
        corrections.value = emptyList()
    }

    // Test helpers
    fun setCorrections(newCorrections: List<CategoryCorrectionEntity>) {
        corrections.value = newCorrections
    }

    fun clear() {
        corrections.value = emptyList()
    }

    fun getAll(): List<CategoryCorrectionEntity> = corrections.value
}

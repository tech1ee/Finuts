package com.finuts.data.repository

import com.finuts.data.local.dao.CategoryCorrectionDao
import com.finuts.data.local.mapper.toDomain
import com.finuts.data.local.mapper.toEntity
import com.finuts.domain.entity.CategoryCorrection
import com.finuts.domain.repository.CategoryCorrectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryCorrectionRepositoryImpl(
    private val dao: CategoryCorrectionDao
) : CategoryCorrectionRepository {

    override suspend fun save(correction: CategoryCorrection) {
        dao.insert(correction.toEntity())
    }

    override suspend fun getByTransactionId(transactionId: String): CategoryCorrection? {
        return dao.getByTransactionId(transactionId)?.toDomain()
    }

    override suspend fun getByNormalizedMerchant(
        merchantNormalized: String
    ): List<CategoryCorrection> {
        return dao.getByNormalizedMerchant(merchantNormalized).map { it.toDomain() }
    }

    override suspend fun getByMerchantAndCategory(
        merchantNormalized: String,
        categoryId: String
    ): List<CategoryCorrection> {
        return dao.getByMerchantAndCategory(merchantNormalized, categoryId)
            .map { it.toDomain() }
    }

    override fun getAllCorrections(): Flow<List<CategoryCorrection>> {
        return dao.getAllCorrections().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun countByMerchant(merchantNormalized: String): Int {
        return dao.countByMerchant(merchantNormalized)
    }

    override suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }
}

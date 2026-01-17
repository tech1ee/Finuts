package com.finuts.data.repository

import com.finuts.data.categorization.MerchantNormalizerInterface
import com.finuts.data.local.dao.LearnedMerchantDao
import com.finuts.data.local.mapper.toDomain
import com.finuts.data.local.mapper.toEntity
import com.finuts.domain.entity.LearnedMerchant
import com.finuts.domain.entity.LearnedMerchantSource
import com.finuts.domain.repository.LearnedMerchantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LearnedMerchantRepositoryImpl(
    private val dao: LearnedMerchantDao,
    private val merchantNormalizer: MerchantNormalizerInterface
) : LearnedMerchantRepository {

    override suspend fun save(merchant: LearnedMerchant) {
        dao.insert(merchant.toEntity())
    }

    override suspend fun update(merchant: LearnedMerchant) {
        dao.update(merchant.toEntity())
    }

    override suspend fun getByPattern(pattern: String): LearnedMerchant? {
        return dao.getByPattern(pattern)?.toDomain()
    }

    override suspend fun findMatch(description: String): LearnedMerchant? {
        // Normalize description before SQL query for consistent matching
        val normalizedDescription = merchantNormalizer.normalize(description)
        if (normalizedDescription.isBlank()) return null

        return dao.findMatchingMerchant(normalizedDescription)?.toDomain()
    }

    override fun getAllMerchants(): Flow<List<LearnedMerchant>> {
        return dao.getAllMerchants().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getHighConfidenceMerchants(
        minConfidence: Float
    ): List<LearnedMerchant> {
        return dao.getHighConfidenceMerchants(minConfidence).map { it.toDomain() }
    }

    override suspend fun getBySource(source: LearnedMerchantSource): List<LearnedMerchant> {
        return dao.getBySource(source.name).map { it.toDomain() }
    }

    override suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }
}

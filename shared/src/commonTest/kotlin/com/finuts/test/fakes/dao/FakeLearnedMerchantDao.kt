package com.finuts.test.fakes.dao

import com.finuts.data.local.dao.LearnedMerchantDao
import com.finuts.data.local.entity.LearnedMerchantEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of LearnedMerchantDao for unit testing.
 */
class FakeLearnedMerchantDao : LearnedMerchantDao {

    private val merchants = MutableStateFlow<List<LearnedMerchantEntity>>(emptyList())

    override suspend fun insert(merchant: LearnedMerchantEntity) {
        merchants.update { list ->
            list.filterNot { it.id == merchant.id } + merchant
        }
    }

    override suspend fun update(merchant: LearnedMerchantEntity) {
        merchants.update { list ->
            list.map { if (it.id == merchant.id) merchant else it }
        }
    }

    override suspend fun getByPattern(pattern: String): LearnedMerchantEntity? =
        merchants.value.find { it.merchantPattern == pattern }

    override suspend fun findMatchingMerchant(
        normalizedDescription: String
    ): LearnedMerchantEntity? {
        val upper = normalizedDescription.uppercase()
        return merchants.value
            .filter { upper.contains(it.merchantPattern.uppercase()) }
            .maxByOrNull { it.confidence }
    }

    override fun getAllMerchants(): Flow<List<LearnedMerchantEntity>> =
        merchants.map { it.sortedByDescending { m -> m.lastUsedAt } }

    override suspend fun getHighConfidenceMerchants(
        minConfidence: Float
    ): List<LearnedMerchantEntity> =
        merchants.value
            .filter { it.confidence >= minConfidence }
            .sortedByDescending { it.confidence }

    override suspend fun getBySource(source: String): List<LearnedMerchantEntity> =
        merchants.value
            .filter { it.source == source }
            .sortedByDescending { it.lastUsedAt }

    override suspend fun deleteById(id: String) {
        merchants.update { list -> list.filter { it.id != id } }
    }

    override suspend fun deleteAll() {
        merchants.value = emptyList()
    }

    // Test helpers
    fun setMerchants(newMerchants: List<LearnedMerchantEntity>) {
        merchants.value = newMerchants
    }

    fun clear() {
        merchants.value = emptyList()
    }

    fun getAll(): List<LearnedMerchantEntity> = merchants.value
}

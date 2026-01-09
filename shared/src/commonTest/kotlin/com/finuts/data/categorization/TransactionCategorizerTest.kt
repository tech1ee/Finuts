package com.finuts.data.categorization

import com.finuts.domain.entity.CategorizationSource
import com.finuts.domain.entity.LearnedMerchant
import com.finuts.domain.entity.LearnedMerchantSource
import com.finuts.domain.repository.LearnedMerchantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for TransactionCategorizer.
 * Verifies Tier 0 + Tier 1 cascade behavior.
 */
class TransactionCategorizerTest {

    private val fixedTime = Instant.fromEpochMilliseconds(1704067200000)

    // --- Test Doubles ---

    private class FakeLearnedMerchantRepository : LearnedMerchantRepository {
        var merchants = mutableMapOf<String, LearnedMerchant>()
        var updatedMerchants = mutableListOf<LearnedMerchant>()

        override suspend fun save(merchant: LearnedMerchant) {
            merchants[merchant.merchantPattern] = merchant
        }

        override suspend fun update(merchant: LearnedMerchant) {
            merchants[merchant.merchantPattern] = merchant
            updatedMerchants.add(merchant)
        }

        override suspend fun getByPattern(pattern: String): LearnedMerchant? {
            return merchants[pattern]
        }

        override suspend fun findMatch(description: String): LearnedMerchant? {
            val upperDesc = description.uppercase()
            return merchants.values.find { merchant ->
                upperDesc.contains(merchant.merchantPattern.uppercase())
            }
        }

        override fun getAllMerchants(): Flow<List<LearnedMerchant>> =
            flowOf(merchants.values.toList())

        override suspend fun getHighConfidenceMerchants(
            minConfidence: Float
        ): List<LearnedMerchant> {
            return merchants.values.filter { it.confidence >= minConfidence }
        }

        override suspend fun getBySource(source: LearnedMerchantSource): List<LearnedMerchant> {
            return merchants.values.filter { it.source == source }
        }

        override suspend fun deleteById(id: String) {
            merchants.values.removeAll { it.id == id }
        }
    }

    // --- Tier 0 Priority Tests ---

    @Test
    fun `uses learned mapping when available (Tier 0)`() = runTest {
        val merchantRepo = FakeLearnedMerchantRepository()
        merchantRepo.merchants["MY COFFEE"] = LearnedMerchant(
            id = "lm-1",
            merchantPattern = "MY COFFEE",
            categoryId = "food",
            confidence = 0.95f,
            source = LearnedMerchantSource.USER,
            sampleCount = 5,
            lastUsedAt = fixedTime,
            createdAt = fixedTime
        )

        val ruleBasedCategorizer = RuleBasedCategorizer(MerchantDatabase())
        val categorizer = TransactionCategorizer(merchantRepo, ruleBasedCategorizer)

        val result = categorizer.categorize("tx-1", "MY COFFEE SHOP ORDER")

        assertNotNull(result)
        assertEquals("food", result.categoryId)
        assertEquals(CategorizationSource.USER_LEARNED, result.source)
    }

    @Test
    fun `tier 0 takes priority over tier 1`() = runTest {
        val merchantRepo = FakeLearnedMerchantRepository()
        // User learned that MAGNUM is "other" (overriding merchant database)
        merchantRepo.merchants["MAGNUM"] = LearnedMerchant(
            id = "lm-1",
            merchantPattern = "MAGNUM",
            categoryId = "other",
            confidence = 0.95f,
            source = LearnedMerchantSource.USER,
            sampleCount = 3,
            lastUsedAt = fixedTime,
            createdAt = fixedTime
        )

        val ruleBasedCategorizer = RuleBasedCategorizer(MerchantDatabase())
        val categorizer = TransactionCategorizer(merchantRepo, ruleBasedCategorizer)

        val result = categorizer.categorize("tx-1", "MAGNUM SUPER ALMATY")

        assertNotNull(result)
        // Should use user learned mapping, not merchant database
        assertEquals("other", result.categoryId)
        assertEquals(CategorizationSource.USER_LEARNED, result.source)
    }

    // --- Fallback to Tier 1 Tests ---

    @Test
    fun `falls back to tier 1 when no learned mapping`() = runTest {
        val merchantRepo = FakeLearnedMerchantRepository() // Empty
        val ruleBasedCategorizer = RuleBasedCategorizer(MerchantDatabase())
        val categorizer = TransactionCategorizer(merchantRepo, ruleBasedCategorizer)

        val result = categorizer.categorize("tx-1", "MAGNUM SUPER ALMATY")

        assertNotNull(result)
        assertEquals("groceries", result.categoryId)
        assertEquals(CategorizationSource.MERCHANT_DATABASE, result.source)
    }

    @Test
    fun `uses regex rules from tier 1`() = runTest {
        val merchantRepo = FakeLearnedMerchantRepository() // Empty
        val ruleBasedCategorizer = RuleBasedCategorizer(MerchantDatabase())
        val categorizer = TransactionCategorizer(merchantRepo, ruleBasedCategorizer)

        val result = categorizer.categorize("tx-1", "ЗАРПЛАТА ЗА ЯНВАРЬ")

        assertNotNull(result)
        assertEquals("salary", result.categoryId)
        assertEquals(CategorizationSource.RULE_BASED, result.source)
    }

    // --- Edge Cases ---

    @Test
    fun `returns null for unknown transactions`() = runTest {
        val merchantRepo = FakeLearnedMerchantRepository()
        val ruleBasedCategorizer = RuleBasedCategorizer(MerchantDatabase())
        val categorizer = TransactionCategorizer(merchantRepo, ruleBasedCategorizer)

        val result = categorizer.categorize("tx-1", "RANDOM UNKNOWN MERCHANT XYZ")

        assertNull(result)
    }

    @Test
    fun `handles empty description`() = runTest {
        val merchantRepo = FakeLearnedMerchantRepository()
        val ruleBasedCategorizer = RuleBasedCategorizer(MerchantDatabase())
        val categorizer = TransactionCategorizer(merchantRepo, ruleBasedCategorizer)

        val result = categorizer.categorize("tx-1", "")

        assertNull(result)
    }

    @Test
    fun `handles whitespace description`() = runTest {
        val merchantRepo = FakeLearnedMerchantRepository()
        val ruleBasedCategorizer = RuleBasedCategorizer(MerchantDatabase())
        val categorizer = TransactionCategorizer(merchantRepo, ruleBasedCategorizer)

        val result = categorizer.categorize("tx-1", "   ")

        assertNull(result)
    }

    // --- Confidence Tests ---

    @Test
    fun `tier 0 results have high confidence`() = runTest {
        val merchantRepo = FakeLearnedMerchantRepository()
        merchantRepo.merchants["SPECIAL STORE"] = LearnedMerchant(
            id = "lm-1",
            merchantPattern = "SPECIAL STORE",
            categoryId = "shopping",
            confidence = 0.80f, // Lower stored confidence
            source = LearnedMerchantSource.USER,
            sampleCount = 2,
            lastUsedAt = fixedTime,
            createdAt = fixedTime
        )

        val ruleBasedCategorizer = RuleBasedCategorizer(MerchantDatabase())
        val categorizer = TransactionCategorizer(merchantRepo, ruleBasedCategorizer)

        val result = categorizer.categorize("tx-1", "SPECIAL STORE ORDER")

        assertNotNull(result)
        // Should be at least base confidence (0.95)
        assertTrue(result.confidence >= 0.95f)
    }

    // --- Statistics Tests ---

    @Test
    fun `returns correct statistics`() = runTest {
        val merchantRepo = FakeLearnedMerchantRepository()
        merchantRepo.merchants["M1"] = LearnedMerchant(
            id = "lm-1",
            merchantPattern = "M1",
            categoryId = "food",
            confidence = 0.95f,
            source = LearnedMerchantSource.USER,
            sampleCount = 5,
            lastUsedAt = fixedTime,
            createdAt = fixedTime
        )
        merchantRepo.merchants["M2"] = LearnedMerchant(
            id = "lm-2",
            merchantPattern = "M2",
            categoryId = "shopping",
            confidence = 0.85f,
            source = LearnedMerchantSource.USER,
            sampleCount = 3,
            lastUsedAt = fixedTime,
            createdAt = fixedTime
        )
        merchantRepo.merchants["M3"] = LearnedMerchant(
            id = "lm-3",
            merchantPattern = "M3",
            categoryId = "transport",
            confidence = 0.70f,
            source = LearnedMerchantSource.ML,
            sampleCount = 2,
            lastUsedAt = fixedTime,
            createdAt = fixedTime
        )

        val ruleBasedCategorizer = RuleBasedCategorizer(MerchantDatabase())
        val categorizer = TransactionCategorizer(merchantRepo, ruleBasedCategorizer)

        val stats = categorizer.getStats()

        assertEquals(3, stats.totalLearnedMappings)
        assertEquals(1, stats.highConfidenceMappings) // Only M1 >= 0.90
        assertEquals(10, stats.totalSamples) // 5 + 3 + 2
    }

    // --- Last Used Update Tests ---

    @Test
    fun `updates lastUsedAt when matching`() = runTest {
        val merchantRepo = FakeLearnedMerchantRepository()
        merchantRepo.merchants["COFFEE"] = LearnedMerchant(
            id = "lm-1",
            merchantPattern = "COFFEE",
            categoryId = "food",
            confidence = 0.95f,
            source = LearnedMerchantSource.USER,
            sampleCount = 5,
            lastUsedAt = fixedTime,
            createdAt = fixedTime
        )

        val ruleBasedCategorizer = RuleBasedCategorizer(MerchantDatabase())
        val categorizer = TransactionCategorizer(merchantRepo, ruleBasedCategorizer)

        categorizer.categorize("tx-1", "COFFEE SHOP")

        // Should have updated the merchant
        assertTrue(merchantRepo.updatedMerchants.isNotEmpty())
        val updated = merchantRepo.updatedMerchants.last()
        assertEquals("lm-1", updated.id)
    }

    // --- Transaction ID Propagation ---

    @Test
    fun `propagates transaction ID correctly`() = runTest {
        val merchantRepo = FakeLearnedMerchantRepository()
        merchantRepo.merchants["TEST"] = LearnedMerchant(
            id = "lm-1",
            merchantPattern = "TEST",
            categoryId = "other",
            confidence = 0.95f,
            source = LearnedMerchantSource.USER,
            sampleCount = 1,
            lastUsedAt = fixedTime,
            createdAt = fixedTime
        )

        val ruleBasedCategorizer = RuleBasedCategorizer(MerchantDatabase())
        val categorizer = TransactionCategorizer(merchantRepo, ruleBasedCategorizer)

        val result = categorizer.categorize("unique-tx-id-123", "TEST MERCHANT")

        assertNotNull(result)
        assertEquals("unique-tx-id-123", result.transactionId)
    }
}

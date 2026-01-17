package com.finuts.data.import.ocr

import com.finuts.ai.providers.LLMProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Enhances locally extracted transactions with Cloud LLM.
 *
 * Uses Claude/GPT structured output to:
 * - Extract merchant names
 * - Categorize transactions
 * - Identify counterparties
 * - Determine transaction types
 *
 * Only receives ANONYMIZED data - PII is replaced with placeholders.
 */
class CloudTransactionEnhancer(
    private val llmProvider: LLMProvider
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Enhance transactions with LLM-extracted metadata.
     *
     * @param transactions Locally extracted transactions (with PII anonymized)
     * @return Enhanced transactions with merchant/category/type info
     */
    suspend fun enhance(transactions: List<PartialTransaction>): List<EnhancedTransaction> {
        if (transactions.isEmpty()) return emptyList()

        return try {
            val prompt = buildPrompt(transactions)
            val response = llmProvider.structuredOutput(prompt, SCHEMA)

            val enhancements = parseResponse(response.content)
            mergeEnhancements(transactions, enhancements)
        } catch (e: Exception) {
            // On error, return transactions with empty enhancements
            transactions.map { tx ->
                EnhancedTransaction(
                    rawDate = tx.rawDate,
                    amountMinorUnits = tx.amountMinorUnits,
                    currency = tx.currency,
                    rawDescription = tx.rawDescription,
                    isCredit = tx.isCredit,
                    isDebit = tx.isDebit,
                    merchant = null,
                    counterpartyName = null,
                    categoryHint = null,
                    transactionType = null
                )
            }
        }
    }

    private fun buildPrompt(transactions: List<PartialTransaction>): String {
        val transactionsText = transactions.mapIndexed { index, tx ->
            "$index: ${tx.rawDate} | ${tx.amountFormatted} ${tx.currency ?: ""} | ${tx.rawDescription}"
        }.joinToString("\n")

        return """
            |You are a financial transaction analyzer. Analyze these transactions and extract:
            |- merchant: business name (null for transfers between persons)
            |- counterpartyName: person's name/placeholder for P2P transfers
            |- categoryHint: category like groceries, food_delivery, transfers, etc.
            |- transactionType: DEBIT, CREDIT, TRANSFER, FEE, INTEREST, or REFUND
            |
            |IMPORTANT:
            |- Preserve placeholders like [PERSON_NAME_1], [IBAN_1] exactly as shown
            |- For P2P transfers, merchant is null, counterpartyName is the person
            |- For business payments, merchant is the business, counterpartyName is null
            |
            |Transactions to analyze:
            |$transactionsText
            |
            |Return JSON array with one object per transaction (use index field):
        """.trimMargin()
    }

    private fun parseResponse(content: String): List<LLMEnhancement> {
        return try {
            json.decodeFromString<List<LLMEnhancement>>(content)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mergeEnhancements(
        transactions: List<PartialTransaction>,
        enhancements: List<LLMEnhancement>
    ): List<EnhancedTransaction> {
        // Create a map of index -> enhancement
        val enhancementMap = enhancements.associateBy { it.index }

        return transactions.mapIndexed { index, tx ->
            val enhancement = enhancementMap[index]
            EnhancedTransaction(
                rawDate = tx.rawDate,
                amountMinorUnits = tx.amountMinorUnits,
                currency = tx.currency,
                rawDescription = tx.rawDescription,
                isCredit = tx.isCredit,
                isDebit = tx.isDebit,
                merchant = enhancement?.merchant,
                counterpartyName = enhancement?.counterpartyName,
                categoryHint = enhancement?.categoryHint,
                transactionType = enhancement?.transactionType
            )
        }
    }

    companion object {
        private const val SCHEMA = """
        {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "index": {"type": "integer"},
                    "merchant": {"type": ["string", "null"]},
                    "counterpartyName": {"type": ["string", "null"]},
                    "categoryHint": {"type": ["string", "null"]},
                    "transactionType": {
                        "type": ["string", "null"],
                        "enum": ["DEBIT", "CREDIT", "TRANSFER", "FEE", "INTEREST", "REFUND", null]
                    }
                },
                "required": ["index"]
            }
        }
        """
    }
}

/**
 * LLM response structure for transaction enhancement.
 */
@Serializable
private data class LLMEnhancement(
    val index: Int,
    val merchant: String? = null,
    val counterpartyName: String? = null,
    val categoryHint: String? = null,
    val transactionType: String? = null
)

/**
 * Transaction enhanced with LLM-extracted metadata.
 *
 * Contains both locally extracted data (from PartialTransaction)
 * and LLM-enhanced data (merchant, category, type).
 */
data class EnhancedTransaction(
    // From local extraction
    val rawDate: String,
    val amountMinorUnits: Long,
    val currency: String?,
    val rawDescription: String,
    val isCredit: Boolean,
    val isDebit: Boolean,

    // From LLM enhancement
    val merchant: String?,
    val counterpartyName: String?,
    val categoryHint: String?,
    val transactionType: String?
)

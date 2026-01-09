package com.finuts.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Database migrations for Finuts.
 *
 * Migration 1 → 2: Add transfer support fields
 * - linkedTransactionId: Links two transactions for double-entry accounting
 * - transferAccountId: The other account involved in the transfer
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE transactions ADD COLUMN linkedTransactionId TEXT DEFAULT NULL"
        )
        connection.execSQL(
            "ALTER TABLE transactions ADD COLUMN transferAccountId TEXT DEFAULT NULL"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_transactions_linkedTransactionId ON transactions(linkedTransactionId)"
        )
    }
}

/**
 * Migration 2 → 3: Add initialBalance for calculated balance
 * - initialBalance: Starting balance before any transactions
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE accounts ADD COLUMN initialBalance INTEGER NOT NULL DEFAULT 0"
        )
    }
}

/**
 * Migration 3 → 4: Add AI learning tables
 * - category_corrections: Stores user category corrections for learning
 * - learned_merchants: Stores learned merchant-to-category mappings (Tier 0)
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        // Create category_corrections table
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS category_corrections (
                id TEXT NOT NULL PRIMARY KEY,
                transactionId TEXT NOT NULL,
                originalCategoryId TEXT,
                correctedCategoryId TEXT NOT NULL,
                merchantName TEXT,
                merchantNormalized TEXT,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY (transactionId) REFERENCES transactions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_category_corrections_transactionId ON category_corrections(transactionId)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_category_corrections_merchantNormalized ON category_corrections(merchantNormalized)"
        )

        // Create learned_merchants table
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS learned_merchants (
                id TEXT NOT NULL PRIMARY KEY,
                merchantPattern TEXT NOT NULL,
                categoryId TEXT NOT NULL,
                confidence REAL NOT NULL,
                source TEXT NOT NULL,
                sampleCount INTEGER NOT NULL,
                lastUsedAt INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_learned_merchants_merchantPattern ON learned_merchants(merchantPattern)"
        )
    }
}

/**
 * Migration 4 → 5: Add AI categorization metadata to transactions
 * - categorizationSource: Which tier/method assigned the category (Tier 0-3)
 * - categorizationConfidence: Confidence score (0.0-1.0) for AI decisions
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE transactions ADD COLUMN categorizationSource TEXT DEFAULT NULL"
        )
        connection.execSQL(
            "ALTER TABLE transactions ADD COLUMN categorizationConfidence REAL DEFAULT NULL"
        )
    }
}

/**
 * All migrations in order.
 */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

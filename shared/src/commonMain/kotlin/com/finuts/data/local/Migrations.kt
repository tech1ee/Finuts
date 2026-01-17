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
 * Migration 5 → 6: Database encryption enabled
 *
 * This migration is a version marker only - no SQL changes needed.
 * Encryption is handled at the file level by:
 * - Android: SQLCipher with keys from AndroidKeyStore
 * - iOS: iOS Data Protection (NSFileProtectionComplete)
 *
 * Existing data is migrated via EncryptionMigrationManager before
 * Room opens the database.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        // No SQL changes - encryption is handled at file level
        // This migration exists to track that encryption has been enabled
    }
}

/**
 * Migration 6 → 7: Seed default categories for AI categorization
 *
 * This migration ensures all default categories exist in the database.
 * These categories are required for:
 * - MerchantDatabase patterns (Tier 1 categorization)
 * - RuleBasedCategorizer patterns
 * - TransactionCategorizer fallbacks
 *
 * Uses INSERT OR IGNORE to avoid duplicates if categories already exist.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        // Expense categories - IDs must match MerchantDatabase patterns
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('groceries', 'Groceries', 'shopping_cart', '#4CAF50', 'EXPENSE', NULL, 1, 1)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('food_delivery', 'Food Delivery', 'delivery_dining', '#8BC34A', 'EXPENSE', NULL, 1, 2)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('transport', 'Transport', 'directions_car', '#2196F3', 'EXPENSE', NULL, 1, 3)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('shopping', 'Shopping', 'shopping_bag', '#9C27B0', 'EXPENSE', NULL, 1, 4)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('utilities', 'Utilities', 'power', '#FF9800', 'EXPENSE', NULL, 1, 5)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('healthcare', 'Healthcare', 'medical_services', '#F44336', 'EXPENSE', NULL, 1, 6)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('entertainment', 'Entertainment', 'sports_esports', '#E91E63', 'EXPENSE', NULL, 1, 7)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('education', 'Education', 'school', '#3F51B5', 'EXPENSE', NULL, 1, 8)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('housing', 'Housing', 'home', '#795548', 'EXPENSE', NULL, 1, 9)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('transfer', 'Transfers', 'swap_horiz', '#607D8B', 'EXPENSE', NULL, 1, 10)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('other', 'Other', 'more_horiz', '#9E9E9E', 'EXPENSE', NULL, 1, 11)"""
        )

        // Income categories
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('salary', 'Salary', 'work', '#4CAF50', 'INCOME', NULL, 1, 12)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('freelance', 'Freelance', 'computer', '#00BCD4', 'INCOME', NULL, 1, 13)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('investments', 'Investments', 'trending_up', '#8BC34A', 'INCOME', NULL, 1, 14)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('gifts', 'Gifts', 'card_giftcard', '#FF5722', 'INCOME', NULL, 1, 15)"""
        )
        connection.execSQL(
            """INSERT OR IGNORE INTO categories (id, name, icon, color, type, parentId, isDefault, sortOrder)
               VALUES ('other_income', 'Other Income', 'attach_money', '#607D8B', 'INCOME', NULL, 1, 16)"""
        )
    }
}

/**
 * All migrations in order.
 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7
)

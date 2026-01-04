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
 * All migrations in order.
 */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)

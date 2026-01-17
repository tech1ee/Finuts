package com.finuts.data.local

import android.content.Context
import co.touchlab.kermit.Logger
import com.finuts.core.security.DatabaseKeyProvider
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

/**
 * Manages migration from unencrypted to encrypted database.
 *
 * For fresh installations: Database is created encrypted from the start.
 * For existing installations: Existing unencrypted data is migrated to encrypted format.
 */
class EncryptionMigrationManager(
    private val context: Context,
    private val keyProvider: DatabaseKeyProvider
) {
    private val log = Logger.withTag("EncryptionMigration")

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if migration is needed and perform it.
     * Safe to call multiple times - will only migrate once.
     */
    suspend fun migrateIfNeeded() {
        if (isDatabaseEncrypted()) {
            log.d { "Database already encrypted, skipping migration" }
            return
        }

        val dbFile = context.getDatabasePath(FinutsDatabase.DATABASE_NAME)
        if (!dbFile.exists()) {
            log.d { "No existing database, marking as encrypted for fresh start" }
            markAsEncrypted()
            return
        }

        if (isAlreadySqlCipher(dbFile)) {
            log.d { "Database appears to be already encrypted, marking as encrypted" }
            markAsEncrypted()
            return
        }

        performMigration(dbFile)
    }

    /**
     * Check if the database is marked as encrypted in preferences.
     */
    fun isDatabaseEncrypted(): Boolean {
        return prefs.getBoolean(KEY_IS_ENCRYPTED, false)
    }

    /**
     * Check if a database file is already SQLCipher encrypted.
     * SQLCipher databases have a different header than plain SQLite.
     */
    private fun isAlreadySqlCipher(dbFile: File): Boolean {
        return try {
            dbFile.inputStream().use { stream ->
                val header = ByteArray(SQLITE_HEADER_SIZE)
                val bytesRead = stream.read(header)
                if (bytesRead < SQLITE_HEADER_SIZE) return false

                val headerString = String(header, Charsets.US_ASCII)
                !headerString.startsWith(SQLITE_HEADER_STRING)
            }
        } catch (e: Exception) {
            log.w(e) { "Could not check database header" }
            false
        }
    }

    private suspend fun performMigration(dbFile: File) {
        log.i { "Starting database encryption migration" }

        val passphrase = keyProvider.getOrCreatePassphrase()
        val tempEncryptedFile = File(dbFile.parent, TEMP_ENCRYPTED_DB)
        tempEncryptedFile.delete()

        try {
            migrateDatabase(dbFile, tempEncryptedFile, passphrase)

            dbFile.delete()
            File(dbFile.absolutePath + "-wal").delete()
            File(dbFile.absolutePath + "-shm").delete()
            File(dbFile.absolutePath + "-journal").delete()

            if (!tempEncryptedFile.renameTo(dbFile)) {
                throw MigrationException("Failed to rename encrypted database")
            }

            markAsEncrypted()
            log.i { "Database encryption migration completed successfully" }

        } catch (e: Exception) {
            log.e(e) { "Database encryption migration failed" }
            tempEncryptedFile.delete()

            if (e is MigrationException) throw e
            throw MigrationException("Failed to migrate database to encrypted storage", e)
        }
    }

    private fun migrateDatabase(source: File, target: File, passphrase: ByteArray) {
        val sourceDb = android.database.sqlite.SQLiteDatabase.openDatabase(
            source.absolutePath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READONLY
        )

        val targetDb = SQLiteDatabase.openOrCreateDatabase(
            target.absolutePath,
            passphrase,
            null,
            null,
            null
        )

        try {
            val tables = mutableListOf<String>()
            sourceDb.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%'",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0))
                }
            }

            for (tableName in tables) {
                copyTable(sourceDb, targetDb, tableName)
            }

            log.i { "Migrated ${tables.size} tables to encrypted database" }
        } finally {
            sourceDb.close()
            @Suppress("CAST_NEVER_SUCCEEDS")
            (targetDb as? java.io.Closeable)?.close()
        }
    }

    private fun copyTable(
        source: android.database.sqlite.SQLiteDatabase,
        target: SQLiteDatabase,
        tableName: String
    ) {
        source.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use { cursor ->
            if (cursor.moveToFirst()) {
                val createSql = cursor.getString(0)
                if (createSql != null) {
                    target.execSQL(createSql)
                }
            }
        }

        source.rawQuery("SELECT * FROM \"$tableName\"", null).use { cursor ->
            if (cursor.count == 0) return

            val columnCount = cursor.columnCount
            val columnNames = (0 until columnCount).map { cursor.getColumnName(it) }
            val placeholders = (0 until columnCount).joinToString(",") { "?" }
            val insertSql = "INSERT INTO \"$tableName\" (${columnNames.joinToString(",") { "\"$it\"" }}) VALUES ($placeholders)"

            val statement = target.compileStatement(insertSql)

            while (cursor.moveToNext()) {
                statement.clearBindings()
                for (i in 0 until columnCount) {
                    when (cursor.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> statement.bindNull(i + 1)
                        android.database.Cursor.FIELD_TYPE_INTEGER -> statement.bindLong(i + 1, cursor.getLong(i))
                        android.database.Cursor.FIELD_TYPE_FLOAT -> statement.bindDouble(i + 1, cursor.getDouble(i))
                        android.database.Cursor.FIELD_TYPE_STRING -> statement.bindString(i + 1, cursor.getString(i))
                        android.database.Cursor.FIELD_TYPE_BLOB -> statement.bindBlob(i + 1, cursor.getBlob(i))
                    }
                }
                statement.executeInsert()
            }
        }

        log.d { "Copied table: $tableName" }
    }

    private fun markAsEncrypted() {
        prefs.edit()
            .putBoolean(KEY_IS_ENCRYPTED, true)
            .putInt(KEY_ENCRYPTION_VERSION, ENCRYPTION_VERSION)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "finuts_db_migration"
        private const val KEY_IS_ENCRYPTED = "db_encrypted_v1"
        private const val KEY_ENCRYPTION_VERSION = "encryption_version"
        private const val ENCRYPTION_VERSION = 1
        private const val TEMP_ENCRYPTED_DB = "finuts_encrypted_temp.db"
        private const val SQLITE_HEADER_SIZE = 16
        private const val SQLITE_HEADER_STRING = "SQLite format 3"
    }
}

/**
 * Exception thrown when database migration fails.
 */
class MigrationException(message: String, cause: Throwable? = null) : Exception(message, cause)

package com.finuts.data.local

import android.content.Context
import androidx.room.Room
import co.touchlab.kermit.Logger
import com.finuts.core.security.DatabaseKeyProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

private val log = Logger.withTag("DatabaseBuilder")

/**
 * Creates encrypted Room database using SQLCipher.
 *
 * The database is encrypted at rest using AES-256 encryption.
 * The encryption key is securely stored in Android KeyStore.
 *
 * @param context Application context
 * @return Encrypted FinutsDatabase instance
 */
fun getDatabaseBuilder(context: Context): FinutsDatabase {
    // Load SQLCipher native libraries
    System.loadLibrary("sqlcipher")

    val keyProvider = DatabaseKeyProvider(context)
    val migrationManager = EncryptionMigrationManager(context, keyProvider)

    // Perform migration from unencrypted to encrypted if needed
    // This must complete before opening the database
    runBlocking {
        migrationManager.migrateIfNeeded()
    }

    // Get or create the encryption passphrase
    val passphrase = runBlocking {
        keyProvider.getOrCreatePassphrase()
    }

    log.i { "Opening encrypted database" }

    val factory = SupportOpenHelperFactory(passphrase)
    val dbFile = context.getDatabasePath(FinutsDatabase.DATABASE_NAME)

    return Room.databaseBuilder<FinutsDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
        .openHelperFactory(factory)
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(*ALL_MIGRATIONS)
        .build()
}

package com.finuts.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileProtectionComplete
import platform.Foundation.NSFileProtectionKey
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

private val log = Logger.withTag("DatabaseBuilder")

/**
 * Creates Room database for iOS with file-level encryption.
 *
 * Note: iOS uses built-in Data Protection for encryption at rest.
 * Files in the app's container are automatically encrypted by iOS
 * when the device is locked (using NSFileProtectionComplete).
 *
 * This provides:
 * - Hardware-backed encryption using Secure Enclave
 * - Automatic key management by iOS
 * - Protection when device is locked
 *
 * Future improvement: Integrate SQLCipher for iOS when Room KMP
 * provides official support or via C interop.
 */
@OptIn(ExperimentalForeignApi::class)
fun getDatabaseBuilder(): FinutsDatabase {
    val dbFilePath = documentDirectory() + "/" + FinutsDatabase.DATABASE_NAME

    // Set file protection attribute for encryption at rest
    setFileProtection(dbFilePath)

    log.i { "Opening database with iOS Data Protection" }

    return Room.databaseBuilder<FinutsDatabase>(
        name = dbFilePath
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(*ALL_MIGRATIONS)
        .build()
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    return requireNotNull(documentDirectory?.path)
}

/**
 * Set NSFileProtectionComplete on the database file.
 * This ensures the file is encrypted when the device is locked.
 */
@OptIn(ExperimentalForeignApi::class)
private fun setFileProtection(filePath: String) {
    try {
        val fileManager = NSFileManager.defaultManager
        val fileUrl = NSURL.fileURLWithPath(filePath)

        // Check if file exists before setting attributes
        if (fileManager.fileExistsAtPath(filePath)) {
            val attributes = mapOf<Any?, Any?>(
                NSFileProtectionKey to NSFileProtectionComplete
            )

            val success = fileManager.setAttributes(
                attributes = attributes,
                ofItemAtPath = filePath,
                error = null
            )

            if (success) {
                log.d { "File protection set to Complete for database" }
            } else {
                log.w { "Failed to set file protection attribute" }
            }
        }
    } catch (e: Exception) {
        log.w(e) { "Could not set file protection" }
    }
}

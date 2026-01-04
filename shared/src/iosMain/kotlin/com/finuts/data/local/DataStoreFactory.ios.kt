@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.finuts.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS-specific DataStore creation.
 */
fun createDataStore(): DataStore<Preferences> = createDataStore {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    requireNotNull(documentDirectory).path + "/$DATASTORE_FILE_NAME"
}

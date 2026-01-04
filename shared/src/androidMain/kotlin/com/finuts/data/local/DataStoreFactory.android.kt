package com.finuts.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Android-specific DataStore creation.
 */
fun createDataStore(context: Context): DataStore<Preferences> =
    createDataStore {
        context.filesDir.resolve(DATASTORE_FILE_NAME).absolutePath
    }

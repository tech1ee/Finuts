package com.finuts.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun getDatabaseBuilder(context: Context): FinutsDatabase {
    val dbFile = context.getDatabasePath(FinutsDatabase.DATABASE_NAME)
    return Room.databaseBuilder<FinutsDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(*ALL_MIGRATIONS)
        .build()
}

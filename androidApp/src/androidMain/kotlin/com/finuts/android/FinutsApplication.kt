package com.finuts.android

import android.app.Application
import co.touchlab.kermit.Logger
import com.finuts.app.di.appModules
import com.finuts.core.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class FinutsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@FinutsApplication)
            modules(appModules + platformModule)
        }

        Logger.d { "Finuts Application started" }
    }
}

package com.finuts.app

import com.finuts.app.di.appModules
import com.finuts.core.di.platformModule
import org.koin.core.context.startKoin

actual object IosEntry {
    actual fun initialize() {
        startKoin {
            modules(appModules + platformModule)
        }
    }
}

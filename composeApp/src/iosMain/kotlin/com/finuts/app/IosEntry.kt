@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.finuts.app

import co.touchlab.kermit.Logger
import co.touchlab.kermit.NSLogWriter
import com.finuts.app.di.appModules
import com.finuts.core.di.platformModule
import org.koin.core.context.startKoin
import org.koin.core.error.KoinApplicationAlreadyStartedException
import kotlin.native.setUnhandledExceptionHook

actual object IosEntry {
    private val log = Logger.withTag("IosEntry")
    private var isInitialized = false

    actual fun initialize() {
        // Prevent double initialization
        if (isInitialized) {
            log.d { "IosEntry already initialized, skipping" }
            return
        }

        // Initialize Kermit Logger with NSLogWriter for iOS
        // This enables logs to be visible via: xcrun simctl spawn booted log stream --level debug
        Logger.setLogWriters(NSLogWriter())

        // Set up global exception handler to log unhandled exceptions before crash
        setUnhandledExceptionHook { throwable ->
            Logger.e(throwable) { "UNHANDLED EXCEPTION: ${throwable.message}" }
        }

        // Start Koin, handling case where it's already started
        try {
            startKoin {
                modules(appModules + platformModule)
            }
            log.i { "Koin started" }
        } catch (_: KoinApplicationAlreadyStartedException) {
            log.d { "Koin already running, skipping startKoin" }
        }

        isInitialized = true
        log.i { "iOS App initialized with NSLog logging" }
    }
}

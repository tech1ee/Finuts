@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.finuts.app

import com.finuts.app.di.appModules
import com.finuts.core.di.platformModule
import org.koin.core.context.startKoin
import kotlin.native.setUnhandledExceptionHook

actual object IosEntry {
    actual fun initialize() {
        // Set up global exception handler to log unhandled exceptions before crash
        setUnhandledExceptionHook { throwable ->
            println("=== UNHANDLED EXCEPTION ===")
            println("Exception type: ${throwable::class.simpleName}")
            println("Message: ${throwable.message}")
            throwable.printStackTrace()
            println("=== END UNHANDLED EXCEPTION ===")
        }

        startKoin {
            modules(appModules + platformModule)
        }
    }
}

package com.finuts.app.core.locale

import kotlin.system.exitProcess

/**
 * Android implementation: exits the app using exitProcess.
 * The saved language preference will apply on next launch.
 */
actual fun exitApp() {
    exitProcess(0)
}

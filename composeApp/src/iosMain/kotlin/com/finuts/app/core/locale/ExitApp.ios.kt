package com.finuts.app.core.locale

import platform.posix.exit

/**
 * iOS implementation: exits the app using POSIX exit.
 * The saved language preference will apply on next launch.
 */
actual fun exitApp() {
    exit(0)
}

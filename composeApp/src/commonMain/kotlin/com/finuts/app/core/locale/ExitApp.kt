package com.finuts.app.core.locale

/**
 * Exits the app.
 * Used for language change on iOS where restart is required.
 *
 * Platform implementations:
 * - iOS: Uses exit(0) to terminate the process
 * - Android: Uses exitProcess(0) to terminate the process
 */
expect fun exitApp()

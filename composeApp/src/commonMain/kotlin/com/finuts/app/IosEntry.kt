package com.finuts.app

/**
 * Platform-specific initialization.
 * This expect declaration ensures iOS code gets linked into the framework.
 */
expect object IosEntry {
    fun initialize()
}

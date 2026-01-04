package com.finuts.core.util

/**
 * Platform-agnostic time provider.
 * Uses expect/actual pattern for cross-platform compatibility.
 */
expect fun currentTimeMillis(): Long

package com.finuts.test.fakes

import com.finuts.core.security.DATABASE_KEY_SIZE

/**
 * Fake implementation of DatabaseKeyProvider for testing.
 * Provides deterministic key generation for reproducible tests.
 *
 * Note: This fake cannot implement the actual expect class since
 * expect/actual requires platform-specific implementations.
 * Use this in tests that need to simulate key provider behavior.
 */
class FakeDatabaseKeyProvider {
    private var storedKey: ByteArray? = null
    private var shouldFail = false
    private var failureException: Exception? = null

    suspend fun getOrCreatePassphrase(): ByteArray {
        if (shouldFail) {
            throw failureException ?: FakeDatabaseKeyException("Simulated failure")
        }

        return storedKey ?: generateDeterministicKey().also {
            storedKey = it
        }
    }

    fun hasExistingKey(): Boolean {
        return storedKey != null
    }

    suspend fun deleteKey() {
        if (shouldFail) {
            throw failureException ?: FakeDatabaseKeyException("Simulated failure")
        }
        storedKey = null
    }

    // Test helpers

    /**
     * Set a specific key for testing.
     */
    fun setKey(key: ByteArray) {
        require(key.size == DATABASE_KEY_SIZE) {
            "Key must be $DATABASE_KEY_SIZE bytes"
        }
        storedKey = key.copyOf()
    }

    /**
     * Get the current stored key (for verification in tests).
     */
    fun getStoredKey(): ByteArray? = storedKey?.copyOf()

    /**
     * Configure the fake to throw on next operation.
     */
    fun setFailure(exception: Exception? = null) {
        shouldFail = true
        failureException = exception
    }

    /**
     * Reset failure state.
     */
    fun clearFailure() {
        shouldFail = false
        failureException = null
    }

    /**
     * Reset all state.
     */
    fun reset() {
        storedKey = null
        shouldFail = false
        failureException = null
    }

    private fun generateDeterministicKey(): ByteArray {
        // Generate a deterministic key for testing (NOT for production use)
        return ByteArray(DATABASE_KEY_SIZE) { index ->
            (index * 7 + 13).toByte()
        }
    }
}

class FakeDatabaseKeyException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

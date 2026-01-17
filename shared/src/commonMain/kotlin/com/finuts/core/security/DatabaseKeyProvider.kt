package com.finuts.core.security

/**
 * Platform-specific secure key storage for database encryption.
 * Keys are generated and stored in hardware-backed secure storage:
 * - Android: AndroidKeyStore (TEE-backed on supported devices)
 * - iOS: Keychain (Secure Enclave on modern devices)
 *
 * Security properties:
 * - Keys never leave secure storage in plaintext
 * - Keys are not included in device backups
 * - Keys are device-specific and non-exportable
 */
expect class DatabaseKeyProvider {
    /**
     * Get or create the database encryption passphrase.
     * On first call, generates a new key and stores it securely.
     * On subsequent calls, retrieves the existing key.
     *
     * @return ByteArray containing the passphrase for SQLCipher
     */
    suspend fun getOrCreatePassphrase(): ByteArray

    /**
     * Check if a key already exists in secure storage.
     * Used to detect first-time setup vs existing encrypted database.
     */
    fun hasExistingKey(): Boolean

    /**
     * Delete the existing key from secure storage.
     * WARNING: This will make the encrypted database permanently inaccessible.
     * Only use for complete data reset scenarios.
     */
    suspend fun deleteKey()
}

/**
 * Alias used for storing the database encryption key.
 */
const val DATABASE_KEY_ALIAS = "finuts_db_encryption_key_v1"

/**
 * Key size in bytes (256-bit for AES-256).
 */
const val DATABASE_KEY_SIZE = 32

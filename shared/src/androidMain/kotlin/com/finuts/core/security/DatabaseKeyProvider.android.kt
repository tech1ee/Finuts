package com.finuts.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import co.touchlab.kermit.Logger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android implementation of DatabaseKeyProvider using AndroidKeyStore.
 *
 * The key derivation approach:
 * 1. Generate/retrieve an AES-256 key in AndroidKeyStore
 * 2. Use that key to encrypt a known salt value
 * 3. The encrypted output becomes the SQLCipher passphrase
 *
 * This approach is needed because AndroidKeyStore keys are non-exportable,
 * but SQLCipher requires a passphrase bytes. By encrypting a known value,
 * we get consistent output that can be used as the passphrase.
 */
actual class DatabaseKeyProvider(private val context: Context) {
    private val log = Logger.withTag("DatabaseKeyProvider")

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual suspend fun getOrCreatePassphrase(): ByteArray {
        return try {
            val secretKey = getOrCreateKey()
            derivePassphrase(secretKey)
        } catch (e: Exception) {
            log.e(e) { "Failed to get/create passphrase" }
            throw DatabaseKeyException("Failed to initialize database encryption", e)
        }
    }

    actual fun hasExistingKey(): Boolean {
        return try {
            keyStore.containsAlias(DATABASE_KEY_ALIAS)
        } catch (e: Exception) {
            log.w(e) { "Error checking key existence" }
            false
        }
    }

    actual suspend fun deleteKey() {
        try {
            keyStore.deleteEntry(DATABASE_KEY_ALIAS)
            prefs.edit().remove(KEY_IV).apply()
            log.i { "Database encryption key deleted" }
        } catch (e: Exception) {
            log.e(e) { "Failed to delete key" }
            throw DatabaseKeyException("Failed to delete database encryption key", e)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val existingKey = keyStore.getKey(DATABASE_KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            log.d { "Using existing encryption key" }
            return existingKey
        }

        log.i { "Generating new encryption key" }
        return generateKey()
    }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val spec = KeyGenParameterSpec.Builder(
            DATABASE_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Derive SQLCipher passphrase from the KeyStore key.
     * We encrypt a known salt and use the ciphertext as passphrase.
     * The IV is stored in SharedPreferences for consistency.
     */
    private fun derivePassphrase(secretKey: SecretKey): ByteArray {
        val storedIv = prefs.getString(KEY_IV, null)

        return if (storedIv != null) {
            decryptWithStoredIv(secretKey, storedIv)
        } else {
            encryptAndStoreIv(secretKey)
        }
    }

    private fun encryptAndStoreIv(secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(SALT_VALUE.toByteArray(Charsets.UTF_8))

        prefs.edit()
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()

        log.d { "Created new passphrase derivation" }
        return ciphertext.copyOf(DATABASE_KEY_SIZE)
    }

    private fun decryptWithStoredIv(secretKey: SecretKey, storedIvBase64: String): ByteArray {
        val storedCiphertext = prefs.getString(KEY_CIPHERTEXT, null)
        if (storedCiphertext != null) {
            return Base64.decode(storedCiphertext, Base64.NO_WRAP).copyOf(DATABASE_KEY_SIZE)
        }

        val iv = Base64.decode(storedIvBase64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val ciphertext = cipher.doFinal(SALT_VALUE.toByteArray(Charsets.UTF_8))

        prefs.edit()
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()

        return ciphertext.copyOf(DATABASE_KEY_SIZE)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PREFS_NAME = "finuts_db_security"
        private const val KEY_IV = "db_key_iv"
        private const val KEY_CIPHERTEXT = "db_key_ct"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH = 128
        private const val SALT_VALUE = "finuts_db_encryption_salt_v1"
    }
}

/**
 * Exception thrown when database key operations fail.
 */
class DatabaseKeyException(message: String, cause: Throwable? = null) : Exception(message, cause)

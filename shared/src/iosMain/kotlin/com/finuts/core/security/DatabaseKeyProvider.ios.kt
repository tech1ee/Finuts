package com.finuts.core.security

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecRandomDefault
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * iOS implementation of DatabaseKeyProvider using Keychain Services.
 *
 * Security properties:
 * - Key stored in Keychain with kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
 * - Key is device-specific and not included in backups
 * - Key accessible after device first unlock (allows background operations)
 */
@OptIn(ExperimentalForeignApi::class)
actual class DatabaseKeyProvider {
    private val log = Logger.withTag("DatabaseKeyProvider")

    actual suspend fun getOrCreatePassphrase(): ByteArray {
        return try {
            val existingKey = retrieveKeyFromKeychain()
            if (existingKey != null) {
                log.d { "Using existing encryption key from Keychain" }
                existingKey
            } else {
                log.i { "Generating new encryption key" }
                generateAndStoreKey()
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to get/create passphrase" }
            throw DatabaseKeyException("Failed to initialize database encryption", e)
        }
    }

    actual fun hasExistingKey(): Boolean {
        return try {
            retrieveKeyFromKeychain() != null
        } catch (e: Exception) {
            log.w(e) { "Error checking key existence" }
            false
        }
    }

    actual suspend fun deleteKey() {
        try {
            deleteKeyFromKeychain()
            log.i { "Database encryption key deleted from Keychain" }
        } catch (e: Exception) {
            log.e(e) { "Failed to delete key" }
            throw DatabaseKeyException("Failed to delete database encryption key", e)
        }
    }

    private fun retrieveKeyFromKeychain(): ByteArray? = memScoped {
        val query = CFDictionaryCreateMutable(null, 5, null, null)
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, CFBridgingRetain(KEYCHAIN_SERVICE))
        CFDictionarySetValue(query, kSecAttrAccount, CFBridgingRetain(DATABASE_KEY_ALIAS))
        CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)

        when (status) {
            errSecSuccess -> {
                val data = CFBridgingRelease(result.value) as? NSData
                data?.toByteArray()
            }
            errSecItemNotFound -> null
            else -> {
                log.w { "Keychain query failed with status: $status" }
                null
            }
        }
    }

    private fun generateAndStoreKey(): ByteArray = memScoped {
        val keyData = NSMutableData.create(length = DATABASE_KEY_SIZE.toULong())
            ?: throw DatabaseKeyException("Failed to allocate key data")

        val randomStatus = SecRandomCopyBytes(
            kSecRandomDefault,
            DATABASE_KEY_SIZE.toULong(),
            keyData.mutableBytes
        )

        if (randomStatus != errSecSuccess.toInt()) {
            throw DatabaseKeyException("Failed to generate random key: $randomStatus")
        }

        val keyBytes = keyData.toByteArray()

        val query = CFDictionaryCreateMutable(null, 5, null, null)
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, CFBridgingRetain(KEYCHAIN_SERVICE))
        CFDictionarySetValue(query, kSecAttrAccount, CFBridgingRetain(DATABASE_KEY_ALIAS))
        CFDictionarySetValue(query, kSecValueData, CFBridgingRetain(keyData))
        CFDictionarySetValue(
            query,
            kSecAttrAccessible,
            kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        )

        val status = SecItemAdd(query, null)
        when (status) {
            errSecSuccess, errSecDuplicateItem -> {
                log.d { "Key stored in Keychain successfully" }
                keyBytes
            }
            else -> throw DatabaseKeyException("Failed to store key in Keychain: $status")
        }
    }

    private fun deleteKeyFromKeychain() = memScoped {
        val query = CFDictionaryCreateMutable(null, 3, null, null)
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, CFBridgingRetain(KEYCHAIN_SERVICE))
        CFDictionarySetValue(query, kSecAttrAccount, CFBridgingRetain(DATABASE_KEY_ALIAS))

        val status = SecItemDelete(query)
        if (status != errSecSuccess && status != errSecItemNotFound) {
            throw DatabaseKeyException("Failed to delete key from Keychain: $status")
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val length = this.length.toInt()
        if (length == 0) return ByteArray(0)

        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            platform.posix.memcpy(
                pinned.addressOf(0),
                this.bytes,
                length.toULong()
            )
        }
        return bytes
    }

    companion object {
        private const val KEYCHAIN_SERVICE = "com.finuts.database"
    }
}

/**
 * Exception thrown when database key operations fail.
 */
class DatabaseKeyException(message: String, cause: Throwable? = null) : Exception(message, cause)

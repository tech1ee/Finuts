package com.finuts.test.fakes

import com.finuts.core.security.DATABASE_KEY_SIZE
import com.finuts.test.BaseTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeDatabaseKeyProviderTest : BaseTest() {

    private lateinit var keyProvider: FakeDatabaseKeyProvider

    @Test
    fun `getOrCreatePassphrase returns key of correct size`() = runTest {
        keyProvider = FakeDatabaseKeyProvider()

        val passphrase = keyProvider.getOrCreatePassphrase()

        assertEquals(DATABASE_KEY_SIZE, passphrase.size)
    }

    @Test
    fun `getOrCreatePassphrase returns same key on subsequent calls`() = runTest {
        keyProvider = FakeDatabaseKeyProvider()

        val first = keyProvider.getOrCreatePassphrase()
        val second = keyProvider.getOrCreatePassphrase()

        assertContentEquals(first, second)
    }

    @Test
    fun `hasExistingKey returns false initially`() {
        keyProvider = FakeDatabaseKeyProvider()

        assertFalse(keyProvider.hasExistingKey())
    }

    @Test
    fun `hasExistingKey returns true after getOrCreatePassphrase`() = runTest {
        keyProvider = FakeDatabaseKeyProvider()

        keyProvider.getOrCreatePassphrase()

        assertTrue(keyProvider.hasExistingKey())
    }

    @Test
    fun `deleteKey removes existing key`() = runTest {
        keyProvider = FakeDatabaseKeyProvider()
        keyProvider.getOrCreatePassphrase()

        keyProvider.deleteKey()

        assertFalse(keyProvider.hasExistingKey())
        assertNull(keyProvider.getStoredKey())
    }

    @Test
    fun `setKey allows setting specific key`() = runTest {
        keyProvider = FakeDatabaseKeyProvider()
        val customKey = ByteArray(DATABASE_KEY_SIZE) { 42 }

        keyProvider.setKey(customKey)
        val retrieved = keyProvider.getOrCreatePassphrase()

        assertContentEquals(customKey, retrieved)
    }

    @Test
    fun `setKey rejects wrong size key`() {
        keyProvider = FakeDatabaseKeyProvider()
        val wrongSizeKey = ByteArray(16) { 1 }

        assertFailsWith<IllegalArgumentException> {
            keyProvider.setKey(wrongSizeKey)
        }
    }

    @Test
    fun `setFailure causes operations to throw`() = runTest {
        keyProvider = FakeDatabaseKeyProvider()
        keyProvider.setFailure()

        assertFailsWith<FakeDatabaseKeyException> {
            keyProvider.getOrCreatePassphrase()
        }
    }

    @Test
    fun `setFailure with custom exception throws that exception`() = runTest {
        keyProvider = FakeDatabaseKeyProvider()
        val customException = IllegalStateException("Custom error")
        keyProvider.setFailure(customException)

        val thrown = assertFailsWith<IllegalStateException> {
            keyProvider.getOrCreatePassphrase()
        }
        assertEquals("Custom error", thrown.message)
    }

    @Test
    fun `clearFailure restores normal operation`() = runTest {
        keyProvider = FakeDatabaseKeyProvider()
        keyProvider.setFailure()
        keyProvider.clearFailure()

        val passphrase = keyProvider.getOrCreatePassphrase()

        assertNotNull(passphrase)
        assertEquals(DATABASE_KEY_SIZE, passphrase.size)
    }

    @Test
    fun `reset clears all state`() = runTest {
        keyProvider = FakeDatabaseKeyProvider()
        keyProvider.getOrCreatePassphrase()
        keyProvider.setFailure()

        keyProvider.reset()

        assertFalse(keyProvider.hasExistingKey())
        // Should not throw after reset
        assertNotNull(keyProvider.getOrCreatePassphrase())
    }

    @Test
    fun `getStoredKey returns copy of key`() = runTest {
        keyProvider = FakeDatabaseKeyProvider()
        keyProvider.getOrCreatePassphrase()

        val stored1 = keyProvider.getStoredKey()
        val stored2 = keyProvider.getStoredKey()

        assertNotNull(stored1)
        assertNotNull(stored2)
        assertContentEquals(stored1, stored2)
        // Verify they are different instances (defensive copy)
        assertTrue(stored1 !== stored2)
    }

    @Test
    fun `deleteKey throws on failure`() = runTest {
        keyProvider = FakeDatabaseKeyProvider()
        keyProvider.setFailure()

        assertFailsWith<FakeDatabaseKeyException> {
            keyProvider.deleteKey()
        }
    }

    @Test
    fun `FakeDatabaseKeyException stores message`() {
        val exception = FakeDatabaseKeyException("test message")

        assertEquals("test message", exception.message)
    }

    @Test
    fun `FakeDatabaseKeyException stores cause`() {
        val cause = RuntimeException("root cause")
        val exception = FakeDatabaseKeyException("wrapper", cause)

        assertEquals("wrapper", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `setKey creates independent copy`() = runTest {
        keyProvider = FakeDatabaseKeyProvider()
        val originalKey = ByteArray(DATABASE_KEY_SIZE) { 42 }
        keyProvider.setKey(originalKey)

        // Modify original key
        originalKey[0] = 99

        // Retrieved key should still be 42
        val retrieved = keyProvider.getOrCreatePassphrase()
        assertEquals(42.toByte(), retrieved[0])
    }
}

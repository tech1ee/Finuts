package com.finuts.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for database encryption constants.
 * Verifies security-critical values are correct.
 */
class DatabaseKeyConstantsTest {

    @Test
    fun `DATABASE_KEY_SIZE is 32 bytes for AES-256`() {
        assertEquals(32, DATABASE_KEY_SIZE)
    }

    @Test
    fun `DATABASE_KEY_ALIAS is properly namespaced`() {
        assertTrue(DATABASE_KEY_ALIAS.startsWith("finuts_"))
        assertTrue(DATABASE_KEY_ALIAS.contains("encryption"))
    }

    @Test
    fun `DATABASE_KEY_ALIAS includes version for future migration support`() {
        assertTrue(DATABASE_KEY_ALIAS.contains("v1"))
    }
}

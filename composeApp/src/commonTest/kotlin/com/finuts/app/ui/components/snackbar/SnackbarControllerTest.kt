package com.finuts.app.ui.components.snackbar

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for SnackbarController logic.
 * Tests the undo snackbar behavior, timing, and callback handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SnackbarControllerTest {

    @Test
    fun `UndoSnackbarState has correct default values`() {
        val state = UndoSnackbarState()

        assertFalse(state.isVisible)
        assertEquals("", state.message)
        assertEquals(5000L, state.durationMs)
    }

    @Test
    fun `UndoSnackbarState can be created with custom message`() {
        val state = UndoSnackbarState(
            isVisible = true,
            message = "Account archived"
        )

        assertTrue(state.isVisible)
        assertEquals("Account archived", state.message)
    }

    @Test
    fun `UndoSnackbarState can be created with custom duration`() {
        val state = UndoSnackbarState(
            isVisible = true,
            message = "Budget deactivated",
            durationMs = 3500L
        )

        assertEquals(3500L, state.durationMs)
    }

    @Test
    fun `SnackbarDurations has correct values for finance app`() {
        // Based on UX research - finance apps need longer durations
        assertEquals(5000L, SnackbarDurations.DELETE_TRANSACTION)
        assertEquals(4000L, SnackbarDurations.ARCHIVE_ACCOUNT)
        assertEquals(3500L, SnackbarDurations.DEACTIVATE_BUDGET)
        assertEquals(5000L, SnackbarDurations.DELETE_CATEGORY)
    }

    @Test
    fun `SnackbarDurations are within acceptable range`() {
        // Snackbar should be visible for 3-6 seconds for undo actions
        val durations = listOf(
            SnackbarDurations.DELETE_TRANSACTION,
            SnackbarDurations.ARCHIVE_ACCOUNT,
            SnackbarDurations.DEACTIVATE_BUDGET,
            SnackbarDurations.DELETE_CATEGORY
        )

        durations.forEach { duration ->
            assertTrue(duration >= 3000L, "Duration $duration should be >= 3000ms")
            assertTrue(duration <= 6000L, "Duration $duration should be <= 6000ms")
        }
    }

    @Test
    fun `UndoSnackbarState dismissed state`() {
        val initialState = UndoSnackbarState(
            isVisible = true,
            message = "Test message"
        )

        val dismissedState = initialState.copy(isVisible = false)

        assertFalse(dismissedState.isVisible)
        assertEquals("Test message", dismissedState.message) // Message preserved
    }

    @Test
    fun `action label should be localized`() = runTest {
        // Default action label for Russian locale
        val actionLabel = SnackbarActionLabels.UNDO_RU
        assertEquals("ОТМЕНИТЬ", actionLabel)
    }

    @Test
    fun `SnackbarType enum has all required types`() {
        val types = SnackbarType.entries

        assertTrue(types.contains(SnackbarType.UNDO))
        assertTrue(types.contains(SnackbarType.SUCCESS))
        assertTrue(types.contains(SnackbarType.ERROR))
        assertEquals(3, types.size)
    }
}

package com.finuts.app.ui.components.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Durations for undo snackbars based on UX research.
 * Finance apps require longer durations due to high-stakes actions.
 */
object SnackbarDurations {
    /** Delete transaction - high stakes, matches Gmail/Google Pay */
    const val DELETE_TRANSACTION = 5000L
    /** Archive account - reversible, medium impact */
    const val ARCHIVE_ACCOUNT = 4000L
    /** Deactivate budget - low impact, standard Material3 */
    const val DEACTIVATE_BUDGET = 3500L
    /** Delete category - may affect multiple transactions */
    const val DELETE_CATEGORY = 5000L
}

/**
 * Localized action labels for snackbar buttons.
 */
object SnackbarActionLabels {
    const val UNDO_RU = "ОТМЕНИТЬ"
    const val UNDO_EN = "UNDO"
}

/**
 * Types of snackbar messages.
 */
enum class SnackbarType {
    UNDO,
    SUCCESS,
    ERROR
}

/**
 * State for undo snackbar.
 */
@Immutable
data class UndoSnackbarState(
    val isVisible: Boolean = false,
    val message: String = "",
    val durationMs: Long = 5000L
)

/**
 * Controller for showing snackbars throughout the app.
 * Provides undo functionality for destructive actions.
 */
@Immutable
class SnackbarController(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    /**
     * Show undo snackbar with custom duration.
     * @param message Message to display
     * @param durationMs Duration in milliseconds (default 5000ms for finance apps)
     * @param onUndo Callback when user taps UNDO
     * @param onTimeout Callback when snackbar times out (commit the action)
     */
    fun showUndoSnackbar(
        message: String,
        durationMs: Long = SnackbarDurations.DELETE_TRANSACTION,
        onUndo: () -> Unit,
        onTimeout: () -> Unit
    ) {
        scope.launch {
            // Dismiss any existing snackbar
            hostState.currentSnackbarData?.dismiss()

            val result = hostState.showSnackbar(
                message = message,
                actionLabel = SnackbarActionLabels.UNDO_RU,
                duration = SnackbarDuration.Indefinite,
                withDismissAction = true
            )

            when (result) {
                SnackbarResult.ActionPerformed -> onUndo()
                SnackbarResult.Dismissed -> onTimeout()
            }
        }

        // Auto-dismiss after duration
        scope.launch {
            delay(durationMs)
            hostState.currentSnackbarData?.dismiss()
        }
    }

    /**
     * Show success message snackbar.
     */
    fun showSuccess(message: String) {
        scope.launch {
            hostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    /**
     * Show error message snackbar.
     */
    fun showError(message: String) {
        scope.launch {
            hostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
        }
    }

    /**
     * Show informational message snackbar.
     * Used for non-critical feedback like learning progress.
     */
    fun showInfo(message: String) {
        scope.launch {
            hostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    /**
     * Get the SnackbarHostState for use in Scaffold.
     */
    val snackbarHostState: SnackbarHostState get() = hostState
}

/**
 * CompositionLocal for accessing SnackbarController throughout the app.
 */
val LocalSnackbarController = staticCompositionLocalOf<SnackbarController> {
    error("SnackbarController not provided. Wrap your content with ProvideSnackbarController.")
}

/**
 * Remember a SnackbarController instance.
 */
@Composable
fun rememberSnackbarController(
    hostState: SnackbarHostState = remember { SnackbarHostState() }
): SnackbarController {
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) {
        SnackbarController(hostState, scope)
    }
}

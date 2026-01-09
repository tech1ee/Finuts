package com.finuts.app.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finuts.app.navigation.Route
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/**
 * Base ViewModel with common patterns:
 * - One-time error events via Channel (not StateFlow - prevents re-showing on recomposition)
 * - Navigation events via Channel
 * - Safe error handling with launch wrapper
 * - Global exception handler to prevent iOS crashes
 */
abstract class BaseViewModel : ViewModel() {

    // Global exception handler for unhandled coroutine exceptions
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("BaseViewModel: Caught unhandled exception: ${throwable.message}")
        throwable.printStackTrace()
        // Send error through channel (non-blocking)
        _errorChannel.trySend(ErrorEvent(throwable.message ?: "Unknown error", throwable))
    }

    // Safe scope with SupervisorJob + exception handler
    protected val safeScope: CoroutineScope by lazy {
        viewModelScope + SupervisorJob() + exceptionHandler
    }

    // One-time error events
    private val _errorChannel = Channel<ErrorEvent>(Channel.BUFFERED)
    val errorEvents: Flow<ErrorEvent> = _errorChannel.receiveAsFlow()

    // Navigation events
    private val _navigationChannel = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationChannel.receiveAsFlow()

    // Snackbar events (for success/info messages)
    private val _snackbarChannel = Channel<SnackbarEvent>(Channel.BUFFERED)
    val snackbarEvents: Flow<SnackbarEvent> = _snackbarChannel.receiveAsFlow()

    /**
     * Send error event to UI
     */
    protected fun sendError(message: String, throwable: Throwable? = null) {
        safeScope.launch {
            _errorChannel.send(ErrorEvent(message, throwable))
        }
    }

    /**
     * Navigate to a route
     */
    protected fun navigateTo(route: Route) {
        safeScope.launch {
            _navigationChannel.send(NavigationEvent.NavigateTo(route))
        }
    }

    /**
     * Navigate back
     */
    protected fun navigateBack() {
        safeScope.launch {
            _navigationChannel.send(NavigationEvent.PopBackStack)
        }
    }

    /**
     * Send snackbar event to UI (success/info messages)
     */
    protected fun sendSnackbar(message: String, type: SnackbarMessageType = SnackbarMessageType.INFO) {
        safeScope.launch {
            _snackbarChannel.send(SnackbarEvent(message, type))
        }
    }

    /**
     * Safe launch with error handling (uses safeScope with exception handler)
     */
    protected fun launchSafe(
        onError: (Throwable) -> Unit = { sendError(it.message ?: "Unknown error", it) },
        block: suspend () -> Unit
    ) {
        safeScope.launch {
            try {
                block()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}

/**
 * Error event for one-time display (snackbar, toast, dialog)
 */
data class ErrorEvent(
    val message: String,
    val throwable: Throwable? = null
)

/**
 * Snackbar message types for user feedback.
 */
enum class SnackbarMessageType {
    SUCCESS,
    INFO,
    ERROR
}

/**
 * Snackbar event for one-time display.
 */
data class SnackbarEvent(
    val message: String,
    val type: SnackbarMessageType = SnackbarMessageType.INFO
)

/**
 * Navigation events for ViewModel -> UI navigation
 */
sealed class NavigationEvent {
    data class NavigateTo(val route: Route) : NavigationEvent()
    data object PopBackStack : NavigationEvent()
}

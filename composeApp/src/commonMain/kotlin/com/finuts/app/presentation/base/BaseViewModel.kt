package com.finuts.app.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finuts.app.navigation.Route
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel with common patterns:
 * - One-time error events via Channel (not StateFlow - prevents re-showing on recomposition)
 * - Navigation events via Channel
 * - Safe error handling with launch wrapper
 */
abstract class BaseViewModel : ViewModel() {

    // One-time error events
    private val _errorChannel = Channel<ErrorEvent>(Channel.BUFFERED)
    val errorEvents: Flow<ErrorEvent> = _errorChannel.receiveAsFlow()

    // Navigation events
    private val _navigationChannel = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationChannel.receiveAsFlow()

    /**
     * Send error event to UI
     */
    protected fun sendError(message: String, throwable: Throwable? = null) {
        viewModelScope.launch {
            _errorChannel.send(ErrorEvent(message, throwable))
        }
    }

    /**
     * Navigate to a route
     */
    protected fun navigateTo(route: Route) {
        viewModelScope.launch {
            _navigationChannel.send(NavigationEvent.NavigateTo(route))
        }
    }

    /**
     * Navigate back
     */
    protected fun navigateBack() {
        viewModelScope.launch {
            _navigationChannel.send(NavigationEvent.PopBackStack)
        }
    }

    /**
     * Safe launch with error handling
     */
    protected fun launchSafe(
        onError: (Throwable) -> Unit = { sendError(it.message ?: "Unknown error", it) },
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
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
 * Navigation events for ViewModel -> UI navigation
 */
sealed class NavigationEvent {
    data class NavigateTo(val route: Route) : NavigationEvent()
    data object PopBackStack : NavigationEvent()
}

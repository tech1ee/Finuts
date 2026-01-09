package com.finuts.app.ui.components.snackbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

/**
 * Provides SnackbarController to the composition tree.
 * Wraps content with Scaffold containing SnackbarHost.
 *
 * Usage:
 * ```
 * ProvideSnackbarController {
 *     // Your app content
 *     val snackbarController = LocalSnackbarController.current
 *     snackbarController.showUndoSnackbar(...)
 * }
 * ```
 */
@Composable
fun ProvideSnackbarController(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val snackbarController = rememberSnackbarController()

    CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(hostState = snackbarController.snackbarHostState)
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                content()
            }
        }
    }
}

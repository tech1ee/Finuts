package com.finuts.app.ui.components.state

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsMotion

/**
 * Animated state content with crossfade transition.
 *
 * Usage:
 * ```
 * AnimatedStateContent(
 *     targetState = uiState,
 *     contentKey = { state -> state::class }
 * ) { state ->
 *     when (state) {
 *         is UiState.Loading -> LoadingScreen()
 *         is UiState.Success -> SuccessScreen(state.data)
 *         is UiState.Error -> ErrorScreen(state.message)
 *     }
 * }
 * ```
 */
@Composable
fun <S> AnimatedStateContent(
    targetState: S,
    modifier: Modifier = Modifier,
    contentKey: (S) -> Any? = { it },
    transitionSpec: AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        fadeIn(animationSpec = tween(FinutsMotion.emphasis)) togetherWith
            fadeOut(animationSpec = tween(FinutsMotion.emphasis))
    },
    content: @Composable (S) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = transitionSpec,
        contentKey = contentKey,
        label = "StateTransition"
    ) { state ->
        content(state)
    }
}

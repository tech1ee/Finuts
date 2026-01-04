package com.finuts.app.ui.modifiers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.finuts.app.theme.FinutsMotion

/**
 * Interactive Modifiers
 *
 * Reusable modifiers for Linear-style interactive feedback:
 * - Press: scale 0.98
 * - Hover: scale 1.01, lift -2dp (desktop only)
 */

/**
 * Adds press scale animation to any composable.
 * On press: scales down to 0.98
 * On release: returns to 1.0
 */
fun Modifier.pressScale(
    onClick: () -> Unit,
    enabled: Boolean = true
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) FinutsMotion.pressScale else 1f,
        animationSpec = FinutsMotion.fastTween(),
        label = "pressScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Adds press scale animation with custom press handling.
 * Use when you need more control over press events.
 */
fun Modifier.interactiveScale(
    isPressed: Boolean
): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) FinutsMotion.pressScale else 1f,
        animationSpec = FinutsMotion.fastTween(),
        label = "interactiveScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Composable for handling press state with tap gestures.
 * Returns the pressed state and a modifier to apply.
 */
@Composable
fun rememberPressState(): Pair<Boolean, Modifier> {
    var isPressed by remember { mutableStateOf(false) }

    val modifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                isPressed = true
                tryAwaitRelease()
                isPressed = false
            }
        )
    }

    return isPressed to modifier
}

/**
 * Adds subtle elevation animation on press.
 * Useful for cards and buttons.
 */
fun Modifier.pressElevation(
    isPressed: Boolean,
    defaultElevation: Float = 1f,
    pressedElevation: Float = 0f
): Modifier = composed {
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) pressedElevation else defaultElevation,
        animationSpec = FinutsMotion.fastTween(),
        label = "pressElevation"
    )

    this.graphicsLayer {
        shadowElevation = elevation
    }
}

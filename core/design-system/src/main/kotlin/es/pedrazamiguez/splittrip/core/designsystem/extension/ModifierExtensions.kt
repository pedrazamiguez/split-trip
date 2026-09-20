package es.pedrazamiguez.splittrip.core.designsystem.extension

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants

private const val SPRING_PRESS_SCALE = 0.98f

/**
 * Applies a subtle Material 3 Expressive spring scale effect on press/release without layout recalculation.
 */
fun Modifier.springPressFeedback(
    interactionSource: InteractionSource
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) SPRING_PRESS_SCALE else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "springPressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Creates a debounced event handler that ignores subsequent invocations within the [debounceInterval].
 */
@Composable
fun debounced(
    debounceInterval: Long = UiConstants.DEFAULT_DEBOUNCE_MS,
    onClick: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    return {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceInterval) {
            lastClickTime = currentTime
            onClick()
        }
    }
}

/**
 * A debounced version of [Modifier.clickable] that prevents rapid double clicks.
 */
fun Modifier.debouncedClickable(
    debounceInterval: Long = UiConstants.DEFAULT_DEBOUNCE_MS,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    enableSpringPress: Boolean = false,
    onClick: () -> Unit
): Modifier = composed {
    val debouncedOnClick = debounced(debounceInterval, onClick)
    if (enableSpringPress) {
        val interactionSource = remember { MutableInteractionSource() }
        springPressFeedback(interactionSource).clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = debouncedOnClick
        )
    } else {
        clickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = debouncedOnClick
        )
    }
}

/**
 * A debounced version of [Modifier.clickable] with custom interaction source and indication.
 */
fun Modifier.debouncedClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    debounceInterval: Long = UiConstants.DEFAULT_DEBOUNCE_MS,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    enableSpringPress: Boolean = false,
    onClick: () -> Unit
): Modifier = composed {
    val debouncedOnClick = debounced(debounceInterval, onClick)
    val modifier = if (enableSpringPress) springPressFeedback(interactionSource) else this
    modifier.clickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = debouncedOnClick
    )
}

/**
 * A debounced version of [Modifier.clickable] with no ripple or click indication.
 */
fun Modifier.debouncedClickableNoRipple(
    debounceInterval: Long = UiConstants.DEFAULT_DEBOUNCE_MS,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    debouncedClickable(
        interactionSource = interactionSource,
        indication = null,
        debounceInterval = debounceInterval,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick
    )
}

/**
 * A debounced version of [Modifier.combinedClickable] that prevents rapid double clicks on its primary onClick event.
 */
@Suppress("LongParameterList", "kotlin:S107")
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.debouncedCombinedClickable(
    debounceInterval: Long = UiConstants.DEFAULT_DEBOUNCE_MS,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    enableSpringPress: Boolean = false,
    onClick: () -> Unit
): Modifier = composed {
    val debouncedOnClick = debounced(debounceInterval, onClick)
    if (enableSpringPress) {
        val interactionSource = remember { MutableInteractionSource() }
        springPressFeedback(interactionSource).combinedClickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            onClick = debouncedOnClick
        )
    } else {
        combinedClickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            onClick = debouncedOnClick
        )
    }
}

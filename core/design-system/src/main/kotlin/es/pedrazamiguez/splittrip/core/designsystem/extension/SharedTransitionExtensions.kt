package es.pedrazamiguez.splittrip.core.designsystem.extension

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.transition.SPRING_DAMPING_RATIO
import es.pedrazamiguez.splittrip.core.designsystem.transition.SPRING_STIFFNESS
import es.pedrazamiguez.splittrip.core.designsystem.transition.TRANSITION_DURATION_MS

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedElementAnimation(
    key: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
): Modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
    with(sharedTransitionScope) {
        this@sharedElementAnimation.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibilityScope,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            boundsTransform = { _, _ ->
                spring(dampingRatio = SPRING_DAMPING_RATIO, stiffness = SPRING_STIFFNESS)
            },
            enter = fadeIn(tween(durationMillis = TRANSITION_DURATION_MS)),
            exit = fadeOut(tween(durationMillis = TRANSITION_DURATION_MS))
        )
    }
} else {
    this
}

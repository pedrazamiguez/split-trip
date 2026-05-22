package es.pedrazamiguez.splittrip.core.designsystem.transition

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

/**
 * Default animation parameters for FAB → Screen container-transform transitions.
 *
 * Both [SharedTransitionSurface] and [fabSharedTransitionModifier] use these values
 * so the source (FAB) and target (Screen) sides of the animation stay in sync.
 */
private const val TRANSITION_DURATION_MS = 300
private const val SPRING_DAMPING_RATIO = 0.8f
private const val SPRING_STIFFNESS = 300f

/**
 * A full-screen [Surface] that participates in a shared-element container-transform animation.
 *
 * Use this as the root of any screen that is the **destination** of a FAB navigation
 * (e.g., AddExpense, AddCashWithdrawal). The FAB (source) side must pass the same
 * [sharedElementKey] via [fabSharedTransitionModifier] or
 * [ExpressiveFab.sharedTransitionKey][es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.ExpressiveFab].
 *
 * If no [LocalSharedTransitionScope] or [LocalAnimatedVisibilityScope] is available
 * (e.g., in a `@Preview`), the component renders as a plain [Surface] — no crash.
 *
 * @param sharedElementKey The key that links this surface to its matching FAB.
 * @param modifier         Additional modifiers applied **before** the shared-bounds modifier.
 * @param color            Background color of the surface.
 * @param content          Screen content.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionSurface(
    sharedElementKey: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit
) {
    val sharedModifier = fabSharedTransitionModifier(sharedElementKey)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .then(sharedModifier),
        color = color,
        content = content
    )
}

/**
 * Builds the [Modifier] for a FAB → Screen container-transform shared transition.
 *
 * This is the **single source of truth** for the animation spec used by both the
 * FAB (source) and the target screen. `ExpressiveFab` and [SharedTransitionSurface]
 * both delegate here so the parameters can never drift out of sync.
 *
 * @param key The shared-element key that pairs source and target.
 * @return A [Modifier] with `sharedBounds` applied, or [Modifier] if the
 *            transition scope is not available (previews, tests).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun fabSharedTransitionModifier(key: String): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    return if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Fit),
                boundsTransform = { _, _ ->
                    spring(dampingRatio = SPRING_DAMPING_RATIO, stiffness = SPRING_STIFFNESS)
                },
                enter = fadeIn(tween(durationMillis = TRANSITION_DURATION_MS)),
                exit = fadeOut(tween(durationMillis = TRANSITION_DURATION_MS))
            )
        }
    } else {
        Modifier
    }
}

/**
 * Builds the [Modifier] for a receipt thumbnail → full-screen receipt viewer shared-element transition.
 *
 * This is used to morph the receipt image smoothly from the detail view/form thumbnail to the
 * full-screen viewer. It uses [Modifier.sharedElement] to transition only the image content,
 * preventing container distortion.
 *
 * @param key The shared-element key that pairs thumbnail and viewer.
 * @return A [Modifier] with `sharedElement` applied, or [Modifier] if the transition scope is not available.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun receiptSharedElementModifier(key: String): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    return if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ ->
                    spring(dampingRatio = SPRING_DAMPING_RATIO, stiffness = SPRING_STIFFNESS)
                }
            )
        }
    } else {
        Modifier
    }
}

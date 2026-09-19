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
internal const val TRANSITION_DURATION_MS = 300
internal const val SPRING_DAMPING_RATIO = 0.8f
internal const val SPRING_STIFFNESS = 300f

/**
 * A full-screen [Surface] that participates in a shared-element container-transform animation.
 *
 * Use this as the root of any screen that is the **destination** of a container navigation
 * (e.g., AddExpense, AddCashWithdrawal, GroupDetail, ExpenseDetail).
 *
 * If no [LocalSharedTransitionScope] or [LocalAnimatedVisibilityScope] is available
 * (e.g., in a `@Preview`), the component renders as a plain [Surface] — no crash.
 *
 * @param sharedElementKey The key that links this surface to its matching source.
 * @param modifier         Additional modifiers applied **before** the shared-bounds modifier.
 * @param resizeMode       Resize mode for the container transform.
 * @param color            Background color of the surface.
 * @param content          Screen content.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionSurface(
    sharedElementKey: String,
    modifier: Modifier = Modifier,
    resizeMode: SharedTransitionScope.ResizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
    color: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit
) {
    val sharedModifier = containerSharedTransitionModifier(
        key = sharedElementKey,
        resizeMode = resizeMode
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .then(sharedModifier),
        color = color,
        content = content
    )
}

/**
 * Builds the [Modifier] for a container-transform shared transition.
 *
 * This is the **single source of truth** for the animation spec used by both
 * sources and targets participating in container transforms.
 *
 * @param key The shared-element key that pairs source and target.
 * @param resizeMode How the content scales during bounds changes.
 * @return A [Modifier] with `sharedBounds` applied, or [Modifier] if the
 *         transition scope is not available (previews, tests).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun containerSharedTransitionModifier(
    key: String,
    resizeMode: SharedTransitionScope.ResizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    return if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = resizeMode,
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
 * Builds the [Modifier] for a FAB → Screen container-transform shared transition.
 *
 * Delegates to [containerSharedTransitionModifier] with [SharedTransitionScope.ResizeMode.scaleToBounds]
 * using [ContentScale.Fit].
 *
 * @param key The shared-element key that pairs source and target.
 * @return A [Modifier] with `sharedBounds` applied, or [Modifier] if the
 *         transition scope is not available (previews, tests).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun fabSharedTransitionModifier(key: String): Modifier =
    containerSharedTransitionModifier(
        key = key,
        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Fit)
    )

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

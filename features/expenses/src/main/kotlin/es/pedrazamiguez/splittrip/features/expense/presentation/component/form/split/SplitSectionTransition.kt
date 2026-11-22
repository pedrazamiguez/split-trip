package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset

private const val MODE_FADE_IN_MS = 180
private const val MODE_FADE_IN_DELAY_MS = 60
private const val MODE_FADE_OUT_MS = 120
private const val SLIDE_OFFSET_DIVISOR = 6
private const val ERROR_FADE_IN_MS = 180
private const val ERROR_FADE_OUT_MS = 120

internal enum class SplitEditorMode {
    SUBUNIT,
    FLAT,
    NONE
}

internal fun resolveSplitEditorMode(
    isSubunitMode: Boolean,
    hasEntitySplits: Boolean,
    hasFlatSplits: Boolean
): SplitEditorMode = when {
    isSubunitMode && hasEntitySplits -> SplitEditorMode.SUBUNIT
    hasFlatSplits -> SplitEditorMode.FLAT
    else -> SplitEditorMode.NONE
}

internal fun AnimatedContentTransitionScope<SplitEditorMode>.splitEditorModeContentTransform(): ContentTransform {
    val slideDirection = when {
        initialState == SplitEditorMode.FLAT && targetState == SplitEditorMode.SUBUNIT -> 1
        initialState == SplitEditorMode.SUBUNIT && targetState == SplitEditorMode.FLAT -> -1
        else -> 0
    }
    val slideSpec = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val contentTransform = if (slideDirection != 0) {
        val enter = fadeIn(tween(MODE_FADE_IN_MS, delayMillis = MODE_FADE_IN_DELAY_MS)) +
            slideInHorizontally(animationSpec = slideSpec) { fullWidth ->
                slideDirection * (fullWidth / SLIDE_OFFSET_DIVISOR)
            }
        val exit = fadeOut(tween(MODE_FADE_OUT_MS)) +
            slideOutHorizontally(animationSpec = slideSpec) { fullWidth ->
                -slideDirection * (fullWidth / SLIDE_OFFSET_DIVISOR)
            }
        enter togetherWith exit
    } else {
        fadeIn(tween(MODE_FADE_IN_MS)) togetherWith fadeOut(tween(MODE_FADE_OUT_MS))
    }

    return contentTransform.using(
        SizeTransform(clip = true) { _, _ ->
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        }
    )
}

internal fun splitErrorEnterTransition(): EnterTransition =
    fadeIn(tween(ERROR_FADE_IN_MS)) + expandVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        expandFrom = Alignment.Top
    )

internal fun splitErrorExitTransition(): ExitTransition =
    fadeOut(tween(ERROR_FADE_OUT_MS)) + shrinkVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        shrinkTowards = Alignment.Top
    )

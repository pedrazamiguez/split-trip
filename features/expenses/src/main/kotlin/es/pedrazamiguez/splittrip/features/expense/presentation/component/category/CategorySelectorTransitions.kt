package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

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
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment

internal object CategorySelectorTransitions {
    private const val FADE_IN_MS = 220
    private const val FADE_IN_DELAY_MS = 60
    private const val FADE_OUT_MS = 150

    fun trayEnterTransition(): EnterTransition =
        fadeIn(tween(FADE_IN_MS)) + expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            expandFrom = Alignment.Top
        )

    fun trayExitTransition(): ExitTransition =
        fadeOut(tween(FADE_OUT_MS)) + shrinkVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            shrinkTowards = Alignment.Top
        )

    fun AnimatedContentTransitionScope<*>.trayContentTransform(): ContentTransform =
        (
            fadeIn(tween(FADE_IN_MS, delayMillis = FADE_IN_DELAY_MS))
                togetherWith fadeOut(tween(FADE_OUT_MS))
            ).using(
            SizeTransform(clip = false) { _, _ ->
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            }
        )
}

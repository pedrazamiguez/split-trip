package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntSize

internal object CategorySelectorTransitions {
    private const val FADE_IN_MS = 220
    private const val FADE_IN_DELAY_MS = 60
    private const val FADE_OUT_MS = 150
    private const val BADGE_FADE_MS = 180

    fun trayResizeSpec(): FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun badgeEnterTransition(): EnterTransition =
        fadeIn(tween(BADGE_FADE_MS)) + expandHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            expandFrom = Alignment.Start
        )

    fun badgeExitTransition(): ExitTransition =
        fadeOut(tween(FADE_OUT_MS)) + shrinkHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            shrinkTowards = Alignment.Start
        )

    fun AnimatedContentTransitionScope<*>.badgeCountTransform(): ContentTransform =
        (fadeIn(tween(BADGE_FADE_MS)) togetherWith fadeOut(tween(FADE_OUT_MS)))
            .using(SizeTransform(clip = false))

    fun trayEnterTransition(): EnterTransition =
        fadeIn(tween(FADE_IN_MS)) + expandVertically(
            animationSpec = trayResizeSpec(),
            expandFrom = Alignment.Top
        )

    fun trayExitTransition(): ExitTransition =
        fadeOut(tween(FADE_OUT_MS)) + shrinkVertically(
            animationSpec = trayResizeSpec(),
            shrinkTowards = Alignment.Top
        )

    fun AnimatedContentTransitionScope<*>.trayContentTransform(): ContentTransform =
        (
            fadeIn(tween(FADE_IN_MS, delayMillis = FADE_IN_DELAY_MS))
                togetherWith fadeOut(tween(FADE_OUT_MS))
            ).using(
            SizeTransform(clip = false) { _, _ ->
                trayResizeSpec()
            }
        )
}

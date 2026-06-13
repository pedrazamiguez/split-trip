package es.pedrazamiguez.splittrip.core.designsystem.transition

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

/**
 * Centralised transition specs for within-tab navigation.
 *
 * Tab switching (bottom nav) is instant — each tab has its own [NavHost],
 * and visibility is toggled by `if (isSelected)`. These specs only apply
 * to forward/back navigation **inside** a single tab.
 */
object NavTransitionDefaults {

    // ── Within-tab NavHost content transitions ──────────────────────────

    /** Duration for enter / pop-enter content transitions. */
    private const val CONTENT_ENTER_DURATION_MS = 300

    /** Duration for exit / pop-exit content transitions (shorter → old screen disappears fast). */
    private const val CONTENT_EXIT_DURATION_MS = 150

    val contentEnterTransition: EnterTransition =
        slideInHorizontally(
            animationSpec = tween(durationMillis = CONTENT_ENTER_DURATION_MS)
        ) { it } + fadeIn(animationSpec = tween(durationMillis = CONTENT_ENTER_DURATION_MS))

    val contentExitTransition: ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(durationMillis = CONTENT_EXIT_DURATION_MS)
        ) { -it / 3 } + fadeOut(animationSpec = tween(durationMillis = CONTENT_EXIT_DURATION_MS))

    val contentPopEnterTransition: EnterTransition =
        slideInHorizontally(
            animationSpec = tween(durationMillis = CONTENT_ENTER_DURATION_MS)
        ) { -it / 3 } + fadeIn(animationSpec = tween(durationMillis = CONTENT_ENTER_DURATION_MS))

    val contentPopExitTransition: ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(durationMillis = CONTENT_EXIT_DURATION_MS)
        ) { it } + fadeOut(animationSpec = tween(durationMillis = CONTENT_EXIT_DURATION_MS))

    // ── Profile / Edit Profile vertical transitions (Modal-like) ──────

    val modalEnterTransition: EnterTransition =
        slideInVertically(
            animationSpec = tween(durationMillis = CONTENT_ENTER_DURATION_MS)
        ) { it } + fadeIn(animationSpec = tween(durationMillis = CONTENT_ENTER_DURATION_MS))

    val modalExitTransition: ExitTransition =
        slideOutVertically(
            animationSpec = tween(durationMillis = CONTENT_EXIT_DURATION_MS)
        ) { -it / 3 } + fadeOut(animationSpec = tween(durationMillis = CONTENT_EXIT_DURATION_MS))

    val modalPopEnterTransition: EnterTransition =
        slideInVertically(
            animationSpec = tween(durationMillis = CONTENT_ENTER_DURATION_MS)
        ) { -it / 3 } + fadeIn(animationSpec = tween(durationMillis = CONTENT_ENTER_DURATION_MS))

    val modalPopExitTransition: ExitTransition =
        slideOutVertically(
            animationSpec = tween(durationMillis = CONTENT_EXIT_DURATION_MS)
        ) { it } + fadeOut(animationSpec = tween(durationMillis = CONTENT_EXIT_DURATION_MS))

    // ── Top bar AnimatedContent transitions ─────────────────────────────

    /** Duration for the top bar crossfade (symmetric in/out). */
    private const val TOP_BAR_CROSSFADE_DURATION_MS = 250

    val topBarEnterTransition: EnterTransition =
        fadeIn(animationSpec = tween(durationMillis = TOP_BAR_CROSSFADE_DURATION_MS))

    val topBarExitTransition: ExitTransition =
        fadeOut(animationSpec = tween(durationMillis = TOP_BAR_CROSSFADE_DURATION_MS))

    /**
     * Size transform for the top bar [AnimatedContent].
     *
     * `clip = false` lets both old/new content render fully during the crossfade
     * while the container height animates (e.g., status-bar spacer → TopAppBar).
     * The Scaffold recalculates `innerPadding.calculateTopPadding()` on each frame,
     * producing a smooth content-area shift.
     */
    val topBarSizeTransform: SizeTransform =
        SizeTransform(clip = false) { _, _ ->
            tween(durationMillis = TOP_BAR_CROSSFADE_DURATION_MS)
        }
}

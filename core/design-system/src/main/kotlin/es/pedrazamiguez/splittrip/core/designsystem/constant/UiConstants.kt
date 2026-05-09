package es.pedrazamiguez.splittrip.core.designsystem.constant

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object UiConstants {
    const val NAV_FEEDBACK_DELAY = 200L
    const val SCROLL_POSITION_DEBOUNCE_MS = 300L

    /**
     * Height reserved for the [WizardNavigationBar] overlay.
     *
     * Calculated from GradientButton height (56 dp) + vertical padding (12 dp × 2).
     * All wizard orchestrators use this value as bottom content padding so that
     * the last scroll items are never hidden behind the fixed-bottom nav bar.
     */
    val WIZARD_NAV_BAR_HEIGHT: Dp = 80.dp

    /**
     * Delay (ms) before auto-advancing to the next wizard step after a
     * selection-only interaction (chip tap, radio button).
     * Allows the selection animation to settle before the step transition fires.
     */
    const val WIZARD_AUTO_ADVANCE_DELAY_MS = 300L

    /**
     * Delay (ms) before showing a loading indicator (e.g. shimmer skeleton).
     * If data arrives within this window, the loading UI is never shown at all,
     * preventing the "flash of loading state" flicker.
     */
    const val LOADING_SHOW_DELAY_MS = 150L

    /**
     * Minimum time (ms) a loading indicator stays visible once it appears.
     * Prevents the loading UI from flashing for just a frame or two when
     * data arrives shortly after the show delay expired.
     */
    const val LOADING_MIN_DISPLAY_TIME_MS = 500L

    /**
     * Duration (ms) of the crossfade transition from the branded splash screen
     * to the resolved start destination (login, onboarding, or main).
     */
    const val SPLASH_CROSSFADE_DURATION_MS = 400
}

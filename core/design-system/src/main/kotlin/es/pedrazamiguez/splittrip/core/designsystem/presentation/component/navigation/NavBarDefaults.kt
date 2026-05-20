package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.navigation

import androidx.compose.ui.unit.dp

/**
 * Dimension tokens for [FloatingNavigationBar].
 *
 * [BottomPadding] is the minimum design gap between the pill and the bottom of the window.
 * At runtime, `WindowInsets.navigationBars` is added on top so the pill always floats
 * above the system navigation bar regardless of nav mode (gesture / 2-button / 3-button).
 */
internal object NavBarDefaults {
    val ItemWidth = 76.dp
    val BarHeight = 64.dp
    const val INDICATOR_CORNER_RADIUS = 50
    val HorizontalPadding = 20.dp
    val BottomPadding = 28.dp
    val InnerHorizontalPadding = 12.dp
    val InnerVerticalPadding = 8.dp
    val ShadowElevation = 12.dp
}

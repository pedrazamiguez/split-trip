package es.pedrazamiguez.splittrip.features.main.presentation.component.navbar

import androidx.compose.ui.unit.dp

/**
 * Constants for the floating bottom navigation bar dimensions.
 *
 * [BottomPadding] is the **minimum** design gap between the pill and the bottom of the window.
 * At runtime, `WindowInsets.navigationBars` is added on top of it so the pill always floats
 * above the system navigation bar regardless of navigation mode (gesture / 2-button / 3-button).
 */
internal object NavBarDefaults {
    val ItemWidth = 76.dp
    val BarHeight = 64.dp
    val BarCornerRadius = 32.dp
    const val INDICATOR_CORNER_RADIUS = 50
    val HorizontalPadding = 20.dp
    val BottomPadding = 28.dp
    val InnerHorizontalPadding = 12.dp
    val InnerVerticalPadding = 8.dp
    val ShadowElevation = 12.dp
    val TonalElevation = 2.dp
}

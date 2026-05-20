package es.pedrazamiguez.splittrip.core.designsystem.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Minimal display contract for a single item in [FloatingNavigationBar].
 *
 * Decoupled from navigation wiring ([NavigationProvider]) so that any feature
 * can display the floating bar without declaring nav graphs or routes.
 */
interface FloatingNavTab {

    /** Stable identifier for this tab — used to determine the selected item. */
    val id: String

    @Composable
    fun Icon(isSelected: Boolean, tint: Color = Color.Unspecified)

    @Composable
    fun getLabel(): String
}

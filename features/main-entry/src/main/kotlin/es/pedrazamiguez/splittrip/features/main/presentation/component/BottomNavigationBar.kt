package es.pedrazamiguez.splittrip.features.main.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.navigation.FloatingNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction

/**
 * Main-tab bottom navigation bar.
 *
 * A thin wrapper around the reusable [FloatingNavigationBar] from the design system.
 * [NavigationProvider] extends [FloatingNavTab], so the list can be passed directly.
 */
@Composable
fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    selectedRoute: String = "",
    onTabSelected: (String) -> Unit = {},
    items: List<NavigationProvider> = emptyList(),
    mainAction: MainAction? = null,
    hazeState: HazeState? = null
) {
    FloatingNavigationBar(
        modifier = modifier,
        selectedId = selectedRoute,
        onTabSelected = onTabSelected,
        items = items,
        mainAction = mainAction,
        hazeState = hazeState
    )
}

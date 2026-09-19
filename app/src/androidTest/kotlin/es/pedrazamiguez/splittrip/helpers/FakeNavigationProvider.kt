package es.pedrazamiguez.splittrip.helpers

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider

/**
 * A minimal [NavigationProvider] for instrumentation tests.
 *
 * Renders plain [Text] instead of real feature composables, so there are
 * no Koin / ViewModel dependencies to satisfy.
 */
class FakeNavigationProvider(
    override val route: String,
    override val order: Int,
    override val requiresSelectedGroup: Boolean = false,
    private val label: String = route,
    private val subRoutes: List<String> = emptyList()
) : NavigationProvider {

    @Composable
    override fun Icon(isSelected: Boolean, tint: Color) {
        // No-op icon for tests
    }

    @Composable
    override fun getLabel(): String = label

    override fun buildGraph(builder: NavGraphBuilder) {
        builder.composable(route) {
            Text("Content: $label")
        }
        for (subRoute in subRoutes) {
            builder.composable(subRoute) {
                Text("Content: $label - $subRoute")
            }
        }
    }
}

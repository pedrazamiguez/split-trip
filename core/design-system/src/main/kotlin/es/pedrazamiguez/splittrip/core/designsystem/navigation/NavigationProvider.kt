package es.pedrazamiguez.splittrip.core.designsystem.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder

interface NavigationProvider : FloatingNavTab {

    val route: String

    val order: Int

    val requiresSelectedGroup: Boolean

    // Satisfy FloatingNavTab.id via the navigation route — no change required in implementations.
    override val id: String get() = route

    @Composable
    override fun Icon(isSelected: Boolean, tint: Color)

    @Composable
    override fun getLabel(): String

    fun buildGraph(builder: NavGraphBuilder)
}

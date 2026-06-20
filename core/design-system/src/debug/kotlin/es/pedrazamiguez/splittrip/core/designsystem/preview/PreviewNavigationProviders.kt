package es.pedrazamiguez.splittrip.core.designsystem.preview

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.BasketFilled
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.UserFilled
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Basket
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Scale
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.User
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.NavigationBarIcon

/**
 * Preview data for navigation-related components.
 *
 * This object provides sample [NavigationProvider] implementations for use in Compose previews.
 * These are not production providers but simplified versions for preview purposes.
 */
object PreviewNavigationProviders {

    val groups: NavigationProvider = createProvider(
        route = Routes.GROUPS,
        order = 10,
        requiresSelectedGroup = false,
        labelResId = R.string.nav_groups,
        selectedIcon = TablerIcons.Outline.UsersGroup,
        unselectedIcon = TablerIcons.Outline.UsersGroup
    )

    val balances: NavigationProvider = createProvider(
        route = Routes.BALANCES,
        order = 20,
        requiresSelectedGroup = true,
        labelResId = R.string.nav_balances,
        selectedIcon = TablerIcons.Outline.Scale,
        unselectedIcon = TablerIcons.Outline.Scale
    )

    val expenses: NavigationProvider = createProvider(
        route = Routes.EXPENSES,
        order = 50,
        requiresSelectedGroup = true,
        labelResId = R.string.nav_expenses,
        selectedIcon = TablerIcons.Filled.BasketFilled,
        unselectedIcon = TablerIcons.Outline.Basket
    )

    val profile: NavigationProvider = createProvider(
        route = Routes.PROFILE,
        order = 90,
        requiresSelectedGroup = false,
        labelResId = R.string.nav_profile,
        selectedIcon = TablerIcons.Filled.UserFilled,
        unselectedIcon = TablerIcons.Outline.User
    )

    /**
     * A minimal set of navigation items (Groups, Profile) for compact previews.
     */
    val minimal: List<NavigationProvider> = listOf(
        groups,
        profile
    )

    /**
     * A full set of navigation items representing the main app navigation.
     */
    val full: List<NavigationProvider> = listOf(
        groups,
        balances,
        expenses,
        profile
    )

    private fun createProvider(
        route: String,
        order: Int,
        requiresSelectedGroup: Boolean,
        @StringRes
        labelResId: Int,
        selectedIcon: ImageVector,
        unselectedIcon: ImageVector
    ): NavigationProvider = object : NavigationProvider {
        override val route: String = route
        override val order: Int = order
        override val requiresSelectedGroup: Boolean = requiresSelectedGroup

        @Composable
        override fun Icon(isSelected: Boolean, tint: Color) = NavigationBarIcon(
            icon = if (isSelected) selectedIcon else unselectedIcon,
            contentDescription = getLabel(),
            isSelected = isSelected,
            tint = tint
        )

        @Composable
        override fun getLabel(): String = stringResource(labelResId)

        override fun buildGraph(builder: NavGraphBuilder) {
            // No-op for previews
        }
    }
}

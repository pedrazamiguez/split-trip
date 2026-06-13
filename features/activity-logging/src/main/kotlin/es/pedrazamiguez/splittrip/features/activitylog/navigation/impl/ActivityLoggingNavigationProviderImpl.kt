package es.pedrazamiguez.splittrip.features.activitylog.navigation.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.ClockFilled
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Clock
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.NavigationBarIcon
import es.pedrazamiguez.splittrip.features.activitylog.R
import es.pedrazamiguez.splittrip.features.activitylog.navigation.activityLoggingGraph

class ActivityLoggingNavigationProviderImpl(
    override val route: String = Routes.ACTIVITY_LOGGING,
    override val requiresSelectedGroup: Boolean = true,
    override val order: Int = 70
) : NavigationProvider {

    @Composable
    override fun Icon(isSelected: Boolean, tint: Color) = NavigationBarIcon(
        icon = if (isSelected) TablerIcons.Filled.ClockFilled else TablerIcons.Outline.Clock,
        contentDescription = getLabel(),
        isSelected = isSelected,
        tint = tint
    )

    @Composable
    override fun getLabel(): String = stringResource(R.string.activity_logging_title)

    override fun buildGraph(builder: NavGraphBuilder) {
        builder.activityLoggingGraph()
    }
}

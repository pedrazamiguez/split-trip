package es.pedrazamiguez.splittrip.features.activitylog.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.activitylog.presentation.feature.ActivityLoggingFeature

fun NavGraphBuilder.activityLoggingGraph() {
    composable(route = Routes.ACTIVITY_LOGGING) {
        ActivityLoggingFeature()
    }
}

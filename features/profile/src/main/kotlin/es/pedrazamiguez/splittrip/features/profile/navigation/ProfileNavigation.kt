package es.pedrazamiguez.splittrip.features.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.profile.presentation.feature.EditProfileFeature
import es.pedrazamiguez.splittrip.features.profile.presentation.feature.ProfileFeature

fun NavGraphBuilder.profileGraph() {
    composable(route = Routes.PROFILE) {
        ProfileFeature()
    }
    composable(route = Routes.EDIT_PROFILE) {
        EditProfileFeature()
    }
}

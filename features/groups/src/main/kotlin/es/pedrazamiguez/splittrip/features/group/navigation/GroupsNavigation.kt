package es.pedrazamiguez.splittrip.features.group.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedComposable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.group.presentation.feature.CreateGroupFeature
import es.pedrazamiguez.splittrip.features.group.presentation.feature.EditGroupFeature
import es.pedrazamiguez.splittrip.features.group.presentation.feature.GroupDetailFeature
import es.pedrazamiguez.splittrip.features.group.presentation.feature.GroupsFeature

fun NavGraphBuilder.groupsGraph() {
    sharedComposable(Routes.GROUPS) {
        GroupsFeature()
    }
    sharedComposable(Routes.CREATE_GROUP) {
        val navController = LocalTabNavController.current
        CreateGroupFeature(
            onCreateGroupSuccess = {
                navController.popBackStack()
            }
        )
    }
    sharedComposable(
        route = Routes.GROUP_DETAIL,
        arguments = listOf(
            navArgument("groupId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val groupId = backStackEntry.arguments?.getString("groupId") ?: return@sharedComposable
        GroupDetailFeature(groupId = groupId)
    }
    sharedComposable(
        route = Routes.EDIT_GROUP,
        arguments = listOf(
            navArgument("groupId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val groupId = backStackEntry.arguments?.getString("groupId") ?: return@sharedComposable
        EditGroupFeature(groupId = groupId)
    }
}

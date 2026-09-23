package es.pedrazamiguez.splittrip.features.group.navigation

import android.app.Activity
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import es.pedrazamiguez.splittrip.core.designsystem.ad.InterstitialAdManager
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedComposable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.group.presentation.feature.CreateEditGroupFeature
import es.pedrazamiguez.splittrip.features.group.presentation.feature.GroupDetailFeature
import es.pedrazamiguez.splittrip.features.group.presentation.feature.GroupsFeature
import org.koin.compose.getKoin

fun NavGraphBuilder.groupsGraph() {
    sharedComposable(Routes.GROUPS) {
        GroupsFeature()
    }
    sharedComposable(Routes.CREATE_GROUP) {
        val navController = LocalTabNavController.current
        val context = LocalContext.current
        val koin = getKoin()
        val interstitialAdManager = remember(koin) { koin.get<InterstitialAdManager>() }
        CreateEditGroupFeature(
            groupId = null,
            onSuccess = {
                val activity = context as? Activity
                if (activity != null) {
                    interstitialAdManager.onActionCompleted(activity) {
                        navController.popBackStack()
                    }
                } else {
                    navController.popBackStack()
                }
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
        val navController = LocalTabNavController.current
        val context = LocalContext.current
        val koin = getKoin()
        val interstitialAdManager = remember(koin) { koin.get<InterstitialAdManager>() }
        CreateEditGroupFeature(
            groupId = groupId,
            onSuccess = {
                val activity = context as? Activity
                if (activity != null) {
                    interstitialAdManager.onActionCompleted(activity) {
                        navController.popBackStack()
                    }
                } else {
                    navController.popBackStack()
                }
            }
        )
    }
}

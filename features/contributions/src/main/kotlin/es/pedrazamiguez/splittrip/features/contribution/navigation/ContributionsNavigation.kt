package es.pedrazamiguez.splittrip.features.contribution.navigation

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
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes.CONTRIBUTION_DETAIL_ARG_CONTRIBUTION_ID
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes.CONTRIBUTION_DETAIL_ARG_GROUP_ID
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes.CONTRIBUTION_WIZARD_ARG_CONTRIBUTION_ID
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes.CONTRIBUTION_WIZARD_ARG_GROUP_ID
import es.pedrazamiguez.splittrip.features.contribution.presentation.feature.AddContributionFeature
import es.pedrazamiguez.splittrip.features.contribution.presentation.feature.ContributionDetailFeature
import org.koin.compose.getKoin

fun NavGraphBuilder.contributionsGraph() {
    sharedComposable(
        route = Routes.CONTRIBUTION_WIZARD,
        arguments = listOf(
            navArgument(CONTRIBUTION_WIZARD_ARG_GROUP_ID) { type = NavType.StringType },
            navArgument(CONTRIBUTION_WIZARD_ARG_CONTRIBUTION_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue =
                    null
            }
        )
    ) { backStackEntry ->
        val navController = LocalTabNavController.current
        val context = LocalContext.current
        val koin = getKoin()
        val interstitialAdManager = remember(koin) { koin.get<InterstitialAdManager>() }
        val groupId = backStackEntry.arguments?.getString(CONTRIBUTION_WIZARD_ARG_GROUP_ID)
        val contributionId = backStackEntry.arguments?.getString(CONTRIBUTION_WIZARD_ARG_CONTRIBUTION_ID)

        AddContributionFeature(
            groupId = groupId ?: "",
            contributionId = contributionId,
            onContributionSuccess = {
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
        route = Routes.CONTRIBUTION_DETAIL,
        arguments = listOf(
            navArgument(CONTRIBUTION_DETAIL_ARG_GROUP_ID) { type = NavType.StringType },
            navArgument(CONTRIBUTION_DETAIL_ARG_CONTRIBUTION_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val groupId = backStackEntry.arguments?.getString(CONTRIBUTION_DETAIL_ARG_GROUP_ID) ?: return@sharedComposable
        val contributionId =
            backStackEntry.arguments?.getString(CONTRIBUTION_DETAIL_ARG_CONTRIBUTION_ID) ?: return@sharedComposable
        ContributionDetailFeature(
            groupId = groupId,
            contributionId = contributionId
        )
    }
}

package es.pedrazamiguez.splittrip.features.withdrawal.navigation

import android.app.Activity
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import es.pedrazamiguez.splittrip.core.designsystem.ad.InterstitialAdManager
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedComposable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.feature.AddCashWithdrawalFeature
import org.koin.compose.getKoin

fun NavGraphBuilder.withdrawalsGraph() {
    sharedComposable(route = Routes.ADD_CASH_WITHDRAWAL) {
        val navController = LocalTabNavController.current
        val context = LocalContext.current
        val koin = getKoin()
        val interstitialAdManager = remember(koin) { koin.get<InterstitialAdManager>() }
        AddCashWithdrawalFeature(
            onWithdrawalSuccess = {
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

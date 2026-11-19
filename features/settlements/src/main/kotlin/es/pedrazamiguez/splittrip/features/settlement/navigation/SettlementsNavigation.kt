package es.pedrazamiguez.splittrip.features.settlement.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedComposable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.settlement.presentation.feature.DisputeSettlementFeature
import es.pedrazamiguez.splittrip.features.settlement.presentation.feature.GroupSettlementOverviewFeature
import es.pedrazamiguez.splittrip.features.settlement.presentation.feature.YourBalanceFeature

fun NavGraphBuilder.settlementsGraph() {
    sharedComposable(route = Routes.YOUR_BALANCE) {
        YourBalanceFeature()
    }
    sharedComposable(route = Routes.GROUP_SETTLEMENT_OVERVIEW) { backStackEntry ->
        val groupId = backStackEntry.arguments?.getString("groupId") ?: return@sharedComposable
        GroupSettlementOverviewFeature(groupId = groupId)
    }
    sharedComposable(route = Routes.DISPUTE_SETTLEMENT) { backStackEntry ->
        val groupId = backStackEntry.arguments
            ?.getString(Routes.DISPUTE_SETTLEMENT_ARG_GROUP_ID)
            ?.let(Uri::decode)
            ?: return@sharedComposable
        val settlementId = backStackEntry.arguments
            ?.getString(Routes.DISPUTE_SETTLEMENT_ARG_SETTLEMENT_ID)
            ?.let(Uri::decode)
            ?: return@sharedComposable
        DisputeSettlementFeature(
            groupId = groupId,
            settlementId = settlementId
        )
    }
}

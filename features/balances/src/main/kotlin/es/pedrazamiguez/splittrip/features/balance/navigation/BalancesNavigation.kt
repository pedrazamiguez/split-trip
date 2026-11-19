package es.pedrazamiguez.splittrip.features.balance.navigation

import androidx.navigation.NavGraphBuilder
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedComposable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.balance.presentation.feature.BalancesFeature

fun NavGraphBuilder.balancesGraph() {
    sharedComposable(route = Routes.BALANCES) {
        BalancesFeature()
    }
}

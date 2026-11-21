package es.pedrazamiguez.splittrip.features.balance.navigation.impl

import androidx.navigation.NavGraphBuilder
import es.pedrazamiguez.splittrip.core.designsystem.navigation.TabGraphContributor
import es.pedrazamiguez.splittrip.features.balance.navigation.spendingInsightsGraph

/**
 * Contributes the unified [spendingInsightsGraph] into the Expenses tab's NavHost
 * without introducing a compile-time dependency between `:features:expenses` and `:features:balances`.
 */
class SpendingInsightsTabGraphContributorImpl : TabGraphContributor {
    override fun contributeGraph(builder: NavGraphBuilder) {
        builder.spendingInsightsGraph()
    }
}

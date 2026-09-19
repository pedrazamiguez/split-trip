package es.pedrazamiguez.splittrip.features.settlement.navigation.impl

import androidx.navigation.NavGraphBuilder
import es.pedrazamiguez.splittrip.core.designsystem.navigation.TabGraphContributor
import es.pedrazamiguez.splittrip.features.settlement.navigation.settlementsGraph

/**
 * Contributes the Your Position route into the host tab's [NavHost].
 *
 * Registered via Koin as a [TabGraphContributor] so the balances tab can
 * discover and include this graph at runtime without a compile-time dependency
 * on the `:features:settlements` module.
 */
class SettlementsTabGraphContributorImpl : TabGraphContributor {
    override fun contributeGraph(builder: NavGraphBuilder) {
        builder.settlementsGraph()
    }
}

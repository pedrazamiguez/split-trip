package es.pedrazamiguez.splittrip.features.expense.navigation.impl

import androidx.navigation.NavGraphBuilder
import es.pedrazamiguez.splittrip.core.designsystem.navigation.TabGraphContributor
import es.pedrazamiguez.splittrip.features.expense.navigation.expenseDetailGraph

class ExpenseDetailTabGraphContributorImpl : TabGraphContributor {
    override fun contributeGraph(builder: NavGraphBuilder) {
        builder.expenseDetailGraph()
    }
}

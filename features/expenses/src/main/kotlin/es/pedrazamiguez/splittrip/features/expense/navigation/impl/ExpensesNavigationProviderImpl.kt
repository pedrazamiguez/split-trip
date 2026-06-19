package es.pedrazamiguez.splittrip.features.expense.navigation.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.BasketFilled
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Basket
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.NavigationBarIcon
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.navigation.expensesGraph

class ExpensesNavigationProviderImpl(
    override val route: String = Routes.EXPENSES,
    override val requiresSelectedGroup: Boolean = true,
    override val order: Int = 50
) : NavigationProvider {

    @Composable
    override fun Icon(isSelected: Boolean, tint: Color) = NavigationBarIcon(
        icon = if (isSelected) TablerIcons.Filled.BasketFilled else TablerIcons.Outline.Basket,
        contentDescription = getLabel(),
        isSelected = isSelected,
        tint = tint
    )

    @Composable
    override fun getLabel(): String = stringResource(R.string.expenses_title)

    override fun buildGraph(builder: NavGraphBuilder) {
        builder.expensesGraph()
    }
}

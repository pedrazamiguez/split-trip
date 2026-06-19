package es.pedrazamiguez.splittrip.features.subunit.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.SquareRoundedPlus
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.screen.CREATE_EDIT_SUBUNIT_SHARED_ELEMENT_KEY

class SubunitManagementScreenUiProviderImpl(override val route: String = Routes.MANAGE_SUBUNITS) : ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val navController = LocalTabNavController.current
        DynamicTopAppBar(
            title = stringResource(R.string.subunit_manage_title),
            onBack = { navController.popBackStack() }
        )
    }

    override val mainAction: MainAction?
        @Composable
        get() {
            val navController = LocalTabNavController.current
            val backStackEntry by navController.currentBackStackEntryAsState()
            val groupId = backStackEntry?.arguments?.getString("groupId") ?: return null
            return MainAction(
                icon = TablerIcons.Outline.SquareRoundedPlus,
                contentDescription = stringResource(R.string.subunit_create),
                onClick = { navController.navigate(Routes.createEditSubunitRoute(groupId)) },
                sharedTransitionKey = CREATE_EDIT_SUBUNIT_SHARED_ELEMENT_KEY
            )
        }
}

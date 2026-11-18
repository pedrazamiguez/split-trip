package es.pedrazamiguez.splittrip.features.subunit.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.CreateEditSubunitViewModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.CreateEditSubunitUiEvent
import org.koin.androidx.compose.koinViewModel

class CreateEditSubunitScreenUiProviderImpl(override val route: String = Routes.CREATE_EDIT_SUBUNIT) :
    ScreenUiProvider {

    override val showBottomBar: Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val navController = LocalTabNavController.current
        val backStackEntry = navController.currentBackStackEntry
        val vm: CreateEditSubunitViewModel? = backStackEntry?.let {
            koinViewModel(viewModelStoreOwner = it)
        }

        DynamicTopAppBar(
            title = stringResource(R.string.subunit_create_edit_title),
            onBack = { vm?.onEvent(CreateEditSubunitUiEvent.PreviousStep) ?: navController.popBackStack() },
            onBackLongPress = { vm?.onEvent(CreateEditSubunitUiEvent.CloseWizard) }
        )
    }
}

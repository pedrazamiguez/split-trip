package es.pedrazamiguez.splittrip.features.group.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.InfoCircle
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.CreateEditGroupViewModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import org.koin.androidx.compose.koinViewModel

class EditGroupScreenUiProviderImpl(override val route: String = Routes.EDIT_GROUP) : ScreenUiProvider {

    override val showBottomBar: Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val navController = LocalTabNavController.current
        val backStackEntry = navController.currentBackStackEntry
        val vm: CreateEditGroupViewModel? = backStackEntry?.let {
            koinViewModel(viewModelStoreOwner = it)
        }

        DynamicTopAppBar(
            title = stringResource(R.string.group_edit_title),
            onBack = { vm?.onEvent(CreateEditGroupUiEvent.PreviousStep) {} ?: navController.popBackStack() },
            onBackLongPress = { vm?.onEvent(CreateEditGroupUiEvent.CloseWizard) {} },
            actions = {
                IconButton(
                    onClick = { vm?.onEvent(CreateEditGroupUiEvent.ToggleStepInfoSheet(visible = true)) {} }
                ) {
                    Icon(
                        imageVector = TablerIcons.Outline.InfoCircle,
                        contentDescription = stringResource(DesignSystemR.string.wizard_step_info_action)
                    )
                }
            }
        )
    }
}

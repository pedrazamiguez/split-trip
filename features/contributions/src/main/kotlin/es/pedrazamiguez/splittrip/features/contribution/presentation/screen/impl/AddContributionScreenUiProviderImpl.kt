package es.pedrazamiguez.splittrip.features.contribution.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.InfoCircle
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.features.contribution.R
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.AddContributionViewModel
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.event.AddContributionUiEvent
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.androidx.compose.koinViewModel

class AddContributionScreenUiProviderImpl(override val route: String = Routes.CONTRIBUTION_WIZARD) :
    ScreenUiProvider {

    override val showBottomBar: Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val navController = LocalTabNavController.current
        val sharedViewModel: SharedViewModel = koinViewModel(
            viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
        )
        val groupName by sharedViewModel.selectedGroupName.collectAsStateWithLifecycle()

        val backStackEntry = navController.currentBackStackEntry
        val vm: AddContributionViewModel? = backStackEntry?.let {
            koinViewModel(viewModelStoreOwner = it)
        }
        val uiState by (
            vm?.uiState ?: MutableStateFlow(
                AddContributionUiState()
            )
            ).collectAsStateWithLifecycle()
        val titleResId = if (uiState.isEditMode) {
            R.string.contribution_edit_title
        } else {
            R.string.contribution_add_money_title
        }

        DynamicTopAppBar(
            title = stringResource(titleResId),
            subtitle = groupName,
            onBack = {
                vm?.onEvent(AddContributionUiEvent.PreviousStep)
                    ?: navController.popBackStack()
            },
            onBackLongPress = {
                vm?.onEvent(AddContributionUiEvent.CloseWizard)
            },
            actions = {
                IconButton(
                    onClick = { vm?.onEvent(AddContributionUiEvent.ToggleStepInfoSheet(visible = true)) }
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

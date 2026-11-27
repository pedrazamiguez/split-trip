package es.pedrazamiguez.splittrip.features.subunit.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.navigation.UpgradeNavigator
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillStyle
import es.pedrazamiguez.splittrip.features.subunit.presentation.screen.SubunitManagementScreen
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.SubunitManagementViewModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.action.SubunitManagementUiAction
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SubunitManagementFeature(
    groupId: String,
    viewModel: SubunitManagementViewModel = koinViewModel<SubunitManagementViewModel>(),
    upgradeNavigator: UpgradeNavigator = koinInject<UpgradeNavigator>()
) {
    val navController = LocalTabNavController.current
    val rootNavController = LocalRootNavController.current
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var proPreviewPrompt by remember { mutableStateOf<ProFeaturePreviewPrompt?>(null) }

    // Set the groupId for the ViewModel
    LaunchedEffect(groupId) {
        viewModel.setGroupId(groupId)
    }

    // Collect and handle UiActions
    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is SubunitManagementUiAction.ShowSuccess -> {
                    pillController.showPill(
                        message = action.message.asString(context),
                        style = TopPillStyle.SUCCESS
                    )
                }

                is SubunitManagementUiAction.ShowError -> {
                    pillController.showPill(
                        message = action.message.asString(context),
                        style = TopPillStyle.ERROR
                    )
                }

                is SubunitManagementUiAction.NavigateToCreateSubunit -> {
                    navController.navigate(Routes.createEditSubunitRoute(action.groupId))
                }

                is SubunitManagementUiAction.NavigateToEditSubunit -> {
                    navController.navigate(
                        Routes.createEditSubunitRoute(action.groupId, action.subunitId)
                    )
                }

                is SubunitManagementUiAction.ShowProFeaturePreview -> {
                    proPreviewPrompt = action.prompt
                }
            }
        }
    }

    proPreviewPrompt?.let { prompt ->
        ProFeaturePreviewBottomSheet(
            previewType = prompt.previewType,
            onUpgrade = {
                proPreviewPrompt = null
                upgradeNavigator.navigateToUpgrade(
                    rootNavController,
                    source = prompt.entrySource
                )
            },
            onDismiss = { proPreviewPrompt = null }
        )
    }

    SubunitManagementScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

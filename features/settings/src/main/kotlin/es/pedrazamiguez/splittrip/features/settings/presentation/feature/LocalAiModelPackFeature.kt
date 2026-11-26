package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.navigation.UpgradeNavigator
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.LocalAiModelPackScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.LocalAiModelPackViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.LocalAiModelPackUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.LocalAiModelPackUiEvent
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun LocalAiModelPackFeature(
    navController: NavHostController = LocalRootNavController.current,
    viewModel: LocalAiModelPackViewModel = koinViewModel<LocalAiModelPackViewModel>(),
    upgradeNavigator: UpgradeNavigator = koinInject<UpgradeNavigator>()
) {
    val context = LocalContext.current
    val pillController = LocalTopPillController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is LocalAiModelPackUiAction.ShowTopPill -> {
                    pillController.showPill(action.message.asString(context))
                }

                LocalAiModelPackUiAction.RequestUpgrade -> {
                    upgradeNavigator.navigateToUpgrade(
                        navController = navController,
                        source = MonetizationTelemetry.EntrySource.LOCAL_AI_PACK
                    )
                }
            }
        }
    }

    if (uiState.showDeleteConfirmation) {
        DestructiveConfirmationDialog(
            title = stringResource(R.string.local_ai_pack_delete_dialog_title),
            text = stringResource(R.string.local_ai_pack_delete_dialog_message),
            confirmLabel = stringResource(R.string.local_ai_pack_delete_dialog_confirm),
            onConfirm = { viewModel.onEvent(LocalAiModelPackUiEvent.ConfirmDelete) },
            onDismiss = { viewModel.onEvent(LocalAiModelPackUiEvent.DismissDeleteDialog) }
        )
    }

    FeatureScaffold(currentRoute = Routes.SETTINGS_LOCAL_AI_PACK) {
        LocalAiModelPackScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent
        )
    }
}

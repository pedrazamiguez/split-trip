package es.pedrazamiguez.splittrip.features.balance.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.UpgradeNavigator
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.navigation.KEY_EXPORT_REPORT_REQUESTED
import es.pedrazamiguez.splittrip.features.balance.presentation.screen.SpendingInsightsScreen
import es.pedrazamiguez.splittrip.features.balance.presentation.util.ReportShareLauncher
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.SpendingInsightsViewModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.action.SpendingInsightsUiAction
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.SpendingInsightsUiEvent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Suppress("LongMethod")
@Composable
fun SpendingInsightsFeature(
    backStackEntry: NavBackStackEntry? = null,
    initialFilterCriteria: ExpenseFilterCriteria = ExpenseFilterCriteria(),
    spendingInsightsViewModel: SpendingInsightsViewModel = koinViewModel<SpendingInsightsViewModel>(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    ),
    upgradeNavigator: UpgradeNavigator = koinInject<UpgradeNavigator>()
) {
    val tabNavController = LocalTabNavController.current
    val rootNavController = LocalRootNavController.current
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    val uiState by spendingInsightsViewModel.uiState.collectAsStateWithLifecycle()
    val selectedGroupId by sharedViewModel.selectedGroupId.collectAsStateWithLifecycle()
    var proPreviewPrompt by remember { mutableStateOf<ProFeaturePreviewPrompt?>(null) }
    val shareChooserTitle = stringResource(R.string.balances_export_share_chooser_title)
    val noShareAppMessage = stringResource(R.string.balances_export_error_no_share_app)

    // Apply filter criteria before selectedGroupId so initial emission uses active filters without flashing.
    LaunchedEffect(initialFilterCriteria) {
        spendingInsightsViewModel.setFilterCriteria(initialFilterCriteria)
    }

    LaunchedEffect(selectedGroupId) {
        spendingInsightsViewModel.setSelectedGroup(selectedGroupId)
    }

    val insightsBackStackEntry = backStackEntry ?: tabNavController.currentBackStackEntry
    LaunchedEffect(insightsBackStackEntry) {
        insightsBackStackEntry?.savedStateHandle
            ?.getStateFlow(KEY_EXPORT_REPORT_REQUESTED, false)
            ?.collectLatest { requested ->
                if (requested) {
                    spendingInsightsViewModel.onEvent(SpendingInsightsUiEvent.RequestExportReport)
                    insightsBackStackEntry.savedStateHandle.remove<Boolean>(KEY_EXPORT_REPORT_REQUESTED)
                }
            }
    }

    LaunchedEffect(Unit) {
        spendingInsightsViewModel.actions.collectLatest { action ->
            when (action) {
                SpendingInsightsUiAction.NavigateBack -> tabNavController.navigateUp()
                is SpendingInsightsUiAction.ShowLoadError -> {
                    pillController.showPill(message = action.message.asString(context))
                }
                is SpendingInsightsUiAction.ShowExportError -> {
                    pillController.showPill(message = action.message.asString(context))
                }
                is SpendingInsightsUiAction.ShowProFeaturePreview -> {
                    proPreviewPrompt = action.prompt
                }
                is SpendingInsightsUiAction.ShareExportedReport -> {
                    val launched = ReportShareLauncher.launchShareChooser(
                        context = context,
                        contentUri = action.contentUri,
                        mimeType = action.mimeType,
                        chooserTitle = shareChooserTitle
                    )
                    if (!launched) {
                        pillController.showPill(message = noShareAppMessage)
                    }
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

    // Prevent stale data flash during group transition
    val isTransitioning = selectedGroupId != null && selectedGroupId != uiState.groupId
    val effectiveUiState = remember(uiState, isTransitioning) {
        if (isTransitioning) {
            uiState.copy(
                isLoading = true,
                burnRate = null,
                categoryItems = persistentListOf(),
                entityItems = persistentListOf(),
                memberItems = persistentListOf()
            )
        } else {
            uiState
        }
    }

    SpendingInsightsScreen(
        uiState = effectiveUiState,
        onEvent = spendingInsightsViewModel::onEvent
    )
}

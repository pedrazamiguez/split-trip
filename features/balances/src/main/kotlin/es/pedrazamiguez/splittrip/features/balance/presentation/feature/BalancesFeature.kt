package es.pedrazamiguez.splittrip.features.balance.presentation.feature

import android.content.Context
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
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.navigation.UpgradeNavigator
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.navigation.KEY_EXPORT_REPORT_REQUESTED
import es.pedrazamiguez.splittrip.features.balance.presentation.screen.BalancesScreen
import es.pedrazamiguez.splittrip.features.balance.presentation.util.ReportShareLauncher
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.BalancesViewModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.action.BalancesUiAction
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.BalancesUiEvent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Suppress("LongMethod")
@Composable
fun BalancesFeature(
    backStackEntry: NavBackStackEntry? = null,
    balancesViewModel: BalancesViewModel = koinViewModel<BalancesViewModel>(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    ),
    upgradeNavigator: UpgradeNavigator = koinInject<UpgradeNavigator>()
) {
    val navController = LocalTabNavController.current
    val rootNavController = LocalRootNavController.current
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    val uiState by balancesViewModel.uiState.collectAsStateWithLifecycle()
    val selectedGroupId by sharedViewModel.selectedGroupId.collectAsStateWithLifecycle()
    var proPreviewPrompt by remember { mutableStateOf<ProFeaturePreviewPrompt?>(null) }
    val shareChooserTitle = stringResource(R.string.balances_export_share_chooser_title)
    val noShareAppMessage = stringResource(R.string.balances_export_error_no_share_app)

    LaunchedEffect(selectedGroupId) {
        balancesViewModel.setSelectedGroup(selectedGroupId)
    }

    val balancesBackStackEntry = backStackEntry ?: navController.currentBackStackEntry
    LaunchedEffect(balancesBackStackEntry) {
        balancesBackStackEntry?.savedStateHandle
            ?.getStateFlow(KEY_EXPORT_REPORT_REQUESTED, false)
            ?.collectLatest { requested ->
                if (requested) {
                    // Reset to false instead of remove(): SavedStateHandle.remove() drops the
                    // backing StateFlow from SavedStateHandleImpl.flows, silently breaking all
                    // subsequent taps on the Share action.
                    balancesBackStackEntry.savedStateHandle[KEY_EXPORT_REPORT_REQUESTED] = false
                    balancesViewModel.onEvent(BalancesUiEvent.RequestExportReport)
                }
            }
    }

    LaunchedEffect(Unit) {
        balancesViewModel.actions.collectLatest { action ->
            handleBalancesUiAction(
                action = action,
                context = context,
                pillController = pillController,
                shareChooserTitle = shareChooserTitle,
                noShareAppMessage = noShareAppMessage,
                onShowProPreview = { proPreviewPrompt = it }
            )
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
                contributions = persistentListOf(),
                cashWithdrawals = persistentListOf(),
                memberBalances = persistentListOf(),
                settlements = persistentListOf()
            )
        } else {
            uiState
        }
    }

    BalancesScreen(
        uiState = effectiveUiState,
        onEvent = balancesViewModel::onEvent,
        onNavigateToContribution = { navController.navigate(Routes.contributionWizardRoute(selectedGroupId ?: "")) },
        onNavigateToContributionDetail = { contributionId ->
            navController.navigate(Routes.contributionDetailRoute(selectedGroupId ?: "", contributionId))
        },
        onNavigateToEditContribution = { contributionId ->
            navController.navigate(Routes.contributionWizardRoute(selectedGroupId ?: "", contributionId))
        },
        onNavigateToWithdrawal = { navController.navigate(Routes.ADD_CASH_WITHDRAWAL) }
    )
}

@Suppress("LongParameterList")
private suspend fun handleBalancesUiAction(
    action: BalancesUiAction,
    context: Context,
    pillController: TopPillController,
    shareChooserTitle: String,
    noShareAppMessage: String,
    onShowProPreview: (ProFeaturePreviewPrompt) -> Unit
) {
    when (action) {
        is BalancesUiAction.ShowLoadError ->
            pillController.showPill(message = action.message.asString(context))
        is BalancesUiAction.ShowContributionSuccess ->
            pillController.showPill(message = action.message.asString(context))
        is BalancesUiAction.ShowContributionError ->
            pillController.showPill(message = action.message.asString(context))
        is BalancesUiAction.ShowDeleteContributionSuccess ->
            pillController.showPill(message = action.message.asString(context))
        is BalancesUiAction.ShowDeleteContributionError ->
            pillController.showPill(message = action.message.asString(context))
        is BalancesUiAction.ShowDeleteWithdrawalSuccess ->
            pillController.showPill(message = action.message.asString(context))
        is BalancesUiAction.ShowDeleteWithdrawalError ->
            pillController.showPill(message = action.message.asString(context))
        is BalancesUiAction.ShowExportError ->
            pillController.showPill(message = action.message.asString(context))
        is BalancesUiAction.ShowProFeaturePreview ->
            onShowProPreview(action.prompt)
        is BalancesUiAction.ShareExportedReport -> {
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

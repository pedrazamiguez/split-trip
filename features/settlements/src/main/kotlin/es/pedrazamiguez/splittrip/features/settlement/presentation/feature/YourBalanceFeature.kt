package es.pedrazamiguez.splittrip.features.settlement.presentation.feature

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Wallet
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.core.designsystem.transition.SharedTransitionSurface
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.YourBalanceViewModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.action.YourBalanceUiAction
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun YourBalanceFeature(
    yourBalanceViewModel: YourBalanceViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    ),
    modifier: Modifier = Modifier
) {
    val uiState by yourBalanceViewModel.uiState.collectAsStateWithLifecycle()
    val selectedGroupId by sharedViewModel.selectedGroupId.collectAsStateWithLifecycle()
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    LaunchedEffect(selectedGroupId) { yourBalanceViewModel.setSelectedGroup(selectedGroupId) }

    LaunchedEffect(Unit) {
        yourBalanceViewModel.actions.collectLatest { action ->
            val message = when (action) {
                is YourBalanceUiAction.ShowError -> action.message.asString(context)
                is YourBalanceUiAction.ShowSuccess -> action.message.asString(context)
            }
            pillController.showPill(message = message)
        }
    }

    SharedTransitionSurface(sharedElementKey = SharedElementKeys.YOUR_BALANCE, modifier = modifier) {
        DeferredLoadingContainer(
            isLoading = uiState.isLoading,
            loadingContent = { ShimmerLoadingList(modifier = Modifier.fillMaxSize()) }
        ) {
            when {
                uiState.personalPosition != null -> YourBalanceFeatureBody(
                    uiState = uiState,
                    onEvent = yourBalanceViewModel::onEvent
                )
                else -> EmptyStateView(
                    icon = TablerIcons.Outline.Wallet,
                    title = stringResource(R.string.your_balance_empty_title),
                    description = stringResource(R.string.your_balance_empty_description),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

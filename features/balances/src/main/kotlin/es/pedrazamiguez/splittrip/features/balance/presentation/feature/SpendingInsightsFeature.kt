package es.pedrazamiguez.splittrip.features.balance.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.balance.presentation.screen.SpendingInsightsScreen
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.SpendingInsightsViewModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.action.SpendingInsightsUiAction
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun SpendingInsightsFeature(
    initialFilterCriteria: ExpenseFilterCriteria = ExpenseFilterCriteria(),
    spendingInsightsViewModel: SpendingInsightsViewModel = koinViewModel<SpendingInsightsViewModel>(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    )
) {
    val tabNavController = LocalTabNavController.current
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    val uiState by spendingInsightsViewModel.uiState.collectAsStateWithLifecycle()
    val selectedGroupId by sharedViewModel.selectedGroupId.collectAsStateWithLifecycle()

    // Apply filter criteria before selectedGroupId so initial emission uses active filters without flashing.
    LaunchedEffect(initialFilterCriteria) {
        spendingInsightsViewModel.setFilterCriteria(initialFilterCriteria)
    }

    LaunchedEffect(selectedGroupId) {
        spendingInsightsViewModel.setSelectedGroup(selectedGroupId)
    }

    LaunchedEffect(Unit) {
        spendingInsightsViewModel.actions.collectLatest { action ->
            when (action) {
                SpendingInsightsUiAction.NavigateBack -> tabNavController.navigateUp()
                is SpendingInsightsUiAction.ShowLoadError -> {
                    pillController.showPill(message = action.message.asString(context))
                }
            }
        }
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

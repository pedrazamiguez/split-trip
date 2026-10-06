package es.pedrazamiguez.splittrip.features.withdrawal.presentation.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.transition.SharedTransitionSurface
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.component.WithdrawalConfigLoadFailedContent
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.component.WithdrawalWizard
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.event.AddCashWithdrawalUiEvent
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState

@Composable
fun AddCashWithdrawalScreen(
    groupId: String? = null,
    uiState: AddCashWithdrawalUiState,
    onEvent: (AddCashWithdrawalUiEvent) -> Unit = {}
) {
    val topPadding = LocalTopPadding.current

    LaunchedEffect(groupId) {
        onEvent(AddCashWithdrawalUiEvent.LoadGroupConfig(groupId))
    }

    SharedTransitionSurface(sharedElementKey = SharedElementKeys.ADD_CASH_WITHDRAWAL) {
        when {
            uiState.isReady -> {
                WithdrawalWizard(
                    groupId = groupId,
                    uiState = uiState,
                    onEvent = onEvent
                )
            }

            uiState.configLoadFailed -> {
                WithdrawalConfigLoadFailedContent(
                    modifier = Modifier.padding(
                        top = topPadding,
                        bottom = LocalBottomPadding.current
                    ),
                    onRetry = { onEvent(AddCashWithdrawalUiEvent.RetryLoadConfig(groupId)) }
                )
            }

            else -> {
                ShimmerLoadingList(modifier = Modifier.padding(top = topPadding))
            }
        }
    }
}

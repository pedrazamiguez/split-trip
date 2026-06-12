package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.settings.presentation.component.AccountStatusContent
import es.pedrazamiguez.splittrip.features.settings.presentation.component.LinkEmailDialog
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountStatusUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState

@Composable
fun AccountStatusScreen(
    uiState: AccountStatusUiState,
    onLinkGoogleClick: () -> Unit,
    onEvent: (AccountStatusUiEvent) -> Unit
) {
    DeferredLoadingContainer(
        isLoading = uiState.isLoading,
        loadingContent = { ShimmerLoadingList() }
    ) {
        AccountStatusContent(
            uiState = uiState,
            onLinkGoogleClick = onLinkGoogleClick,
            onEvent = onEvent
        )
    }

    if (uiState.showLinkEmailDialog) {
        LinkEmailDialog(
            uiState = uiState,
            onEvent = onEvent
        )
    }
}

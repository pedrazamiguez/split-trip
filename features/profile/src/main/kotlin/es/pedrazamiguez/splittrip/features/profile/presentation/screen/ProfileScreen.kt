package es.pedrazamiguez.splittrip.features.profile.presentation.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.profile.presentation.component.GuestProfileContent
import es.pedrazamiguez.splittrip.features.profile.presentation.component.ProfileContent
import es.pedrazamiguez.splittrip.features.profile.presentation.component.ProfileErrorState
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.ProfileUiEvent
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.ProfileUiState

@Composable
fun ProfileScreen(
    uiState: ProfileUiState = ProfileUiState(),
    onLinkAccountClick: () -> Unit = {},
    onEvent: (ProfileUiEvent) -> Unit = {}
) {
    DeferredLoadingContainer(
        isLoading = uiState.isLoading,
        loadingContent = { ShimmerLoadingList() }
    ) {
        when {
            uiState.isAnonymous -> {
                GuestProfileContent(
                    onLinkAccountClick = onLinkAccountClick
                )
            }
            uiState.hasError && uiState.profile == null -> {
                ProfileErrorState(
                    onRetry = { onEvent(ProfileUiEvent.LoadProfile) },
                    modifier = Modifier.padding(
                        top = LocalTopPadding.current,
                        bottom = LocalBottomPadding.current
                    )
                )
            }
            uiState.profile != null -> {
                ProfileContent(
                    profile = uiState.profile
                )
            }
        }
    }
}

package es.pedrazamiguez.splittrip.features.contribution.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Wallet
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.contribution.R
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.ContributionDetailUiState

@Composable
fun ContributionDetailScreen(
    uiState: ContributionDetailUiState,
    modifier: Modifier = Modifier
) {
    val bottomPadding = LocalBottomPadding.current

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        DeferredLoadingContainer(
            isLoading = uiState.isLoading,
            loadingContent = { ShimmerLoadingList() }
        ) {
            when {
                uiState.hasError || uiState.contribution == null -> {
                    EmptyStateView(
                        title = stringResource(R.string.contribution_detail_error_loading),
                        icon = TablerIcons.Outline.Wallet
                    )
                }
                else -> {
                    ContributionDetailContent(
                        contribution = uiState.contribution,
                        bottomPadding = bottomPadding
                    )
                }
            }
        }
    }
}

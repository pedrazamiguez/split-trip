package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.balance.presentation.screen.BalancesScreen
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.BalancesUiState

@PreviewComplete
@Composable
private fun BalancesScreenLoadingPreview() {
    PreviewThemeWrapper {
        BalancesScreen(
            uiState = BalancesUiState(isLoading = true)
        )
    }
}

@PreviewComplete
@Composable
private fun BalancesScreenEmptyPreview() {
    PreviewThemeWrapper {
        BalancesScreen(
            uiState = BalancesUiState(isLoading = false, groupId = "group-1")
        )
    }
}

@PreviewComplete
@Composable
private fun BalancesScreenWithDataPreview() {
    BalanceCardPreviewHelper { balanceUiModel ->
        ActivityListPreviewHelper { activityUiModels ->
            BalancesScreen(
                uiState = BalancesUiState(
                    isLoading = false,
                    groupId = "group-1",
                    pocketBalance = balanceUiModel,
                    activityItems = activityUiModels
                )
            )
        }
    }
}

@PreviewComplete
@Composable
private fun BalancesScreenArchivedPreview() {
    BalanceCardPreviewHelper { balanceUiModel ->
        ActivityListPreviewHelper { activityUiModels ->
            BalancesScreen(
                uiState = BalancesUiState(
                    isLoading = false,
                    groupId = "group-1",
                    isGroupArchived = true,
                    pocketBalance = balanceUiModel,
                    activityItems = activityUiModels
                )
            )
        }
    }
}

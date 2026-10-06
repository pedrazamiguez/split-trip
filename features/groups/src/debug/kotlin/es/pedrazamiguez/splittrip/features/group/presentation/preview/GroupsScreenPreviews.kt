package es.pedrazamiguez.splittrip.features.group.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.group.presentation.screen.GroupsScreen
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupsUiState

@PreviewComplete
@Composable
private fun GroupsScreenLoadingPreview() {
    PreviewThemeWrapper {
        GroupsScreen(
            uiState = GroupsUiState(isLoading = true)
        )
    }
}

@PreviewComplete
@Composable
private fun GroupsScreenEmptyPreview() {
    PreviewThemeWrapper {
        GroupsScreen(
            uiState = GroupsUiState(isLoading = false)
        )
    }
}

@PreviewComplete
@Composable
private fun GroupsScreenAnonymousEmptyPreview() {
    PreviewThemeWrapper {
        GroupsScreen(
            uiState = GroupsUiState(
                isLoading = false,
                isAnonymous = true
            )
        )
    }
}

/** Full list: hero card for the selected group + compact items for the rest. */
@PreviewComplete
@Composable
private fun GroupsScreenWithDataPreview() {
    GroupsScreenPreviewHelper(domainGroups = PREVIEW_GROUPS_WITH_MANY) {
        GroupsScreen(
            uiState = GroupsUiState(
                isLoading = false,
                groups = it
            ),
            selectedGroupId = GROUP_DOMAIN_1.id
        )
    }
}

/** No selection — all groups render as compact items. */
@PreviewComplete
@Composable
private fun GroupsScreenNoSelectionPreview() {
    GroupsScreenPreviewHelper(domainGroups = PREVIEW_GROUPS_WITH_MANY) {
        GroupsScreen(
            uiState = GroupsUiState(
                isLoading = false,
                groups = it
            ),
            selectedGroupId = null
        )
    }
}

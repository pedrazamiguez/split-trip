package es.pedrazamiguez.splittrip.features.group.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.features.group.presentation.component.detail.GroupDetailActionsSection
import es.pedrazamiguez.splittrip.features.group.presentation.component.detail.GroupDetailInfoSection
import es.pedrazamiguez.splittrip.features.group.presentation.component.detail.GroupDetailMembersSection
import es.pedrazamiguez.splittrip.features.group.presentation.component.detail.GroupDetailSubunitCard
import es.pedrazamiguez.splittrip.features.group.presentation.component.detail.GroupMemberItem
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupMemberUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.screen.GroupDetailScreen
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupDetailUiState

@PreviewComplete
@Composable
private fun GroupDetailScreenLoadingPreview() {
    PreviewThemeWrapper {
        GroupDetailScreen(
            uiState = GroupDetailUiState(isLoading = true)
        )
    }
}

@PreviewComplete
@Composable
private fun GroupDetailScreenErrorPreview() {
    PreviewThemeWrapper {
        GroupDetailScreen(
            uiState = GroupDetailUiState(isLoading = false, hasError = true)
        )
    }
}

@PreviewComplete
@Composable
private fun GroupDetailScreenActivePreview() {
    GroupUiPreviewHelper(domainGroup = GROUP_DOMAIN_1) { groupUiModel ->
        GroupDetailScreen(
            uiState = GroupDetailUiState(
                isLoading = false,
                group = groupUiModel,
                isUserAdmin = true,
                isOnlyGroup = false,
                subunitsCount = 3
            ),
            isActiveGroup = true
        )
    }
}

@PreviewComplete
@Composable
private fun GroupDetailScreenArchivedPreview() {
    GroupUiPreviewHelper(domainGroup = GROUP_DOMAIN_1) { groupUiModel ->
        GroupDetailScreen(
            uiState = GroupDetailUiState(
                isLoading = false,
                group = groupUiModel.copy(status = GroupStatus.ARCHIVED),
                isUserAdmin = true,
                isOnlyGroup = false,
                subunitsCount = 1
            ),
            isActiveGroup = false
        )
    }
}

@PreviewComplete
@Composable
private fun GroupDetailScreenDeleteDialogPreview() {
    GroupUiPreviewHelper(domainGroup = GROUP_DOMAIN_1) { groupUiModel ->
        GroupDetailScreen(
            uiState = GroupDetailUiState(
                isLoading = false,
                group = groupUiModel,
                showDeleteConfirmation = true,
                isUserAdmin = true,
                isOnlyGroup = false
            ),
            isActiveGroup = false
        )
    }
}

@PreviewComplete
@Composable
private fun GroupDetailInfoSectionPreview() {
    GroupUiPreviewHelper(domainGroup = GROUP_DOMAIN_1) { groupUiModel ->
        GroupDetailInfoSection(group = groupUiModel)
    }
}

@PreviewComplete
@Composable
private fun GroupDetailSubunitCardPreview() {
    PreviewThemeWrapper {
        GroupDetailSubunitCard(
            subunitsCount = 3,
            onManageSubunits = {}
        )
    }
}

@PreviewComplete
@Composable
private fun GroupDetailMembersSectionPreview() {
    GroupUiPreviewHelper(domainGroup = GROUP_DOMAIN_1) { groupUiModel ->
        GroupDetailMembersSection(members = groupUiModel.members)
    }
}

@PreviewComplete
@Composable
private fun GroupDetailActionsSectionPreview() {
    PreviewThemeWrapper {
        GroupDetailActionsSection(
            isActiveGroup = true,
            isOnlyGroup = false,
            isUserAdmin = true,
            isLeaving = false,
            status = GroupStatus.ACTIVE,
            onSelectGroup = {},
            onArchiveGroup = {},
            onLeaveGroup = {}
        )
    }
}

@PreviewComplete
@Composable
private fun GroupMemberItemPreview() {
    PreviewThemeWrapper {
        GroupMemberItem(
            member = GroupMemberUiModel(
                userId = "1",
                displayName = "Antonio García",
                avatarUrl = "https://i.pravatar.cc/150?u=antonio",
                isCreator = true,
                roleBadgeText = "Admin"
            )
        )
    }
}

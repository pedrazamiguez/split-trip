package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.InlineWarningBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupsUiState

@Composable
internal fun GroupsScreenContent(
    uiState: GroupsUiState,
    selectedGroupId: String?,
    listState: LazyListState,
    bottomPadding: Dp,
    onGroupClicked: (String, String, String) -> Unit,
    onGroupLongClicked: (GroupUiModel) -> Unit,
    onUpgradeClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        InlineWarningBanner(
            warning = if (uiState.isAnonymous) UiText.StringResource(R.string.groups_anonymous_warning) else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.Default,
                    vertical = MaterialTheme.spacing.Small
                )
                .clickable { onUpgradeClicked() }
        )
        Box(modifier = Modifier.weight(1f)) {
            DeferredLoadingContainer(
                isLoading = uiState.isLoading,
                loadingContent = { ShimmerLoadingList() }
            ) {
                if (uiState.groups.isEmpty()) {
                    EmptyStateView(
                        title = stringResource(R.string.groups_not_found),
                        icon = TablerIcons.Outline.UsersGroup
                    )
                } else {
                    GroupsListContent(
                        groups = uiState.groups,
                        selectedGroupId = selectedGroupId,
                        listState = listState,
                        bottomPadding = bottomPadding,
                        onGroupClicked = onGroupClicked,
                        onGroupLongClicked = onGroupLongClicked
                    )
                }
            }
        }
    }
}

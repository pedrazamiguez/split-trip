package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Plus
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.StickyActionBar
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.screen.CREATE_GROUP_SHARED_ELEMENT_KEY

@Composable
internal fun GroupsScreenContent(
    uiState: es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupsUiState,
    selectedGroupId: String?,
    listState: LazyListState,
    bottomPadding: Dp,
    onCreateGroupClick: () -> Unit,
    onGroupClicked: (String, String, String) -> Unit,
    onGroupLongClicked: (GroupUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
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
        StickyActionBar(
            text = stringResource(R.string.groups_create),
            icon = TablerIcons.Outline.Plus,
            onClick = onCreateGroupClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = MaterialTheme.spacing.ExtraLarge)
                .padding(bottom = bottomPadding + MaterialTheme.spacing.ExtraSmall),
            sharedTransitionKey = CREATE_GROUP_SHARED_ELEMENT_KEY
        )
    }
}

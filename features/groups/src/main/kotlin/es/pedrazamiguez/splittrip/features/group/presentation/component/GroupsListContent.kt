package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedElementAnimation
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalAnimatedVisibilityScope
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalSharedTransitionScope
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongMethod")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun GroupsListContent(
    groups: ImmutableList<GroupUiModel>,
    selectedGroupId: String?,
    listState: LazyListState,
    bottomPadding: Dp,
    onGroupClicked: (String, String, String) -> Unit,
    onGroupLongClicked: (GroupUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    val unselectedGroups = groups.filter { it.id != selectedGroupId }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.Default,
            top = MaterialTheme.spacing.Default,
            end = MaterialTheme.spacing.Default,
            bottom = MaterialTheme.spacing.Default + bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        if (selectedGroup != null) {
            item(key = "selected-${selectedGroup.id}") {
                SelectedGroupCard(
                    groupUiModel = selectedGroup,
                    modifier = Modifier
                        .animateItem(fadeInSpec = null, fadeOutSpec = null)
                        .sharedElementAnimation(
                            key = "group-${selectedGroup.id}",
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    onClick = onGroupClicked,
                    onLongClick = { onGroupLongClicked(selectedGroup) }
                )
            }
        }

        items(items = unselectedGroups, key = { it.id }) { group ->
            GroupItem(
                modifier = Modifier
                    .animateItem()
                    .sharedElementAnimation(
                        key = "group-${group.id}",
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                groupUiModel = group,
                onClick = onGroupClicked,
                onLongClick = { onGroupLongClicked(group) }
            )
        }
    }
}

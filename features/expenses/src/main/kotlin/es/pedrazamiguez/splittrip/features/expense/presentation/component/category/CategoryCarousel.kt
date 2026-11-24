package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun CategoryCarousel(
    items: ImmutableList<CategoryCarouselItem>,
    focusedItemId: String?,
    onItemClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectionRole: Role = Role.RadioButton
) {
    if (items.isEmpty()) return

    val listState = rememberLazyListState()

    LaunchedEffect(focusedItemId, items) {
        val targetIndex = items.indexOfFirst { it.id == focusedItemId }
        if (targetIndex >= 0) {
            val layoutInfo = listState.layoutInfo
            val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
            val isFullyVisible = visibleItem != null &&
                visibleItem.offset >= layoutInfo.viewportStartOffset &&
                visibleItem.offset + visibleItem.size <= layoutInfo.viewportEndOffset
            if (!isFullyVisible) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
        contentPadding = PaddingValues(MaterialTheme.spacing.None)
    ) {
        items(items = items, key = { it.id }) { item ->
            CategoryCarouselPill(
                item = item,
                onClick = { onItemClicked(item.id) },
                selectionRole = selectionRole
            )
        }
    }
}

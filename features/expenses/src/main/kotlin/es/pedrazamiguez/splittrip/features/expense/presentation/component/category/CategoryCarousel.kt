package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import kotlinx.collections.immutable.ImmutableList

private val EDGE_FADE_WIDTH = 24.dp
private const val FADE_ANIMATION_MS = 180

@Composable
internal fun CategoryCarousel(
    items: ImmutableList<CategoryCarouselItem>,
    focusedItemId: String?,
    onItemClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 0.dp,
    selectionRole: Role = Role.RadioButton
) {
    if (items.isEmpty()) return

    val listState = rememberLazyListState()
    var lastAutoScrolledId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(focusedItemId, items) {
        val targetIndex = items.indexOfFirst { it.id == focusedItemId }
        if (focusedItemId != null && focusedItemId != lastAutoScrolledId && targetIndex >= 0) {
            lastAutoScrolledId = focusedItemId
            if (!listState.layoutInfo.isItemFullyVisible(targetIndex)) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    val startFadeAlpha by animateFloatAsState(
        targetValue = if (listState.canScrollBackward) 1f else 0f,
        animationSpec = tween(FADE_ANIMATION_MS),
        label = "CategoryCarouselStartFade"
    )
    val endFadeAlpha by animateFloatAsState(
        targetValue = if (listState.canScrollForward) 1f else 0f,
        animationSpec = tween(FADE_ANIMATION_MS),
        label = "CategoryCarouselEndFade"
    )

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .horizontalEdgeBleed(edgePadding)
            .horizontalEdgeFade(startFadeAlpha = startFadeAlpha, endFadeAlpha = endFadeAlpha)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
        contentPadding = PaddingValues(horizontal = edgePadding)
    ) {
        items(items = items, key = { it.id }) { item ->
            CategoryCarouselPill(
                item = item,
                onClick = { onItemClicked(item.id) },
                modifier = Modifier.animateItem(),
                selectionRole = selectionRole
            )
        }
    }
}

private fun LazyListLayoutInfo.isItemFullyVisible(index: Int): Boolean {
    val visibleItem = visibleItemsInfo.firstOrNull { it.index == index } ?: return false
    return visibleItem.offset >= viewportStartOffset &&
        visibleItem.offset + visibleItem.size <= viewportEndOffset
}

private fun Modifier.horizontalEdgeBleed(edgePadding: Dp): Modifier =
    if (edgePadding <= 0.dp) {
        this
    } else {
        layout { measurable, constraints ->
            val bleedPx = edgePadding.roundToPx()
            val expandedConstraints = if (constraints.hasBoundedWidth) {
                constraints.copy(maxWidth = constraints.maxWidth + bleedPx * 2)
            } else {
                constraints
            }
            val placeable = measurable.measure(expandedConstraints)
            layout(constraints.maxWidth, placeable.height) {
                placeable.place(-bleedPx, 0)
            }
        }
    }

private fun Modifier.horizontalEdgeFade(
    startFadeAlpha: Float,
    endFadeAlpha: Float
): Modifier = graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fadePx = EDGE_FADE_WIDTH.toPx().coerceAtMost(size.width / 2f)
        if (fadePx <= 0f) return@drawWithContent
        if (startFadeAlpha > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black.copy(alpha = 1f - startFadeAlpha), Color.Black),
                    startX = 0f,
                    endX = fadePx
                ),
                topLeft = Offset.Zero,
                size = Size(width = fadePx, height = size.height),
                blendMode = BlendMode.DstIn
            )
        }
        if (endFadeAlpha > 0f) {
            val startX = size.width - fadePx
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Black.copy(alpha = 1f - endFadeAlpha)),
                    startX = startX,
                    endX = size.width
                ),
                topLeft = Offset(x = startX, y = 0f),
                size = Size(width = fadePx, height = size.height),
                blendMode = BlendMode.DstIn
            )
        }
    }

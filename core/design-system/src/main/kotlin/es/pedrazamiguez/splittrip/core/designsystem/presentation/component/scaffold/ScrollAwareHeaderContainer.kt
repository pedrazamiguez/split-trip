package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

private const val HEADER_SLOT_ID = "header"
private const val CONTENT_SLOT_ID = "content"

/**
 * State holder for [ScrollAwareHeaderContainer] that tracks the measured header height and
 * vertical translation offset in pixels.
 */
@Stable
class ScrollAwareHeaderState(initialOffsetPx: Float = 0f) {

    var heightPx: Int by mutableIntStateOf(0)
        internal set

    var offsetPx: Float by mutableFloatStateOf(initialOffsetPx)
        internal set

    val isCollapsed: Boolean
        get() = heightPx > 0 && offsetPx <= -heightPx.toFloat()

    val expandedFraction: Float
        get() = if (heightPx <= 0) {
            1f
        } else {
            (1f + offsetPx / heightPx.toFloat()).coerceIn(0f, 1f)
        }

    fun updateHeight(newHeightPx: Int) {
        val coercedHeight = newHeightPx.coerceAtLeast(0)
        heightPx = coercedHeight
        offsetPx = offsetPx.coerceIn(-coercedHeight.toFloat(), 0f)
    }

    fun onScrollDelta(deltaY: Float) {
        if (heightPx <= 0) return
        offsetPx = (offsetPx + deltaY).coerceIn(-heightPx.toFloat(), 0f)
    }

    fun resolveSnapTarget(isFirstItemBelowTopEdge: Boolean): Float {
        if (heightPx <= 0 || isFirstItemBelowTopEdge) return 0f
        return if (offsetPx > -heightPx / 2f) 0f else -heightPx.toFloat()
    }

    fun expand() {
        offsetPx = 0f
    }

    suspend fun animateTo(
        targetOffsetPx: Float,
        animationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow)
    ) {
        val clampedTarget = targetOffsetPx.coerceIn(-heightPx.toFloat(), 0f)
        if (offsetPx == clampedTarget) return
        animate(
            initialValue = offsetPx,
            targetValue = clampedTarget,
            animationSpec = animationSpec
        ) { value, _ ->
            offsetPx = value
        }
    }
}

@Composable
fun rememberScrollAwareHeaderState(): ScrollAwareHeaderState =
    remember { ScrollAwareHeaderState() }

/**
 * Hosts a scroll-aware overlay [header] above [content] without resizing the scrollable
 * container bounds mid-scroll or consuming nested scroll deltas.
 */
@Composable
fun ScrollAwareHeaderContainer(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    headerState: ScrollAwareHeaderState = rememberScrollAwareHeaderState(),
    isPinned: Boolean = false,
    header: @Composable () -> Unit,
    content: @Composable (headerHeight: Dp) -> Unit
) {
    ObserveScrollAwareHeaderSnap(
        listState = listState,
        headerState = headerState,
        isPinned = isPinned
    )
    val nestedScrollConnection = rememberHeaderNestedScrollConnection(
        headerState = headerState,
        isPinned = isPinned
    )

    SubcomposeLayout(
        modifier = modifier
            .clipToBounds()
            .nestedScroll(nestedScrollConnection)
    ) { constraints ->
        val headerConstraints = constraints.copy(minHeight = 0)
        val headerPlaceables = subcompose(HEADER_SLOT_ID) {
            HeaderSemanticsWrapper(isCollapsed = headerState.isCollapsed, header = header)
        }.map { it.measure(headerConstraints) }

        val measuredHeightPx = headerPlaceables.maxOfOrNull { it.height } ?: 0
        if (headerState.heightPx != measuredHeightPx) {
            headerState.updateHeight(measuredHeightPx)
        }

        val contentPlaceables = subcompose(CONTENT_SLOT_ID) {
            content(measuredHeightPx.toDp())
        }.map { it.measure(constraints) }

        val layoutSize = resolveLayoutSize(
            constraints = constraints,
            headerPlaceables = headerPlaceables,
            contentPlaceables = contentPlaceables,
            headerHeightPx = measuredHeightPx
        )

        layout(layoutSize.width, layoutSize.height) {
            contentPlaceables.forEach { it.placeRelative(0, 0) }
            val offsetY = headerState.offsetPx.roundToInt()
            headerPlaceables.forEach { it.placeRelative(0, offsetY) }
        }
    }
}

@Composable
private fun ObserveScrollAwareHeaderSnap(
    listState: LazyListState,
    headerState: ScrollAwareHeaderState,
    isPinned: Boolean
) {
    LaunchedEffect(isPinned, headerState) {
        if (isPinned) {
            headerState.animateTo(0f)
        }
    }

    LaunchedEffect(listState, headerState, isPinned) {
        if (isPinned) return@LaunchedEffect
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collectLatest { inProgress ->
                if (!inProgress) {
                    val target = headerState.resolveSnapTarget(
                        isFirstItemBelowTopEdge = isFirstItemBelowTopEdge(listState, headerState)
                    )
                    headerState.animateTo(target)
                }
            }
    }
}

@Composable
private fun rememberHeaderNestedScrollConnection(
    headerState: ScrollAwareHeaderState,
    isPinned: Boolean
): NestedScrollConnection = remember(headerState, isPinned) {
    object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (!isPinned && consumed.y != 0f) {
                headerState.onScrollDelta(consumed.y)
            }
            return Offset.Zero
        }
    }
}

@Composable
private fun HeaderSemanticsWrapper(
    isCollapsed: Boolean,
    header: @Composable () -> Unit
) {
    Box(modifier = if (isCollapsed) Modifier.clearAndSetSemantics {} else Modifier) {
        header()
    }
}

private fun isFirstItemBelowTopEdge(
    listState: LazyListState,
    headerState: ScrollAwareHeaderState
): Boolean {
    val firstItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
    return firstItem == null || (firstItem.index == 0 && firstItem.offset > -headerState.heightPx)
}

private fun resolveLayoutSize(
    constraints: Constraints,
    headerPlaceables: List<Placeable>,
    contentPlaceables: List<Placeable>,
    headerHeightPx: Int
): IntSize {
    val width = if (constraints.hasBoundedWidth) {
        constraints.maxWidth
    } else {
        maxOf(
            contentPlaceables.maxOfOrNull { it.width } ?: 0,
            headerPlaceables.maxOfOrNull { it.width } ?: 0
        )
    }
    val height = if (constraints.hasBoundedHeight) {
        constraints.maxHeight
    } else {
        maxOf(
            contentPlaceables.maxOfOrNull { it.height } ?: 0,
            headerHeightPx
        )
    }
    return IntSize(width = width, height = height)
}

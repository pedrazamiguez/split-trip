package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import kotlinx.collections.immutable.ImmutableList

private const val COMPACT_BAR_THRESHOLD = 10
private const val REGULAR_BAR_ALPHA = 0.40f
private const val ZERO_BAR_MIN_HEIGHT_DP = 4f
private val BAR_CORNER_RADIUS = 6.dp
private val SCROLLABLE_BAR_SLOT_WIDTH = 44.dp
private val COMPACT_BAR_WIDTH = 22.dp
private val SCROLLABLE_BAR_WIDTH = 24.dp

@Composable
fun VerticalBarChart(
    items: ImmutableList<VerticalBarChartItem>,
    selectedId: String?,
    onSelectBar: (String) -> Unit,
    modifier: Modifier = Modifier,
    barAreaHeight: Dp = 128.dp,
    animationKey: Any = Unit
) {
    if (items.isEmpty()) return

    var isAnimated by remember(animationKey) { mutableStateOf(false) }
    LaunchedEffect(animationKey) {
        isAnimated = true
    }

    if (items.size <= COMPACT_BAR_THRESHOLD) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEach { item ->
                VerticalBarColumn(
                    item = item,
                    isSelected = item.id == selectedId,
                    isAnimated = isAnimated,
                    barAreaHeight = barAreaHeight,
                    barWidth = COMPACT_BAR_WIDTH,
                    onClick = { onSelectBar(item.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        val listState = rememberLazyListState()
        LaunchedEffect(animationKey, items.size) {
            if (items.isNotEmpty()) {
                listState.scrollToItem(items.lastIndex)
            }
        }

        LazyRow(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
            verticalAlignment = Alignment.Bottom
        ) {
            items(items = items, key = { it.id }) { item ->
                VerticalBarColumn(
                    item = item,
                    isSelected = item.id == selectedId,
                    isAnimated = isAnimated,
                    barAreaHeight = barAreaHeight,
                    barWidth = SCROLLABLE_BAR_WIDTH,
                    onClick = { onSelectBar(item.id) },
                    modifier = Modifier.width(SCROLLABLE_BAR_SLOT_WIDTH)
                )
            }
        }
    }
}

@Composable
private fun VerticalBarColumn(
    item: VerticalBarChartItem,
    isSelected: Boolean,
    isAnimated: Boolean,
    barAreaHeight: Dp,
    barWidth: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetRatio = if (isAnimated) item.valueRatio.coerceIn(0f, 1f) else 0f
    val animatedRatio by animateFloatAsState(
        targetValue = targetRatio,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "vertical_bar_ratio"
    )
    val barColor by animateColorAsState(
        targetValue = resolveBarColor(item = item, isSelected = isSelected),
        label = "vertical_bar_color"
    )

    val minBarHeight = ZERO_BAR_MIN_HEIGHT_DP.dp
    val computedHeight = if (item.valueRatio <= 0f) {
        minBarHeight
    } else {
        (barAreaHeight * animatedRatio).coerceAtLeast(minBarHeight)
    }

    val labelColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .debouncedClickable(role = Role.Button, onClickLabel = item.contentDescription, onClick = onClick)
            .semantics { contentDescription = item.contentDescription }
            .padding(vertical = MaterialTheme.spacing.ExtraSmall),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(barAreaHeight),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(computedHeight)
                    .clip(RoundedCornerShape(topStart = BAR_CORNER_RADIUS, topEnd = BAR_CORNER_RADIUS))
                    .background(barColor)
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun resolveBarColor(
    item: VerticalBarChartItem,
    isSelected: Boolean
): Color = when {
    item.valueRatio <= 0f -> MaterialTheme.colorScheme.surfaceContainerHighest
    isSelected -> MaterialTheme.colorScheme.primary
    item.isHighlighted -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary.copy(alpha = REGULAR_BAR_ALPHA)
}

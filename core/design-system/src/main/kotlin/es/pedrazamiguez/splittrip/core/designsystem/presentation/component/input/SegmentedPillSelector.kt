package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Lock
import kotlinx.collections.immutable.ImmutableList

private val LOCK_ICON_SIZE = 14.dp
private val LOCK_ICON_SPACING = 4.dp

/**
 * Reusable pill-shaped segmented selector adhering to The Horizon Narrative design language.
 *
 * Uses tonal layering with zero `1px` borders, a spring-animated sliding pill indicator,
 * slot-width-aware compact typography, `selectableGroup()` accessibility semantics,
 * and [Modifier.debouncedClickable] interaction handling.
 */
@Suppress("LongParameterList")
@Composable
fun <T> SegmentedPillSelector(
    options: ImmutableList<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    optionKey: (T) -> Any? = { it },
    colors: SegmentedPillSelectorColors = SegmentedPillSelectorDefaults.colors(),
    enabled: Boolean = true,
    isOptionLocked: (T) -> Boolean = { false }
) {
    if (options.isEmpty()) return

    val selectedIndex = resolveSelectedOptionIndex(
        options = options,
        selectedOption = selectedOption,
        optionKey = optionKey
    )
    val trackColor = if (enabled) colors.trackColor else colors.disabledTrackColor
    val indicatorColor = if (enabled) colors.indicatorColor else colors.disabledIndicatorColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SegmentedPillSelectorDefaults.TrackHeight)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(SegmentedPillSelectorDefaults.TrackPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            val slotWidth = calculateSegmentSlotWidth(
                containerWidth = maxWidth,
                optionCount = options.size
            )
            val isCompact = isCompactSegmentSlot(slotWidth = slotWidth)

            if (selectedIndex in options.indices) {
                SegmentedPillSlidingIndicator(
                    selectedIndex = selectedIndex,
                    slotWidth = slotWidth,
                    indicatorColor = indicatorColor
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEachIndexed { index, option ->
                    SegmentedPillSegment(
                        label = optionLabel(option),
                        isSelected = index == selectedIndex,
                        enabled = enabled,
                        isLocked = isOptionLocked(option),
                        isCompact = isCompact,
                        colors = colors,
                        onClick = { onOptionSelected(option) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentedPillSlidingIndicator(
    selectedIndex: Int,
    slotWidth: Dp,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    val indicatorOffset by animateDpAsState(
        targetValue = calculateSegmentIndicatorOffset(selectedIndex, slotWidth),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "segmentedPillIndicatorOffset"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(x = indicatorOffset.roundToPx(), y = 0) }
            .width(slotWidth)
            .fillMaxHeight()
            .clip(CircleShape)
            .background(indicatorColor)
    )
}

@Suppress("LongParameterList")
@Composable
private fun SegmentedPillSegment(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    isLocked: Boolean,
    isCompact: Boolean,
    colors: SegmentedPillSelectorColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = resolveSegmentContentColor(enabled, isSelected, isLocked, colors),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "segmentedPillContentColor"
    )
    val textStyle = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val horizontalPadding = if (isCompact) {
        SegmentedPillSelectorDefaults.CompactSegmentHorizontalPadding
    } else {
        SegmentedPillSelectorDefaults.SegmentHorizontalPadding
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .semantics { selected = isSelected }
            .debouncedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LOCK_ICON_SPACING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLocked) {
                Icon(
                    imageVector = TablerIcons.Outline.Lock,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(LOCK_ICON_SIZE)
                )
            }
            Text(
                text = label,
                style = textStyle,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun resolveSegmentContentColor(
    enabled: Boolean,
    isSelected: Boolean,
    isLocked: Boolean,
    colors: SegmentedPillSelectorColors
): Color = when {
    !enabled && isSelected -> colors.disabledSelectedContentColor
    !enabled || isLocked -> colors.disabledUnselectedContentColor
    isSelected -> colors.selectedContentColor
    else -> colors.unselectedContentColor
}

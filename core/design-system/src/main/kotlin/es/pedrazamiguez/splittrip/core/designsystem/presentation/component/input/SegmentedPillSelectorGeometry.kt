package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal const val NO_SELECTION_INDEX = -1

internal fun <T> resolveSelectedOptionIndex(
    options: List<T>,
    selectedOption: T?,
    optionKey: (T) -> Any?
): Int {
    if (options.isEmpty() || selectedOption == null) return NO_SELECTION_INDEX
    val selectedKey = optionKey(selectedOption)
    return options.indexOfFirst { option -> optionKey(option) == selectedKey }
}

internal fun calculateSegmentSlotWidth(
    containerWidth: Dp,
    optionCount: Int
): Dp {
    if (optionCount <= 0 || containerWidth <= 0.dp) return 0.dp
    return containerWidth / optionCount
}

internal fun calculateSegmentIndicatorOffset(
    selectedIndex: Int,
    slotWidth: Dp
): Dp {
    if (selectedIndex <= 0 || slotWidth <= 0.dp) return 0.dp
    return slotWidth * selectedIndex
}

internal fun isCompactSegmentSlot(
    slotWidth: Dp,
    threshold: Dp = SegmentedPillSelectorDefaults.CompactSlotWidthThreshold
): Boolean = slotWidth < threshold

package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val DISABLED_TRACK_ALPHA = 0.5f
private const val DISABLED_INDICATOR_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f

/**
 * Defaults and factory methods for [SegmentedPillSelector].
 */
object SegmentedPillSelectorDefaults {
    val TrackHeight: Dp = 44.dp
    val TrackPadding: Dp = 4.dp
    val SegmentHorizontalPadding: Dp = 8.dp
    val CompactSegmentHorizontalPadding: Dp = 4.dp
    val CompactSlotWidthThreshold: Dp = 100.dp

    /**
     * Color tokens for a standalone [SegmentedPillSelector] placed directly on `surface`.
     */
    @Composable
    fun colors(
        trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        indicatorColor: Color = MaterialTheme.colorScheme.primaryContainer,
        selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrackColor: Color = trackColor.copy(alpha = DISABLED_TRACK_ALPHA),
        disabledIndicatorColor: Color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = DISABLED_INDICATOR_ALPHA
        ),
        disabledSelectedContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = DISABLED_CONTENT_ALPHA
        ),
        disabledUnselectedContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = DISABLED_CONTENT_ALPHA
        )
    ): SegmentedPillSelectorColors = SegmentedPillSelectorColors(
        trackColor = trackColor,
        indicatorColor = indicatorColor,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        disabledTrackColor = disabledTrackColor,
        disabledIndicatorColor = disabledIndicatorColor,
        disabledSelectedContentColor = disabledSelectedContentColor,
        disabledUnselectedContentColor = disabledUnselectedContentColor
    )

    /**
     * Color tokens for a [SegmentedPillSelector] nested inside an elevated [FlatCard]
     * (`surfaceContainerLowest` light / `surfaceContainerHighest` dark).
     */
    @Composable
    fun nestedColors(
        trackColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        indicatorColor: Color = MaterialTheme.colorScheme.primaryContainer,
        selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrackColor: Color = trackColor.copy(alpha = DISABLED_TRACK_ALPHA),
        disabledIndicatorColor: Color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = DISABLED_INDICATOR_ALPHA
        ),
        disabledSelectedContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = DISABLED_CONTENT_ALPHA
        ),
        disabledUnselectedContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = DISABLED_CONTENT_ALPHA
        )
    ): SegmentedPillSelectorColors = colors(
        trackColor = trackColor,
        indicatorColor = indicatorColor,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        disabledTrackColor = disabledTrackColor,
        disabledIndicatorColor = disabledIndicatorColor,
        disabledSelectedContentColor = disabledSelectedContentColor,
        disabledUnselectedContentColor = disabledUnselectedContentColor
    )
}

/**
 * Immutable color configuration for [SegmentedPillSelector].
 */
@Immutable
data class SegmentedPillSelectorColors(
    val trackColor: Color,
    val indicatorColor: Color,
    val selectedContentColor: Color,
    val unselectedContentColor: Color,
    val disabledTrackColor: Color,
    val disabledIndicatorColor: Color,
    val disabledSelectedContentColor: Color,
    val disabledUnselectedContentColor: Color
)

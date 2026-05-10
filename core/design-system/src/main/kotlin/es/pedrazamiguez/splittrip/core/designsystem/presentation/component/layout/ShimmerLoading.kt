package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val SHIMMER_GRADIENT_OFFSET = 200f
private const val TITLE_SHIMMER_WEIGHT = 0.6f
private const val BUTTON_SHIMMER_HEIGHT = 48f
private const val DASHBOARD_MEMBER_PLACEHOLDER_COUNT = 3
private const val DASHBOARD_NAME_WIDTH_FRACTION = 0.4f
private const val DASHBOARD_BALANCE_WIDTH_FRACTION = 0.55f

/**
 * Creates a shimmer brush effect for skeleton loading states.
 * Material 3 Expressive uses subtle, smooth shimmer effects.
 */
@Composable
fun shimmerBrush(
    targetValue: Float = 1000f,
    shimmerColors: List<Color> = listOf(
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
    )
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - SHIMMER_GRADIENT_OFFSET, translateAnimation - SHIMMER_GRADIENT_OFFSET),
        end = Offset(translateAnimation, translateAnimation)
    )
}

/**
 * A shimmer placeholder box for skeleton loading.
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, height: Dp = 16.dp, width: Dp? = null) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(MaterialTheme.shapes.small)
            .background(brush)
    )
}

/**
 * Shimmer skeleton card that mimics the ExpenseItem/GroupItem layout.
 */
@Composable
fun ShimmerItemCard(modifier: Modifier = Modifier) {
    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier.weight(TITLE_SHIMMER_WEIGHT),
                    height = 20.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                ShimmerBox(
                    width = 80.dp,
                    height = 24.dp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(
                    width = 100.dp,
                    height = 14.dp
                )
                ShimmerBox(
                    width = 60.dp,
                    height = 14.dp
                )
            }
        }
    }
}

/**
 * A list of shimmer skeleton items for loading states.
 */
@Composable
fun ShimmerLoadingList(modifier: Modifier = Modifier, itemCount: Int = 5) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            ShimmerItemCard()
        }
    }
}

/**
 * Shimmer skeleton that mirrors the `GroupPocketBalanceCard` structure:
 * a hero balance area, two currency rows, and two side-by-side button placeholders.
 */
@Composable
fun ShimmerDashboardCard(modifier: Modifier = Modifier) {
    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Group name placeholder
            ShimmerBox(modifier = Modifier.fillMaxWidth(DASHBOARD_NAME_WIDTH_FRACTION), height = 14.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Large balance placeholder
            ShimmerBox(modifier = Modifier.fillMaxWidth(DASHBOARD_BALANCE_WIDTH_FRACTION), height = 36.dp)

            Spacer(modifier = Modifier.height(24.dp))

            // Stats row: contributed (left) / spent (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShimmerBox(width = 80.dp, height = 12.dp)
                    ShimmerBox(width = 100.dp, height = 18.dp)
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShimmerBox(width = 70.dp, height = 12.dp)
                    ShimmerBox(width = 90.dp, height = 18.dp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Two currency rows (e.g. THB, USD)
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(width = 48.dp, height = 14.dp)
                    ShimmerBox(width = 88.dp, height = 18.dp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Two side-by-side button placeholders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(modifier = Modifier.weight(1f), height = BUTTON_SHIMMER_HEIGHT.dp)
                ShimmerBox(modifier = Modifier.weight(1f), height = BUTTON_SHIMMER_HEIGHT.dp)
            }
        }
    }
}

/**
 * A composite shimmer layout for dashboard-style screens: an optional header slot, a hero
 * [ShimmerDashboardCard], followed by [DASHBOARD_MEMBER_PLACEHOLDER_COUNT] [ShimmerItemCard]s
 * for list rows. Suitable wherever a screen leads with a summary card and a list below it.
 *
 * @param bottomPadding Extra bottom padding added to the list — callers should pass
 *   `LocalBottomPadding.current` so the skeleton clears the floating bottom nav bar,
 *   matching the real content's `LazyColumn` padding.
 * @param headerContent Optional composable slot rendered as the first list item. Use it to
 *   mirror a screen-specific title/subtitle area and avoid a vertical layout shift when the
 *   real content appears.
 */
@Composable
fun DashboardShimmer(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    headerContent: (@Composable () -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 16.dp + bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (headerContent != null) {
            item {
                headerContent()
            }
        }
        item {
            ShimmerDashboardCard()
        }
        items(DASHBOARD_MEMBER_PLACEHOLDER_COUNT) {
            ShimmerItemCard()
        }
    }
}

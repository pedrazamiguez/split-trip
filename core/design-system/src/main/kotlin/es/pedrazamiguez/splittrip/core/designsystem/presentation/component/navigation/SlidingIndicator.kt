package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Sliding indicator that animates between navigation items with a bouncy spring. */
@Composable
internal fun SlidingIndicator(
    selectedIndex: Int,
    itemCount: Int,
    itemWidth: Dp,
    containerWidth: Dp,
    modifier: Modifier = Modifier
) {
    val slotWidth = if (itemCount > 0) containerWidth / itemCount else 0.dp
    val indicatorWidth = minOf(itemWidth, slotWidth)
    val itemOffset = slotWidth * selectedIndex + (slotWidth - indicatorWidth) / 2

    val indicatorOffset by animateDpAsState(
        targetValue = itemOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "indicatorOffset"
    )

    Box(
        modifier = modifier
            .offset(x = indicatorOffset)
            .width(indicatorWidth)
            .height(NavBarDefaults.BarHeight - 16.dp)
            .clip(RoundedCornerShape(NavBarDefaults.INDICATOR_CORNER_RADIUS))
            .background(MaterialTheme.colorScheme.primaryContainer)
    )
}

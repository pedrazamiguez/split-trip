package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing

private const val FULL_PERCENT = 50
private const val HALF = 2f

/**
 * Animated pill-shaped single-value progress bar modeled on `AnimatedSpendingBar`.
 *
 * Animates width from `0f` to [progress] on first composition (or when [animationKey] changes)
 * using medium-bouncy spring physics, and smoothly transitions [fillColor] changes.
 */
@Composable
fun AnimatedProgressBar(
    progress: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    barHeight: Dp = MaterialTheme.spacing.Medium,
    animationKey: Any = Unit
) {
    var isAnimated by remember(animationKey) { mutableStateOf(false) }
    LaunchedEffect(animationKey) {
        isAnimated = true
    }

    val targetProgress = if (isAnimated) progress.coerceIn(0f, 1f) else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "progress_fraction"
    )

    val animatedFillColor by animateColorAsState(
        targetValue = fillColor,
        label = "progress_fill_color"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(percent = FULL_PERCENT))
    ) {
        drawRect(
            color = trackColor,
            size = Size(size.width, size.height)
        )

        val clampedProgress = animatedProgress.coerceIn(0f, 1f)
        if (clampedProgress > 0f) {
            val fillWidth = clampedProgress * size.width
            val radius = size.height / HALF
            drawRoundRect(
                color = animatedFillColor,
                topLeft = Offset.Zero,
                size = Size(fillWidth, size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private const val OVERLAY_ALPHA = 0.7f

@Composable
internal fun AvatarCropCanvas(
    cropSizePx: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        drawRect(color = Color.Black.copy(alpha = OVERLAY_ALPHA))
        drawCircle(
            color = Color.Transparent,
            radius = cropSizePx / 2f,
            center = center,
            blendMode = BlendMode.Clear
        )
        drawCircle(
            color = Color.White,
            radius = cropSizePx / 2f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
